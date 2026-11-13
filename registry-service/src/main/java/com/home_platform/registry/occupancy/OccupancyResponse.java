package com.home_platform.registry.occupancy;

import java.time.Instant;
import java.time.LocalDate;

public record OccupancyResponse(long id, long placeId, String placeName, long personId, String personFullName,
		Relation relation, LocalDate startDate, LocalDate endDate, OccupancyStatus status, String note,
		Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version) {
}
