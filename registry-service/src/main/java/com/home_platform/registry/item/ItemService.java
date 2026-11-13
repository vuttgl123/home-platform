package com.home_platform.registry.item;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.history.ChangeAction;
import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.history.EntityVersion;
import com.home_platform.common.history.EntityVersionSummary;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.ConflictException;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.common.web.NotFoundException;
import com.home_platform.registry.catalog.Text;
import com.home_platform.registry.fielddef.AttributeValidation;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.itemcategory.ItemCategory;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.support.BusinessDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đồ đạc đặt trong địa điểm. Mỗi lần ghi để lại một version (loại {@value #ENTITY_TYPE}). Lệnh
 * sửa không đổi chỗ đặt hay trạng thái gỡ; món đã gỡ chỉ đọc được, trừ "đưa lại" và xoá.
 */
@Service
public class ItemService {

	public static final String ENTITY_TYPE = "ITEM";

	final ItemRepository repository;

	final PlaceService places;

	final ItemCategoryService categories;

	final AttributeValidation attributeValidation;

	final EntityHistory history;

	final BusinessDate businessDate;

	ItemService(ItemRepository repository, PlaceService places, ItemCategoryService categories,
			AttributeValidation attributeValidation, EntityHistory history, BusinessDate businessDate) {
		this.businessDate = businessDate;
		this.repository = repository;
		this.places = places;
		this.categories = categories;
		this.attributeValidation = attributeValidation;
		this.history = history;
	}

	/** @throws com.home_platform.common.web.NotFoundException nếu không có hoặc đã xoá */
	public Item get(long workspaceId, long id) {
		return this.repository.getById(workspaceId, id);
	}

	public ItemView view(long workspaceId, long id) {
		return views(workspaceId, List.of(get(workspaceId, id))).get(0);
	}

	@Transactional
	public Item create(WorkspaceContext ctx, ItemRequest request) {
		if (request.placeId() == null) {
			throw new BadRequestException("placeId is required");
		}
		this.places.get(ctx.workspaceId(), request.placeId());
		requireSettableStatus(request.status());
		if (request.categoryId() != null) {
			this.categories.requireActive(ctx.workspaceId(), request.categoryId());
		}
		Item item = new Item(0, 0, request.placeId(), request.categoryId(), request.code(), request.name(),
				request.quantity(), request.unit(), request.status(), request.placedAt(), null, null,
				request.description(), validAttributes(ctx, request.categoryId(), request.attributes()),
				request.coverFileId(), null, 0, null, 0, 0, null);
		Item stored = this.repository.insert(ctx, item);
		record(ctx, stored, ChangeAction.CREATE, request.note());
		return stored;
	}

	@Transactional
	public Item update(WorkspaceContext ctx, long id, ItemUpdateRequest request) {
		Item current = requireNotRemoved(get(ctx.workspaceId(), id));
		requireSettableStatus(request.status());
		if (request.categoryId() != null && !Objects.equals(request.categoryId(), current.categoryId())) {
			this.categories.requireActive(ctx.workspaceId(), request.categoryId());
		}
		Item stored = this.repository.update(ctx, current.withVersion(request.version())
			.withCategoryId(request.categoryId())
			.withCode(request.code())
			.withName(request.name())
			.withQuantity(request.quantity())
			.withUnit(request.unit())
			.withStatus(request.status())
			.withPlacedAt(request.placedAt())
			.withDescription(request.description())
			.withAttributes(validAttributes(ctx, request.categoryId(), request.attributes()))
			.withCoverFileId(request.coverFileId()));
		record(ctx, stored, ChangeAction.UPDATE, request.note());
		return stored;
	}

	/** Chuyển sang địa điểm khác chưa xoá; vị trí cũ xem được trong version trước. */
	@Transactional
	public Item move(WorkspaceContext ctx, long id, ItemMoveRequest request) {
		Item current = requireNotRemoved(get(ctx.workspaceId(), id));
		this.places.get(ctx.workspaceId(), request.placeId());
		Item stored = this.repository.update(ctx,
				current.withVersion(request.version()).withPlaceId(request.placeId()));
		record(ctx, stored, ChangeAction.UPDATE, request.note());
		return stored;
	}

	/** Gỡ: món vẫn ở lại địa điểm, trong danh sách "đã gỡ". */
	@Transactional
	public Item remove(WorkspaceContext ctx, long id, ItemRemoveRequest request) {
		Item current = requireNotRemoved(get(ctx.workspaceId(), id));
		return removeItem(ctx, current, request.version(), request.removedAt(), null, request.note());
	}

	/**
	 * Thay món cũ bằng món mới trong một transaction: tạo món mới, rồi gỡ món cũ và trỏ nó tới
	 * món mới. Món mới không hợp lệ thì không có gì được ghi.
	 * @return món cũ đã gỡ và món mới
	 */
	@Transactional
	public Item[] replace(WorkspaceContext ctx, long id, ItemReplaceRequest request) {
		Item old = requireNotRemoved(get(ctx.workspaceId(), id));
		if (old.version() != request.version()) {
			throw new ConflictException("Item " + id + " was changed by someone else");
		}
		LocalDate removedAt = removalDate(old, request.removedAt());
		ItemRequest replacement = request.replacement();
		Item fresh = create(ctx,
				(replacement.placeId() != null) ? replacement : replacement.atPlace(old.placeId()));
		Item removed = removeItem(ctx, old, request.version(), removedAt, fresh.id(),
				"Replaced by item " + fresh.id());
		return new Item[] { removed, fresh };
	}

	/**
	 * Đưa lại một món đã gỡ.
	 * @throws ConflictException nếu món chưa gỡ, đã bị thay bằng món khác, hoặc địa điểm của nó đã
	 * bị xoá (địa điểm xoá được khi chỉ còn đồ đã gỡ)
	 */
	@Transactional
	public Item putBack(WorkspaceContext ctx, long id, ItemVersionRequest request) {
		Item current = get(ctx.workspaceId(), id);
		if (!current.removed()) {
			throw new ConflictException("Item " + id + " is not removed");
		}
		if (current.replacedByItemId() != null) {
			throw new ConflictException("Item " + id + " was replaced by item " + current.replacedByItemId());
		}
		if (!this.places.isAlive(ctx.workspaceId(), current.placeId())) {
			throw new ConflictException("Restore place " + current.placeId() + " first");
		}
		Item stored = this.repository.update(ctx,
				current.withVersion(request.version()).withRemovedAt(null).withStatus(ItemStatus.IN_USE));
		record(ctx, stored, ChangeAction.UPDATE, request.note());
		return stored;
	}

	/** Xoá mềm; dành cho món nhập nhầm. Món không còn dùng thì gỡ. */
	@Transactional
	public void delete(WorkspaceContext ctx, long id, int version, String note) {
		get(ctx.workspaceId(), id);
		Item stored = this.repository.softDelete(ctx, id, version);
		record(ctx, stored, ChangeAction.DELETE, note);
	}

	/**
	 * Bỏ xoá.
	 * @throws ConflictException nếu địa điểm của món đang bị xoá
	 */
	@Transactional
	public Item restore(WorkspaceContext ctx, long id, ItemVersionRequest request) {
		Item deleted = this.repository.findByIdIncludingDeleted(ctx.workspaceId(), id)
			.orElseThrow(() -> NotFoundException.of("Item", id));
		if (deleted.deletedAt() != null && !this.places.isAlive(ctx.workspaceId(), deleted.placeId())) {
			throw new ConflictException("Restore place " + deleted.placeId() + " first");
		}
		Item stored = this.repository.restore(ctx, id, request.version());
		record(ctx, stored, ChangeAction.RESTORE, request.note());
		return stored;
	}

	/** Các version của một món, mới nhất trước; thấy cả khi món đã bị xoá. */
	public CursorPage<EntityVersionSummary> versions(long workspaceId, long id, CursorRequest page) {
		this.repository.findByIdIncludingDeleted(workspaceId, id).orElseThrow(() -> NotFoundException.of("Item", id));
		return this.history.list(workspaceId, ENTITY_TYPE, id, page);
	}

	public EntityVersion version(long workspaceId, long id, int versionNo) {
		return this.history.get(workspaceId, ENTITY_TYPE, id, versionNo);
	}

	public ItemSnapshot snapshotOf(EntityVersion version) {
		return this.history.snapshotAs(version, ItemSnapshot.class);
	}

	/**
	 * Khôi phục nội dung một version cũ như một lần sửa bình thường. Không đổi chỗ đặt, không bỏ
	 * gỡ; nếu version cũ là lúc món bị gỡ thì giữ trạng thái hiện tại.
	 * @throws ConflictException nếu món đang bị xoá hoặc đã gỡ
	 */
	@Transactional
	public Item restoreVersion(WorkspaceContext ctx, long id, int versionNo, ItemVersionRequest request) {
		long workspaceId = ctx.workspaceId();
		Item current = requireNotRemoved(this.repository.findById(workspaceId, id)
			.orElseThrow(() -> this.repository.findByIdIncludingDeleted(workspaceId, id).isPresent()
					? new ConflictException("Restore item " + id + " before restoring an old version")
					: NotFoundException.of("Item", id)));
		ItemSnapshot old = snapshotOf(version(workspaceId, id, versionNo));
		if (old.categoryId() != null && !Objects.equals(old.categoryId(), current.categoryId())) {
			this.categories.requireActive(workspaceId, old.categoryId());
		}
		Item stored = this.repository.update(ctx, current.withVersion(request.version())
			.withCategoryId(old.categoryId())
			.withCode(old.code())
			.withName(old.name())
			.withQuantity(old.quantity())
			.withUnit(old.unit())
			.withStatus((old.status() == ItemStatus.REMOVED) ? current.status() : old.status())
			.withPlacedAt(old.placedAt())
			.withDescription(old.description())
			.withAttributes(validAttributes(ctx, old.categoryId(), old.attributes()))
			.withCoverFileId(old.coverFileId()));
		String note = "Restored from version " + versionNo + ((request.note() != null) ? ": " + request.note() : "");
		record(ctx, stored, ChangeAction.UPDATE, (note.length() > 255) ? note.substring(0, 255) : note);
		return stored;
	}

	private Item removeItem(WorkspaceContext ctx, Item current, int version, LocalDate removedAt, Long replacedBy,
			String note) {
		Item stored = this.repository.update(ctx, current.withVersion(version)
			.withRemovedAt(removalDate(current, removedAt))
			.withStatus(ItemStatus.REMOVED)
			.withReplacedByItemId(replacedBy));
		record(ctx, stored, ChangeAction.UPDATE, note);
		return stored;
	}

	/** Ngày gỡ: mặc định hôm nay, không trước ngày đưa vào. */
	private LocalDate removalDate(Item item, LocalDate requested) {
		LocalDate removedAt = (requested != null) ? requested : this.businessDate.today();
		if (item.placedAt() != null && removedAt.isBefore(item.placedAt())) {
			throw new BadRequestException("removedAt must not be before placedAt");
		}
		return removedAt;
	}

	/** @throws NotFoundException nếu địa điểm không có hoặc đã xoá */
	public CursorPage<ItemView> atPlace(long workspaceId, long placeId, boolean removed, CursorRequest page) {
		this.places.get(workspaceId, placeId);
		return withViews(workspaceId, this.repository.atPlace(workspaceId, placeId, removed, page));
	}

	/** Đồ chưa gỡ trong cả cây con, ví dụ cả một toà nhà. */
	public CursorPage<ItemView> inSubtree(long workspaceId, long rootPlaceId, CursorRequest page) {
		String rootPath = this.places.get(workspaceId, rootPlaceId).path();
		return withViews(workspaceId, this.repository.inSubtree(workspaceId, rootPath, page));
	}

	public CursorPage<ItemView> byCategory(long workspaceId, long categoryId, CursorRequest page) {
		this.categories.get(workspaceId, categoryId);
		return withViews(workspaceId, this.repository.byCategory(workspaceId, categoryId, page));
	}

	public CursorPage<ItemView> searchByName(long workspaceId, String prefix, CursorRequest page) {
		String cleaned = Text.trimToNull(prefix);
		if (cleaned == null) {
			throw new BadRequestException("name must not be blank");
		}
		return withViews(workspaceId, this.repository.searchByName(workspaceId, Text.likePrefix(cleaned), page));
	}

	public CursorPage<ItemView> findByCode(long workspaceId, String code, CursorRequest page) {
		String cleaned = Text.trimToNull(code);
		if (cleaned == null) {
			throw new BadRequestException("code must not be blank");
		}
		return withViews(workspaceId, this.repository.findByCode(workspaceId, cleaned, page));
	}

	private CursorPage<ItemView> withViews(long workspaceId, CursorPage<Item> page) {
		return new CursorPage<>(views(workspaceId, page.items()), page.nextCursor());
	}

	/** Gắn tên địa điểm và tên nhóm (lấy theo lô) vào từng món. */
	List<ItemView> views(long workspaceId, List<Item> items) {
		Map<Long, String> placeNames = this.places.names(workspaceId,
				items.stream().map(Item::placeId).distinct().toList());
		Map<Long, String> categoryNames = new java.util.HashMap<>();
		if (items.stream().anyMatch(item -> item.categoryId() != null)) {
			for (ItemCategory category : this.categories.list(workspaceId)) {
				categoryNames.put(category.id(), category.name());
			}
		}
		return items.stream()
			.map(item -> new ItemView(item, placeNames.get(item.placeId()),
					(item.categoryId() != null) ? categoryNames.get(item.categoryId()) : null))
			.toList();
	}

	/** @throws ConflictException nếu món đã gỡ: món đã gỡ chỉ đọc được, trừ "đưa lại" và xoá */
	static Item requireNotRemoved(Item item) {
		if (item.removed()) {
			throw new ConflictException("Item " + item.id() + " is removed");
		}
		return item;
	}

	static void requireSettableStatus(ItemStatus status) {
		if (status == ItemStatus.REMOVED) {
			throw new BadRequestException("status REMOVED is set by removing or replacing an item");
		}
	}

	Map<String, Object> validAttributes(WorkspaceContext ctx, Long categoryId, Map<String, Object> values) {
		return this.attributeValidation.validate(ctx.workspaceId(), FieldTarget.ITEM,
				(categoryId != null) ? categoryId : 0, values);
	}

	void record(WorkspaceContext ctx, Item item, ChangeAction action, String note) {
		this.history.record(ctx, ENTITY_TYPE, item.id(), item.version(), action, ItemSnapshot.of(item), note);
	}

}
