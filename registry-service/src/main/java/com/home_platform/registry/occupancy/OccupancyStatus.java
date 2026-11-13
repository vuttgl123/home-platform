package com.home_platform.registry.occupancy;

import java.time.LocalDate;

/** Trạng thái suy ra từ ngày, không lưu. {@code endDate} là ngày cuối cùng còn ở. */
public enum OccupancyStatus {

	CURRENT, UPCOMING, PAST;

	public static OccupancyStatus of(LocalDate startDate, LocalDate endDate, LocalDate today) {
		if (startDate.isAfter(today)) {
			return UPCOMING;
		}
		return (endDate != null && endDate.isBefore(today)) ? PAST : CURRENT;
	}

}
