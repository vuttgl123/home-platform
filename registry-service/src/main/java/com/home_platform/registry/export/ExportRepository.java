package com.home_platform.registry.export;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.json.StorageJson;
import com.home_platform.registry.item.ItemStatus;
import com.home_platform.registry.occupancy.Relation;
import com.home_platform.registry.person.Gender;
import com.home_platform.registry.place.PlaceStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Các câu đọc của việc xuất Excel. Danh sách dài đọc theo lô, mỗi lô bắt đầu sau dòng cuối của lô
 * trước (keyset), nên không dòng nào bị bỏ sót hay lặp lại dù dữ liệu lớn. Cột cuối của mọi thứ tự
 * sắp là {@code id}, để hai dòng giống nhau ở các cột trước vẫn có thứ tự xác định.
 */
@Repository
public class ExportRepository {

	/**
	 * Phần chung của câu đọc cư trú và sở hữu ({@code s}): cả hai gắn một người với một địa điểm trong
	 * một khoảng ngày. Người và địa điểm đã xoá vẫn được nối vào: dòng đã kết thúc không mất theo họ.
	 */
	private static final String STAY_JOINS = " s JOIN reg_place p ON p.workspace_id = s.workspace_id "
			+ "AND p.id = s.place_id JOIN reg_person pe ON pe.workspace_id = s.workspace_id AND pe.id = s.person_id "
			+ "WHERE s.workspace_id = :workspaceId AND s.deleted_at IS NULL";

	private static final String STAY_AFTER = " AND (p.path > :afterPath OR (p.path = :afterPath "
			+ "AND (s.start_date > :afterStart OR (s.start_date = :afterStart AND s.id > :afterId))))";

	private static final String STAY_ORDER = " ORDER BY p.path, s.start_date, s.id LIMIT :limit";

	private final JdbcClient jdbc;

	private final StorageJson json;

	ExportRepository(RepositorySupport support) {
		this.jdbc = support.jdbc();
		this.json = support.json();
	}

	/**
	 * Mọi địa điểm của không gian, kể cả đã xoá: một món đã gỡ hay một dòng cư trú đã kết thúc có thể
	 * còn nằm ở địa điểm đã xoá, và file vẫn phải ghi được nó ở đâu. Chỉ ba cột nên đủ nhẹ cho hàng
	 * chục nghìn địa điểm.
	 */
	public List<PlaceNode> placeNodes(long workspaceId) {
		return this.jdbc.sql("SELECT id, parent_id, name FROM reg_place WHERE workspace_id = :workspaceId")
			.param("workspaceId", workspaceId)
			.query((rs, rowNum) -> new PlaceNode(rs.getLong("id"), nullableLong(rs, "parent_id"),
					rs.getString("name")))
			.list();
	}

	/**
	 * Một lô địa điểm chưa xoá theo thứ tự của cây: con đứng ngay sau cha. Index (workspace_id, path).
	 * @param afterPath {@code path} của dòng cuối lô trước; {@code null} là lô đầu
	 */
	public List<ExportPlaceRow> places(long workspaceId, String afterPath, int limit) {
		Map<String, Object> params = params(workspaceId, limit);
		StringBuilder sql = new StringBuilder("SELECT id, path, place_type_id, code, name, status, capacity, description, "
				+ "attributes FROM reg_place WHERE workspace_id = :workspaceId AND deleted_at IS NULL");
		if (afterPath != null) {
			sql.append(" AND path > :afterPath");
			params.put("afterPath", afterPath);
		}
		sql.append(" ORDER BY path LIMIT :limit");
		return this.jdbc.sql(sql.toString())
			.params(params)
			.query((rs, rowNum) -> new ExportPlaceRow(rs.getLong("id"), rs.getString("path"),
					rs.getLong("place_type_id"), rs.getString("code"), rs.getString("name"),
					PlaceStatus.valueOf(rs.getString("status")), rs.getObject("capacity", Integer.class),
					rs.getString("description"), attributes(rs)))
			.list();
	}

	/**
	 * Một lô người chưa xoá, theo họ tên rồi {@code id}. Index (workspace_id, full_name).
	 * @param after dòng cuối của lô trước; {@code null} là lô đầu
	 */
	public List<ExportPersonRow> people(long workspaceId, PersonCursor after, int limit) {
		Map<String, Object> params = params(workspaceId, limit);
		StringBuilder sql = new StringBuilder("SELECT id, full_name, phone, email, id_type, id_number, "
				+ "date_of_birth, gender, note, attributes FROM reg_person "
				+ "WHERE workspace_id = :workspaceId AND deleted_at IS NULL");
		if (after != null) {
			sql.append(" AND (full_name > :afterName OR (full_name = :afterName AND id > :afterId))");
			params.put("afterName", after.fullName());
			params.put("afterId", after.id());
		}
		sql.append(" ORDER BY full_name, id LIMIT :limit");
		return this.jdbc.sql(sql.toString()).params(params).query((rs, rowNum) -> personRow(rs)).list();
	}

