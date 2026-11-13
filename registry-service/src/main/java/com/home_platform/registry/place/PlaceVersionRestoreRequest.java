package com.home_platform.registry.place;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** @param version {@code version} hiện tại của địa điểm (không phải số version muốn khôi phục) */
public record PlaceVersionRestoreRequest(@NotNull Integer version, @Size(max = 255) String note) {

	public PlaceVersionRestoreRequest {
		note = Text.trimToNull(note);
	}

}
