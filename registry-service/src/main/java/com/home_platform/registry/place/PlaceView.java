package com.home_platform.registry.place;

import java.util.List;

/**
 * Một địa điểm cùng những gì cần để hiển thị nó.
 *
 * @param address địa chỉ của chính nó, có thể null
 * @param effectiveAddress địa chỉ của chính nó hoặc của tổ tiên gần nhất có địa chỉ, có thể null
 * @param effectiveAddressFrom địa điểm sở hữu {@code effectiveAddress}
 */
public record PlaceView(Place place, List<PlaceRef> ancestors, PlaceAddress address, PlaceAddress effectiveAddress,
		Long effectiveAddressFrom) {
}
