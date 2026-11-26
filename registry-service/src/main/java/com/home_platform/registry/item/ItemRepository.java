package com.home_platform.registry.item;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.registry.search.SqlCondition;
import com.home_platform.registry.search.TextSearch;
import org.springframework.stereotype.Repository;

@Repository
public class ItemRepository extends WorkspaceRepository<Item> {

	ItemRepository(RepositorySupport support) {
		super(support, "reg_item", Item.class);
	}

	/** Tên của các món đồ (kể cả đã xoá) theo {@code id}. */
	public Map<Long, String> names(long workspaceId, List<Long> ids) {
		Map<Long, String> names = new HashMap<>();
		if (ids.isEmpty()) {
			return names;
		}
		jdbc().sql("SELECT id, name FROM reg_item WHERE workspace_id = :workspaceId AND id IN (:ids)")
			.param("workspaceId", workspaceId)
			.param("ids", ids)
			.query((rs, rowNum) -> names.put(rs.getLong("id"), rs.getString("name")))
			.list();
		return names;
	}

	/**
	 * Ngày của lần bảo trì định kỳ ({@code SERVICE}) chưa xoá muộn nhất của từng món; món chưa có lần nào thì
	 * không có trong kết quả. Đi theo index {@code (workspace_id, item_id, done_on)} của {@code reg_maintenance}.
	 */
	public Map<Long, LocalDate> lastServiceOn(long workspaceId, Collection<Long> itemIds) {
		Map<Long, LocalDate> found = new HashMap<>();
		if (itemIds.isEmpty()) {
			return found;
		}
		jdbc().sql("SELECT item_id, MAX(done_on) AS done_on FROM reg_maintenance WHERE workspace_id = :workspaceId "
				+ "AND item_id IN (:ids) AND kind = 'SERVICE' AND deleted_at IS NULL GROUP BY item_id")
			.param("workspaceId", workspaceId)
			.param("ids", itemIds)
			.query((rs, rowNum) -> found.put(rs.getLong("item_id"), rs.getObject("done_on", LocalDate.class)))
			.list();
		return found;
	}

	/**
	 * Đồ ở một địa điểm. Chưa gỡ: theo tên; đã gỡ: gỡ gần nhất trước. Index
	 * (workspace_id, place_id, removed_at, name).
	 */
	public CursorPage<Item> atPlace(long workspaceId, long placeId, boolean removed, CursorRequest page) {
		Map<String, Object> params = Map.of("placeId", placeId);
		return removed
				? keyset().where("place_id = :placeId AND removed_at IS NOT NULL", params)
					.orderByDesc("removed_at")
					.fetch(workspaceId, page)
				: keyset().where("place_id = :placeId AND removed_at IS NULL", params)
					.orderBy("name")
					.fetch(workspaceId, page);
	}

	/** Đồ chưa gỡ ở địa điểm có {@code path} này hoặc bất kỳ hậu duệ chưa xoá nào; theo tên. */
	public CursorPage<Item> inSubtree(long workspaceId, String rootPath, CursorRequest page) {
		return keyset()
			.where("place_id IN (SELECT p.id FROM reg_place p WHERE p.workspace_id = :workspaceId "
					+ "AND p.path LIKE :subtreePrefix AND p.deleted_at IS NULL) AND removed_at IS NULL",
					Map.of("subtreePrefix", rootPath + "%"))
			.orderBy("name")
			.fetch(workspaceId, page);
	}

	/** Đồ chưa gỡ của một người, theo tên. Index (workspace_id, owner_person_id, removed_at, name). */
	public CursorPage<Item> ownedBy(long workspaceId, long ownerPersonId, CursorRequest page) {
		return keyset().where("owner_person_id = :ownerId AND removed_at IS NULL", Map.of("ownerId", ownerPersonId))
			.orderBy("name")
			.fetch(workspaceId, page);
	}

