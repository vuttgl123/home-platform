package com.home_platform.registry.place;

/**
 * Một địa điểm trong danh sách, kèm việc nó có con hay không.
 *
 * @param hasChildren có ít nhất một địa điểm con chưa xoá
 */
public record PlaceListItem(Place place, boolean hasChildren) {
}
