package com.home_platform.registry.place;

import java.util.Map;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param parentId địa điểm cha; bỏ trống là cấp cao nhất
 * @param address địa chỉ, tuỳ chọn; ghi cùng lúc với địa điểm
 * @param note ghi chú cho version được tạo
 */
public record PlaceCreateRequest(Long parentId, @NotNull Long placeTypeId, @Size(max = 50) String code,
		@NotBlank @Size(max = 255) String name, @Size(max = 10000) String description, PlaceStatus status,
		Map<String, Object> attributes, Long coverFileId, @Valid AddressRequest address,
		@Size(max = 255) String note) {

	public PlaceCreateRequest {
		code = Text.trimToNull(code);
		name = Text.trimToNull(name);
		description = Text.trimToNull(description);
		status = (status != null) ? status : PlaceStatus.ACTIVE;
		note = Text.trimToNull(note);
	}

}
