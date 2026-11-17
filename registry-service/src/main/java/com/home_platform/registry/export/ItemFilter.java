package com.home_platform.registry.export;

import com.home_platform.registry.item.ItemStatus;

/**
 * Món nào vào sheet đồ đạc. Món đã xoá không bao giờ vào.
 *
 * @param status khác {@code null} là đúng trạng thái đó
 * @param categoryId khác {@code null} là chỉ nhóm đó
 * @param withRemoved khi {@code status} trống: {@code true} là mọi món, {@code false} là chỉ món chưa gỡ
 */
public record ItemFilter(ItemStatus status, Long categoryId, boolean withRemoved) {

	/** Lọc như {@code GET /items} không phạm vi: không cho trạng thái thì chỉ món chưa gỡ. */
	static ItemFilter likeTheList(ItemStatus status, Long categoryId) {
		return new ItemFilter(status, categoryId, false);
	}

	/** Mọi món chưa xoá, kể cả món đã gỡ. */
	static ItemFilter everything() {
		return new ItemFilter(null, null, true);
	}

}
