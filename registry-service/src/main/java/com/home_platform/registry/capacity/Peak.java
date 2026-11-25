package com.home_platform.registry.capacity;

import java.time.LocalDate;

/**
 * Số người nhiều nhất có mặt cùng một ngày trong một khoảng ngày.
 *
 * @param date ngày đầu tiên đạt {@code count}; {@code null} khi {@code count} là 0
 */
record Peak(int count, LocalDate date) {
}
