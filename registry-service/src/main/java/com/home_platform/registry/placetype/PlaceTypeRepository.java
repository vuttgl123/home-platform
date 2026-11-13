package com.home_platform.registry.placetype;

import java.util.List;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PlaceTypeRepository extends WorkspaceRepository<PlaceType> {

	PlaceTypeRepository(RepositorySupport support) {
		super(support, "reg_place_type", PlaceType.class);
	}

	/** Mọi loại của không gian, mọi trạng thái, theo thứ tự hiển thị rồi tên. */
	public List<PlaceType> listAll(long workspaceId) {
		return jdbc().sql("SELECT " + columns() + " FROM reg_place_type WHERE workspace_id = :workspaceId "
				+ "ORDER BY sort_order, name, id")
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
