package com.home_platform.registry.setup;

import java.util.HashMap;
import java.util.Map;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.itemcategory.ItemCategoryRequest;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khởi tạo một không gian mới với các loại địa điểm, nhóm đồ và mục tuỳ biến mặc định.
 *
 * <p>Chỉ làm khi không gian chưa có loại địa điểm nào, nên gọi lại nhiều lần vẫn an toàn và
 * không tạo lại thứ người dùng đã cố ý xoá. Hai lần gọi đồng thời trên cùng không gian: lần
 * thua nhận 409 do trùng mã; gọi lại thì không làm gì.
 */
@Service
public class SetupService {

	private final PlaceTypeService placeTypes;

	private final ItemCategoryService itemCategories;

	private final FieldDefService fieldDefs;

	SetupService(PlaceTypeService placeTypes, ItemCategoryService itemCategories, FieldDefService fieldDefs) {
		this.placeTypes = placeTypes;
		this.itemCategories = itemCategories;
		this.fieldDefs = fieldDefs;
	}

	/** @return {@code true} nếu đã tạo dữ liệu mặc định, {@code false} nếu không gian đã có loại địa điểm */
	@Transactional
	public boolean setup(WorkspaceContext ctx) {
		if (this.placeTypes.anyExists(ctx.workspaceId())) {
			return false;
		}
		Map<String, Long> typeIds = new HashMap<>();
		int sortOrder = 0;
		for (String[] type : DefaultCatalog.PLACE_TYPES) {
			sortOrder += DefaultCatalog.SORT_STEP;
			typeIds.put(type[0], this.placeTypes
				.create(ctx, new PlaceTypeCreateRequest(type[0], type[1], null, sortOrder))
				.id());
		}
		sortOrder = 0;
		for (String name : DefaultCatalog.ITEM_CATEGORIES) {
			sortOrder += DefaultCatalog.SORT_STEP;
			this.itemCategories.create(ctx, new ItemCategoryRequest(name, sortOrder));
		}
		sortOrder = 0;
		for (String[] field : DefaultCatalog.PLACE_FIELDS) {
			sortOrder += DefaultCatalog.SORT_STEP;
			this.fieldDefs.create(ctx, new FieldDefCreateRequest(FieldTarget.PLACE, typeIds.get(field[0]), field[1],
					field[2], FieldDataType.NUMBER, null, false, sortOrder));
		}
		return true;
	}

}
