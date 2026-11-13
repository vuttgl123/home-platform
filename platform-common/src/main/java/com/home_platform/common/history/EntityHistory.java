package com.home_platform.common.history;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
		JdbcClient.StatementSpec statement = (page.cursor() != null)
				? this.jdbc.sql(this.listAfter).param("cursorVersion", decode(page.cursor()))
				: this.jdbc.sql(this.listFirst);
		List<EntityVersionSummary> rows = statement.params(key(workspaceId, entityType, entityId))
			.param("limit", page.limit() + 1)
			.query((rs, rowNum) -> new EntityVersionSummary(rs.getInt("version_no"),
					ChangeAction.valueOf(rs.getString("action")), readChangedFields(rs), rs.getLong("changed_by"),
					readInstant(rs, "changed_at"), rs.getString("note")))
			.list();
		if (rows.size() <= page.limit()) {
			return new CursorPage<>(rows, null);
		}
		List<EntityVersionSummary> items = List.copyOf(rows.subList(0, page.limit()));
		String next = CursorCodec.encode(List.of(String.valueOf(items.get(items.size() - 1).versionNo())));
		return new CursorPage<>(items, next);
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
	 * bên. Cả hai cây đều được đọc lại từ chuỗi JSON nên cách biểu diễn số giống nhau.
	 */
	private static List<String> changedFields(JsonNode previous, JsonNode current) {
		Set<String> changed = new LinkedHashSet<>();
		for (Map.Entry<String, JsonNode> field : current.properties()) {
			if (!field.getValue().equals(previous.get(field.getKey()))) {
				changed.add(field.getKey());
			}
		}
		for (Map.Entry<String, JsonNode> field : previous.properties()) {
			if (!current.has(field.getKey())) {
				changed.add(field.getKey());
			}
		}
		return new ArrayList<>(changed);
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

	private static Map<String, Object> key(long workspaceId, String entityType, long entityId) {
		return Map.of("workspaceId", workspaceId, "entityType", entityType, "entityId", entityId);
	}

}
