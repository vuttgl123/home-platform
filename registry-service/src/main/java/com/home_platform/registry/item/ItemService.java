package com.home_platform.registry.item;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
import com.home_platform.registry.code.CodeIssuer;
import com.home_platform.registry.code.CodeReissueRequest;
import com.home_platform.registry.code.CodeTarget;
import com.home_platform.registry.support.Text;
import com.home_platform.registry.fielddef.AttributeValidation;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.itemcategory.ItemCategory;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.person.PersonService;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.support.BusinessDate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đồ đạc đặt trong địa điểm. Mỗi lần ghi để lại một version (loại {@value #ENTITY_TYPE}). Lệnh
 * sửa không đổi chỗ đặt hay trạng thái gỡ; món đã gỡ chỉ đọc được, trừ "đưa lại" và xoá.
 */
@Service
public class ItemService {

	public static final String ENTITY_TYPE = "ITEM";

	/** Đầu ghi chú của version khi một lần sửa chữa đưa món hỏng về dùng lại. */
	static final String FIXED_NOTE = "Đã sửa: ";

	/** Độ dài của cột {@code note} trong bảng version. */
	private static final int NOTE_LENGTH = 255;

	private final ItemRepository repository;

	private final PlaceService places;

	private final ItemCategoryService categories;

	private final AttributeValidation attributeValidation;

	private final EntityHistory history;

	private final BusinessDate businessDate;

	private final CodeIssuer codes;

	private final PersonService people;

	ItemService(ItemRepository repository, PlaceService places, ItemCategoryService categories,
			AttributeValidation attributeValidation, EntityHistory history, BusinessDate businessDate,
			CodeIssuer codes, PersonService people) {
		this.businessDate = businessDate;
		this.codes = codes;
		this.people = people;
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

	/** Giữ riêng dòng của món cho tới hết transaction; rỗng khi không có hoặc đã xoá. */
	public Optional<Item> findForUpdate(long workspaceId, long id) {
		return this.repository.findByIdForUpdate(workspaceId, id);
	}

	/**
	 * Món hỏng đã sửa xong (spec Đợt 8, L2): chuyển về {@code IN_USE}, ghi version có ghi chú
	 * "Đã sửa: {@code work}" (cắt còn 255 ký tự).
	 * @throws BadRequestException nếu món không đang {@code BROKEN}
	 * @throws ConflictException nếu {@code version} lệch
	 */
	@Transactional
	public Item markFixed(WorkspaceContext ctx, long id, int version, String work) {
		Item current = get(ctx.workspaceId(), id);
		if (current.status() != ItemStatus.BROKEN) {
			throw new BadRequestException("Item " + id + " is not broken");
		}
		if (current.version() != version) {
			throw ConflictException.versionConflict("Item", id);
		}
		Item stored = this.repository.update(ctx, current.withStatus(ItemStatus.IN_USE));
		String note = FIXED_NOTE + work;
		record(ctx, stored, ChangeAction.UPDATE, (note.length() > NOTE_LENGTH) ? note.substring(0, NOTE_LENGTH) : note);
		return stored;
	}

	@Transactional
	public Item create(WorkspaceContext ctx, ItemRequest request) {
		if (request.placeId() == null) {
			throw new BadRequestException("placeId is required");
		}
		this.places.getForShare(ctx.workspaceId(), request.placeId());
		if (request.ownerPersonId() != null) {
			this.people.getForUpdate(ctx.workspaceId(), request.ownerPersonId());
		}
		requireSettableStatus(request.status());
		if (request.categoryId() != null) {
			this.categories.requireActive(ctx.workspaceId(), request.categoryId());
		}
		Map<String, Object> attributes = validAttributes(ctx, request.categoryId(), request.attributes());
		String code = issueCode(ctx, request.categoryId());
		Item item = new Item(0, 0, request.placeId(), request.categoryId(), code, request.name(), request.quantity(),
				request.unit(), request.status(), request.placedAt(), request.warrantyUntil(),
				request.serviceEveryMonths(), null, null, request.ownerPersonId(),
				request.description(), attributes, request.coverFileId(), null, 0, null, 0, 0, null);
		Item stored = insertWithCode(ctx, item);
		record(ctx, stored, ChangeAction.CREATE, request.note());
		return stored;
	}

	/**
	 * Cấp mã kế tiếp theo mẫu của nhóm hiện tại. Mã cũ không được trả lại và không ai nhận lại nó.
	 * @throws ConflictException nếu món đã gỡ hoặc {@code version} lệch
	 */
	@Transactional
	public Item reissueCode(WorkspaceContext ctx, long id, CodeReissueRequest request) {
		Item current = requireNotRemoved(get(ctx.workspaceId(), id));
		if (current.version() != request.version()) {
			throw ConflictException.versionConflict("Item", id);
		}
		Item stored = this.repository.update(ctx, current.withCode(issueCode(ctx, current.categoryId())));
		record(ctx, stored, ChangeAction.UPDATE, request.noteFor(current.code()));
		return stored;
	}

	public int countWithoutCode(long workspaceId) {
		return this.repository.countWithoutCode(workspaceId);
	}

	/**
	 * Cấp mã cho tối đa {@code limit} món chưa xoá, chưa gỡ, chưa có mã, cũ trước; mỗi món một version
	 * ghi chú {@value CodeIssuer#CODE_ASSIGNED}. Món vừa được lệnh khác cấp mã hay vừa bị gỡ thì bỏ qua.
	 * @return số món vừa được cấp mã
	 */
	@Transactional
	public int assignMissingCodes(WorkspaceContext ctx, int limit) {
		int assigned = 0;
		for (long id : this.repository.idsWithoutCode(ctx.workspaceId(), limit)) {
			Item current = this.repository.findByIdForUpdate(ctx.workspaceId(), id).orElse(null);
			if (current == null || current.code() != null || current.removed()) {
				continue;
			}
			Item stored = this.repository.update(ctx, current.withCode(issueCode(ctx, current.categoryId())));
			record(ctx, stored, ChangeAction.UPDATE, CodeIssuer.CODE_ASSIGNED);
			assigned++;
		}
		return assigned;
	}

	private String issueCode(WorkspaceContext ctx, Long categoryId) {
		return this.codes.issue(ctx.workspaceId(), CodeTarget.ITEM, (categoryId != null) ? categoryId : 0,
				candidate -> this.repository.codeExists(ctx.workspaceId(), candidate));
	}

	/** Khoá duy nhất trên mã là chốt cuối; bộ đếm đúng thì không bao giờ tới đây. */
	private Item insertWithCode(WorkspaceContext ctx, Item item) {
		try {
			return this.repository.insert(ctx, item);
		}
		catch (DuplicateKeyException ex) {
			throw new ConflictException("Code " + item.code() + " is already in use; try again");
		}
	}

	@Transactional
	public Item update(WorkspaceContext ctx, long id, ItemUpdateRequest request) {
		Item current = requireNotRemoved(get(ctx.workspaceId(), id));
		Item stored = applyContent(ctx, current, request);
		record(ctx, stored, ChangeAction.UPDATE, request.note());
		return stored;
	}

	/**
	 * Đường ghi nội dung chung của {@link #update} và {@link #restoreVersion}: kiểm tra trạng thái,
	 * nhóm và {@code attributes}, rồi ghi mọi trường sửa được. Không ghi version; hàm gọi tự ghi.
	 * Khôi phục version đi qua đây bằng một {@link ItemUpdateRequest} dựng từ snapshot, nên thêm một
	 * trường sửa được mà quên phần khôi phục thì không biên dịch được.
	 */
	private Item applyContent(WorkspaceContext ctx, Item current, ItemUpdateRequest request) {
		requireSettableStatus(request.status());
		if (request.categoryId() != null && !Objects.equals(request.categoryId(), current.categoryId())) {
			this.categories.requireActive(ctx.workspaceId(), request.categoryId());
		}
		return this.repository.update(ctx, current.withVersion(request.version())
			.withCategoryId(request.categoryId())
			.withName(request.name())
			.withQuantity(request.quantity())
			.withUnit(request.unit())
			.withStatus(request.status())
			.withPlacedAt(request.placedAt())
			.withWarrantyUntil(request.warrantyUntil())
			.withServiceEveryMonths(request.serviceEveryMonths())
			.withDescription(request.description())
			.withAttributes(validAttributes(ctx, request.categoryId(), request.attributes()))
			.withCoverFileId(request.coverFileId()));
	}

	/**
	 * Đổi chủ; {@code ownerPersonId} trống là trả món về cho địa điểm. Chủ mới bằng chủ cũ thì không ghi gì.
	 * @throws ConflictException nếu món đã gỡ hoặc {@code version} lệch
	 * @throws NotFoundException nếu chủ mới không có hoặc đã xoá
	 */
	@Transactional
	public Item changeOwner(WorkspaceContext ctx, long id, ItemOwnerRequest request) {
		Item current = requireNotRemoved(get(ctx.workspaceId(), id));
		if (current.version() != request.version()) {
			throw ConflictException.versionConflict("Item", id);
		}
		if (Objects.equals(current.ownerPersonId(), request.ownerPersonId())) {
			return current;
		}
		if (request.ownerPersonId() != null) {
			this.people.getForUpdate(ctx.workspaceId(), request.ownerPersonId());
		}
		Item stored = this.repository.update(ctx, current.withOwnerPersonId(request.ownerPersonId()));
		record(ctx, stored, ChangeAction.UPDATE, request.note());
		return stored;
	}

	/** Món có chủ mà chủ đã bị xoá thì phải khôi phục người đó trước, như với địa điểm của món. */
	private void requireLiveOwner(WorkspaceContext ctx, Item item) {
		if (item.ownerPersonId() != null && this.people.findForUpdate(ctx.workspaceId(), item.ownerPersonId()).isEmpty()) {
			throw new ConflictException("Restore person " + item.ownerPersonId() + " first");
		}
	}

	/** Chuyển sang địa điểm khác chưa xoá; vị trí cũ xem được trong version trước. */
	@Transactional
	public Item move(WorkspaceContext ctx, long id, ItemMoveRequest request) {
		Item current = requireNotRemoved(get(ctx.workspaceId(), id));
		this.places.getForShare(ctx.workspaceId(), request.placeId());
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
	 */
	@Transactional
	public ItemReplacement replace(WorkspaceContext ctx, long id, ItemReplaceRequest request) {
		Item old = requireNotRemoved(get(ctx.workspaceId(), id));
		if (old.version() != request.version()) {
			throw ConflictException.versionConflict("Item", id);
		}
		LocalDate removedAt = removalDate(old, request.removedAt());
		Item fresh = create(ctx, request.replacement().inheriting(old));
		Item removed = removeItem(ctx, old, request.version(), removedAt, fresh.id(),
				"Replaced by item " + fresh.id());
		return new ItemReplacement(removed, fresh);
	}

	/**
	 * Đưa lại một món đã gỡ. Món đã bị thay chỉ đưa lại được khi món thay nó đã bị xoá (thay nhầm);
	 * khi đó nó không còn trỏ tới món kia nữa.
	 * @throws ConflictException nếu món chưa gỡ, đã bị thay bằng một món còn đó, hoặc địa điểm của
	 * nó đã bị xoá (địa điểm xoá được khi chỉ còn đồ đã gỡ)
	 */
	@Transactional
	public Item putBack(WorkspaceContext ctx, long id, ItemVersionRequest request) {
		Item current = get(ctx.workspaceId(), id);
		if (!current.removed()) {
			throw new ConflictException("Item " + id + " is not removed");
		}
		if (current.replacedByItemId() != null
				&& this.repository.findById(ctx.workspaceId(), current.replacedByItemId()).isPresent()) {
			throw new ConflictException("Item " + id + " was replaced by item " + current.replacedByItemId());
		}
		if (this.places.findForShare(ctx.workspaceId(), current.placeId()).isEmpty()) {
			throw new ConflictException("Restore place " + current.placeId() + " first");
		}
		requireLiveOwner(ctx, current);
		Item stored = this.repository.update(ctx,
				current.withVersion(request.version())
					.withRemovedAt(null)
					.withStatus(ItemStatus.IN_USE)
					.withReplacedByItemId(null));
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
		if (deleted.deletedAt() != null && this.places.findForShare(ctx.workspaceId(), deleted.placeId()).isEmpty()) {
			throw new ConflictException("Restore place " + deleted.placeId() + " first");
		}
		if (deleted.deletedAt() != null) {
			requireLiveOwner(ctx, deleted);
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
		// Version cũ là lúc món bị gỡ thì giữ trạng thái hiện tại: khôi phục nội dung không gỡ món.
		ItemStatus status = (old.status() == ItemStatus.REMOVED) ? current.status() : old.status();
		String note = EntityHistory.restoredNote(versionNo, request.note());
		Item stored = applyContent(ctx, current,
				new ItemUpdateRequest(request.version(), old.categoryId(), old.name(), old.quantity(),
						old.unit(), status, old.placedAt(), old.warrantyUntil(), old.serviceEveryMonths(),
						old.description(), old.attributes(), old.coverFileId(), note));
		record(ctx, stored, ChangeAction.UPDATE, note);
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

	/** Những bản ghi đang bị xoá mềm, mới xoá trước: thùng rác. */
	public CursorPage<Item> deleted(long workspaceId, CursorRequest page) {
		return this.repository.deleted(workspaceId, page);
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

	/** Đồ chưa gỡ của một người. @throws NotFoundException nếu người đó không có hoặc đã xoá */
	public CursorPage<ItemView> ownedBy(long workspaceId, long ownerPersonId, CursorRequest page) {
		this.people.get(workspaceId, ownerPersonId);
		return withViews(workspaceId, this.repository.ownedBy(workspaceId, ownerPersonId, page));
	}

	/**
	 * Đồ của cả không gian (hoặc của một nhóm), theo tên; xem {@link ItemRepository#all}.
	 * @throws NotFoundException nếu nhóm không có
	 */
	public CursorPage<ItemView> all(long workspaceId, ItemStatus status, Long categoryId, CursorRequest page) {
		if (categoryId != null) {
			this.categories.get(workspaceId, categoryId);
		}
		return withViews(workspaceId, this.repository.all(workspaceId, status, categoryId, page));
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

	/** Gắn tên địa điểm, tên nhóm và tên chủ (lấy theo lô) vào từng món. */
	List<ItemView> views(long workspaceId, List<Item> items) {
		Map<Long, String> placeNames = this.places.names(workspaceId,
				items.stream().map(Item::placeId).distinct().toList());
		Map<Long, String> categoryNames = new HashMap<>();
		if (items.stream().anyMatch(item -> item.categoryId() != null)) {
			for (ItemCategory category : this.categories.list(workspaceId)) {
				categoryNames.put(category.id(), category.name());
			}
		}
		List<Long> ownerIds = items.stream().map(Item::ownerPersonId).filter(Objects::nonNull).distinct().toList();
		Map<Long, String> ownerNames = ownerIds.isEmpty() ? Map.of() : this.people.names(workspaceId, ownerIds);
		List<Long> scheduled = items.stream().filter(item -> item.serviceEveryMonths() != null).map(Item::id).toList();
		Map<Long, LocalDate> lastService = scheduled.isEmpty() ? Map.of()
				: this.repository.lastServiceOn(workspaceId, scheduled);
		return items.stream()
			.map(item -> new ItemView(item, placeNames.get(item.placeId()),
					(item.categoryId() != null) ? categoryNames.get(item.categoryId()) : null,
					(item.ownerPersonId() != null) ? ownerNames.get(item.ownerPersonId()) : null,
					ServiceSchedule.nextServiceOn(lastService.get(item.id()), item.placedAt(),
							LocalDate.ofInstant(item.createdAt(), this.businessDate.zone()), item.serviceEveryMonths())))
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

	/** Tên của các món đồ (kể cả đã xoá) theo {@code id}. */
	public Map<Long, String> names(long workspaceId, List<Long> ids) {
		return this.repository.names(workspaceId, ids);
	}

}
