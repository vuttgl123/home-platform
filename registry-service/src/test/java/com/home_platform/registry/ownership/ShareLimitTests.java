package com.home_platform.registry.ownership;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class ShareLimitTests {

	private static final LocalDate JAN = LocalDate.of(2026, 1, 1);

	@Test
	void noShareMeansNoLimit() {
		assertThat(ShareLimit.firstOverflow(List.of(row("90", JAN, null)), null, JAN, null)).isEmpty();
	}

	@Test
	void overlappingSharesOverOneHundredOverflowOnTheFirstSharedDay() {
		assertThat(ShareLimit.firstOverflow(List.of(row("60", JAN, null)), new BigDecimal("50"), LocalDate.of(2026, 6, 1), null))
			.hasValueSatisfying(overflow -> {
				assertThat(overflow.date()).isEqualTo(LocalDate.of(2026, 6, 1));
				assertThat(overflow.total()).isEqualByComparingTo("110");
			});
	}

	@Test
	void sharesThatNeverMeetDoNotAddUp() {
		List<Ownership> others = List.of(row("60", JAN, LocalDate.of(2026, 6, 30)), row("60", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31)));
		assertThat(ShareLimit.firstOverflow(others, new BigDecimal("40"), JAN, LocalDate.of(2026, 12, 31))).isEmpty();
	}

	@Test
	void overflowCanStartLaterThanTheNewRow() {
		List<Ownership> others = List.of(row("40", JAN, LocalDate.of(2026, 6, 30)), row("60", LocalDate.of(2026, 7, 1), null));
		assertThat(ShareLimit.firstOverflow(others, new BigDecimal("50"), JAN, LocalDate.of(2026, 12, 31)))
			.hasValueSatisfying(overflow -> {
				assertThat(overflow.date()).isEqualTo(LocalDate.of(2026, 7, 1));
				assertThat(overflow.total()).isEqualByComparingTo("110");
			});
	}

	@Test
	void exactlyOneHundredIsAllowed() {
		assertThat(ShareLimit.firstOverflow(List.of(row("60", JAN, null)), new BigDecimal("40"), JAN, null)).isEmpty();
	}

	@Test
	void openEndedRowsRunForever() {
		assertThat(ShareLimit.firstOverflow(List.of(row("60", LocalDate.of(2040, 1, 1), null)), new BigDecimal("50"), JAN, null))
			.hasValueSatisfying(overflow -> assertThat(overflow.date()).isEqualTo(LocalDate.of(2040, 1, 1)));
	}

	private static Ownership row(String share, LocalDate start, LocalDate end) {
		return new Ownership(-1, 1, 10, 20, new BigDecimal(share), start, end, null, null, null, 0, null, 0, 1, null);
	}

}
