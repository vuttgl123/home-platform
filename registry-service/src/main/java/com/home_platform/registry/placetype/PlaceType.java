package com.home_platform.registry.placetype;

import java.time.Instant;

import com.home_platform.registry.catalog.CatalogStatus;
import lombok.With;

/** Một dòng của {@code reg_place_type}: loại địa điểm do người dùng định nghĩa. */
@With
public record PlaceType(long workspaceId, long id, String code, String name, String description, int sortOrder,
		CatalogStatus status, Instant createdAt, Instant updatedAt) {
}
