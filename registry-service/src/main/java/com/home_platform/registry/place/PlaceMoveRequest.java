package com.home_platform.registry.place;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** @param parentId cha mới; {@code null} là đưa lên cấp cao nhất */
public record PlaceMoveRequest(@NotNull Integer version, Long parentId, @Size(max = 255) String note) {

	public PlaceMoveRequest {
		note = Text.trimToNull(note);
	}

}
