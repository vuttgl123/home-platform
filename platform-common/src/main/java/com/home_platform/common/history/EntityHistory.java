package com.home_platform.common.history;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.json.StorageJson;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorCodec;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.common.web.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

/**
 * Lối vào duy nhất của bảng version (ví dụ {@code reg_entity_version}): ghi một version, liệt
 * kê các version của một bản ghi, đọc một version kèm {@code snapshot}.
 *
 * <p>Nội dung {@code snapshot} do nghiệp vụ quyết định, nên service của nghiệp vụ gọi
 * {@link #record} ngay sau lần sửa, trong cùng transaction; repository không tự làm việc này.
 * {@code snapshot} được lưu theo quy ước của {@link StorageJson}: số vẫn là số.
 *
 * <p>Bảng version có khoá {@code (workspace_id, entity_type, entity_id, version_no)}, không
 * phải {@code (workspace_id, id)}, nên lớp này tự viết SQL thay vì kế thừa
 * {@code WorkspaceRepository}.
 */
public class EntityHistory {

	private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+");

	/** Độ dài của cột {@code note}. */
	private static final int NOTE_LENGTH = 255;

	private static final String KEY = "workspace_id = :workspaceId AND entity_type = :entityType AND entity_id = :entityId";

	private final JdbcClient jdbc;

	private final StorageJson json;

	private final RepositorySupport support;

	private final ApplicationEventPublisher events;

	private final String insert;

	private final String selectPrevious;

	private final String selectOne;

	private final String listFirst;

	private final String listAfter;

	private final String listRecent;

	/**
	 * @param table tên bảng version, lấy từ {@code platform.history.table}
	 * @throws IllegalArgumentException nếu {@code table} không phải một tên bảng hợp lệ
	 */
	public EntityHistory(RepositorySupport support, ApplicationEventPublisher events, String table) {
		if (table == null || !IDENTIFIER.matcher(table).matches()) {
			throw new IllegalArgumentException("platform.history.table is not a table name: " + table);
		}
		this.support = support;
		this.jdbc = support.jdbc();
		this.json = support.json();
		this.events = events;
		String summaryColumns = "version_no, action, changed_fields, changed_by, changed_at, note";
		this.insert = "INSERT INTO " + table + " (workspace_id, entity_type, entity_id, version_no, action, snapshot, "
				+ "changed_fields, changed_by, changed_at, note) VALUES (:workspaceId, :entityType, :entityId, "
				+ ":versionNo, :action, :snapshot, :changedFields, :changedBy, :changedAt, :note)";
		this.selectPrevious = "SELECT snapshot FROM " + table + " WHERE " + KEY
				+ " AND version_no < :versionNo ORDER BY version_no DESC LIMIT 1";
		this.selectOne = "SELECT " + summaryColumns + ", snapshot FROM " + table + " WHERE " + KEY
				+ " AND version_no = :versionNo";
		this.listFirst = "SELECT " + summaryColumns + " FROM " + table + " WHERE " + KEY
				+ " ORDER BY version_no DESC LIMIT :limit";
		this.listAfter = "SELECT " + summaryColumns + " FROM " + table + " WHERE " + KEY
				+ " AND version_no < :cursorVersion ORDER BY version_no DESC LIMIT :limit";
		this.listRecent = "SELECT entity_type, entity_id, " + summaryColumns + " FROM " + table
				+ " WHERE workspace_id = :workspaceId";
	}

	/**
	 * Ghi một version, rồi phát {@link EntityChangedEvent}. {@code changed_fields} được tính
	 * bằng cách so với version liền trước của cùng bản ghi.
	 * @param versionNo thường là {@code version} của record sau khi ghi
	 * @param snapshot đối tượng bất kỳ mà Jackson ghi được, ví dụ record của bảng kèm phần liên quan
	 * @throws IllegalTransactionStateException nếu không có transaction đang mở: version phải
	 * được ghi cùng lúc với lần sửa, không tách rời
	 */
	public void record(WorkspaceContext ctx, String entityType, long entityId, int versionNo, ChangeAction action,
			Object snapshot, String note) {
		if (!TransactionSynchronizationManager.isActualTransactionActive()) {
			throw new IllegalTransactionStateException(
					"EntityHistory.record must run inside the transaction of the change it records");
		}
		String snapshotJson = this.json.write(snapshot);
		List<String> changedFields = (action == ChangeAction.CREATE) ? null
				: previousSnapshot(ctx.workspaceId(), entityType, entityId, versionNo)
					.map(previous -> changedFields(previous, this.json.readTree(snapshotJson)))
					.orElse(null);
		Instant now = this.support.clock().instant().truncatedTo(ChronoUnit.MILLIS);
		this.jdbc.sql(this.insert)
			.params(key(ctx.workspaceId(), entityType, entityId))
			.param("versionNo", versionNo)
			.param("action", action.name())
			.param("snapshot", snapshotJson)
			.param("changedFields", (changedFields != null) ? this.json.write(changedFields) : null)
			.param("changedBy", ctx.userId())
			.param("changedAt", LocalDateTime.ofInstant(now, ZoneOffset.UTC))
			.param("note", note)
			.update();
		this.events.publishEvent(new EntityChangedEvent(ctx.workspaceId(), entityType, entityId, versionNo, action,
				ctx.userId(), now));
	}

