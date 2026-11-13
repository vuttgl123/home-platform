package com.home_platform.registry.person;

import java.time.Instant;
import java.util.List;

import com.home_platform.common.history.ChangeAction;

public record PersonVersionResponse(int versionNo, ChangeAction action, PersonSnapshot snapshot,
		List<String> changedFields, long changedBy, Instant changedAt, String note) {
}
