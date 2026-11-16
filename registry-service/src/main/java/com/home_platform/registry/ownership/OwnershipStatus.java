package com.home_platform.registry.ownership;

import java.time.LocalDate;

/** Trạng thái suy ra từ ngày, không lưu; cùng luật với cư trú. {@code endDate} là ngày cuối cùng còn sở hữu. */
public enum OwnershipStatus {

	CURRENT, UPCOMING, PAST;

	public static OwnershipStatus of(LocalDate startDate, LocalDate endDate, LocalDate today) {
		if (startDate.isAfter(today)) {
			return UPCOMING;
		}
		return (endDate != null && endDate.isBefore(today)) ? PAST : CURRENT;
	}

}
