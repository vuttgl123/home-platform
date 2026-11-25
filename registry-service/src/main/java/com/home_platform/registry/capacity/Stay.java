package com.home_platform.registry.capacity;

import java.time.LocalDate;

/**
 * Một lần ở của một người tại địa điểm đang xét, dùng để tìm đỉnh.
 *
 * @param end ngày cuối cùng còn ở; {@code null} là chưa có ngày kết thúc
 */
record Stay(long personId, LocalDate start, LocalDate end) {
}
