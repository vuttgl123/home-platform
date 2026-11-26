package com.home_platform.registry.imports;

/** Một ô không đọc được, hoặc giá trị không hợp lệ ở một cột; thành lỗi của dòng. */
final class CellError extends RuntimeException {

	private final String column;

	private final String text;

	/**
	 * @param column tiêu đề cột như trong mẫu; {@code null} là lỗi của cả dòng
	 * @param text lời nhắn tiếng Việt cho người dùng
	 */
	CellError(String column, String text) {
		super(text, null, false, false);
		this.column = column;
		this.text = text;
	}

	String column() {
		return this.column;
	}

	String text() {
		return this.text;
	}

}
