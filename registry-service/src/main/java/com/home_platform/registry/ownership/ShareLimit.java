package com.home_platform.registry.ownership;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/** Luật "tổng tỷ lệ sở hữu của một địa điểm không vượt 100% trong bất kỳ ngày nào". */
final class ShareLimit {

	private static final BigDecimal LIMIT = new BigDecimal("100");

	private ShareLimit() {
	}

	/** Ngày đầu tiên tổng vượt 100, và tổng của ngày đó. */
	record Overflow(LocalDate date, BigDecimal total) {
	}

	/**
	 * Ngày đầu tiên trong {@code [start, end]} ({@code end} trống là vô hạn) mà tổng tỷ lệ của các dòng
	 * {@code others} đang có hiệu lực cộng {@code share} vượt 100. Tổng chỉ tăng ở ngày bắt đầu của một
	 * dòng, nên chỉ xét {@code start} và ngày bắt đầu của các dòng khác nằm trong khoảng đó.
	 * @param share {@code null} là dòng không ghi tỷ lệ: không bao giờ vượt
	 */
	static Optional<Overflow> firstOverflow(List<Ownership> others, BigDecimal share, LocalDate start, LocalDate end) {
		if (share == null) {
			return Optional.empty();
		}
		TreeSet<LocalDate> days = new TreeSet<>();
		days.add(start);
		for (Ownership other : others) {
			if (other.startDate().isAfter(start) && (end == null || !other.startDate().isAfter(end))) {
				days.add(other.startDate());
			}
		}
		for (LocalDate day : days) {
			BigDecimal total = share;
			for (Ownership other : others) {
				if (other.sharePercent() != null && covers(other, day)) {
					total = total.add(other.sharePercent());
				}
			}
			if (total.compareTo(LIMIT) > 0) {
				return Optional.of(new Overflow(day, total));
			}
		}
		return Optional.empty();
	}

	private static boolean covers(Ownership row, LocalDate day) {
		return !row.startDate().isAfter(day) && (row.endDate() == null || !row.endDate().isBefore(day));
	}

}
