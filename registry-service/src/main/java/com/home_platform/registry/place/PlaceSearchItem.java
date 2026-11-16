package com.home_platform.registry.place;

import java.util.List;

/**
 * Một kết quả tìm địa điểm: địa điểm, việc nó có con hay không, và tổ tiên của nó.
 *
 * @param ancestors từ gốc tới cha, để phân biệt hai địa điểm cùng tên ở hai nơi
 */
public record PlaceSearchItem(Place place, boolean hasChildren, List<PlaceRef> ancestors) {
}
