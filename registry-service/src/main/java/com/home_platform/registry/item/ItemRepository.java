package com.home_platform.registry.item;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ItemRepository extends WorkspaceRepository<Item> {

	ItemRepository(RepositorySupport support) {
		super(support, "reg_item", Item.class);
	}

}
