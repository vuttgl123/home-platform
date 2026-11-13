package com.home_platform.registry.fielddef;

import java.time.Instant;
import java.util.List;

import com.home_platform.registry.catalog.CatalogStatus;

public record FieldDefResponse(long id, FieldTarget target, long typeId, String fieldKey, String label,
		FieldDataType dataType, List<String> options, boolean required, int sortOrder, CatalogStatus status,
		Instant createdAt, Instant updatedAt) {
}
