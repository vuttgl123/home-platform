package com.home_platform.common.jdbc;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.ConflictException;
import com.home_platform.common.web.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Lớp cha của repository cho một bảng có khoá chính {@code (workspace_id, id)}. Lớp con cấp
 * tên bảng và kiểu record, nhận sẵn các thao tác thêm, sửa, xoá, lấy theo {@code id}, rồi
 * viết thêm các truy vấn riêng của nghiệp vụ bằng {@link #jdbc()}.
 *
 * <p>Các cột chuẩn được tự lo nếu record có thành phần tương ứng: {@code createdAt},
 * {@code createdBy}, {@code updatedAt}, {@code updatedBy}, {@code version} (kiểu
 * {@code int}, chặn hai người sửa đè lên nhau) và {@code deletedAt} (xoá mềm). Record không
 * có thành phần nào thì phần đó được bỏ qua.
 *
 * <p>{@code workspace_id} trong mọi câu SQL lấy từ tham số của hàm, không bao giờ lấy từ
 * record truyền vào, nên một record đọc từ không gian khác không thể bị ghi nhầm chỗ.
 *
 * <p>Repository không tự mở transaction; service của nghiệp vụ đặt {@code @Transactional}
 * quanh các thao tác phải đi cùng nhau.
 *
 * @param <T> record ứng với một dòng của bảng
 */
public abstract class WorkspaceRepository<T extends Record> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final RepositorySupport support;

	private final String table;

	private final EntityMetadata<T> metadata;

	private final CrudSql sql;

	private final RowMapper<T> rowMapper;

	private final String entityName;

	protected WorkspaceRepository(RepositorySupport support, String table, Class<T> type) {
		requireKey(type);
		this.support = support;
		this.table = table;
		this.metadata = EntityMetadata.of(type, support.json());
		this.sql = new CrudSql(table, this.metadata);
		this.rowMapper = this.metadata.rowMapper();
		this.entityName = type.getSimpleName();
		this.log.debug("{} on {}: [{}] [{}] [{}]", this.entityName, table, this.sql.insert(), this.sql.selectById(),
				this.sql.update());
	}

	/**
	 * Ghi một dòng mới. {@code id} bằng 0 thì được sinh mới, khác 0 thì được giữ (dành cho
	 * nghiệp vụ cần biết {@code id} trước khi ghi).
	 * @return record như đã được lưu, với các cột chuẩn đã điền
	 */
	public T insert(WorkspaceContext ctx, T entity) {
		Instant now = now();
		Map<String, Object> standard = new HashMap<>();
		standard.put(CrudSql.WORKSPACE_ID, ctx.workspaceId());
		if (idOf(entity) == 0L) {
			standard.put(CrudSql.ID, this.support.idGenerator().nextId());
		}
		putIfPresent(standard, CrudSql.CREATED_AT, now);
		putIfPresent(standard, CrudSql.CREATED_BY, ctx.userId());
		putIfPresent(standard, CrudSql.UPDATED_AT, now);
		putIfPresent(standard, CrudSql.UPDATED_BY, ctx.userId());
		putIfPresent(standard, CrudSql.VERSION, 1);
		putIfPresent(standard, CrudSql.DELETED_AT, null);
		T stored = this.metadata.with(entity, standard);
		jdbc().sql(this.sql.insert()).params(this.metadata.parameters(stored)).update();
		return stored;
	}

	/** Dòng có {@code id} này trong không gian, nếu có và chưa bị xoá mềm. */
	public Optional<T> findById(long workspaceId, long id) {
		return select(this.sql.selectById(), workspaceId, id);
	}

	/**
	 * @throws NotFoundException nếu không có dòng nào, hoặc dòng đã bị xoá mềm
	 */
	public T getById(long workspaceId, long id) {
		return findById(workspaceId, id).orElseThrow(() -> NotFoundException.of(this.entityName, id));
	}

	/** Như {@link #findById} nhưng thấy cả dòng đã xoá mềm; dùng cho việc khôi phục. */
	public Optional<T> findByIdIncludingDeleted(long workspaceId, long id) {
		return select(this.sql.selectByIdIncludingDeleted(), workspaceId, id);
	}

	/**
	 * Ghi đè toàn bộ cột nghiệp vụ của một dòng. Record mang {@code version} mà người gọi đã
	 * đọc; nếu trong lúc đó có người khác sửa, lệnh bị từ chối.
	 * @return record như đã được lưu
	 * @throws NotFoundException nếu dòng không tồn tại hoặc đã bị xoá mềm
	 * @throws ConflictException nếu {@code version} đã khác
	 */
	public T update(WorkspaceContext ctx, T entity) {
		long id = idOf(entity);
		Map<String, Object> standard = new HashMap<>();
		standard.put(CrudSql.WORKSPACE_ID, ctx.workspaceId());
		putIfPresent(standard, CrudSql.UPDATED_AT, now());
		putIfPresent(standard, CrudSql.UPDATED_BY, ctx.userId());
		T candidate = this.metadata.with(entity, standard);
		int rows = jdbc().sql(this.sql.update()).params(this.metadata.parameters(candidate)).update();
		if (rows == 0) {
			throw missingOrStale(ctx.workspaceId(), id);
		}
		if (!this.sql.versioned()) {
			return candidate;
		}
		int version = (Integer) this.metadata.get(candidate, CrudSql.VERSION);
		return this.metadata.with(candidate, Map.of(CrudSql.VERSION, version + 1));
	}

	/**
	 * Đánh dấu một dòng là đã xoá. Chỉ dùng được với bảng có cột {@code deleted_at}.
	 * @param expectedVersion {@code version} người gọi đã đọc; bỏ qua nếu bảng không có cột này
	 * @return record sau khi ghi; {@code version} của nó dùng để ghi lịch sử
	 * @throws NotFoundException nếu dòng không tồn tại hoặc đã bị xoá mềm
	 * @throws ConflictException nếu {@code version} đã khác
	 */
	public T softDelete(WorkspaceContext ctx, long id, int expectedVersion) {
		Instant now = now();
		int rows = jdbc().sql(this.sql.softDelete())
			.params(keyAndAudit(ctx, id, expectedVersion, now))
			.param(CrudSql.DELETED_AT, jdbcValue(CrudSql.DELETED_AT, now))
			.update();
		if (rows == 0) {
			throw missingOrStale(ctx.workspaceId(), id);
		}
		return findByIdIncludingDeleted(ctx.workspaceId(), id).orElseThrow();
	}

	/**
	 * Bỏ đánh dấu đã xoá của một dòng.
	 * @return record sau khi ghi
	 * @throws NotFoundException nếu dòng không tồn tại
	 * @throws ConflictException nếu dòng chưa bị xoá, hoặc {@code version} đã khác
	 */
	public T restore(WorkspaceContext ctx, long id, int expectedVersion) {
		int rows = jdbc().sql(this.sql.restore()).params(keyAndAudit(ctx, id, expectedVersion, now())).update();
		if (rows == 0) {
			T current = findByIdIncludingDeleted(ctx.workspaceId(), id)
				.orElseThrow(() -> NotFoundException.of(this.entityName, id));
			if (this.metadata.get(current, CrudSql.DELETED_AT) == null) {
				throw new ConflictException(this.entityName + " " + id + " is not deleted");
			}
			throw stale(id);
		}
		return findById(ctx.workspaceId(), id).orElseThrow();
	}

	/**
	 * Xoá thật một dòng. Dành cho bảng không có cột {@code deleted_at}.
	 * @return {@code true} nếu có dòng bị xoá
	 */
	public boolean deletePermanently(long workspaceId, long id) {
		return jdbc().sql(this.sql.deletePermanently())
			.param(CrudSql.WORKSPACE_ID, workspaceId)
			.param(CrudSql.ID, id)
			.update() > 0;
	}

	/**
	 * Bắt đầu một truy vấn danh sách phân trang trên bảng này. Mỗi lần gọi trả về một bộ dựng
	 * mới; điều kiện {@code workspace_id} và {@code deleted_at IS NULL} đã có sẵn.
	 */
	protected KeysetQuery<T> keyset() {
		return new KeysetQuery<>(jdbc(), this.table, this.metadata, this.rowMapper);
	}

	/** Nơi chạy các câu SQL viết tay của lớp con. */
	protected JdbcClient jdbc() {
		return this.support.jdbc();
	}

	/** Đổi một dòng thành record; đọc theo tên cột. */
	protected RowMapper<T> rowMapper() {
		return this.rowMapper;
	}

	/** Mọi cột của bảng, cách nhau bằng dấu phẩy, để đặt sau {@code SELECT}. */
	protected String columns() {
		return this.metadata.selectList();
	}

	protected String table() {
		return this.table;
	}

	protected EntityMetadata<T> metadata() {
		return this.metadata;
	}

	private Optional<T> select(String statement, long workspaceId, long id) {
		return jdbc().sql(statement)
			.param(CrudSql.WORKSPACE_ID, workspaceId)
			.param(CrudSql.ID, id)
			.query(this.rowMapper)
			.optional();
	}

	private Map<String, Object> keyAndAudit(WorkspaceContext ctx, long id, int expectedVersion, Instant now) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put(CrudSql.WORKSPACE_ID, ctx.workspaceId());
		parameters.put(CrudSql.ID, id);
		parameters.put(CrudSql.VERSION, expectedVersion);
		parameters.put(CrudSql.UPDATED_BY, ctx.userId());
		if (this.metadata.has(CrudSql.UPDATED_AT)) {
			parameters.put(CrudSql.UPDATED_AT, jdbcValue(CrudSql.UPDATED_AT, now));
		}
		return parameters;
	}

	private Object jdbcValue(String component, Object value) {
		return this.metadata.typeOfColumn(this.metadata.column(component)).toJdbc(value);
	}

	/**
	 * Lệnh ghi không trúng dòng nào: hoặc dòng không tồn tại (với người gọi, dòng đã xoá mềm
	 * cũng vậy), hoặc nó tồn tại nhưng đã bị người khác sửa.
	 */
	private RuntimeException missingOrStale(long workspaceId, long id) {
		Optional<T> current = findById(workspaceId, id);
		return current.isEmpty() ? NotFoundException.of(this.entityName, id) : stale(id);
	}

	private ConflictException stale(long id) {
		return new ConflictException(this.entityName + " " + id + " was changed by someone else");
	}

	private long idOf(T entity) {
		return (Long) this.metadata.get(entity, CrudSql.ID);
	}

	private void putIfPresent(Map<String, Object> values, String component, Object value) {
		if (this.metadata.has(component)) {
			values.put(component, value);
		}
	}

	/** {@code DATETIME(3)} giữ tới mili giây; cắt trước để record trả về khớp với dòng đã lưu. */
	private Instant now() {
		return this.support.clock().instant().truncatedTo(ChronoUnit.MILLIS);
	}

	private static void requireKey(Class<? extends Record> type) {
		boolean workspaceId = false;
		boolean id = false;
		for (RecordComponent component : type.getRecordComponents()) {
			if (component.getType() == long.class) {
				workspaceId |= component.getName().equals(CrudSql.WORKSPACE_ID);
				id |= component.getName().equals(CrudSql.ID);
			}
		}
		if (!workspaceId || !id) {
			throw new IllegalArgumentException(type.getSimpleName() + " must declare long workspaceId and long id");
		}
	}

}
