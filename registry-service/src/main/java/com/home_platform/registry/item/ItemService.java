package com.home_platform.registry.item;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.history.ChangeAction;
import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.ConflictException;
import com.home_platform.registry.fielddef.AttributeValidation;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.itemcategory.ItemCategory;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.place.PlaceService;
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

	ItemService(ItemRepository repository, PlaceService places, ItemCategoryService categories,
			AttributeValidation attributeValidation, EntityHistory history) {
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
