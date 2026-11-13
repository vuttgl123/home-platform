package com.home_platform.registry.catalog;

/** Chuẩn hoá chuỗi nhận từ client. */
public final class Text {

	private Text() {
	}

	/**
	 * Mẫu {@code LIKE} "bắt đầu bằng": thoát {@code \\}, {@code %}, {@code _} để chúng được hiểu
	 * theo nghĩa đen, rồi nối {@code %}. Dùng với ký tự thoát mặc định của MySQL.
	 */
	public static String likePrefix(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
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
