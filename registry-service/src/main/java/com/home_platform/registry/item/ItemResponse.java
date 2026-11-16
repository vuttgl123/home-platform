package com.home_platform.registry.item;

import java.time.LocalDate;

/** Một món trong danh sách. */
public record ItemResponse(long id, long placeId, String placeName, Long categoryId, String categoryName,
		Long ownerPersonId, String ownerName, String code,
		String name, int quantity, String unit, ItemStatus status, LocalDate removedAt, int version) {
}
