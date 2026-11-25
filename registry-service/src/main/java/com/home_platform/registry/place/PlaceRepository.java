package com.home_platform.registry.place;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.jdbc.KeysetQuery;
import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorCodec;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.springframework.stereotype.Repository;

@Repository
public class PlaceRepository extends WorkspaceRepository<Place> {

	/** Những địa điểm trong {@code :ids} có ít nhất một con chưa xoá. Index (workspace_id, parent_id, name). */
	static final String PARENTS_WITH_CHILDREN = "SELECT DISTINCT parent_id FROM reg_place "
			+ "WHERE workspace_id = :workspaceId AND parent_id IN (:ids) AND deleted_at IS NULL";

	/**
	 * Số người đang ở trực tiếp tại dòng {@code reg_place} đang xét, ngày {@code :today}: người khác nhau,
	 * chưa xoá, có dòng cư trú chưa xoá với {@code start_date <= :today <= end_date} (ngày kết thúc vẫn
	 * là ngày còn ở), quan hệ nào cũng tính (spec Đợt 6, L2). Là một subquery tương quan theo tên bảng,
	 * không theo alias, để dùng được cả trong {@link #keyset()}.
	 */
	public static final String PEOPLE_LIVING_HERE = "(SELECT COUNT(DISTINCT o.person_id) FROM reg_occupancy o "
			+ "JOIN reg_person pe ON pe.workspace_id = o.workspace_id AND pe.id = o.person_id "
			+ "WHERE o.workspace_id = reg_place.workspace_id AND o.place_id = reg_place.id "
			+ "AND o.deleted_at IS NULL AND pe.deleted_at IS NULL AND o.start_date <= :today "
			+ "AND (o.end_date IS NULL OR o.end_date >= :today))";

	PlaceRepository(RepositorySupport support) {
		super(support, "reg_place", Place.class);
	}

	/**
	 * Trong {@code placeIds}, những địa điểm có ít nhất một con chưa xoá. Một truy vấn cho cả danh
	 * sách; danh sách rỗng thì không chạy SQL.
	 */
	public Set<Long> parentsWithChildren(long workspaceId, Collection<Long> placeIds) {
		if (placeIds.isEmpty()) {
			return Set.of();
		}
		return new HashSet<>(jdbc().sql(PARENTS_WITH_CHILDREN)
			.param("workspaceId", workspaceId)
			.param("ids", placeIds)
			.query(Long.class)
			.list());
	}

