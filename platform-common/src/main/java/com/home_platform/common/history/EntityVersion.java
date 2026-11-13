package com.home_platform.common.history;

import java.time.Instant;
import java.util.List;

import tools.jackson.databind.JsonNode;

/**
 * Một version đầy đủ, kèm {@code snapshot} ở dạng cây JSON đúng như đã lưu (số vẫn là số).
 * Đổi về kiểu Java bằng {@link EntityHistory#snapshotAs}; không trả thẳng cây này ra API.
 *
 * @param changedFields xem {@link EntityVersionSummary#changedFields()}
 */
public record EntityVersion(int versionNo, ChangeAction action, JsonNode snapshot, List<String> changedFields,
		long changedBy, Instant changedAt, String note) {
}
