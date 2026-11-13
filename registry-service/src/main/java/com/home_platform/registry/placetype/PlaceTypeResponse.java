package com.home_platform.registry.placetype;

import java.time.Instant;

import com.home_platform.registry.catalog.CatalogStatus;

public record PlaceTypeResponse(long id, String code, String name, String description, int sortOrder,
		CatalogStatus status, Instant createdAt, Instant updatedAt) {
}
