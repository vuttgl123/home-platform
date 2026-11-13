package com.home_platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CursorCodecTests {

	@Test
	void roundTripsPartsIncludingVietnamese() {
		List<String> parts = List.of("Phòng họp – tầng 3", "123");

		String cursor = CursorCodec.encode(parts);

		assertThat(cursor).matches("[A-Za-z0-9_-]+");
		assertThat(CursorCodec.decode(cursor)).isEqualTo(parts);
	}

	@ParameterizedTest
	@MethodSource("garbage")
	void garbageIsRejectedAs400(String cursor) {
		assertThatThrownBy(() -> CursorCodec.decode(cursor)).isInstanceOf(BadRequestException.class)
			.satisfies(ex -> assertThat(((BadRequestException) ex).getBody().getDetail()).isEqualTo("Invalid cursor"));
	}

	static List<String> garbage() {
		return List.of("", "!!!", base64("{\"a\":1}"), base64("[1,2]"), base64("\"abc\""), base64("[\"a\",null]"),
				base64("[]"));
	}

	private static String base64(String text) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
	}

}
