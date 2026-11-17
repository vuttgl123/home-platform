package com.home_platform.registry.export;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.json.StorageJson;
import com.home_platform.registry.item.ItemStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Các câu đọc của việc xuất Excel. Danh sách dài đọc theo lô, mỗi lô bắt đầu sau dòng cuối của lô
 * trước (keyset), nên không dòng nào bị bỏ sót hay lặp lại dù dữ liệu lớn.
 */
@Repository
public class ExportRepository {

	private final JdbcClient jdbc;

	private final StorageJson json;

	ExportRepository(RepositorySupport support) {
		this.jdbc = support.jdbc();
		this.json = support.json();
	}

	/**
	 * Mọi địa điểm của không gian, kể cả đã xoá: một món đã gỡ có thể còn nằm ở địa điểm đã xoá, và
	 * file vẫn phải ghi được nó từng ở đâu. Chỉ ba cột nên đủ nhẹ cho hàng chục nghìn địa điểm.
	 */
	public List<PlaceNode> placeNodes(long workspaceId) {
		return this.jdbc.sql("SELECT id, parent_id, name FROM reg_place WHERE workspace_id = :workspaceId")
			.param("workspaceId", workspaceId)
			.query((rs, rowNum) -> new PlaceNode(rs.getLong("id"), nullableLong(rs, "parent_id"),
					rs.getString("name")))
			.list();
	}

	/**
	 * Một lô đồ đạc chưa xoá, sắp theo đường dẫn của địa điểm, rồi tên, rồi {@code id}: đồ của một
	 * địa điểm đứng cạnh nhau, địa điểm con đứng ngay sau cha.
	 * @param status {@code null} là mọi món chưa gỡ; có giá trị là đúng trạng thái đó (như
	 * {@code GET /items})
	 * @param categoryId {@code null} là mọi nhóm
	 * @param after dòng cuối của lô trước; {@code null} là lô đầu
	 */
	public List<ExportItemRow> items(long workspaceId, ItemStatus status, Long categoryId, ItemCursor after,
			int limit) {
		Map<String, Object> params = new HashMap<>();
		params.put("workspaceId", workspaceId);
		params.put("limit", limit);
		StringBuilder sql = new StringBuilder("SELECT i.id, i.place_id, p.path, i.category_id, i.code, i.name, "
				+ "i.quantity, i.unit, i.status, i.placed_at, i.removed_at, i.description, i.attributes, "
				+ "o.full_name AS owner_name FROM reg_item i "
				+ "JOIN reg_place p ON p.workspace_id = i.workspace_id AND p.id = i.place_id "
				+ "LEFT JOIN reg_person o ON o.workspace_id = i.workspace_id AND o.id = i.owner_person_id "
				+ "WHERE i.workspace_id = :workspaceId AND i.deleted_at IS NULL");
		if (status == null) {
			sql.append(" AND i.removed_at IS NULL");
		}
		else {
			sql.append(" AND i.status = :status");
			params.put("status", status.name());
		}
		if (categoryId != null) {
			sql.append(" AND i.category_id = :categoryId");
			params.put("categoryId", categoryId);
		}
		if (after != null) {
			sql.append(" AND (p.path > :afterPath OR (p.path = :afterPath AND (i.name > :afterName "
					+ "OR (i.name = :afterName AND i.id > :afterId))))");
			params.put("afterPath", after.placePath());
			params.put("afterName", after.name());
			params.put("afterId", after.id());
		}
		sql.append(" ORDER BY p.path, i.name, i.id LIMIT :limit");
		return this.jdbc.sql(sql.toString()).params(params).query((rs, rowNum) -> itemRow(rs)).list();
	}

	private ExportItemRow itemRow(ResultSet rs) throws SQLException {
		return new ExportItemRow(rs.getLong("id"), rs.getLong("place_id"), rs.getString("path"),
				nullableLong(rs, "category_id"), rs.getString("code"), rs.getString("name"), rs.getInt("quantity"),
				rs.getString("unit"), ItemStatus.valueOf(rs.getString("status")),
				rs.getObject("placed_at", LocalDate.class), rs.getObject("removed_at", LocalDate.class),
				rs.getString("owner_name"), rs.getString("description"), attributes(rs));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> attributes(ResultSet rs) throws SQLException {
		String value = rs.getString("attributes");
		return (value != null) ? (Map<String, Object>) this.json.read(value, Map.class) : null;
	}

	private static Long nullableLong(ResultSet rs, String column) throws SQLException {
		long value = rs.getLong(column);
		return rs.wasNull() ? null : value;
	}

}