	/**
	 * Nơi mỗi người trong {@code personIds} đang ở hôm nay: dòng cư trú chưa xoá, còn hiệu lực, ở địa
	 * điểm chưa xoá. Một truy vấn cho cả lô; người không ở đâu thì không có khoá.
	 */
	public Map<Long, List<Long>> currentPlacesOf(long workspaceId, Collection<Long> personIds, LocalDate today) {
		Map<Long, List<Long>> places = new HashMap<>();
		if (personIds.isEmpty()) {
			return places;
		}
		this.jdbc.sql("SELECT o.person_id, o.place_id FROM reg_occupancy o "
				+ "JOIN reg_place p ON p.workspace_id = o.workspace_id AND p.id = o.place_id "
				+ "WHERE o.workspace_id = :workspaceId AND o.person_id IN (:personIds) AND o.deleted_at IS NULL "
				+ "AND p.deleted_at IS NULL AND o.start_date <= :today "
				+ "AND (o.end_date IS NULL OR o.end_date >= :today)")
			.param("workspaceId", workspaceId)
			.param("personIds", personIds)
			.param("today", today)
			.query((rs, rowNum) -> places.computeIfAbsent(rs.getLong("person_id"), id -> new ArrayList<>())
				.add(rs.getLong("place_id")))
			.list();
		return places;
	}

	/**
	 * Một lô dòng cư trú chưa xoá (cả dòng đã kết thúc), theo đường dẫn của địa điểm, ngày bắt đầu, rồi
	 * {@code id}.
	 * @param after dòng cuối của lô trước; {@code null} là lô đầu
	 */
	public List<ExportOccupancyRow> occupancies(long workspaceId, StayCursor after, int limit) {
		return this.jdbc
			.sql("SELECT s.id, s.place_id, p.path, p.code AS place_code, pe.full_name, s.relation, s.start_date, "
					+ "s.end_date, s.note FROM reg_occupancy" + STAY_JOINS + ((after != null) ? STAY_AFTER : "")
					+ STAY_ORDER)
			.params(stayParams(workspaceId, after, limit))
			.query((rs, rowNum) -> new ExportOccupancyRow(rs.getLong("id"), rs.getLong("place_id"),
					rs.getString("path"), rs.getString("place_code"), rs.getString("full_name"),
					Relation.valueOf(rs.getString("relation")), rs.getObject("start_date", LocalDate.class),
					rs.getObject("end_date", LocalDate.class), rs.getString("note")))
			.list();
	}

	/**
	 * Một lô dòng sở hữu chưa xoá (cả dòng đã kết thúc), theo đường dẫn của địa điểm, ngày bắt đầu, rồi
	 * {@code id}.
	 * @param after dòng cuối của lô trước; {@code null} là lô đầu
	 */
	public List<ExportOwnershipRow> ownerships(long workspaceId, StayCursor after, int limit) {
		return this.jdbc
			.sql("SELECT s.id, s.place_id, p.path, p.code AS place_code, pe.full_name, s.share_percent, "
					+ "s.start_date, s.end_date, s.document_no, s.note FROM reg_ownership" + STAY_JOINS
					+ ((after != null) ? STAY_AFTER : "") + STAY_ORDER)
			.params(stayParams(workspaceId, after, limit))
			.query((rs, rowNum) -> new ExportOwnershipRow(rs.getLong("id"), rs.getLong("place_id"),
					rs.getString("path"), rs.getString("place_code"), rs.getString("full_name"),
					rs.getBigDecimal("share_percent"), rs.getObject("start_date", LocalDate.class),
					rs.getObject("end_date", LocalDate.class), rs.getString("document_no"), rs.getString("note")))
			.list();
	}

	/**
	 * Một lô đồ đạc chưa xoá, sắp theo đường dẫn của địa điểm, rồi tên, rồi {@code id}: đồ của một
	 * địa điểm đứng cạnh nhau, địa điểm con đứng ngay sau cha.
	 * @param after dòng cuối của lô trước; {@code null} là lô đầu
	 */
	public List<ExportItemRow> items(long workspaceId, ItemFilter filter, ItemCursor after, int limit) {
		Map<String, Object> params = params(workspaceId, limit);
		StringBuilder sql = new StringBuilder("SELECT i.id, i.place_id, p.path, i.category_id, i.code, i.name, "
				+ "i.quantity, i.unit, i.status, i.placed_at, i.removed_at, i.description, i.attributes, "
				+ "o.full_name AS owner_name FROM reg_item i "
				+ "JOIN reg_place p ON p.workspace_id = i.workspace_id AND p.id = i.place_id "
				+ "LEFT JOIN reg_person o ON o.workspace_id = i.workspace_id AND o.id = i.owner_person_id "
				+ "WHERE i.workspace_id = :workspaceId AND i.deleted_at IS NULL");
		if (filter.status() != null) {
			sql.append(" AND i.status = :status");
			params.put("status", filter.status().name());
		}
		else if (!filter.withRemoved()) {
			sql.append(" AND i.removed_at IS NULL");
		}
		if (filter.categoryId() != null) {
			sql.append(" AND i.category_id = :categoryId");
			params.put("categoryId", filter.categoryId());
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

	private static Map<String, Object> params(long workspaceId, int limit) {
		Map<String, Object> params = new HashMap<>();
		params.put("workspaceId", workspaceId);
		params.put("limit", limit);
		return params;
	}

	private static Map<String, Object> stayParams(long workspaceId, StayCursor after, int limit) {
		Map<String, Object> params = params(workspaceId, limit);
		if (after != null) {
			params.put("afterPath", after.placePath());
			params.put("afterStart", after.startDate());
			params.put("afterId", after.id());
		}
		return params;
	}

	private ExportPersonRow personRow(ResultSet rs) throws SQLException {
		String gender = rs.getString("gender");
		return new ExportPersonRow(rs.getLong("id"), rs.getString("full_name"), rs.getString("phone"),
				rs.getString("email"), rs.getString("id_type"), rs.getString("id_number"),
				rs.getObject("date_of_birth", LocalDate.class), (gender != null) ? Gender.valueOf(gender) : null,
				rs.getString("note"), attributes(rs));
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
