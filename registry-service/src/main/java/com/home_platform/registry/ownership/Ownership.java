package com.home_platform.registry.ownership;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import lombok.With;

/**
 * Một dòng của {@code reg_ownership}: người {@code personId} sở hữu {@code sharePercent} phần trăm
 * (trống là không ghi) của địa điểm {@code placeId} từ {@code startDate} tới {@code endDate} (ngày
 * cuối cùng còn sở hữu; trống là đang sở hữu). Lịch sử sở hữu chính là các dòng này.
 */
@With
public record Ownership(long workspaceId, long id, long placeId, long personId, BigDecimal sharePercent,
		LocalDate startDate, LocalDate endDate, String documentNo, String note, Instant createdAt, long createdBy,
		Instant updatedAt, long updatedBy, int version, Instant deletedAt) {
}
