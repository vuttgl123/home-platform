package com.home_platform.registry.export;

/** Mốc đọc tiếp của danh sách đồ đạc: dòng cuối của lô trước, theo đúng thứ tự sắp. */
public record ItemCursor(String placePath, String name, long id) {

	static ItemCursor after(ExportItemRow row) {
		return new ItemCursor(row.placePath(), row.name(), row.id());
	}

}