	/**
	 * Các version của một bản ghi, mới nhất trước, không kèm {@code snapshot}.
	 * @throws BadRequestException nếu {@code cursor} hỏng
	 */
	public CursorPage<EntityVersionSummary> list(long workspaceId, String entityType, long entityId,
			CursorRequest page) {
		if (page.limit() < 1) {
			throw new IllegalArgumentException("limit must be positive: " + page.limit());
		}
		JdbcClient.StatementSpec statement = (page.cursor() != null)
				? this.jdbc.sql(this.listAfter).param("cursorVersion", decode(page.cursor()))
				: this.jdbc.sql(this.listFirst);
		List<EntityVersionSummary> rows = statement.params(key(workspaceId, entityType, entityId))
			.param("limit", page.limit() + 1)
			.query((rs, rowNum) -> new EntityVersionSummary(rs.getInt("version_no"),
					ChangeAction.valueOf(rs.getString("action")), readChangedFields(rs), rs.getLong("changed_by"),
					readInstant(rs, "changed_at"), rs.getString("note")))
			.list();
		return CursorPage.of(rows, page.limit(), last -> List.of(String.valueOf(last.versionNo())));
	}

	/**
	 * Mọi version của một không gian, mới nhất trước, không kèm {@code snapshot}: hoạt động gần đây.
	 *
	 * <p>Sắp theo {@code changed_at} rồi khoá chính, giảm dần, nên nhiều version cùng mili giây vẫn
	 * có thứ tự chắc chắn. Bảng version cần index {@code (workspace_id, changed_at)}: index của
	 * InnoDB mang sẵn các cột khoá chính ở cuối, nên cả bốn cột sắp xếp đều đi theo index.
	 * @param entityType {@code null} là mọi loại bản ghi
	 * @throws BadRequestException nếu {@code cursor} hỏng
	 */
	public CursorPage<EntityChange> listRecent(long workspaceId, String entityType, CursorRequest page) {
		if (page.limit() < 1) {
			throw new IllegalArgumentException("limit must be positive: " + page.limit());
		}
		StringBuilder sql = new StringBuilder(this.listRecent);
		Map<String, Object> params = new HashMap<>();
		params.put("workspaceId", workspaceId);
		if (entityType != null) {
			sql.append(" AND entity_type = :entityType");
			params.put("entityType", entityType);
		}
		if (page.cursor() != null) {
			params.putAll(decodeRecent(page.cursor()));
			sql.append(" AND (changed_at < :t OR (changed_at = :t AND (entity_type < :e OR (entity_type = :e AND "
					+ "(entity_id < :i OR (entity_id = :i AND version_no < :v))))))");
		}
		sql.append(" ORDER BY changed_at DESC, entity_type DESC, entity_id DESC, version_no DESC LIMIT :limit");
		params.put("limit", page.limit() + 1);
		List<EntityChange> rows = this.jdbc.sql(sql.toString())
			.params(params)
			.query((rs, rowNum) -> new EntityChange(rs.getString("entity_type"), rs.getLong("entity_id"),
					rs.getInt("version_no"), ChangeAction.valueOf(rs.getString("action")), readChangedFields(rs),
					rs.getLong("changed_by"), readInstant(rs, "changed_at"), rs.getString("note")))
			.list();
		return CursorPage.of(rows, page.limit(), last -> List.of(last.changedAt().toString(), last.entityType(),
				String.valueOf(last.entityId()), String.valueOf(last.versionNo())));
	}

	/**
	 * Một version kèm {@code snapshot}.
	 * @throws NotFoundException nếu không có version này
	 */
	public EntityVersion get(long workspaceId, String entityType, long entityId, int versionNo) {
		return this.jdbc.sql(this.selectOne)
			.params(key(workspaceId, entityType, entityId))
			.param("versionNo", versionNo)
			.query((rs, rowNum) -> new EntityVersion(rs.getInt("version_no"),
					ChangeAction.valueOf(rs.getString("action")), this.json.readTree(rs.getString("snapshot")),
					readChangedFields(rs), rs.getLong("changed_by"), readInstant(rs, "changed_at"),
					rs.getString("note")))
			.optional()
			.orElseThrow(() -> new NotFoundException(
					entityType + " " + entityId + " version " + versionNo + " not found"));
	}

