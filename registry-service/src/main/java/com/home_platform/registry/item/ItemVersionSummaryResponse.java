package com.home_platform.registry.item;

import java.time.Instant;
import java.util.List;

import com.home_platform.common.history.ChangeAction;

public record ItemVersionSummaryResponse(int versionNo, ChangeAction action, List<String> changedFields,
		long changedBy, Instant changedAt, String note) {
}
