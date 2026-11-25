package com.home_platform.registry.capacity;

import java.util.List;

import com.home_platform.registry.place.PlaceRef;
import com.home_platform.registry.place.PlaceStatus;

/**
 * Một địa điểm còn chỗ hôm nay (spec Đợt 6, L4).
 *
 * @param ancestors tổ tiên từ gốc tới cha, để hiện "Toà A › Tầng 1"
 * @param capacity sức chứa; {@code null} khi chưa đặt (địa điểm lá đang trống)
 * @param occupantCount số người đang ở hôm nay, mọi quan hệ
 * @param free số chỗ còn lại; {@code null} khi chưa đặt sức chứa
 */
public record VacancyResponse(long placeId, String code, String name, long placeTypeId, String placeTypeName,
		PlaceStatus status, List<PlaceRef> ancestors, Integer capacity, int occupantCount, Integer free) {
}
