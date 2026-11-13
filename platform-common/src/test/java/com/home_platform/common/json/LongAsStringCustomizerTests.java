package com.home_platform.common.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.home_platform.common.autoconfigure.PlatformCommonAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class LongAsStringCustomizerTests {

	private static final long BEYOND_JS_PRECISION = 9007199254740993L;

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PlatformCommonAutoConfiguration.class, CacheAutoConfiguration.class,
				JacksonAutoConfiguration.class));

	@Test
	void longsAreWrittenAsStringsAndIntsAsNumbers() {
		this.runner.run(context -> {
			JsonMapper mapper = context.getBean(JsonMapper.class);
			Sample sample = new Sample(BEYOND_JS_PRECISION, null, 3, Instant.parse("2026-11-12T06:40:00.123Z"),
					LocalDate.of(2026, 11, 12), Map.of("big", BEYOND_JS_PRECISION));

			JsonNode json = mapper.readTree(mapper.writeValueAsString(sample));

			assertThat(json.get("id").isString()).isTrue();
			assertThat(json.get("id").asString()).isEqualTo("9007199254740993");
			assertThat(json.get("parentId").isNull()).isTrue();
			assertThat(json.get("count").isNumber()).isTrue();
			assertThat(json.get("count").intValue()).isEqualTo(3);
			assertThat(json.get("at").asString()).isEqualTo("2026-11-12T06:40:00.123Z");
			assertThat(json.get("day").asString()).isEqualTo("2026-11-12");
			assertThat(json.get("attrs").get("big").isString()).isTrue();
			assertThat(json.get("attrs").get("big").asString()).isEqualTo("9007199254740993");
		});
	}

	@Test
	void longsAreReadFromStringsAndFromNumbers() {
		this.runner.run(context -> {
			JsonMapper mapper = context.getBean(JsonMapper.class);

			Sample sample = mapper.readValue("{\"id\":\"9007199254740993\",\"parentId\":5,\"count\":3}", Sample.class);

			assertThat(sample.id()).isEqualTo(BEYOND_JS_PRECISION);
			assertThat(sample.parentId()).isEqualTo(5L);
			assertThat(sample.count()).isEqualTo(3);
		});
	}

	record Sample(long id, Long parentId, int count, Instant at, LocalDate day, Map<String, Object> attrs) {
	}

}
