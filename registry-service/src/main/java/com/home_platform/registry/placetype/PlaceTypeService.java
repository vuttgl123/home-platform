package com.home_platform.registry.placetype;

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
 * Loại địa điểm của một không gian. {@code code} không đổi sau khi tạo; loại không xoá mềm mà
 * ngừng dùng bằng lưu trữ, và chỉ xoá hẳn được khi chưa địa điểm nào dùng.
 */
@Service
public class PlaceTypeService {

	private final PlaceTypeRepository repository;

	private final CatalogCache catalogCache;

	PlaceTypeService(PlaceTypeRepository repository, CatalogCache catalogCache) {
		this.repository = repository;
		this.catalogCache = catalogCache;
	}

	/** Mọi loại của không gian, mọi trạng thái, theo thứ tự hiển thị rồi tên. Đọc qua cache. */
	@Cacheable(cacheNames = CatalogCache.PLACE_TYPES, key = "#workspaceId")
	public List<PlaceType> list(long workspaceId) {
		return List.copyOf(this.repository.listAll(workspaceId));
	}

	public PlaceType get(long workspaceId, long id) {
		return this.repository.getById(workspaceId, id);
	}

	public boolean anyExists(long workspaceId) {
		return this.repository.anyExists(workspaceId);
	}

	/**
	 * Loại dùng được cho địa điểm mới.
	 * @throws com.home_platform.common.web.NotFoundException nếu không có
	 * @throws ConflictException nếu đã lưu trữ
	 */
	public PlaceType requireActive(long workspaceId, long id) {
		PlaceType placeType = get(workspaceId, id);
		if (placeType.status() == CatalogStatus.ARCHIVED) {
			throw new ConflictException("PlaceType " + id + " is archived");
		}
		return placeType;
	}

	@Transactional
	public PlaceType create(WorkspaceContext ctx, PlaceTypeCreateRequest request) {
		PlaceType placeType = new PlaceType(0, 0, request.code(), request.name(), request.description(),
				request.sortOrder(), CatalogStatus.ACTIVE, null, null);
		try {
			return written(ctx, this.repository.insert(ctx, placeType));
		}
		catch (DuplicateKeyException ex) {
			throw new ConflictException("Place type code " + request.code() + " already exists");
		}
	}

	@Transactional
	public PlaceType update(WorkspaceContext ctx, long id, PlaceTypeUpdateRequest request) {
		PlaceType current = get(ctx.workspaceId(), id);
		return written(ctx, this.repository.update(ctx, current.withName(request.name())
			.withDescription(request.description())
			.withSortOrder(request.sortOrder())));
	}

	@Transactional
	public PlaceType archive(WorkspaceContext ctx, long id) {
		return setStatus(ctx, id, CatalogStatus.ARCHIVED);
	}

	@Transactional
	public PlaceType unarchive(WorkspaceContext ctx, long id) {
		return setStatus(ctx, id, CatalogStatus.ACTIVE);
	}

	/**
	 * @throws ConflictException nếu còn địa điểm (kể cả đã xoá mềm) thuộc loại này
	 */
	@Transactional
	public void delete(WorkspaceContext ctx, long id) {
		get(ctx.workspaceId(), id);
		if (this.repository.isUsed(ctx.workspaceId(), id)) {
			throw new ConflictException("PlaceType " + id + " is used by places; archive it instead");
		}
		this.repository.deletePermanently(ctx.workspaceId(), id);
		this.catalogCache.evict(CatalogCache.PLACE_TYPES, ctx.workspaceId());
	}

	private PlaceType setStatus(WorkspaceContext ctx, long id, CatalogStatus status) {
		PlaceType current = get(ctx.workspaceId(), id);
		return written(ctx, this.repository.update(ctx, current.withStatus(status)));
	}

	private PlaceType written(WorkspaceContext ctx, PlaceType placeType) {
		this.catalogCache.evict(CatalogCache.PLACE_TYPES, ctx.workspaceId());
		return placeType;
	}

}
