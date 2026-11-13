package com.home_platform.registry.itemcategory;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dùng cho cả tạo và sửa một nhóm đồ. */
public record ItemCategoryRequest(@NotBlank @Size(max = 100) String name, Integer sortOrder) {

	public ItemCategoryRequest {
		name = Text.trimToNull(name);
		sortOrder = (sortOrder != null) ? sortOrder : 0;
	}

}
