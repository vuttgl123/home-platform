package com.home_platform.registry.export;

import java.util.Map;

import com.home_platform.registry.place.PlaceStatus;

/**
 * Một địa điểm cho file Excel.
 *
 * @param capacity sức chứa, {@code null} khi chưa đặt
 * @param path đường dẫn {@code id} của địa điểm; thứ tự sắp và mốc đọc tiếp
 */
public record ExportPlaceRow(long id, String path, long placeTypeId, String code, String name, PlaceStatus status,
		Integer capacity, String description, Map<String, Object> attributes) {
}
