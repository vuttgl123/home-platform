package com.home_platform.registry.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class TextSearchTests {

	@Test
	void patternContainsTheTrimmedText() {
		assertThat(TextSearch.pattern("  ab ")).isEqualTo("%ab%");
	}

	@Test
	void shortOrBlankTextIsNoSearch() {
		assertThat(TextSearch.pattern("a")).isNull();
		assertThat(TextSearch.pattern(" ")).isNull();
		assertThat(TextSearch.pattern(null)).isNull();
	}

	@Test
	void likeCharactersAreLiteral() {
		assertThat(TextSearch.pattern("5%_\\")).isEqualTo("%5\\%\\_\\\\%");
	}

	@Test
	void anyColumnMayContainIt() {
		SqlCondition condition = TextSearch.anyColumnContains("%ab%", "name", "code");

		assertThat(condition.sql()).isEqualTo("(name LIKE :text OR code LIKE :text)");
		assertThat(condition.params()).isEqualTo(Map.of("text", "%ab%"));
	}

}