	/**
	 * Đồ của cả không gian, theo tên. {@code status} trống: mọi món chưa gỡ (index (workspace_id, name));
	 * có giá trị: đúng trạng thái đó (index (workspace_id, status, name)). {@code categoryId} khác null
	 * thì chỉ món của nhóm đó (index (workspace_id, category_id, name)). Tìm chữ ({@code text}: tên, mã, mô
	 * tả), toà ({@code rootPath}: gồm chính nó) và mục tuỳ biến quét các dòng đó (spec Đợt 11, S5).
	 */
	public CursorPage<Item> all(long workspaceId, ItemFilter filter, CursorRequest page) {
		var query = keyset();
		query = (filter.status() == null) ? query.where("removed_at IS NULL", Map.of())
				: query.where("status = :status", Map.of("status", filter.status().name()));
		if (filter.categoryId() != null) {
			query = query.where("category_id = :categoryId", Map.of("categoryId", filter.categoryId()));
		}
		if (filter.text() != null) {
			SqlCondition text = TextSearch.anyColumnContains(filter.text(), "name", "code", "description");
			query = query.where(text.sql(), text.params());
		}
		if (filter.rootPath() != null) {
			query = query.where("place_id IN (SELECT p.id FROM reg_place p WHERE p.workspace_id = :workspaceId "
					+ "AND p.path LIKE :rootPrefix AND p.deleted_at IS NULL)", Map.of("rootPrefix", filter.rootPath() + "%"));
		}
		for (SqlCondition condition : filter.attributes()) {
			query = query.where(condition.sql(), condition.params());
		}
		return query.orderBy("name").fetch(workspaceId, page);
	}

	/** Dòng đang bị xoá mềm, mới xoá trước. Index (workspace_id, deleted_at). */
	public CursorPage<Item> deleted(long workspaceId, CursorRequest page) {
		return keyset().onlyDeleted().orderByDesc("deleted_at").fetch(workspaceId, page);
	}

	/** Tên bắt đầu bằng, kể cả đồ đã gỡ; theo tên. Index (workspace_id, name). */
	public CursorPage<Item> searchByName(long workspaceId, String likePrefix, CursorRequest page) {
		return keyset().where("name LIKE :namePrefix", Map.of("namePrefix", likePrefix))
			.orderBy("name")
			.fetch(workspaceId, page);
	}

	/** Số dòng chưa xoá, chưa gỡ chưa có mã. */
	public int countWithoutCode(long workspaceId) {
		return jdbc().sql("SELECT COUNT(*) FROM reg_item WHERE workspace_id = :workspaceId AND code IS NULL "
				+ "AND deleted_at IS NULL AND removed_at IS NULL")
			.param("workspaceId", workspaceId)
			.query(Integer.class)
			.single();
	}

	/** {@code id} của tối đa {@code limit} dòng chưa có mã, cùng điều kiện như {@link #countWithoutCode}; cũ trước. */
	public List<Long> idsWithoutCode(long workspaceId, int limit) {
		return jdbc().sql("SELECT id FROM reg_item WHERE workspace_id = :workspaceId AND code IS NULL "
				+ "AND deleted_at IS NULL AND removed_at IS NULL ORDER BY id LIMIT :limit")
			.param("workspaceId", workspaceId)
			.param("limit", limit)
			.query(Long.class)
			.list();
	}

	/** Đã có dòng nào (kể cả đã xoá) mang mã này chưa. Khoá duy nhất uk_reg_item_code. */
	public boolean codeExists(long workspaceId, String code) {
		return jdbc().sql("SELECT EXISTS (SELECT 1 FROM reg_item WHERE workspace_id = :workspaceId AND code = :code)")
			.param("workspaceId", workspaceId)
			.param("code", code)
			.query(Integer.class)
			.single() == 1;
	}

	/** Đúng mã (không phân biệt hoa thường); theo {@code id}. Khoá duy nhất (workspace_id, code). */
	public CursorPage<Item> findByCode(long workspaceId, String code, CursorRequest page) {
		return keyset().where("code = :code", Map.of("code", code)).fetch(workspaceId, page);
	}

}
