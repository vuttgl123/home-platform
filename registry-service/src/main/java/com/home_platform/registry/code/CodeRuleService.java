package com.home_platform.registry.code;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.NotFoundException;
import com.home_platform.registry.catalog.CatalogCache;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mẫu mã của một không gian. Là danh mục: không có {@code version}, người sửa sau thắng, đọc qua
 * cache theo không gian. Đổi mẫu chỉ ảnh hưởng mã cấp từ lúc đó.
 */
@Service
public class CodeRuleService {

	private final CodeRuleRepository repository;

	private final PlaceTypeService placeTypes;

	private final ItemCategoryService categories;

	private final CatalogCache catalogCache;

	CodeRuleService(CodeRuleRepository repository, PlaceTypeService placeTypes, ItemCategoryService categories,
			CatalogCache catalogCache) {
		this.repository = repository;
		this.placeTypes = placeTypes;
		this.categories = categories;
		this.catalogCache = catalogCache;
	}

	/** Mẫu đã lưu của không gian. Đọc qua cache. */
	@Cacheable(cacheNames = CatalogCache.CODE_RULES, key = "#workspaceId")
	public List<CodeRule> stored(long workspaceId) {
		return List.copyOf(this.repository.listAll(workspaceId));
	}

	/**
	 * Mẫu đang dùng: mọi mẫu đã lưu, cộng dòng chung có sẵn cho đích nào chưa đặt. Địa điểm trước đồ
	 * đạc; trong mỗi đích, dòng chung trước.
	 */
	public List<CodeRuleView> list(long workspaceId) {
		List<CodeRule> stored = this.repository.listAll(workspaceId);
		List<CodeRuleView> views = new ArrayList<>();
		for (CodeTarget target : CodeTarget.values()) {
			if (stored.stream().noneMatch(rule -> rule.target() == target && rule.typeId() == 0)) {
				views.add(new CodeRuleView(target, 0, CodeFormat.builtIn(target), true));
			}
		}
		stored.forEach(rule -> views.add(new CodeRuleView(rule.target(), rule.typeId(), rule.format(), false)));
		views.sort(Comparator.comparing(CodeRuleView::target).thenComparingLong(CodeRuleView::typeId));
		return views;
	}

	/** @throws NotFoundException nếu {@code typeId} khác 0 không phải loại (nhóm) của không gian */
	@Transactional
	public CodeRuleView put(WorkspaceContext ctx, CodeTarget target, long typeId, CodeRuleRequest request) {
		requireType(ctx.workspaceId(), target, typeId);
		this.repository.upsert(ctx.workspaceId(), target, typeId, request.format());
		this.catalogCache.evict(CatalogCache.CODE_RULES, ctx.workspaceId());
		return new CodeRuleView(target, typeId, request.format(), false);
	}

	/** Bỏ mẫu đã lưu; với {@code typeId} 0 là quay về dạng có sẵn. */
	@Transactional
	public void delete(WorkspaceContext ctx, CodeTarget target, long typeId) {
		if (!this.repository.delete(ctx.workspaceId(), target, typeId)) {
			throw new NotFoundException("Code rule " + target + "/" + typeId + " not found");
		}
		this.catalogCache.evict(CatalogCache.CODE_RULES, ctx.workspaceId());
	}

	private void requireType(long workspaceId, CodeTarget target, long typeId) {
		if (typeId == 0) {
			return;
		}
		switch (target) {
			case PLACE -> this.placeTypes.get(workspaceId, typeId);
			case ITEM -> this.categories.get(workspaceId, typeId);
		}
	}

}
