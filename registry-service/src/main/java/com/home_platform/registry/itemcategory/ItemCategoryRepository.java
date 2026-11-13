package com.home_platform.registry.itemcategory;

import java.util.List;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ItemCategoryRepository extends WorkspaceRepository<ItemCategory> {

	ItemCategoryRepository(RepositorySupport support) {
		super(support, "reg_item_category", ItemCategory.class);
	}

	/** Mọi nhóm của không gian, mọi trạng thái, theo thứ tự hiển thị rồi tên. */
	public List<ItemCategory> listAll(long workspaceId) {
		return jdbc().sql("SELECT " + columns() + " FROM reg_item_category WHERE workspace_id = :workspaceId "
				+ "ORDER BY sort_order, name, id")
			.param("workspaceId", workspaceId)
			.query(rowMapper())
			.list();
	}

	/** Có món đồ nào, kể cả đã xoá mềm, thuộc nhóm này không. Dùng index (workspace_id, category_id, name). */
	public boolean isUsed(long workspaceId, long categoryId) {
		return jdbc().sql("SELECT EXISTS (SELECT 1 FROM reg_item WHERE workspace_id = :workspaceId "
				+ "AND category_id = :categoryId)")
			.param("workspaceId", workspaceId)
			.param("categoryId", categoryId)
			.query(Integer.class)
			.single() == 1;
	}

}
