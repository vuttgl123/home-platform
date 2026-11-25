package com.home_platform.registry.maintenance;

import java.time.Instant;
import java.time.LocalDate;

public record MaintenanceResponse(long id, long itemId, MaintenanceKind kind, LocalDate doneOn, String work,
		String doneBy, String note, Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version) {
}
