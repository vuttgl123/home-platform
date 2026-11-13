package com.home_platform.registry.person;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.home_platform.registry.occupancy.OccupancyResponse;

import com.home_platform.registry.support.PlainNumbers;
import tools.jackson.databind.annotation.JsonSerialize;

/** Một người đầy đủ, kèm những nơi người đó đang ở. */
public record PersonDetailResponse(long id, String fullName, String phone, String email, String idType,
		String idNumber, LocalDate dateOfBirth, Gender gender, String note,
		@JsonSerialize(contentUsing = PlainNumbers.class) Map<String, Object> attributes, Long avatarFileId,
		Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version, Instant deletedAt,
		List<OccupancyResponse> currentOccupancies) {
}
