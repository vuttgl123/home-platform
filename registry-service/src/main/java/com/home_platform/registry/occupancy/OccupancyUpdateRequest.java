package com.home_platform.registry.occupancy;

import java.time.LocalDate;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Sửa một dòng nhập nhầm; người và địa điểm không đổi được. */
public record OccupancyUpdateRequest(@NotNull Integer version, @NotNull Relation relation,
		@NotNull LocalDate startDate, LocalDate endDate, @Size(max = 500) String note) {

	public OccupancyUpdateRequest {
		note = Text.trimToNull(note);
	}

}
