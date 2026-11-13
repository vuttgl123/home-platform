package com.home_platform.registry.occupancy;

import java.time.Instant;
import java.time.LocalDate;

import lombok.With;

/**
 * Một dòng của {@code reg_occupancy}: người {@code personId} có quan hệ {@code relation} với địa
 * điểm {@code placeId} từ {@code startDate} tới {@code endDate} (trống là đang ở). Chuyển đi là
 * đặt {@code endDate}, không xoá dòng: lịch sử cư trú chính là các dòng này.
 */
@With
public record Occupancy(long workspaceId, long id, long placeId, long personId, Relation relation,
		LocalDate startDate, LocalDate endDate, String note, Instant createdAt, long createdBy, Instant updatedAt,
		long updatedBy, int version, Instant deletedAt) {
}
