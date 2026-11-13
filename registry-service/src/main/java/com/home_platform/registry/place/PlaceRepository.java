package com.home_platform.registry.place;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorCodec;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.springframework.stereotype.Repository;

@Repository
public class PlaceRepository extends WorkspaceRepository<Place> {

	PlaceRepository(RepositorySupport support) {
		super(support, "reg_place", Place.class);
	}

	/** Địa điểm cấp cao nhất, theo tên. Index (workspace_id, parent_id, name). */
	public CursorPage<Place> topLevel(long workspaceId, CursorRequest page) {
		return keyset().where("parent_id IS NULL", Map.of()).orderBy("name").fetch(workspaceId, page);
	}

	/** Con trực tiếp, theo tên. Index (workspace_id, parent_id, name). */
	public CursorPage<Place> children(long workspaceId, long parentId, CursorRequest page) {
		return keyset().where("parent_id = :parentId", Map.of("parentId", parentId))
			.orderBy("name")
			.fetch(workspaceId, page);
	}

	/** Mọi hậu duệ, không gồm gốc, theo {@code path}. Index (workspace_id, path), quét theo tiền tố. */
	public CursorPage<Place> subtree(long workspaceId, String rootPath, CursorRequest page) {
		return keyset()
			.where("path LIKE :subtreePrefix AND path <> :rootPath",
					Map.of("subtreePrefix", rootPath + "%", "rootPath", rootPath))
			.orderBy("path")
			.fetch(workspaceId, page);
	}

	/** Địa điểm của một loại, theo tên. Index (workspace_id, place_type_id, name). */
	public CursorPage<Place> byType(long workspaceId, long placeTypeId, CursorRequest page) {
		return keyset().where("place_type_id = :placeTypeId", Map.of("placeTypeId", placeTypeId))
			.orderBy("name")
			.fetch(workspaceId, page);
	}

	/**
	 * Tên bắt đầu bằng một chuỗi, theo tên. Index (workspace_id, name).
	 * @param likePrefix tiền tố đã thoát ký tự đặc biệt của {@code LIKE} và có {@code %} ở cuối
	 */
	public CursorPage<Place> searchByName(long workspaceId, String likePrefix, CursorRequest page) {
		return keyset().where("name LIKE :namePrefix", Map.of("namePrefix", likePrefix))
			.orderBy("name")
			.fetch(workspaceId, page);
	}

	/** Đúng mã (không phân biệt hoa thường), theo {@code id}. Index (workspace_id, code). */
	public CursorPage<Place> findByCode(long workspaceId, String code, CursorRequest page) {
		return keyset().where("code = :code", Map.of("code", code)).fetch(workspaceId, page);
	}

	/**
	 * Địa điểm có địa chỉ trong một tỉnh (và một phường nếu có), theo {@code id}. Index
	 * (workspace_id, province_code, ward_code) của bảng địa chỉ; cursor là {@code [id]}.
	 */
	public CursorPage<Place> byArea(long workspaceId, String provinceCode, String wardCode, CursorRequest page) {
		String columns = Arrays.stream(columns().split(", ")).map(column -> "p." + column)
			.collect(Collectors.joining(", "));
		Long afterId = (page.cursor() != null) ? decodeId(page.cursor()) : null;
		var statement = jdbc()
			.sql("SELECT " + columns + " FROM reg_place_address a JOIN reg_place p "
					+ "ON p.workspace_id = a.workspace_id AND p.id = a.place_id "
					+ "WHERE a.workspace_id = :workspaceId AND a.province_code = :provinceCode"
					+ ((wardCode != null) ? " AND a.ward_code = :wardCode" : "")
					+ ((afterId != null) ? " AND a.place_id > :afterId" : "")
					+ " AND p.deleted_at IS NULL ORDER BY a.place_id LIMIT :limit")
			.param("workspaceId", workspaceId)
			.param("provinceCode", provinceCode)
			.param("limit", page.limit() + 1);
		if (wardCode != null) {
			statement = statement.param("wardCode", wardCode);
		}
		if (afterId != null) {
			statement = statement.param("afterId", afterId);
		}
		List<Place> rows = statement.query(rowMapper()).list();
		if (rows.size() <= page.limit()) {
			return new CursorPage<>(rows, null);
		}
		List<Place> items = List.copyOf(rows.subList(0, page.limit()));
		return new CursorPage<>(items, CursorCodec.encode(List.of(String.valueOf(items.getLast().id()))));
	}

	private static long decodeId(String cursor) {
		List<String> parts = CursorCodec.decode(cursor);
		try {
			if (parts.size() != 1) {
				throw new IllegalArgumentException();
			}
			return Long.parseLong(parts.get(0));
		}
		catch (IllegalArgumentException ex) {
			throw new BadRequestException("Invalid cursor");
		}
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
