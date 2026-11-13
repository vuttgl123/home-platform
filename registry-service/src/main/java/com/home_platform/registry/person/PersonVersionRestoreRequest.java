package com.home_platform.registry.person;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** @param version {@code version} hiện tại của người (không phải số version muốn khôi phục) */
public record PersonVersionRestoreRequest(@NotNull Integer version, @Size(max = 255) String note) {

	public PersonVersionRestoreRequest {
		note = Text.trimToNull(note);
	}

}
