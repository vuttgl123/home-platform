package com.home_platform.registry.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ServiceScheduleTests {

	private static final LocalDate CREATED = LocalDate.of(2025, 12, 1);

	@Test
	void noIntervalHasNoDate() {
		assertThat(ServiceSchedule.nextServiceOn(LocalDate.of(2026, 3, 10), null, CREATED, null)).isNull();
	}

	@Test
	void countsFromTheLastService() {
		assertThat(ServiceSchedule.nextServiceOn(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 1, 1), CREATED, 6))
			.isEqualTo(LocalDate.of(2026, 9, 10));
	}

	@Test
	void fallsBackToPlacedAtThenCreation() {
		assertThat(ServiceSchedule.nextServiceOn(null, LocalDate.of(2026, 1, 15), CREATED, 3))
			.isEqualTo(LocalDate.of(2026, 4, 15));
		assertThat(ServiceSchedule.nextServiceOn(null, null, CREATED, 3)).isEqualTo(LocalDate.of(2026, 3, 1));
	}

	@Test
	void endOfMonthClampsToTheLastDay() {
		assertThat(ServiceSchedule.nextServiceOn(LocalDate.of(2026, 1, 31), null, CREATED, 1))
			.isEqualTo(LocalDate.of(2026, 2, 28));
	}

}
