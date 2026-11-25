package com.home_platform.registry.place;

import java.util.Map;

import com.home_platform.registry.support.Text;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Không có {@code code}: mã do hệ thống cấp khi tạo; client gửi kèm thì bị bỏ qua.
 *
 * @param capacity sức chứa (số người), bỏ trống là chưa đặt
 * @param parentId địa điểm cha; bỏ trống là cấp cao nhất
 * @param address địa chỉ, tuỳ chọn; ghi cùng lúc với địa điểm
 * @param note ghi chú cho version được tạo
 */
public record PlaceCreateRequest(Long parentId, @NotNull Long placeTypeId,
		@NotBlank @Size(max = 255) String name, @Size(max = 10000) String description, PlaceStatus status,
		@Min(0) @Max(9999) Integer capacity,
		Map<String, Object> attributes, Long coverFileId, @Valid AddressRequest address,
		@Size(max = 255) String note) {

	public PlaceCreateRequest {
		name = Text.trimToNull(name);
		description = Text.trimToNull(description);
		status = (status != null) ? status : PlaceStatus.ACTIVE;
		note = Text.trimToNull(note);
	}

}
