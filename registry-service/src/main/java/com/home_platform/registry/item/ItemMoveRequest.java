package com.home_platform.registry.item;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ItemMoveRequest(@NotNull Integer version, @NotNull Long placeId, @Size(max = 255) String note) {

	public ItemMoveRequest {
		note = Text.trimToNull(note);
	}

}
