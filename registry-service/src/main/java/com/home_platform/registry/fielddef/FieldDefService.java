package com.home_platform.registry.fielddef;

import java.util.List;
import java.util.Locale;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.ConflictException;
import com.home_platform.registry.catalog.CatalogCache;
import com.home_platform.registry.catalog.CatalogStatus;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mục thông tin tuỳ biến của một không gian. Khoá, đích, phạm vi và kiểu không đổi sau khi
 * tạo vì giá trị đã lưu phụ thuộc vào chúng; mục không xoá, chỉ lưu trữ.
 */
@Service
public class FieldDefService {

	private final FieldDefRepository repository;

	private final PlaceTypeService placeTypes;

	private final ItemCategoryService itemCategories;

	private final CatalogCache catalogCache;

	FieldDefService(FieldDefRepository repository, PlaceTypeService placeTypes, ItemCategoryService itemCategories,
			CatalogCache catalogCache) {
		this.repository = repository;
		this.placeTypes = placeTypes;
		this.itemCategories = itemCategories;
		this.catalogCache = catalogCache;
	}

	/** Mọi mục của không gian, mọi trạng thái; nguồn cho việc kiểm tra {@code attributes}. Đọc qua cache. */
	@Cacheable(cacheNames = CatalogCache.FIELD_DEFS, key = "#workspaceId")
	public List<FieldDef> listAll(long workspaceId) {
		return List.copyOf(this.repository.listAll(workspaceId));
	}

	/**
	 * Mục của một đích, theo thứ tự hiển thị.
	 * @param typeId {@code null} là mọi mục của đích; khác null là mục chung và mục của loại đó
	 */
	public List<FieldDef> list(long workspaceId, FieldTarget target, Long typeId) {
		return this.repository.list(workspaceId, target, typeId);
	}

	public boolean anyExists(long workspaceId) {
		return this.repository.anyExists(workspaceId);
	}

	public FieldDef get(long workspaceId, long id) {
		return this.repository.getById(workspaceId, id);
	}

	@Transactional
	public FieldDef create(WorkspaceContext ctx, FieldDefCreateRequest request) {
		requireScope(ctx.workspaceId(), request.target(), request.typeId());
		List<String> options = FieldOptions.check(request.dataType(), request.options());
		requireRemindable(request.dataType(), request.remindDays());
		if (this.repository.keyOverlaps(ctx.workspaceId(), request.target(), request.typeId(), request.fieldKey())) {
			throw duplicate(request);
		}
		FieldDef fieldDef = new FieldDef(0, 0, request.target(), request.typeId(), request.fieldKey(),
				request.label(), request.dataType(), options, request.required(), request.remindDays(), request.sortOrder(),
				CatalogStatus.ACTIVE, null, null);
		try {
			return written(ctx, this.repository.insert(ctx, fieldDef));
		}
		catch (DuplicateKeyException ex) {
			throw duplicate(request);
		}
	}

	@Transactional
	public FieldDef update(WorkspaceContext ctx, long id, FieldDefUpdateRequest request) {
		FieldDef current = get(ctx.workspaceId(), id);
		List<String> options = FieldOptions.check(current.dataType(), request.options());
		requireRemindable(current.dataType(), request.remindDays());
		return written(ctx, this.repository.update(ctx, current.withLabel(request.label())
			.withOptions(options)
			.withRequired(request.required())
			.withRemindDays(request.remindDays())
			.withSortOrder(request.sortOrder())));
	}

	/** Chỉ mục kiểu ngày mới nhắc được (L4). */
	private static void requireRemindable(FieldDataType dataType, Integer remindDays) {
		if (remindDays != null && dataType != FieldDataType.DATE) {
			throw new BadRequestException("remindDays is only allowed for DATE");
		}
	}

	@Transactional
	public FieldDef archive(WorkspaceContext ctx, long id) {
		return setStatus(ctx, id, CatalogStatus.ARCHIVED);
	}

	@Transactional
	public FieldDef unarchive(WorkspaceContext ctx, long id) {
		return setStatus(ctx, id, CatalogStatus.ACTIVE);
	}

	/** Phạm vi phải là "mọi bản ghi" hoặc một loại, một nhóm đang dùng được của không gian này. */
	private void requireScope(long workspaceId, FieldTarget target, long typeId) {
		if (typeId == 0) {
			return;
		}
		switch (target) {
			case PERSON -> throw new BadRequestException("typeId must be 0 for PERSON");
			case PLACE -> this.placeTypes.requireActive(workspaceId, typeId);
			case ITEM -> this.itemCategories.requireActive(workspaceId, typeId);
		}
	}

	private FieldDef setStatus(WorkspaceContext ctx, long id, CatalogStatus status) {
		FieldDef current = get(ctx.workspaceId(), id);
		return written(ctx, this.repository.update(ctx, current.withStatus(status)));
	}

	private FieldDef written(WorkspaceContext ctx, FieldDef fieldDef) {
		this.catalogCache.evict(CatalogCache.FIELD_DEFS, ctx.workspaceId());
		return fieldDef;
	}

	private static ConflictException duplicate(FieldDefCreateRequest request) {
		return new ConflictException("Field " + request.fieldKey() + " already exists for this "
				+ request.target().name().toLowerCase(Locale.ROOT));
	}

}
