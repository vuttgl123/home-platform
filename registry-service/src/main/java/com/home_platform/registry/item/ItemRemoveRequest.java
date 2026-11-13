package com.home_platform.registry.item;

import java.time.LocalDate;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Gỡ một món. @param removedAt ngày gỡ; bỏ trống là hôm nay */
public record ItemRemoveRequest(@NotNull Integer version, LocalDate removedAt, @Size(max = 255) String note) {

	public ItemRemoveRequest {
		note = Text.trimToNull(note);
	}

}
