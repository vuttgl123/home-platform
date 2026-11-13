package com.home_platform.registry.place;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PlaceRepository extends WorkspaceRepository<Place> {

	PlaceRepository(RepositorySupport support) {
		super(support, "reg_place", Place.class);
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
