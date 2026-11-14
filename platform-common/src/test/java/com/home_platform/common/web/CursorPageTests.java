package com.home_platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class CursorPageTests {

	@Test
	void ofCutsTheExtraRowAndEncodesTheLastKeptOne() {
		CursorPage<String> page = CursorPage.of(List.of("a", "b", "c"), 2, List::of);

		assertThat(page.items()).containsExactly("a", "b");
		assertThat(page.nextCursor()).isEqualTo(CursorCodec.encode(List.of("b")));
	}

	@Test
	void ofWithoutAnExtraRowHasNoCursor() {
		CursorPage<String> page = CursorPage.of(List.of("a", "b"), 2, List::of);

		assertThat(page.items()).containsExactly("a", "b");
		assertThat(page.nextCursor()).isNull();
	}

	@Test
	void ofRejectsANonPositiveLimit() {
		assertThatThrownBy(() -> CursorPage.of(List.of("a"), 0, List::of)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("limit must be positive: 0");
	}

	@Test
	void mapConvertsItemsAndKeepsCursor() {
		assertThat(new CursorPage<>(List.of(1, 2), "c").map(String::valueOf))
			.isEqualTo(new CursorPage<>(List.of("1", "2"), "c"));
	}

}
