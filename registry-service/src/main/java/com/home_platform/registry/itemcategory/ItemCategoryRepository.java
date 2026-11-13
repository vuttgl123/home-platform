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

	/**
	 * Xoá các mục tuỳ biến của riêng nhóm này. Chỉ gọi khi xoá hẳn nhóm: lúc đó chưa bản ghi
	 * nào dùng nhóm, nên không bản ghi nào giữ giá trị của các mục này. Bảng mục tuỳ biến không có
	 * khoá ngoại tới nhóm, nên nếu không xoá, các mục sẽ trỏ tới một nhóm không còn tồn tại.
	 */
	public int deleteOwnFields(long workspaceId, long categoryId) {
		return jdbc().sql("DELETE FROM reg_field_def WHERE workspace_id = :workspaceId AND target = 'ITEM' "
				+ "AND type_id = :categoryId")
			.param("workspaceId", workspaceId)
			.param("categoryId", categoryId)
			.update();
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
