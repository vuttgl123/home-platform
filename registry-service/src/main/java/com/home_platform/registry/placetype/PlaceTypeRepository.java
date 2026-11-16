package com.home_platform.registry.placetype;

import java.util.List;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import com.home_platform.registry.catalog.CatalogCache;
import org.springframework.stereotype.Repository;

@Repository
public class PlaceTypeRepository extends WorkspaceRepository<PlaceType> {

	PlaceTypeRepository(RepositorySupport support) {
		super(support, "reg_place_type", PlaceType.class);
	}

	/**
	 * Mọi loại của không gian, mọi trạng thái, theo thứ tự hiển thị rồi tên. Là nguồn nạp cache nên
	 * đọc bản đã commit mới nhất ({@link CatalogCache#LATEST_COMMITTED}).
	 */
	public List<PlaceType> listAll(long workspaceId) {
		return jdbc().sql("SELECT " + columns() + " FROM reg_place_type WHERE workspace_id = :workspaceId "
				+ "ORDER BY sort_order, name, id" + CatalogCache.LATEST_COMMITTED)
			.param("workspaceId", workspaceId)
			.query(rowMapper())
			.list();
	}

	public boolean anyExists(long workspaceId) {
		return jdbc().sql("SELECT EXISTS (SELECT 1 FROM reg_place_type WHERE workspace_id = :workspaceId)")
			.param("workspaceId", workspaceId)
			.query(Integer.class)
			.single() == 1;
	}

	/**
	 * Xoá các mục tuỳ biến của riêng loại này. Chỉ gọi khi xoá hẳn loại: lúc đó chưa bản ghi
	 * nào dùng loại, nên không bản ghi nào giữ giá trị của các mục này. Bảng mục tuỳ biến không có
	 * khoá ngoại tới loại, nên nếu không xoá, các mục sẽ trỏ tới một loại không còn tồn tại.
	 */
	/** Xoá mẫu mã riêng của loại này; gọi khi xoá hẳn nó. */
	public int deleteOwnCodeRule(long workspaceId, long placeTypeId) {
		return jdbc().sql("DELETE FROM reg_code_rule WHERE workspace_id = :workspaceId AND target = 'PLACE' "
				+ "AND type_id = :placeTypeId")
			.param("workspaceId", workspaceId)
			.param("placeTypeId", placeTypeId)
			.update();
	}

	public int deleteOwnFields(long workspaceId, long placeTypeId) {
		return jdbc().sql("DELETE FROM reg_field_def WHERE workspace_id = :workspaceId AND target = 'PLACE' "
				+ "AND type_id = :placeTypeId")
			.param("workspaceId", workspaceId)
			.param("placeTypeId", placeTypeId)
			.update();
	}

	/** Có địa điểm nào, kể cả đã xoá mềm, thuộc loại này không. Dùng index (workspace_id, place_type_id, name). */
	public boolean isUsed(long workspaceId, long placeTypeId) {
		return jdbc().sql("SELECT EXISTS (SELECT 1 FROM reg_place WHERE workspace_id = :workspaceId "
				+ "AND place_type_id = :placeTypeId)")
			.param("workspaceId", workspaceId)
			.param("placeTypeId", placeTypeId)
			.query(Integer.class)
			.single() == 1;
	}

}
