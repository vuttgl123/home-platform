package com.home_platform.registry.person;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PersonRestoreRequest(@NotNull Integer version, @Size(max = 255) String note) {

	public PersonRestoreRequest {
		note = Text.trimToNull(note);
	}

}
