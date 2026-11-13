package com.home_platform.registry.place;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Bỏ xoá một địa điểm; {@code version} là version của dòng đã xoá. */
public record PlaceRestoreRequest(@NotNull Integer version, @Size(max = 255) String note) {

	public PlaceRestoreRequest {
		note = Text.trimToNull(note);
	}

}
