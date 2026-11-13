package com.home_platform.common.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;

class StorageJsonTests {

	private static final Type MAP = new ParameterizedTypeReference<Map<String, Object>>() {
	}.getType();

	private final StorageJson json = new StorageJson();

	@Test
	void numbersStayNumbers() {
		assertThat(this.json.write(Map.of("n", 9007199254740993L))).isEqualTo("{\"n\":9007199254740993}");
	}

	@Test
	void readsBackTypedValues() {
		Map<String, Object> map = this.json.read("{\"n\":9007199254740993}", MAP);

		assertThat(map.get("n")).isEqualTo(9007199254740993L);
	}

	@Test
	void convertsTreeToType() {
		assertThat(this.json.convert(this.json.readTree("{\"id\":7,\"name\":\"A\"}"), Pair.class))
			.isEqualTo(new Pair(7, "A"));
	}

	@Test
	void instantsAreIsoStrings() {
		String written = this.json.write(new Stamp(Instant.parse("2026-11-12T06:40:00.123Z")));

		assertThat(written).isEqualTo("{\"at\":\"2026-11-12T06:40:00.123Z\"}");
		assertThat(this.json.convert(this.json.readTree(written), Stamp.class))
			.isEqualTo(new Stamp(Instant.parse("2026-11-12T06:40:00.123Z")));
	}

	record Pair(long id, String name) {
	}

	record Stamp(Instant at) {
	}

}
