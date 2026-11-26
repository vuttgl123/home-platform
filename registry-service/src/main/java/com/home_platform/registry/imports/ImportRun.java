package com.home_platform.registry.imports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.registry.export.AttributeColumns;
import com.home_platform.registry.imports.ImportLookupRepository.KnownItem;
import com.home_platform.registry.imports.ImportLookupRepository.KnownPerson;
import com.home_platform.registry.imports.ImportLookupRepository.KnownPlace;
import com.home_platform.registry.itemcategory.ItemCategory;
import com.home_platform.registry.placetype.PlaceType;

/**
 * Trạng thái của một lần chạy (xem trước hoặc nhập): ai chạy, ghi chú version, danh mục, hồ sơ đã có
 * (đọc một lần, khi cần) và ghi chú thêm trong lúc chạy.
 */
final class ImportRun {

	static final int NOTE_LENGTH = 255;

	private final WorkspaceContext ctx;

	private final String versionNote;

	private final Map<SheetKind, AttributeColumns> attributes;

	private final List<PlaceType> placeTypes;

	private final List<ItemCategory> categories;

	private final ImportLookupRepository lookups;

	private final List<String> notices;

	private final Map<String, Object> state = new HashMap<>();

	private List<KnownPlace> knownPlaces;

	private List<KnownPerson> knownPeople;

	private List<KnownItem> knownItems;

	ImportRun(WorkspaceContext ctx, String fileName, Map<SheetKind, AttributeColumns> attributes,
			List<PlaceType> placeTypes, List<ItemCategory> categories, ImportLookupRepository lookups,
			List<String> notices) {
		this.ctx = ctx;
		String note = "Nhập từ Excel: " + fileName;
		this.versionNote = (note.length() > NOTE_LENGTH) ? note.substring(0, NOTE_LENGTH) : note;
		this.attributes = attributes;
		this.placeTypes = placeTypes;
		this.categories = categories;
		this.lookups = lookups;
		this.notices = new ArrayList<>(notices);
	}

	WorkspaceContext ctx() {
		return this.ctx;
	}

	/** Ghi chú của mọi version mà lần nhập tạo ra (spec N9). */
	String versionNote() {
		return this.versionNote;
	}

	/** Cột mục tuỳ biến của một sheet; sheet không có mục tuỳ biến thì là {@code null}. */
	AttributeColumns attributes(SheetKind kind) {
		return this.attributes.get(kind);
	}

	List<PlaceType> placeTypes() {
		return this.placeTypes;
	}

	List<ItemCategory> categories() {
		return this.categories;
	}

	List<KnownPlace> knownPlaces() {
		if (this.knownPlaces == null) {
			this.knownPlaces = this.lookups.places(this.ctx.workspaceId());
		}
		return this.knownPlaces;
	}

	List<KnownPerson> knownPeople() {
		if (this.knownPeople == null) {
			this.knownPeople = this.lookups.people(this.ctx.workspaceId());
		}
		return this.knownPeople;
	}

	List<KnownItem> knownItems() {
		if (this.knownItems == null) {
			this.knownItems = this.lookups.items(this.ctx.workspaceId());
		}
		return this.knownItems;
	}

	void notice(String notice) {
		if (!this.notices.contains(notice)) {
			this.notices.add(notice);
		}
	}

	List<String> notices() {
		return List.copyOf(this.notices);
	}

	/**
	 * Một chỉ mục dùng chung giữa các sheet của lần chạy (ví dụ địa điểm, người đã tạo), dựng lần đầu
	 * khi được hỏi.
	 */
	@SuppressWarnings("unchecked")
	<T> T shared(Class<T> type, java.util.function.Supplier<T> create) {
		return (T) this.state.computeIfAbsent(type.getName(), name -> create.get());
	}

}
