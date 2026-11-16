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
 * cache. *
 * <p>Truy vấn nạp cache (các hàm {@code listAll} của repository danh mục) kết thúc bằng
 * {@link #LATEST_COMMITTED}. Chúng thường chạy bên trong transaction của một lệnh ghi khác (kiểm
 * tra {@code attributes} khi tạo địa điểm), mà transaction đó có thể đã chốt snapshot từ trước
 * khi danh mục đổi: một câu {@code SELECT} thường sẽ nạp bản cũ vào cache sau cả hai lần xoá ở
 * trên, và cache giữ bản cũ cho tới khi hết hạn.
 */
@Component
public class CatalogCache {

	public static final String PLACE_TYPES = "placeTypes";

	public static final String ITEM_CATEGORIES = "itemCategories";

	public static final String FIELD_DEFS = "fieldDefs";

	public static final String CODE_RULES = "codeRules";

	/**
	 * Đuôi của mọi truy vấn nạp cache danh mục. Câu đọc kèm khoá luôn thấy bản đã commit mới nhất,
	 * bất kể transaction đang gọi đã chốt snapshot từ lúc nào; và lệnh sửa danh mục chạy cùng lúc
	 * phải chờ transaction này xong rồi mới commit và xoá cache, nên cache không thể giữ bản cũ hơn
	 * database. Gọi ngoài transaction thì khoá nhả ngay khi câu lệnh xong.
	 */
	public static final String LATEST_COMMITTED = " FOR SHARE";

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
