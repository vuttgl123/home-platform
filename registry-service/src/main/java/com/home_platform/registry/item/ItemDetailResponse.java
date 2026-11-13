package com.home_platform.registry.item;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.home_platform.registry.support.PlainNumbers;
import tools.jackson.databind.annotation.JsonSerialize;

public record ItemDetailResponse(long id, long placeId, String placeName, Long categoryId, String categoryName,
		String code, String name, int quantity, String unit, ItemStatus status, LocalDate placedAt,
		LocalDate removedAt, Long replacedByItemId, String description,
		@JsonSerialize(contentUsing = PlainNumbers.class) Map<String, Object> attributes, Long coverFileId,
		Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version, Instant deletedAt) {
}
