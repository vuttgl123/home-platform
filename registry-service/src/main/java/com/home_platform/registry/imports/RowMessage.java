package com.home_platform.registry.imports;

/**
 * Một lời nhắn về một dòng.
 *
 * @param column tiêu đề cột như trong mẫu; {@code null} là của cả dòng
 */
public record RowMessage(MessageLevel level, String column, String text) {

	static RowMessage error(String column, String text) {
		return new RowMessage(MessageLevel.ERROR, column, text);
	}

	static RowMessage warning(String column, String text) {
		return new RowMessage(MessageLevel.WARNING, column, text);
	}

}
