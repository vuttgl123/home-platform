package com.home_platform.registry.occupancy;

import java.time.LocalDate;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** @param endDate ngày cuối cùng còn ở; bỏ trống là đang ở */
public record OccupancyCreateRequest(@NotNull Long placeId, @NotNull Long personId, @NotNull Relation relation,
		@NotNull LocalDate startDate, LocalDate endDate, @Size(max = 500) String note) {

	public OccupancyCreateRequest {
		note = Text.trimToNull(note);
	}

}
