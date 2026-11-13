package com.home_platform.registry.itemcategory;

import java.time.Instant;

import com.home_platform.registry.catalog.CatalogStatus;

public record ItemCategoryResponse(long id, String name, int sortOrder, CatalogStatus status, Instant createdAt,
		Instant updatedAt) {
}
