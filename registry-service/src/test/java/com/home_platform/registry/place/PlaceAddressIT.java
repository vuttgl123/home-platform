package com.home_platform.registry.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

import com.home_platform.common.history.EntityVersion;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class PlaceAddressIT extends PlaceTestData {

	private static final String TRANG_TIEN = """
			{"fullAddress": " 1 Tràng Tiền ", "provinceCode": "01", "latitude": 21.0245, "longitude": 105.8575}""";

	@Test
	void setsReplacesAndRemovesTheAddressAsPlaceVersions() throws Exception {
		long id = place(null, type("T"), "Nhà hát");

		setAddress(id, 1, TRANG_TIEN).andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(2))
			.andExpect(jsonPath("$.address.fullAddress").value("1 Tràng Tiền"))
			.andExpect(jsonPath("$.address.countryCode").value("VN"))
			.andExpect(jsonPath("$.address.geohash").value(org.hamcrest.Matchers.matchesPattern("[0-9b-hjkmnp-z]{9}")));
		assertThat(version(id, 2).snapshot().get("address").get("fullAddress").asString()).isEqualTo("1 Tràng Tiền");

		setAddress(id, 2, """
				{"fullAddress": "2 Tràng Tiền"}""").andExpect(jsonPath("$.version").value(3))
			.andExpect(jsonPath("$.address.fullAddress").value("2 Tràng Tiền"))
			.andExpect(jsonPath("$.address.latitude").value(nullValue()));

		removeAddress(id, 3).andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(4))
			.andExpect(jsonPath("$.address").value(nullValue()));
		EntityVersion v4 = version(id, 4);
		assertThat(v4.snapshot().get("address").isNull()).isTrue();
		assertThat(v4.changedFields()).containsExactly("address");

		removeAddress(id, 4).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Place " + id + " has no address"));
	}

	@Test
	void coordinatesComeInPairsAndInRange() throws Exception {
		long id = place(null, type("T"), "A");

		setAddress(id, 1, """
				{"fullAddress": "x", "latitude": 21.0}""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("latitude and longitude must be given together"));
		setAddress(id, 1, """
				{"fullAddress": "x", "latitude": 91, "longitude": 1}""").andExpect(status().isBadRequest());
		setAddress(id, 1, """
				{"fullAddress": "x", "countryCode": "VNM"}""").andExpect(status().isBadRequest());
		setAddress(id, 1, """
				{"fullAddress": "  "}""").andExpect(status().isBadRequest());
	}

	@Test
	void coordinatesAreRoundedToSixDecimals() throws Exception {
		long id = place(null, type("T"), "A");

		setAddress(id, 1, """
				{"fullAddress": "x", "latitude": 21.0245678, "longitude": 105.1}""")
			.andExpect(jsonPath("$.address.latitude").value(21.024568));
	}

	@Test
	void childUsesTheNearestAncestorsAddress() throws Exception {
		long type = type("T");
		long a = place(null, type, "A");
		long b = place(a, type, "B");
		long c = place(b, type, "C");
		long lone = place(null, type, "Lone");
		setAddress(a, 1, """
				{"fullAddress": "Địa chỉ A"}""").andExpect(status().isOk());
		setAddress(c, 1, """
				{"fullAddress": "Địa chỉ C"}""").andExpect(status().isOk());

		getPlace(b).andExpect(jsonPath("$.address").value(nullValue()))
			.andExpect(jsonPath("$.effectiveAddress.fromPlaceId").value(String.valueOf(a)))
			.andExpect(jsonPath("$.effectiveAddress.address.fullAddress").value("Địa chỉ A"));
		getPlace(c).andExpect(jsonPath("$.effectiveAddress.fromPlaceId").value(String.valueOf(c)));
		getPlace(lone).andExpect(jsonPath("$.effectiveAddress").value(nullValue()));
	}

	@Test
	void createWithAddressStoresBoth() throws Exception {
		long id = place("""
				{"placeTypeId": "%d", "name": "A", "address": %s}""".formatted(type("T"), TRANG_TIEN));

		getPlace(id).andExpect(jsonPath("$.version").value(1))
			.andExpect(jsonPath("$.address.provinceCode").value("01"));
		assertThat(version(id, 1).snapshot().get("address").get("provinceCode").asString()).isEqualTo("01");
	}

	@Test
	void staleVersionIsAConflict() throws Exception {
		long id = place(null, type("T"), "A");
		setAddress(id, 1, TRANG_TIEN).andExpect(status().isOk());

		setAddress(id, 1, """
				{"fullAddress": "khác"}""").andExpect(status().isConflict());
		getPlace(id).andExpect(jsonPath("$.address.fullAddress").value("1 Tràng Tiền"));
	}

	private ResultActions setAddress(long id, int version, String address) throws Exception {
		return this.mvc.perform(asA(put("/places/" + id + "/address")).contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"version": %d, "address": %s}""".formatted(version, address)));
	}

	private ResultActions removeAddress(long id, int version) throws Exception {
		return this.mvc.perform(asA(delete("/places/" + id + "/address")).param("version", String.valueOf(version)));
	}

}
