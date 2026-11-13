package com.home_platform.registry.place;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.With;

/** Một dòng của {@code reg_place_address}: địa chỉ của một địa điểm, tối đa một. */
@With
public record PlaceAddress(long workspaceId, long placeId, String countryCode, String provinceCode,
		String provinceName, String districtCode, String districtName, String wardCode, String wardName,
		String street, String fullAddress, BigDecimal latitude, BigDecimal longitude, String geohash,
		Instant createdAt, Instant updatedAt) {
}
