package com.home_platform.common.history;

import java.time.Instant;
import java.util.List;

/**
 * Một version trong danh sách hoạt động của cả không gian: như {@link EntityVersionSummary}, kèm
 * bản ghi mà nó thuộc về.
 *
 * @param entityType loại bản ghi, ví dụ {@code PLACE}
 * @param changedFields xem {@link EntityVersionSummary#changedFields()}
 */
public record EntityChange(String entityType, long entityId, int versionNo, ChangeAction action,
		List<String> changedFields, long changedBy, Instant changedAt, String note) {
}
