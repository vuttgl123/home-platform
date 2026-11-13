package com.home_platform.registry.place;

import java.math.BigDecimal;

/** Địa chỉ trong snapshot của một version địa điểm. */
public record AddressSnapshot(String countryCode, String provinceCode, String provinceName, String districtCode,
		String districtName, String wardCode, String wardName, String street, String fullAddress,
		BigDecimal latitude, BigDecimal longitude, String geohash) {

	public static AddressSnapshot of(PlaceAddress address) {
		if (address == null) {
			return null;
		}
		return new AddressSnapshot(address.countryCode(), address.provinceCode(), address.provinceName(),
				address.districtCode(), address.districtName(), address.wardCode(), address.wardName(),
				address.street(), address.fullAddress(), address.latitude(), address.longitude(), address.geohash());
	}

}
