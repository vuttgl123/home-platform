package com.home_platform.registry.placetype;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Toàn bộ phần sửa được của một loại địa điểm; {@code code} không nằm trong đó. */
public record PlaceTypeUpdateRequest(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description,
		Integer sortOrder) {

	public PlaceTypeUpdateRequest {
		name = Text.trimToNull(name);
		description = Text.trimToNull(description);
		sortOrder = (sortOrder != null) ? sortOrder : 0;
	}

}
