package com.home_platform.registry.place;

/**
 * Địa chỉ dùng cho một địa điểm: của chính nó, hoặc của tổ tiên gần nhất có địa chỉ.
 *
 * @param fromPlaceId địa điểm sở hữu địa chỉ này
 */
public record EffectiveAddressResponse(long fromPlaceId, AddressResponse address) {
}
