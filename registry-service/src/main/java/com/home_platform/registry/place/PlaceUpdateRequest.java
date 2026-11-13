package com.home_platform.registry.place;

import java.util.Map;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Toàn bộ phần sửa được của một địa điểm. Không có cha hay vị trí: đổi chỗ là thao tác "di
 * chuyển" riêng.
 *
 * @param version {@code version} đã đọc; lệch thì 409
 */
public record PlaceUpdateRequest(@NotNull Integer version, @NotNull Long placeTypeId, @Size(max = 50) String code,
		@NotBlank @Size(max = 255) String name, @Size(max = 10000) String description, @NotNull PlaceStatus status,
		Map<String, Object> attributes, Long coverFileId, @Size(max = 255) String note) {

	public PlaceUpdateRequest {
		code = Text.trimToNull(code);
		name = Text.trimToNull(name);
		description = Text.trimToNull(description);
		note = Text.trimToNull(note);
	}

}