	/** Dòng đang bị xoá mềm, mới xoá trước. Index (workspace_id, deleted_at). */
	public CursorPage<Place> deleted(long workspaceId, CursorRequest page) {
		return keyset().onlyDeleted().orderByDesc("deleted_at").fetch(workspaceId, page);
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

	/**
	 * Địa điểm còn chỗ ngày {@code today}, theo tên (spec Đợt 6, L4): có sức chứa và số người đang ở
	 * nhỏ hơn sức chứa; hoặc chưa đặt sức chứa, không có con chưa xoá và không có ai đang ở.
	 * @param placeTypeId chỉ loại này; {@code null} là mọi loại
	 * @param rootPath chỉ các hậu duệ (không gồm gốc) của địa điểm có {@code path} này; {@code null} là cả không gian
	 */
	public CursorPage<Place> vacancies(long workspaceId, Long placeTypeId, String rootPath, LocalDate today,
			CursorRequest page) {
		KeysetQuery<Place> query = keyset().where("((capacity IS NOT NULL AND " + PEOPLE_LIVING_HERE
				+ " < capacity) OR (capacity IS NULL AND NOT EXISTS (SELECT 1 FROM reg_place c "
				+ "WHERE c.workspace_id = reg_place.workspace_id AND c.parent_id = reg_place.id "
				+ "AND c.deleted_at IS NULL) AND " + PEOPLE_LIVING_HERE + " = 0))", Map.of("today", today));
		if (placeTypeId != null) {
			query = query.where("place_type_id = :placeTypeId", Map.of("placeTypeId", placeTypeId));
		}
		if (rootPath != null) {
			query = query.where("path LIKE :subtreePrefix AND path <> :rootPath",
					Map.of("subtreePrefix", rootPath + "%", "rootPath", rootPath));
		}
		return query.orderBy("name").fetch(workspaceId, page);
	}

	/**
	 * Số người đang ở ngày {@code today} của từng địa điểm trong {@code ids}, theo
	 * {@link #PEOPLE_LIVING_HERE}. Một truy vấn; danh sách rỗng thì không chạy SQL.
	 */
	public Map<Long, Integer> peopleLiving(long workspaceId, Collection<Long> ids, LocalDate today) {
		if (ids.isEmpty()) {
			return Map.of();
		}
		Map<Long, Integer> counts = new HashMap<>();
		jdbc().sql("SELECT id, " + PEOPLE_LIVING_HERE + " AS living FROM reg_place "
				+ "WHERE workspace_id = :workspaceId AND id IN (:ids)")
			.param("workspaceId", workspaceId)
			.param("ids", ids)
			.param("today", today)
			.query((rs, rowNum) -> counts.put(rs.getLong("id"), rs.getInt("living")))
			.list();
		return counts;
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

	/** Số dòng chưa xoá chưa có mã. */
	public int countWithoutCode(long workspaceId) {
		return jdbc().sql("SELECT COUNT(*) FROM reg_place WHERE workspace_id = :workspaceId AND code IS NULL "
				+ "AND deleted_at IS NULL")
			.param("workspaceId", workspaceId)
			.query(Integer.class)
			.single();
	}

	/** {@code id} của tối đa {@code limit} dòng chưa có mã, cùng điều kiện như {@link #countWithoutCode}; cũ trước. */
	public List<Long> idsWithoutCode(long workspaceId, int limit) {
		return jdbc().sql("SELECT id FROM reg_place WHERE workspace_id = :workspaceId AND code IS NULL "
				+ "AND deleted_at IS NULL ORDER BY id LIMIT :limit")
			.param("workspaceId", workspaceId)
			.param("limit", limit)
			.query(Long.class)
			.list();
	}

	/** Đã có dòng nào (kể cả đã xoá) mang mã này chưa. Khoá duy nhất uk_reg_place_code. */
	public boolean codeExists(long workspaceId, String code) {
		return jdbc().sql("SELECT EXISTS (SELECT 1 FROM reg_place WHERE workspace_id = :workspaceId AND code = :code)")
			.param("workspaceId", workspaceId)
			.param("code", code)
			.query(Integer.class)
			.single() == 1;
	}

	/** Đúng mã (không phân biệt hoa thường), theo {@code id}. Khoá duy nhất (workspace_id, code). */
	public CursorPage<Place> findByCode(long workspaceId, String code, CursorRequest page) {
		return keyset().where("code = :code", Map.of("code", code)).fetch(workspaceId, page);
	}

	/**
	 * Địa điểm có địa chỉ trong một tỉnh (và một phường nếu có), theo {@code id}. Index
	 * (workspace_id, province_code, ward_code) của bảng địa chỉ; cursor là {@code [id]}.
	 */
	public CursorPage<Place> byArea(long workspaceId, String provinceCode, String wardCode, CursorRequest page) {
		Long afterId = (page.cursor() != null) ? decodeId(page.cursor()) : null;
		var statement = jdbc()
			.sql("SELECT " + metadata().selectList("p") + " FROM reg_place_address a JOIN reg_place p "
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
		return CursorPage.of(statement.query(rowMapper()).list(), page.limit(),
				last -> List.of(String.valueOf(last.id())));
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

	/** Số người đang ở trực tiếp tại một địa điểm ngày {@code today}, theo {@link #PEOPLE_LIVING_HERE}. */
	public int countPeopleLiving(long workspaceId, long placeId, LocalDate today) {
		return count("SELECT " + PEOPLE_LIVING_HERE + " FROM reg_place WHERE workspace_id = :workspaceId "
				+ "AND id = :placeId", workspaceId, placeId, today);
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

	/** Số dòng sở hữu chưa xoá đang hoặc sắp có hiệu lực. Index (workspace_id, place_id, end_date, start_date). */
	public int countCurrentOwnerships(long workspaceId, long placeId, LocalDate today) {
		return count("SELECT COUNT(*) FROM reg_ownership WHERE workspace_id = :workspaceId AND place_id = :placeId "
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
