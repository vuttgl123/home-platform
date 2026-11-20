package com.home_platform.registry.overview;

import java.time.LocalDate;

/**
 * Số thay đổi (version) của một ngày theo giờ của không gian, chia theo hành động.
 * @param date ngày theo múi giờ nghiệp vụ ({@code registry.business-zone})
 */
public record DayActivity(LocalDate date, int created, int updated, int deleted, int restored) {
}
