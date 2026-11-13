package com.home_platform.registry.person;

import java.util.Locale;

import com.home_platform.registry.catalog.Text;

/** Chuẩn hoá các trường của người, dùng chung cho request tạo và sửa. */
final class PersonFields {

	private PersonFields() {
	}

	/** Bỏ khoảng trắng và dấu chấm: "090 123.4567" thành "0901234567". */
	static String phone(String value) {
		return (value != null) ? Text.trimToNull(value.replaceAll("[\\s.]", "")) : null;
	}

	static String idType(String value) {
		String trimmed = Text.trimToNull(value);
		return (trimmed != null) ? trimmed.toUpperCase(Locale.ROOT) : null;
	}

}
