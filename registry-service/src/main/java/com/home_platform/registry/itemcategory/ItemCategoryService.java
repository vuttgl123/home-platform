package com.home_platform.registry.itemcategory;

import java.util.List;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.ConflictException;
import com.home_platform.registry.catalog.CatalogCache;
import com.home_platform.registry.catalog.CatalogStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhóm đồ của một không gian. Tên không trùng (không phân biệt hoa thường và dấu, theo
 * collation của MySQL); nhóm không xoá mềm mà ngừng dùng bằng lưu trữ.
 */
@Service
public class ItemCategoryService {

	private final ItemCategoryRepository repository;

	private final CatalogCache catalogCache;

	ItemCategoryService(ItemCategoryRepository repository, CatalogCache catalogCache) {
		this.repository = repository;
		this.catalogCache = catalogCache;
	}

	/** Mọi nhóm của không gian, mọi trạng thái, theo thứ tự hiển thị rồi tên. Đọc qua cache. */
	@Cacheable(cacheNames = CatalogCache.ITEM_CATEGORIES, key = "#workspaceId")
	public List<ItemCategory> list(long workspaceId) {
		return List.copyOf(this.repository.listAll(workspaceId));
	}

	public ItemCategory get(long workspaceId, long id) {
		return this.repository.getById(workspaceId, id);
	}

	/**
	 * Nhóm dùng được cho món đồ mới.
	 * @throws com.home_platform.common.web.NotFoundException nếu không có
	 * @throws ConflictException nếu đã lưu trữ
	 */
	public ItemCategory requireActive(long workspaceId, long id) {
		ItemCategory category = get(workspaceId, id);
		if (category.status() == CatalogStatus.ARCHIVED) {
			throw new ConflictException("ItemCategory " + id + " is archived");
		}
		return category;
	}

	@Transactional
	public ItemCategory create(WorkspaceContext ctx, ItemCategoryRequest request) {
		ItemCategory category = new ItemCategory(0, 0, request.name(), request.sortOrder(), CatalogStatus.ACTIVE,
				null, null);
		try {
			return written(ctx, this.repository.insert(ctx, category));
		}
		catch (DuplicateKeyException ex) {
			throw duplicate(request);
		}
	}

	@Transactional
	public ItemCategory update(WorkspaceContext ctx, long id, ItemCategoryRequest request) {
		ItemCategory current = get(ctx.workspaceId(), id);
		try {
			return written(ctx,
					this.repository.update(ctx, current.withName(request.name()).withSortOrder(request.sortOrder())));
		}
		catch (DuplicateKeyException ex) {
			throw duplicate(request);
		}
	}

	@Transactional
	public ItemCategory archive(WorkspaceContext ctx, long id) {
		return setStatus(ctx, id, CatalogStatus.ARCHIVED);
	}

	@Transactional
	public ItemCategory unarchive(WorkspaceContext ctx, long id) {
		return setStatus(ctx, id, CatalogStatus.ACTIVE);
	}

	/**
	 * Xoá hẳn nhóm cùng các mục tuỳ biến của riêng nó.
	 * @throws ConflictException nếu còn món đồ (kể cả đã xoá mềm) thuộc nhóm này
	 */
	@Transactional
	public void delete(WorkspaceContext ctx, long id) {
		get(ctx.workspaceId(), id);
		if (this.repository.isUsed(ctx.workspaceId(), id)) {
			throw new ConflictException("ItemCategory " + id + " is used by items; archive it instead");
		}
		this.repository.deletePermanently(ctx.workspaceId(), id);
		this.repository.deleteOwnFields(ctx.workspaceId(), id);
		this.catalogCache.evict(CatalogCache.ITEM_CATEGORIES, ctx.workspaceId());
		this.catalogCache.evict(CatalogCache.FIELD_DEFS, ctx.workspaceId());
	}

	private ItemCategory setStatus(WorkspaceContext ctx, long id, CatalogStatus status) {
		ItemCategory current = get(ctx.workspaceId(), id);
		return written(ctx, this.repository.update(ctx, current.withStatus(status)));
	}

	private ItemCategory written(WorkspaceContext ctx, ItemCategory category) {
		this.catalogCache.evict(CatalogCache.ITEM_CATEGORIES, ctx.workspaceId());
		return category;
	}

	private static ConflictException duplicate(ItemCategoryRequest request) {
		return new ConflictException("Item category " + request.name() + " already exists");
	}

}
