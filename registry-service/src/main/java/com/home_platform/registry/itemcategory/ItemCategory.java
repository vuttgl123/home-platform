package com.home_platform.registry.itemcategory;

import java.time.Instant;

import com.home_platform.registry.catalog.CatalogStatus;
import lombok.With;

/** Một dòng của {@code reg_item_category}: nhóm đồ đạc do người dùng định nghĩa. */
@With
public record ItemCategory(long workspaceId, long id, String name, int sortOrder, CatalogStatus status,
		Instant createdAt, Instant updatedAt) {
}
