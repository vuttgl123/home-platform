package com.home_platform.registry.ownership;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.ConflictException;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.common.web.NotFoundException;
import com.home_platform.registry.person.PersonService;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.support.BusinessDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sở hữu địa điểm: ai sở hữu bao nhiêu phần, từ khi nào tới khi nào. Không có version riêng; lịch sử
 * là chính các dòng. Một người không có hai dòng chưa xoá giao thời gian ở cùng một địa điểm, và tổng
 * tỷ lệ của một địa điểm không vượt 100% trong bất kỳ ngày nào.
 *
 * <p>Mọi lệnh ghi giữ riêng dòng địa điểm trước khi kiểm tra, nên hai lệnh cùng lúc trên một địa điểm
 * chạy lần lượt và không cùng qua bước kiểm tra tổng tỷ lệ. Thứ tự giữ: dòng sở hữu, địa điểm, người.
 */
@Service
public class OwnershipService {

	private final OwnershipRepository repository;

	private final PlaceService places;

	private final PersonService people;

	private final BusinessDate businessDate;

	OwnershipService(OwnershipRepository repository, PlaceService places, PersonService people,
			BusinessDate businessDate) {
		this.repository = repository;
		this.places = places;
		this.people = people;
		this.businessDate = businessDate;
	}

	public Ownership get(long workspaceId, long id) {
		return this.repository.getById(workspaceId, id);
	}

	public OwnershipView view(long workspaceId, long id) {
		return views(workspaceId, List.of(get(workspaceId, id))).get(0);
	}

	/** @throws NotFoundException nếu địa điểm không có hoặc đã xoá */
	public CursorPage<OwnershipView> atPlace(long workspaceId, long placeId, OwnershipStatus status, CursorRequest page) {
		this.places.get(workspaceId, placeId);
		return withViews(workspaceId,
				this.repository.atPlace(workspaceId, placeId, status, this.businessDate.today(), page));
	}

	/** Sở hữu hiện tại trong cả cây con, ví dụ các căn của một toà nhà. */
	public CursorPage<OwnershipView> currentInSubtree(long workspaceId, long rootPlaceId, CursorRequest page) {
		String rootPath = this.places.get(workspaceId, rootPlaceId).path();
		return withViews(workspaceId,
				this.repository.currentInSubtree(workspaceId, rootPath, this.businessDate.today(), page));
	}

	/** Mọi chỗ một người đã và đang sở hữu, mới nhất trước. */
	public CursorPage<OwnershipView> ofPerson(long workspaceId, long personId, CursorRequest page) {
		this.people.get(workspaceId, personId);
		return withViews(workspaceId, this.repository.ofPerson(workspaceId, personId, page));
	}

	/** @throws NotFoundException nếu địa điểm hoặc người không có hay đã xoá */
	@Transactional
	public Ownership create(WorkspaceContext ctx, OwnershipCreateRequest request) {
		long workspaceId = ctx.workspaceId();
		this.places.getForUpdate(workspaceId, request.placeId());
		this.people.getForUpdate(workspaceId, request.personId());
		requireValid(workspaceId, request.personId(), request.placeId(), request.sharePercent(), request.startDate(),
				request.endDate(), 0);
		return this.repository.insert(ctx, new Ownership(0, 0, request.placeId(), request.personId(),
				request.sharePercent(), request.startDate(), request.endDate(), request.documentNo(), request.note(),
				null, 0, null, 0, 0, null));
	}

	/**
	 * Sửa một dòng nhập nhầm. Địa điểm và người của dòng phải chưa bị xoá.
	 * @throws ConflictException nếu địa điểm hoặc người đã bị xoá, khoảng thời gian giao dòng khác, hoặc
	 * tổng tỷ lệ vượt 100
	 */
	@Transactional
	public Ownership update(WorkspaceContext ctx, long id, OwnershipUpdateRequest request) {
		long workspaceId = ctx.workspaceId();
		Ownership current = lockForChange(workspaceId, id);
		requireLivePlaceAndPerson(workspaceId, current);
		requireValid(workspaceId, current.personId(), current.placeId(), request.sharePercent(), request.startDate(),
				request.endDate(), id);
		return this.repository.update(ctx, current.withVersion(request.version())
			.withSharePercent(request.sharePercent())
			.withStartDate(request.startDate())
			.withEndDate(request.endDate())
			.withDocumentNo(request.documentNo())
			.withNote(request.note()));
	}

	/**
	 * Thôi sở hữu: đặt ngày cuối cùng còn sở hữu, mặc định hôm nay.
	 * @throws ConflictException nếu dòng đã có ngày kết thúc
	 */
	@Transactional
	public Ownership end(WorkspaceContext ctx, long id, OwnershipEndRequest request) {
		Ownership current = lockForChange(ctx.workspaceId(), id);
		if (current.endDate() != null) {
			throw new ConflictException("Ownership " + id + " has already ended");
		}
		LocalDate endDate = (request.endDate() != null) ? request.endDate() : this.businessDate.today();
		requireOrdered(current.startDate(), endDate);
		return this.repository.update(ctx, current.withVersion(request.version()).withEndDate(endDate));
	}

