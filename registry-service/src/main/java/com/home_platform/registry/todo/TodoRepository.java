package com.home_platform.registry.todo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.registry.fielddef.FieldTarget;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Các nguồn của danh sách việc (spec Đợt 8, L5): mỗi nguồn một truy vấn chỉ đọc, chỉ lấy bản ghi chưa xoá
 * (đồ thì chưa gỡ, ở địa điểm chưa xoá). Khung thời gian của từng loại do {@link TodoService} quyết.
 */
@Repository
class TodoRepository {

	/** Món chưa xoá, chưa gỡ, ở địa điểm chưa xoá; {@code i} là món, {@code p} là địa điểm của nó. */
	private static final String LIVE_ITEMS = "FROM reg_item i "
			+ "JOIN reg_place p ON p.workspace_id = i.workspace_id AND p.id = i.place_id "
			+ "WHERE i.workspace_id = :workspaceId AND i.deleted_at IS NULL AND i.removed_at IS NULL "
			+ "AND p.deleted_at IS NULL";

	private static final String ITEM_COLUMNS = "SELECT i.id, i.code, i.name, p.name AS place_name, i.updated_at, "
			+ "i.created_at, i.placed_at, i.warranty_until, i.service_every_months, "
			+ "(SELECT MAX(m.done_on) FROM reg_maintenance m WHERE m.workspace_id = i.workspace_id "
			+ "AND m.item_id = i.id AND m.kind = 'SERVICE' AND m.deleted_at IS NULL) AS last_service_on, "
			+ "(SELECT MAX(m.updated_at) FROM reg_maintenance m WHERE m.workspace_id = i.workspace_id "
			+ "AND m.item_id = i.id AND m.kind = 'SERVICE') AS service_log_changed_at ";

	private final JdbcClient jdbc;

	TodoRepository(RepositorySupport support) {
		this.jdbc = support.jdbc();
	}

	/** Món đang hỏng. Index (workspace_id, status, name). */
	List<ItemRow> brokenItems(long workspaceId) {
		return items(workspaceId, " AND i.status = 'BROKEN'", Map.of());
	}

	/** Món có ngày hết bảo hành trong {@code [from, to]}. Index (workspace_id, warranty_until). */
	List<ItemRow> warrantiesEnding(long workspaceId, LocalDate from, LocalDate to) {
		return items(workspaceId, " AND i.warranty_until BETWEEN :from AND :to", Map.of("from", from, "to", to));
	}

	/** Món có đặt chu kỳ bảo trì; hạn kế tiếp tính ở Java. */
	List<ItemRow> scheduledItems(long workspaceId) {
		return items(workspaceId, " AND i.service_every_months IS NOT NULL", Map.of());
	}

	/** Cư trú đang có hiệu lực hôm nay, kết thúc trong {@code [today, last]}: đúng L5 của Đợt 6. */
	List<MoveOutRow> movingOut(long workspaceId, LocalDate today, LocalDate last) {
		return this.jdbc.sql("SELECT o.id, o.person_id, pe.full_name, p.name AS place_name, o.end_date, o.updated_at "
				+ "FROM reg_occupancy o "
				+ "JOIN reg_person pe ON pe.workspace_id = o.workspace_id AND pe.id = o.person_id "
				+ "JOIN reg_place p ON p.workspace_id = o.workspace_id AND p.id = o.place_id "
				+ "WHERE o.workspace_id = :workspaceId AND o.deleted_at IS NULL AND pe.deleted_at IS NULL "
				+ "AND p.deleted_at IS NULL AND o.start_date <= :today AND o.end_date BETWEEN :today AND :last")
			.param("workspaceId", workspaceId)
			.param("today", today)
			.param("last", last)
			.query((rs, rowNum) -> new MoveOutRow(rs.getLong("id"), rs.getLong("person_id"), rs.getString("full_name"),
					rs.getString("place_name"), rs.getObject("end_date", LocalDate.class), instant(rs, "updated_at")))
			.list();
	}

	/**
	 * Bản ghi chưa xoá có giá trị ở mục {@code fieldKey}; chữ của giá trị trả nguyên, Java tự đọc ngày nên
	 * một giá trị gõ sai không làm hỏng cả truy vấn.
	 * @param typeId 0 là mọi bản ghi của đích; khác 0 là chỉ loại địa điểm (PLACE) hay nhóm đồ (ITEM) đó
	 */
	List<DateValueRow> dateValues(long workspaceId, FieldTarget target, long typeId, String fieldKey) {
		String value = "JSON_UNQUOTE(JSON_EXTRACT(attributes, :path))";
		String sql = switch (target) {
			case PERSON -> "SELECT id, NULL AS code, full_name AS name, " + value + " AS value FROM reg_person "
					+ "WHERE workspace_id = :workspaceId AND deleted_at IS NULL";
			case PLACE -> "SELECT id, code, name, " + value + " AS value FROM reg_place "
					+ "WHERE workspace_id = :workspaceId AND deleted_at IS NULL"
					+ ((typeId != 0) ? " AND place_type_id = :typeId" : "");
			case ITEM -> "SELECT i.id, i.code, i.name, JSON_UNQUOTE(JSON_EXTRACT(i.attributes, :path)) AS value "
					+ LIVE_ITEMS + ((typeId != 0) ? " AND i.category_id = :typeId" : "");
		};
		return this.jdbc.sql("SELECT * FROM (" + sql + ") t WHERE t.value IS NOT NULL")
			.param("workspaceId", workspaceId)
			.param("path", "$." + fieldKey)
			.param("typeId", typeId)
			.query((rs, rowNum) -> new DateValueRow(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
					rs.getString("value")))
			.list();
	}

	private List<ItemRow> items(long workspaceId, String condition, Map<String, ?> params) {
		return this.jdbc.sql(ITEM_COLUMNS + LIVE_ITEMS + condition)
			.param("workspaceId", workspaceId)
			.params(params)
			.query((rs, rowNum) -> new ItemRow(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
					rs.getString("place_name"), instant(rs, "updated_at"), instant(rs, "created_at"),
					rs.getObject("placed_at", LocalDate.class), rs.getObject("warranty_until", LocalDate.class),
					rs.getObject("service_every_months", Integer.class),
					rs.getObject("last_service_on", LocalDate.class), instant(rs, "service_log_changed_at")))
			.list();
	}

	private static Instant instant(ResultSet rs, String column) throws SQLException {
		LocalDateTime value = rs.getObject(column, LocalDateTime.class);
		return (value != null) ? value.toInstant(ZoneOffset.UTC) : null;
	}

	/**
	 * Một món kèm đủ cột cho ba loại việc của đồ.
	 * @param lastServiceOn ngày của dòng {@code SERVICE} chưa xoá muộn nhất
	 * @param serviceLogChangedAt lần ghi muộn nhất lên một dòng {@code SERVICE} của món, kể cả dòng đã xoá
	 */
	record ItemRow(long id, String code, String name, String placeName, Instant updatedAt, Instant createdAt,
			LocalDate placedAt, LocalDate warrantyUntil, Integer serviceEveryMonths, LocalDate lastServiceOn,
			Instant serviceLogChangedAt) {
	}

	record MoveOutRow(long occupancyId, long personId, String personName, String placeName, LocalDate endDate,
			Instant updatedAt) {
	}

	/** @param value chữ của giá trị trong {@code attributes}, chưa đọc thành ngày */
	record DateValueRow(long id, String code, String name, String value) {
	}

}
