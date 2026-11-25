package com.home_platform.registry.maintenance;

import java.time.LocalDate;

import com.home_platform.registry.support.Text;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Toàn bộ phần sửa được của một dòng; món của dòng không đổi. */
public record MaintenanceUpdateRequest(@NotNull Integer version, @NotNull MaintenanceKind kind,
		@NotNull LocalDate doneOn, @NotBlank @Size(max = 500) String work, @Size(max = 150) String doneBy,
		@Size(max = 1000) String note) {

	public MaintenanceUpdateRequest {
		work = Text.trimToNull(work);
		doneBy = Text.trimToNull(doneBy);
		note = Text.trimToNull(note);
	}

}