	/**
	 * Chuyển nhượng cả dòng: dòng cũ kết thúc hôm trước ngày chuyển, một dòng mới cùng địa điểm, cùng
	 * tỷ lệ bắt đầu từ ngày chuyển cho người nhận. Mọi kiểm tra chạy trước lần ghi đầu tiên, nên lỗi thì
	 * không dòng nào đổi.
	 * @throws ConflictException nếu dòng đã kết thúc, {@code version} lệch, địa điểm đã xoá, hoặc người
	 * nhận đã có dòng giao thời gian ở đây
	 * @throws BadRequestException nếu người nhận là chủ hiện tại, hoặc ngày chuyển không sau ngày bắt đầu
	 */
	@Transactional
	public OwnershipTransfer transfer(WorkspaceContext ctx, long id, OwnershipTransferRequest request) {
		long workspaceId = ctx.workspaceId();
		Ownership current = lockForChange(workspaceId, id);
		if (current.version() != request.version()) {
			throw ConflictException.versionConflict("Ownership", id);
		}
		if (current.endDate() != null) {
			throw new ConflictException("Ownership " + id + " has already ended");
		}
		if (request.toPersonId() == current.personId()) {
			throw new BadRequestException("toPersonId must be a different person");
		}
		LocalDate day = (request.transferDate() != null) ? request.transferDate() : this.businessDate.today();
		if (!day.isAfter(current.startDate())) {
			throw new BadRequestException("transferDate must be after startDate");
		}
		if (this.places.findForUpdate(workspaceId, current.placeId()).isEmpty()) {
			throw new ConflictException("Restore place " + current.placeId() + " first");
		}
		this.people.getForUpdate(workspaceId, request.toPersonId());
		requireValid(workspaceId, request.toPersonId(), current.placeId(), current.sharePercent(), day, null, id);
		Ownership ended = this.repository.update(ctx, current.withEndDate(day.minusDays(1)));
		Ownership started = this.repository.insert(ctx, new Ownership(0, 0, current.placeId(), request.toPersonId(),
				current.sharePercent(), day, null, request.documentNo(), request.note(), null, 0, null, 0, 0, null));
		return new OwnershipTransfer(ended, started);
	}

	/** Xoá mềm; chỉ dành cho dòng nhập nhầm. Thôi sở hữu thì dùng {@link #end}. */
	@Transactional
	public void delete(WorkspaceContext ctx, long id, int version) {
		get(ctx.workspaceId(), id);
		this.repository.softDelete(ctx, id, version);
	}

	Ownership lockForChange(long workspaceId, long id) {
		return this.repository.findByIdForUpdate(workspaceId, id)
			.orElseThrow(() -> NotFoundException.of("Ownership", id));
	}

	void requireLivePlaceAndPerson(long workspaceId, Ownership row) {
		if (this.places.findForUpdate(workspaceId, row.placeId()).isEmpty()) {
			throw new ConflictException("Restore place " + row.placeId() + " first");
		}
		if (this.people.findForUpdate(workspaceId, row.personId()).isEmpty()) {
			throw new ConflictException("Restore person " + row.personId() + " first");
		}
	}

	/**
	 * Thứ tự ngày, không giao thời gian với dòng khác của cùng người ở cùng chỗ, tổng tỷ lệ không vượt 100.
	 * @param excludeId dòng đang sửa (không tính chính nó); 0 khi tạo
	 */
	void requireValid(long workspaceId, long personId, long placeId, BigDecimal share, LocalDate start, LocalDate end,
			long excludeId) {
		requireOrdered(start, end);
		if (this.repository.overlaps(workspaceId, personId, placeId, start, end, excludeId)) {
			throw new ConflictException("Person " + personId + " already has an overlapping ownership at this place");
		}
		ShareLimit.firstOverflow(this.repository.sharedAt(workspaceId, placeId, start, end, excludeId), share, start, end)
			.ifPresent(overflow -> {
				throw new ConflictException("Ownership shares of place " + placeId + " would total "
						+ overflow.total().stripTrailingZeros().toPlainString() + "% on " + overflow.date());
			});
	}

	private static void requireOrdered(LocalDate start, LocalDate end) {
		if (end != null && end.isBefore(start)) {
			throw new BadRequestException("endDate must not be before startDate");
		}
	}

	private CursorPage<OwnershipView> withViews(long workspaceId, CursorPage<Ownership> page) {
		return new CursorPage<>(views(workspaceId, page.items()), page.nextCursor());
	}

	/** Gắn tên địa điểm, tên người (lấy theo lô) và trạng thái hôm nay vào từng dòng. */
	List<OwnershipView> views(long workspaceId, List<Ownership> rows) {
		Map<Long, String> placeNames = this.places.names(workspaceId,
				rows.stream().map(Ownership::placeId).distinct().toList());
		Map<Long, String> personNames = this.people.names(workspaceId,
				rows.stream().map(Ownership::personId).distinct().toList());
		LocalDate today = this.businessDate.today();
		return rows.stream()
			.map(row -> new OwnershipView(row, placeNames.get(row.placeId()), personNames.get(row.personId()),
					OwnershipStatus.of(row.startDate(), row.endDate(), today)))
			.toList();
	}

}
