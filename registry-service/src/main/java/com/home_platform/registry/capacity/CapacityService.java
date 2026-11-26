package com.home_platform.registry.capacity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.registry.place.Place;
import com.home_platform.registry.place.PlaceRef;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.place.PlaceStatus;
import com.home_platform.registry.placetype.PlaceType;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.springframework.stereotype.Service;

/** Sức chứa và chỗ trống (spec Đợt 6, L3 và L4). Chỉ đọc. */
@Service
public class CapacityService {

	private final CapacityRepository repository;

	private final PlaceService places;

	private final PlaceTypeService placeTypes;

	CapacityService(CapacityRepository repository, PlaceService places, PlaceTypeService placeTypes) {
		this.repository = repository;
		this.places = places;
		this.placeTypes = placeTypes;
	}

	/**
	 * Địa điểm còn chỗ hôm nay, theo tên, kèm số người đang ở, tên loại và tổ tiên. Mỗi trang chạy
	 * một câu lọc và một câu đếm, không đếm từng dòng.
	 * @throws com.home_platform.common.web.NotFoundException nếu {@code rootId} không có hoặc đã xoá
	 */
	public CursorPage<VacancyResponse> vacancies(long workspaceId, Long placeTypeId, Long rootId,
			CursorRequest request) {
		return rows(workspaceId, this.places.vacancies(workspaceId, placeTypeId, rootId, request));
	}

	/**
	 * Bảng địa điểm theo bộ lọc (spec Đợt 11, S3), cùng dạng dòng với chỗ trống; tham số như
	 * {@link PlaceService#list}.
	 */
	public CursorPage<VacancyResponse> list(long workspaceId, String q, Long typeId, PlaceStatus status, Long rootId,
			boolean hasRoom, List<String> attr, CursorRequest request) {
		return rows(workspaceId, this.places.list(workspaceId, q, typeId, status, rootId, hasRoom, attr, request));
	}

	/** Kèm số người đang ở, tên loại và tổ tiên; mỗi trang một câu đếm và một câu tên, không đếm từng dòng. */
	private CursorPage<VacancyResponse> rows(long workspaceId, CursorPage<Place> page) {
		Map<Long, Integer> living = this.places.peopleLiving(workspaceId,
				page.items().stream().map(Place::id).toList());
		Map<Long, String> typeNames = this.placeTypes.list(workspaceId)
			.stream()
			.collect(Collectors.toMap(PlaceType::id, PlaceType::name));
		List<Long> ancestorIds = page.items()
			.stream()
			.flatMap(place -> ancestorIds(place).stream())
			.distinct()
			.toList();
		Map<Long, String> names = this.places.names(workspaceId, ancestorIds);
		return page.map(place -> {
			int occupants = living.getOrDefault(place.id(), 0);
			List<PlaceRef> ancestors = ancestorIds(place).stream()
				.map(id -> new PlaceRef(id, names.get(id)))
				.toList();
			Integer free = (place.capacity() != null) ? place.capacity() - occupants : null;
			return new VacancyResponse(place.id(), place.code(), place.name(), place.placeTypeId(),
					typeNames.get(place.placeTypeId()), place.status(), ancestors, place.capacity(), occupants, free);
		});
	}

	private static List<Long> ancestorIds(Place place) {
		List<Long> ids = place.pathIds();
		return ids.subList(0, ids.size() - 1);
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