	/**
	 * Ghi chú của version sinh ra khi khôi phục nội dung một version cũ: "Restored from version 3",
	 * thêm ": " và ghi chú của người dùng nếu có, cắt còn {@value #NOTE_LENGTH} ký tự của cột {@code note}.
	 */
	public static String restoredNote(int versionNo, String note) {
		String text = "Restored from version " + versionNo + ((note != null) ? ": " + note : "");
		return (text.length() > NOTE_LENGTH) ? text.substring(0, NOTE_LENGTH) : text;
	}

	/** Đổi {@code snapshot} của một version về kiểu Java, ví dụ để khôi phục về version đó. */
	public <S> S snapshotAs(EntityVersion version, Class<S> type) {
		return this.json.convert(version.snapshot(), type);
	}

	private Optional<JsonNode> previousSnapshot(long workspaceId, String entityType, long entityId, int versionNo) {
		return this.jdbc.sql(this.selectPrevious)
			.params(key(workspaceId, entityType, entityId))
			.param("versionNo", versionNo)
			.query(String.class)
			.optional()
			.map(this.json::readTree);
	}

	/**
	 * Tên các trường cấp một có giá trị khác nhau giữa hai snapshot, kể cả trường chỉ có ở một
	 * bên (trừ khi giá trị ở bên kia là {@code null}). Cả hai cây đều được đọc lại từ chuỗi JSON
	 * nên cách biểu diễn số giống nhau.
	 * Snapshot không phải object thì không có "trường" để so: trả {@code null}.
	 */
	private static List<String> changedFields(JsonNode previous, JsonNode current) {
		if (!previous.isObject() || !current.isObject()) {
			return null;
		}
		Set<String> changed = new LinkedHashSet<>();
		for (Map.Entry<String, JsonNode> field : current.properties()) {
			if (!valueOf(field.getValue()).equals(valueOf(previous.get(field.getKey())))) {
				changed.add(field.getKey());
			}
		}
		for (Map.Entry<String, JsonNode> field : previous.properties()) {
			if (!current.has(field.getKey()) && !valueOf(field.getValue()).isNull()) {
				changed.add(field.getKey());
			}
		}
		return new ArrayList<>(changed);
	}

	/**
	 * Giá trị để so: trường vắng mặt và trường {@code null} là như nhau. Nhờ vậy thêm một thành phần
	 * mới vào snapshot không làm lần sửa đầu tiên sau đó báo trường đó "đã đổi".
	 */
	private static JsonNode valueOf(JsonNode node) {
		return (node != null) ? node : NullNode.getInstance();
	}

	private List<String> readChangedFields(ResultSet rs) throws SQLException {
		String value = rs.getString("changed_fields");
		if (value == null) {
			return null;
		}
		List<String> fields = new ArrayList<>();
		this.json.readTree(value).forEach(node -> fields.add(node.asString()));
		return List.copyOf(fields);
	}

	private static Instant readInstant(ResultSet rs, String column) throws SQLException {
		return rs.getObject(column, LocalDateTime.class).toInstant(ZoneOffset.UTC);
	}

	private static int decode(String cursor) {
		List<String> parts = CursorCodec.decode(cursor);
		try {
			if (parts.size() != 1) {
				throw new IllegalArgumentException();
			}
			return Integer.parseInt(parts.get(0));
		}
		catch (IllegalArgumentException ex) {
			throw new BadRequestException("Invalid cursor");
		}
	}

	/** Cursor của {@link #listRecent}: {@code [changed_at, entity_type, entity_id, version_no]}. */
	private static Map<String, Object> decodeRecent(String cursor) {
		List<String> parts = CursorCodec.decode(cursor);
		try {
			if (parts.size() != 4) {
				throw new IllegalArgumentException();
			}
			return Map.of("t", LocalDateTime.ofInstant(Instant.parse(parts.get(0)), ZoneOffset.UTC), "e", parts.get(1),
					"i", Long.parseLong(parts.get(2)), "v", Integer.parseInt(parts.get(3)));
		}
		catch (RuntimeException ex) {
			throw new BadRequestException("Invalid cursor");
		}
	}

	private static Map<String, Object> key(long workspaceId, String entityType, long entityId) {
		return Map.of("workspaceId", workspaceId, "entityType", entityType, "entityId", entityId);
	}

}
