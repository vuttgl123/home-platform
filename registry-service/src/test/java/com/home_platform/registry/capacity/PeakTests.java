package com.home_platform.registry.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Đỉnh: số người nhiều nhất có mặt cùng một ngày trong một khoảng ngày (spec Đợt 6, L3). */
class PeakTests {

	private static final LocalDate DAY_1 = LocalDate.of(2026, 12, 1);

	@Test
	void emptyRangeHasNoPeak() {
		assertThat(CapacityService.peak(List.of(), day(1), day(31))).isEqualTo(new Peak(0, null));
	}

	@Test
	void overlappingStaysPeakOnTheirCommonDay() {
		List<Stay> stays = List.of(new Stay(1, day(1), day(10)), new Stay(2, day(5), day(20)),
				new Stay(3, day(8), day(9)));
		assertThat(CapacityService.peak(stays, day(1), day(31))).isEqualTo(new Peak(3, day(8)));
	}

	@Test
	void backToBackStaysCountOnce() {
		List<Stay> stays = List.of(new Stay(1, day(1), day(10)), new Stay(1, day(11), day(20)));
		assertThat(CapacityService.peak(stays, day(1), day(31))).isEqualTo(new Peak(1, day(1)));
	}

	@Test
	void peakOutsideTheRangeIsIgnored() {
		List<Stay> stays = List.of(new Stay(1, day(1), day(3)), new Stay(2, day(1), day(3)),
				new Stay(3, day(6), day(7)));
		assertThat(CapacityService.peak(stays, day(5), day(9))).isEqualTo(new Peak(1, day(6)));
	}

	@Test
	void openRangeSeesLaterStays() {
		List<Stay> stays = List.of(new Stay(1, day(100), null), new Stay(2, day(150), day(200)));
		assertThat(CapacityService.peak(stays, day(1), null)).isEqualTo(new Peak(2, day(150)));
	}

	/** Ngày thứ {@code n} tính từ 01/12/2026 (ngày 1 là 01/12). */
	private static LocalDate day(int n) {
		return DAY_1.plusDays(n - 1);
	}

}
