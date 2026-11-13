package com.home_platform.registry.place;

import java.math.BigDecimal;

public record AddressResponse(String countryCode, String provinceCode, String provinceName, String districtCode,
		String districtName, String wardCode, String wardName, String street, String fullAddress,
		BigDecimal latitude, BigDecimal longitude, String geohash) {
}
