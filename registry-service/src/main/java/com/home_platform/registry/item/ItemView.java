package com.home_platform.registry.item;

import java.time.LocalDate;

/**
 * Một món đồ cùng tên địa điểm, tên nhóm và tên chủ của nó.
 * @param nextServiceOn hạn bảo trì kế tiếp ({@link ServiceSchedule}); {@code null} khi chưa đặt chu kỳ
 */
public record ItemView(Item item, String placeName, String categoryName, String ownerName, LocalDate nextServiceOn) {
}
