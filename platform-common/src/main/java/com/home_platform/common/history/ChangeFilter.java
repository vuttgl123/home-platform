package com.home_platform.common.history;

import java.time.Instant;

/**
 * Lọc của danh sách hoạt động ({@link EntityHistory#listRecent}); mọi trường {@code null} là không
 * lọc theo trường đó.
 * @param entityType loại bản ghi, ví dụ {@code PLACE}
 * @param action hành động của version
 * @param changedBy người tạo version
 * @param from tính cả mốc này
 * @param to không tính mốc này
 */
public record ChangeFilter(String entityType, ChangeAction action, Long changedBy, Instant from, Instant to) {

	private static final ChangeFilter NONE = new ChangeFilter(null, null, null, null, null);

	/** Không lọc gì. */
	public static ChangeFilter none() {
		return NONE;
	}

}
