package com.home_platform.registry.item;

import java.util.Map;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.springframework.stereotype.Repository;

@Repository
public class ItemRepository extends WorkspaceRepository<Item> {

	ItemRepository(RepositorySupport support) {
		super(support, "reg_item", Item.class);
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

	/** Đồ chưa gỡ của một nhóm, theo tên. Index (workspace_id, category_id, name). */
	public CursorPage<Item> byCategory(long workspaceId, long categoryId, CursorRequest page) {
		return keyset().where("category_id = :categoryId AND removed_at IS NULL", Map.of("categoryId", categoryId))
			.orderBy("name")
			.fetch(workspaceId, page);
	}

	/** Tên bắt đầu bằng, kể cả đồ đã gỡ; theo tên. Index (workspace_id, name). */
	public CursorPage<Item> searchByName(long workspaceId, String likePrefix, CursorRequest page) {
		return keyset().where("name LIKE :namePrefix", Map.of("namePrefix", likePrefix))
			.orderBy("name")
			.fetch(workspaceId, page);
	}

	/** Đúng mã (không phân biệt hoa thường); theo {@code id}. Index (workspace_id, code). */
	public CursorPage<Item> findByCode(long workspaceId, String code, CursorRequest page) {
		return keyset().where("code = :code", Map.of("code", code)).fetch(workspaceId, page);
	}

}
