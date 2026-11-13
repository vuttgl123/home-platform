package com.home_platform.registry.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class BusinessDateTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-13T18:30:00Z"), ZoneOffset.UTC);

	@Test
	void todayFollowsTheBusinessZone() {
		assertThat(new BusinessDate(this.clock, ZoneId.of("Asia/Ho_Chi_Minh")).today())
			.isEqualTo(LocalDate.of(2026, 11, 14));
		assertThat(new BusinessDate(this.clock, ZoneOffset.UTC).today()).isEqualTo(LocalDate.of(2026, 11, 13));
	}

}
