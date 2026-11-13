package com.home_platform.registry.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextTests {

	@Test
	void trimsAndTurnsBlankIntoNull() {
		assertThat(Text.trimToNull(null)).isNull();
		assertThat(Text.trimToNull("  a b  ")).isEqualTo("a b");
		assertThat(Text.trimToNull("   ")).isNull();
	}

}
