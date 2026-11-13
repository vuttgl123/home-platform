package com.home_platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CursorPageTests {

	@Test
	void mapConvertsItemsAndKeepsCursor() {
		assertThat(new CursorPage<>(List.of(1, 2), "c").map(String::valueOf))
			.isEqualTo(new CursorPage<>(List.of("1", "2"), "c"));
	}

}
