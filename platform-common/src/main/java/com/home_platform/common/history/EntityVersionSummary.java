package com.home_platform.common.history;

import java.time.Instant;
import java.util.List;

/**
 * Một version trong danh sách lịch sử, không kèm {@code snapshot} để danh sách nhẹ.
 *
 * @param changedFields tên các trường cấp một khác với version liền trước; {@code null} với
 * {@code CREATE} hoặc khi không có version liền trước
 */
public record EntityVersionSummary(int versionNo, ChangeAction action, List<String> changedFields, long changedBy,
		Instant changedAt, String note) {
}
