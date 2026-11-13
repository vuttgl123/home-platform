package com.home_platform.registry.fielddef;

import java.time.Instant;
import java.util.List;

import com.home_platform.common.jdbc.JsonColumn;
import com.home_platform.registry.catalog.CatalogStatus;
import lombok.With;

/**
 * Một dòng của {@code reg_field_def}: một mục thông tin tuỳ biến.
 *
 * @param typeId 0 là áp dụng cho mọi bản ghi của {@code target}; khác 0 là {@code id} loại địa
 * điểm (PLACE) hoặc nhóm đồ (ITEM)
 * @param options các lựa chọn của mục SELECT; {@code null} với kiểu khác
 */
@With
public record FieldDef(long workspaceId, long id, FieldTarget target, long typeId, String fieldKey, String label,
		FieldDataType dataType, @JsonColumn List<String> options, boolean isRequired, int sortOrder,
		CatalogStatus status, Instant createdAt, Instant updatedAt) {
}
