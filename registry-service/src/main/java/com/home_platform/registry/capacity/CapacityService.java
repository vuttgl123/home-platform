package com.home_platform.registry.capacity;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeSet;

import com.home_platform.common.web.BadRequestException;
import com.home_platform.registry.place.Place;
import com.home_platform.registry.place.PlaceService;
import org.springframework.stereotype.Service;

/** Sức chứa và chỗ trống (spec Đợt 6, L3 và L4). Chỉ đọc. */
@Service
public class CapacityService {

	private final CapacityRepository repository;

	private final PlaceService places;

	CapacityService(CapacityRepository repository, PlaceService places) {
		this.repository = repository;
		this.places = places;
	}

	/**
	 * Sức chứa và đỉnh của một địa điểm trong khoảng {@code from} tới {@code to}.
	 * @param to {@code null} là không có ngày kết thúc
	 * @param excludeId dòng cư trú không tính (dòng đang sửa)
	 * @throws com.home_platform.common.web.NotFoundException nếu địa điểm không có hoặc đã xoá
	 * @throws BadRequestException nếu {@code to} trước {@code from}
	 */
	public CapacityCheckResponse check(long workspaceId, long placeId, LocalDate from, LocalDate to, Long excludeId) {
		if (to != null && to.isBefore(from)) {
			throw new BadRequestException("endDate must not be before startDate");
		}
		Place place = this.places.get(workspaceId, placeId);
		Peak peak = peak(this.repository.stays(workspaceId, placeId, from, to, excludeId), from, to);
		return new CapacityCheckResponse(place.capacity(), peak.count(), peak.date());
	}

	/**
	 * Số người khác nhau nhiều nhất có mặt cùng một ngày trong khoảng, và ngày đầu tiên đạt số đó. Số
	 * người chỉ tăng vào ngày một lần ở bắt đầu, nên chỉ cần xét ngày {@code from} và các ngày bắt
	 * đầu nằm trong khoảng.
	 * @param to {@code null} là không có ngày kết thúc
	 */
	static Peak peak(List<Stay> stays, LocalDate from, LocalDate to) {
		TreeSet<LocalDate> days = new TreeSet<>();
		days.add(from);
		for (Stay stay : stays) {
			if (!stay.start().isBefore(from) && (to == null || !stay.start().isAfter(to))) {
				days.add(stay.start());
			}
		}
		Peak best = new Peak(0, null);
		for (LocalDate day : days) {
			long count = stays.stream()
				.filter(stay -> !stay.start().isAfter(day) && (stay.end() == null || !stay.end().isBefore(day)))
				.mapToLong(Stay::personId)
				.distinct()
				.count();
			if (count > best.count()) {
				best = new Peak((int) count, day);
			}
		}
		return best;
	}

}
