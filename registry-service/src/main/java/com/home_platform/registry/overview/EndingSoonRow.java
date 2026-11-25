package com.home_platform.registry.overview;

import java.time.LocalDate;

/** Một người sắp chuyển đi: dòng cư trú đang ở, có ngày kết thúc trong 30 ngày tới. */
public record EndingSoonRow(long occupancyId, long personId, String personName, long placeId, String placeName,
		LocalDate endDate) {
}
