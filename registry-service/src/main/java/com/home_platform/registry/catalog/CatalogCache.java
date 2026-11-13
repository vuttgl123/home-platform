package com.home_platform.registry.catalog;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Xoá cache danh mục của một không gian sau khi danh mục đó đổi. Khoá cache luôn là
 * {@code workspaceId}.
 *
 * <p>Xoá hai lần: ngay lúc ghi, và lần nữa sau khi transaction commit. Lần thứ hai cần vì
 * trong lúc transaction chưa commit, một request khác có thể đọc dữ liệu cũ và nạp nó lại vào
 * cache.
 */
@Component
public class CatalogCache {

	public static final String PLACE_TYPES = "placeTypes";

	public static final String ITEM_CATEGORIES = "itemCategories";

	public static final String FIELD_DEFS = "fieldDefs";

	private final CacheManager cacheManager;

	public CatalogCache(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public void evict(String cacheName, long workspaceId) {
		Cache cache = this.cacheManager.getCache(cacheName);
		if (cache == null) {
			return;
		}
		cache.evict(workspaceId);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					cache.evict(workspaceId);
				}
			});
		}
	}

}
