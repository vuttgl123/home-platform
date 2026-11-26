package com.home_platform.registry.imports;

import java.util.List;

import com.home_platform.registry.catalog.CatalogStatus;
import com.home_platform.registry.itemcategory.ItemCategory;
import com.home_platform.registry.placetype.PlaceType;

/** Tìm loại địa điểm (theo tên hoặc mã) và nhóm đồ (theo tên) trong Danh mục (spec N4). */
final class TypeLookup {

	private final List<PlaceType> placeTypes;

	private final List<ItemCategory> categories;

	TypeLookup(List<PlaceType> placeTypes, List<ItemCategory> categories) {
		this.placeTypes = placeTypes;
		this.categories = categories;
	}

	static TypeLookup of(ImportRun run) {
		return run.shared(TypeLookup.class, () -> new TypeLookup(run.placeTypes(), run.categories()));
	}

	/** Loại ở cột {@code header} (bắt buộc); lỗi ghi vào {@code reader} và trả {@code null}. */
	PlaceType placeType(RowReader reader, String header) {
		String value = reader.requiredText(header);
		if (value == null) {
			return null;
		}
		String key = ImportText.key(value);
		PlaceType type = this.placeTypes.stream()
			.filter(candidate -> ImportText.key(candidate.name()).equals(key))
			.findFirst()
			.or(() -> this.placeTypes.stream().filter(candidate -> ImportText.key(candidate.code()).equals(key)).findFirst())
			.orElse(null);
		if (type == null) {
			reader.error(header, "Chưa có loại '" + value + "' trong Danh mục");
			return null;
		}
		if (type.status() != CatalogStatus.ACTIVE) {
			reader.error(header, "Loại '" + value + "' đang ngừng dùng");
			return null;
		}
		return type;
	}

	/** Nhóm ở cột {@code header}; ô trống là không nhóm. Lỗi ghi vào {@code reader} và trả {@code null}. */
	ItemCategory category(RowReader reader, String header) {
		String value = reader.text(header);
		if (value == null) {
			return null;
		}
		String key = ImportText.key(value);
		ItemCategory category = this.categories.stream()
			.filter(candidate -> ImportText.key(candidate.name()).equals(key))
			.findFirst()
			.orElse(null);
		if (category == null) {
			reader.error(header, "Chưa có nhóm '" + value + "' trong Danh mục");
			return null;
		}
		if (category.status() != CatalogStatus.ACTIVE) {
			reader.error(header, "Nhóm '" + value + "' đang ngừng dùng");
			return null;
		}
		return category;
	}

}
