package com.home_platform.registry.catalog;

/** Chuẩn hoá chuỗi nhận từ client. */
public final class Text {

	private Text() {
	}

	/** Cắt khoảng trắng hai đầu; chuỗi rỗng sau khi cắt thành {@code null}. */
	public static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.strip();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
