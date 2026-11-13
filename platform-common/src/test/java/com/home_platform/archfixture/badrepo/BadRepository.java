package com.home_platform.archfixture.badrepo;

import java.util.List;
import java.util.Optional;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import org.springframework.stereotype.Repository;

/** Mẫu vi phạm: hai hàm public không nhận {@code workspaceId}. */
@Repository
public class BadRepository extends WorkspaceRepository<Thing> {

	public BadRepository(RepositorySupport support) {
		super(support, "thing", Thing.class);
	}

	public Optional<Thing> findByCode(String code) {
		return Optional.empty();
	}

	public List<Thing> findAllEverywhere() {
		return List.of();
	}

}
