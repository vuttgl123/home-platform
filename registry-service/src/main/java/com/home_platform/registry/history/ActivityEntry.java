package com.home_platform.registry.history;

import java.time.Instant;
import java.util.List;

import com.home_platform.common.history.ChangeAction;

/** Một thay đổi trong không gian: một dòng của bảng version, không kèm snapshot. */
public record ActivityEntry(String entityType, long entityId, int versionNo, ChangeAction action,
		List<String> changedFields, long changedBy, Instant changedAt, String note) {
}
