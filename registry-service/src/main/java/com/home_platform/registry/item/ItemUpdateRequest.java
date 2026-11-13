package com.home_platform.registry.item;

import java.time.LocalDate;
import java.util.Map;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Toàn bộ phần sửa được của một món; chỗ đặt và trạng thái gỡ có thao tác riêng. */
public record ItemUpdateRequest(@NotNull Integer version, Long categoryId, @Size(max = 50) String code,
		@NotBlank @Size(max = 255) String name, @Min(1) Integer quantity, @Size(max = 20) String unit,
		@NotNull ItemStatus status, LocalDate placedAt, @Size(max = 10000) String description,
		Map<String, Object> attributes, Long coverFileId, @Size(max = 255) String note) {

	public ItemUpdateRequest {
		code = Text.trimToNull(code);
		name = Text.trimToNull(name);
		quantity = (quantity != null) ? quantity : 1;
		unit = Text.trimToNull(unit);
		description = Text.trimToNull(description);
		note = Text.trimToNull(note);
	}

}
