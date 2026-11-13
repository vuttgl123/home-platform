package com.home_platform.registry.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class CatalogCacheTests {

	private final ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager(CatalogCache.PLACE_TYPES);

	private final CatalogCache catalogCache = new CatalogCache(this.manager);

	private final Cache cache = this.manager.getCache(CatalogCache.PLACE_TYPES);

	@Test
	void evictsNowAndAgainAfterCommit() {
		this.cache.put(-1L, "x");
		TransactionSynchronizationManager.initSynchronization();
		try {
			this.catalogCache.evict(CatalogCache.PLACE_TYPES, -1);
			assertThat(this.cache.get(-1L)).isNull();

			// Một lần đọc khác nạp lại cache trước khi transaction commit.
			this.cache.put(-1L, "y");
			TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

			assertThat(this.cache.get(-1L)).isNull();
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void evictsOnlyThatWorkspace() {
		this.cache.put(-1L, "a");
		this.cache.put(-2L, "b");

		this.catalogCache.evict(CatalogCache.PLACE_TYPES, -1);

		assertThat(this.cache.get(-1L)).isNull();
		assertThat(this.cache.get(-2L).get()).isEqualTo("b");
	}

}
