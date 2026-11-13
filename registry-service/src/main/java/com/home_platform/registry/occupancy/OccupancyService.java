package com.home_platform.registry.occupancy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.ConflictException;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.registry.person.PersonService;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.support.BusinessDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cư trú: ai ở đâu, từ khi nào tới khi nào. Không có version riêng trong bảng version; lịch
 * sử của cư trú là chính các dòng. Một người không có hai dòng chưa xoá ở cùng một địa điểm
 * với khoảng thời gian giao nhau; ở nhiều nơi cùng lúc thì được.
 */
@Service
public class OccupancyService {

	private final OccupancyRepository repository;

	private final PlaceService places;

	private final PersonService people;

	private final BusinessDate businessDate;

	OccupancyService(OccupancyRepository repository, PlaceService places, PersonService people,
			BusinessDate businessDate) {
		this.repository = repository;
		this.places = places;
		this.people = people;
		this.businessDate = businessDate;
	}

	public Occupancy get(long workspaceId, long id) {
		return this.repository.getById(workspaceId, id);
	}

	public OccupancyView view(long workspaceId, long id) {
		return views(workspaceId, List.of(get(workspaceId, id))).get(0);
	}

	/** @throws com.home_platform.common.web.NotFoundException nếu địa điểm không có hoặc đã xoá */
	public CursorPage<OccupancyView> atPlace(long workspaceId, long placeId, OccupancyStatus status,
			CursorRequest page) {
		this.places.get(workspaceId, placeId);
		return withViews(workspaceId,
				this.repository.atPlace(workspaceId, placeId, status, this.businessDate.today(), page));
	}

	/** Người đang ở tại địa điểm gốc hoặc bất kỳ hậu duệ chưa xoá nào, ví dụ cả một toà nhà. */
	public CursorPage<OccupancyView> currentInSubtree(long workspaceId, long rootPlaceId, CursorRequest page) {
		String rootPath = this.places.get(workspaceId, rootPlaceId).path();
		return withViews(workspaceId,
				this.repository.currentInSubtree(workspaceId, rootPath, this.businessDate.today(), page));
	}

	/** Mọi nơi một người đã và đang ở, mới nhất trước. */
	public CursorPage<OccupancyView> ofPerson(long workspaceId, long personId, CursorRequest page) {
		this.people.get(workspaceId, personId);
		return withViews(workspaceId, this.repository.ofPerson(workspaceId, personId, page));
	}

	public List<OccupancyView> currentOfPerson(long workspaceId, long personId) {
		return views(workspaceId,
				this.repository.currentOfPerson(workspaceId, personId, this.businessDate.today()));
	}

	private CursorPage<OccupancyView> withViews(long workspaceId, CursorPage<Occupancy> page) {
		return new CursorPage<>(views(workspaceId, page.items()), page.nextCursor());
	}

	/** Gắn tên địa điểm, tên người (lấy theo lô) và trạng thái hôm nay vào từng dòng. */
	private List<OccupancyView> views(long workspaceId, List<Occupancy> rows) {
		Map<Long, String> placeNames = this.places.names(workspaceId,
				rows.stream().map(Occupancy::placeId).distinct().toList());
		Map<Long, String> personNames = this.people.names(workspaceId,
				rows.stream().map(Occupancy::personId).distinct().toList());
		LocalDate today = this.businessDate.today();
		return rows.stream()
			.map(row -> new OccupancyView(row, placeNames.get(row.placeId()), personNames.get(row.personId()),
					OccupancyStatus.of(row.startDate(), row.endDate(), today)))
			.toList();
	}

	/** @throws com.home_platform.common.web.NotFoundException nếu địa điểm hoặc người không có hay đã xoá */
	@Transactional
	public Occupancy create(WorkspaceContext ctx, OccupancyCreateRequest request) {
		this.places.get(ctx.workspaceId(), request.placeId());
		this.people.get(ctx.workspaceId(), request.personId());
		requireValidRange(ctx.workspaceId(), request.personId(), request.placeId(), request.startDate(),
				request.endDate(), 0);
		return this.repository.insert(ctx, new Occupancy(0, 0, request.placeId(), request.personId(),
				request.relation(), request.startDate(), request.endDate(), request.note(), null, 0, null, 0, 0, null));
	}

	/** Sửa một dòng nhập nhầm. */
	@Transactional
	public Occupancy update(WorkspaceContext ctx, long id, OccupancyUpdateRequest request) {
		Occupancy current = get(ctx.workspaceId(), id);
		requireValidRange(ctx.workspaceId(), current.personId(), current.placeId(), request.startDate(),
				request.endDate(), id);
		return this.repository.update(ctx, current.withVersion(request.version())
			.withRelation(request.relation())
			.withStartDate(request.startDate())
			.withEndDate(request.endDate())
			.withNote(request.note()));
	}

	/**
	 * Chuyển đi: đặt ngày cuối cùng còn ở. Cách kết thúc cư trú bình thường; dòng không bị xoá.
	 * @throws ConflictException nếu dòng đã có ngày kết thúc
	 */
	@Transactional
	public Occupancy end(WorkspaceContext ctx, long id, OccupancyEndRequest request) {
		Occupancy current = get(ctx.workspaceId(), id);
		if (current.endDate() != null) {
			throw new ConflictException("Occupancy " + id + " has already ended");
		}
		LocalDate endDate = (request.endDate() != null) ? request.endDate() : this.businessDate.today();
		requireOrdered(current.startDate(), endDate);
		return this.repository.update(ctx, current.withVersion(request.version()).withEndDate(endDate));
	}

	/** Xoá mềm; chỉ dành cho dòng nhập nhầm. Người chuyển đi thì dùng {@link #end}. */
	@Transactional
	public void delete(WorkspaceContext ctx, long id, int version) {
		get(ctx.workspaceId(), id);
		this.repository.softDelete(ctx, id, version);
	}

	private void requireValidRange(long workspaceId, long personId, long placeId, LocalDate start, LocalDate end,
			long selfId) {
		requireOrdered(start, end);
		if (this.repository.overlaps(workspaceId, personId, placeId, start, end, selfId)) {
			throw new ConflictException("Person " + personId + " already has an overlapping occupancy at this place");
		}
	}

	private static void requireOrdered(LocalDate start, LocalDate end) {
		if (end != null && end.isBefore(start)) {
			throw new BadRequestException("endDate must not be before startDate");
		}
	}

}
