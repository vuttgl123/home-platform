package com.home_platform.registry.overview;

/**
 * Số địa điểm của một loại.
 * @param total số địa điểm chưa xoá thuộc loại này
 * @param occupied trong đó, số địa điểm có ít nhất một người đang ở hôm nay; chỗ trống là
 * {@code total - occupied}
 */
public record PlaceTypeCount(long typeId, String name, int total, int occupied) {
}
