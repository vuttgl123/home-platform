package com.home_platform.registry.place;

import java.util.List;

/** Một địa điểm cùng những gì cần để hiển thị nó. */
public record PlaceView(Place place, List<PlaceRef> ancestors) {
}
