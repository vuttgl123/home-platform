package com.home_platform.registry.maintenance;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.history.ChangeAction;
import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.ConflictException;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.common.web.NotFoundException;
import com.home_platform.registry.item.Item;
import com.home_platform.registry.item.ItemService;
import com.home_platform.registry.item.ItemStatus;
import com.home_platform.registry.support.BusinessDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhật ký bảo trì của món đồ (spec Đợt 8, L1, L2). Mỗi lần ghi thêm một version {@code MAINTENANCE}, nên
 * sửa nhật ký không chạm {@code version} của món. Món đã xoá thì nhật ký ẩn theo và không ghi lên được.
 *
 * <p>Thứ tự giữ dòng: dòng bảo trì, rồi món đồ (như sở hữu giữ dòng rồi giữ địa điểm).
 */
@Service
public class MaintenanceService {

	public static final String ENTITY_TYPE = "MAINTENANCE";

	private final MaintenanceRepository repository;

	private final ItemService items;

	private final EntityHistory history;

	private final BusinessDate businessDate;

	MaintenanceService(MaintenanceRepository repository, ItemService items, EntityHistory history,
			BusinessDate businessDate) {
		this.repository = repository;
		this.items = items;
		this.history = history;
		this.businessDate = businessDate;
	}

	/** @throws NotFoundException nếu món không có hoặc đã xoá */
	public CursorPage<Maintenance> ofItem(long workspaceId, long itemId, CursorRequest page) {
		this.items.get(workspaceId, itemId);
		return this.repository.ofItem(workspaceId, itemId, page);
	}

	/**
	 * Ghi một dòng; với {@code markFixed} thì cùng lúc đưa món hỏng về dùng lại. Mọi kiểm tra chạy trước lần
	 * ghi đầu tiên, nên lỗi thì không có gì được ghi.
	 * @throws NotFoundException nếu món không có hoặc đã xoá
	 * @throws BadRequestException nếu ngày làm sau hôm nay, hoặc {@code markFixed} sai trường hợp
	 * @throws ConflictException nếu {@code itemVersion} lệch
	 */
	@Transactional
	public Maintenance create(WorkspaceContext ctx, MaintenanceCreateRequest request) {
		long workspaceId = ctx.workspaceId();
		Item item = this.items.findForUpdate(workspaceId, request.itemId())
			.orElseThrow(() -> NotFoundException.of("Item", request.itemId()));
		requireNotFuture(request.doneOn());
		if (request.markFixed()) {
			requireFixable(item, request);
		}
		Maintenance stored = this.repository.insert(ctx, new Maintenance(0, 0, item.id(), request.kind(),
				request.doneOn(), request.work(), request.doneBy(), request.note(), null, 0, null, 0, 0, null));
		record(ctx, stored, ChangeAction.CREATE);
		if (request.markFixed()) {
			this.items.markFixed(ctx, item.id(), request.itemVersion(), request.work());
		}
		return stored;
	}

	/** @throws ConflictException nếu món đã xoá hoặc {@code version} lệch */
	@Transactional
	public Maintenance update(WorkspaceContext ctx, long id, MaintenanceUpdateRequest request) {
		Maintenance current = lockForChange(ctx.workspaceId(), id);
		requireNotFuture(request.doneOn());
		Maintenance stored = this.repository.update(ctx, current.withVersion(request.version())
			.withKind(request.kind())
			.withDoneOn(request.doneOn())
			.withWork(request.work())
			.withDoneBy(request.doneBy())
			.withNote(request.note()));
		record(ctx, stored, ChangeAction.UPDATE);
		return stored;
	}

	@Transactional
	public void delete(WorkspaceContext ctx, long id, int version) {
		lockForChange(ctx.workspaceId(), id);
		record(ctx, this.repository.softDelete(ctx, id, version), ChangeAction.DELETE);
	}

	@Transactional
	public Maintenance restore(WorkspaceContext ctx, long id, int version) {
		Maintenance current = this.repository.findByIdIncludingDeleted(ctx.workspaceId(), id)
			.orElseThrow(() -> NotFoundException.of("Maintenance", id));
		requireLiveItem(ctx.workspaceId(), current.itemId());
		Maintenance stored = this.repository.restore(ctx, id, version);
		record(ctx, stored, ChangeAction.RESTORE);
		return stored;
	}

	/** "Tên món: việc" của các dòng theo {@code id}, kể cả đã xoá; cho Hoạt động. */
	public Map<Long, String> names(long workspaceId, List<Long> ids) {
		return this.repository.lines(workspaceId, ids)
			.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().name()));
	}

	/** Món đồ của các dòng theo {@code id}, kể cả đã xoá; cho Hoạt động dẫn link. */
	public Map<Long, Long> itemIds(long workspaceId, List<Long> ids) {
		return this.repository.lines(workspaceId, ids)
			.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().itemId()));
	}

	/** Dòng chưa xoá của một món chưa xoá, đã giữ cả hai. */
	private Maintenance lockForChange(long workspaceId, long id) {
		Maintenance current = this.repository.findByIdForUpdate(workspaceId, id)
			.orElseThrow(() -> NotFoundException.of("Maintenance", id));
		requireLiveItem(workspaceId, current.itemId());
		return current;
	}

	private void requireLiveItem(long workspaceId, long itemId) {
		if (this.items.findForUpdate(workspaceId, itemId).isEmpty()) {
			throw new ConflictException("Restore item " + itemId + " first");
		}
	}

	private void requireNotFuture(LocalDate doneOn) {
		if (doneOn.isAfter(this.businessDate.today())) {
			throw new BadRequestException("doneOn must not be after today");
		}
	}

	private static void requireFixable(Item item, MaintenanceCreateRequest request) {
		if (request.kind() != MaintenanceKind.REPAIR) {
			throw new BadRequestException("markFixed is only for REPAIR entries");
		}
		if (request.itemVersion() == null) {
			throw new BadRequestException("itemVersion is required with markFixed");
		}
		if (item.status() != ItemStatus.BROKEN) {
			throw new BadRequestException("Item " + item.id() + " is not broken");
		}
		if (item.version() != request.itemVersion()) {
			throw ConflictException.versionConflict("Item", item.id());
		}
	}

	private void record(WorkspaceContext ctx, Maintenance row, ChangeAction action) {
		this.history.record(ctx, ENTITY_TYPE, row.id(), row.version(), action, MaintenanceSnapshot.of(row), null);
	}

}
