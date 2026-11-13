package com.home_platform.registry.item;

import java.time.Instant;
import java.util.List;

import com.home_platform.common.history.ChangeAction;

public record ItemVersionResponse(int versionNo, ChangeAction action, ItemSnapshot snapshot,
		List<String> changedFields, long changedBy, Instant changedAt, String note) {
}
