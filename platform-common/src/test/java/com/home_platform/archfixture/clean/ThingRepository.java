package com.home_platform.archfixture.clean;

import java.util.Optional;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import org.springframework.stereotype.Repository;

/** Mẫu đúng luật: mọi hàm public nhận {@code workspaceId} trước tiên. */
@Repository
public class ThingRepository extends WorkspaceRepository<Thing> {

	public ThingRepository(RepositorySupport support) {
		super(support, "thing", Thing.class);
	}

	public Optional<Thing> findByCode(long workspaceId, String code) {
		return jdbc().sql("SELECT " + columns() + " FROM thing WHERE workspace_id = :workspaceId AND code = :code")
			.param("workspaceId", workspaceId)
			.param("code", code)
			.query(rowMapper())
			.optional();
	}

}
