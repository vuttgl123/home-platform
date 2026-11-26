package com.home_platform.registry.imports;

import java.util.HashMap;
import java.util.Map;

import com.home_platform.registry.export.AttributeColumns;

/** Nối tên trường của request với tiêu đề cột, để lỗi của lệnh tạo hiện đúng cột. */
final class Columns {

	private Columns() {
	}

	/** {@code base} cộng {@code attributes.<khoá>} của mỗi cột mục tuỳ biến. */
	static Map<String, String> withAttributes(Map<String, String> base, AttributeColumns attributes) {
		Map<String, String> columns = new HashMap<>(base);
		if (attributes != null) {
			attributes.defsByHeader().forEach((header, def) -> columns.put("attributes." + def.fieldKey(), header));
		}
		return Map.copyOf(columns);
	}

}
