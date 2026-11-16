package com.home_platform.registry.ownership;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OwnershipResponse(long id, long placeId, String placeName, long personId, String personFullName,
		BigDecimal sharePercent, LocalDate startDate, LocalDate endDate, OwnershipStatus status, String documentNo,
		String note, Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version) {
}
