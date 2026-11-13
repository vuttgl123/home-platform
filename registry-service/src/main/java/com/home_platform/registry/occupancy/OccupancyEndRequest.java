package com.home_platform.registry.occupancy;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/** Chuyển đi. @param endDate ngày cuối cùng còn ở; bỏ trống là hôm nay */
public record OccupancyEndRequest(@NotNull Integer version, LocalDate endDate) {
}
