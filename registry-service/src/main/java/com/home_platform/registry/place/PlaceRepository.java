package com.home_platform.registry.place;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PlaceRepository extends WorkspaceRepository<Place> {

	PlaceRepository(RepositorySupport support) {
		super(support, "reg_place", Place.class);
	}

	/** Số địa điểm con chưa xoá. */
	public int countChildren(long workspaceId, long placeId) {
		return count("SELECT COUNT(*) FROM reg_place WHERE workspace_id = :workspaceId AND parent_id = :placeId "
				+ "AND deleted_at IS NULL", workspaceId, placeId, null);
	}

	/** Số dòng cư trú chưa xoá đang hoặc sắp có hiệu lực: chưa có ngày kết thúc, hoặc kết thúc từ hôm nay trở đi. */
	public int countCurrentOccupancies(long workspaceId, long placeId, LocalDate today) {
		return count("SELECT COUNT(*) FROM reg_occupancy WHERE workspace_id = :workspaceId AND place_id = :placeId "
				+ "AND deleted_at IS NULL AND (end_date IS NULL OR end_date >= :today)", workspaceId, placeId, today);
	}

	/** Số món đồ chưa xoá và chưa gỡ. */
	public int countCurrentItems(long workspaceId, long placeId) {
		return count("SELECT COUNT(*) FROM reg_item WHERE workspace_id = :workspaceId AND place_id = :placeId "
				+ "AND deleted_at IS NULL AND removed_at IS NULL", workspaceId, placeId, null);
	}

	private int count(String sql, long workspaceId, long placeId, LocalDate today) {
		var statement = jdbc().sql(sql).param("workspaceId", workspaceId).param("placeId", placeId);
		if (today != null) {
			statement = statement.param("today", today);
		}
		return statement.query(Integer.class).single();
	}

	/** {@code depth} lớn nhất trong cây có gốc mang {@code path} (kể cả gốc và dòng đã xoá). */
	public int maxDepthUnder(long workspaceId, String path) {
		return jdbc().sql("SELECT MAX(depth) FROM reg_place WHERE workspace_id = :workspaceId "
				+ "AND path LIKE CONCAT(:path, '%')")
			.param("workspaceId", workspaceId)
			.param("path", path)
			.query(Integer.class)
			.single();
	}

	/**
	 * Chuyển mọi hậu duệ (kể cả đã xoá, để khôi phục sau này vẫn đúng chỗ) từ {@code oldPath}
	 * sang {@code newPath} bằng một câu lệnh, trừ chính gốc. Tăng {@code version} để client đang
	 * giữ bản cũ của một hậu duệ nhận 409 thay vì ghi đè chỗ đứng mới.
	 * @return số dòng đã đổi
	 */
	public int moveDescendants(WorkspaceContext ctx, String oldPath, String newPath, int depthDelta, Instant now) {
		return jdbc().sql("UPDATE reg_place SET path = CONCAT(:newPath, SUBSTRING(path, :oldLength + 1)), "
				+ "depth = depth + :depthDelta, version = version + 1, updated_at = :now, updated_by = :userId "
				+ "WHERE workspace_id = :workspaceId AND path LIKE CONCAT(:oldPath, '%') AND path <> :oldPath")
			.param("workspaceId", ctx.workspaceId())
			.param("userId", ctx.userId())
			.param("oldPath", oldPath)
			.param("newPath", newPath)
			.param("oldLength", oldPath.length())
			.param("depthDelta", depthDelta)
			.param("now", now)
			.update();
	}

	/** Tên của các địa điểm theo đúng thứ tự {@code ids}; bỏ qua id không có. */
	public List<PlaceRef> refs(long workspaceId, List<Long> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		Map<Long, String> names = new HashMap<>();
		jdbc().sql("SELECT id, name FROM reg_place WHERE workspace_id = :workspaceId AND id IN (:ids)")
			.param("workspaceId", workspaceId)
			.param("ids", ids)
			.query((rs, rowNum) -> names.put(rs.getLong("id"), rs.getString("name")))
			.list();
		return ids.stream().filter(names::containsKey).map(id -> new PlaceRef(id, names.get(id))).toList();
	}

}
