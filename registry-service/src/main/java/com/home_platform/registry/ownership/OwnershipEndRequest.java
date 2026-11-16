package com.home_platform.registry.ownership;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/** Thôi sở hữu. @param endDate ngày cuối cùng còn sở hữu; trống là hôm nay */
public record OwnershipEndRequest(@NotNull Integer version, LocalDate endDate) {
}
