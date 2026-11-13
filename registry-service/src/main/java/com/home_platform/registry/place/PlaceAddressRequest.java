package com.home_platform.registry.place;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Đặt (tạo hoặc thay) địa chỉ của một địa điểm; là một lần sửa địa điểm nên cần {@code version}. */
public record PlaceAddressRequest(@NotNull Integer version, @NotNull @Valid AddressRequest address,
		@Size(max = 255) String note) {

	public PlaceAddressRequest {
		note = Text.trimToNull(note);
	}

}
