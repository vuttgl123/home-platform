package com.home_platform.registry.search;

import java.util.Map;

import com.home_platform.registry.support.Text;

/**
 * Tìm chữ trong danh sách (spec Đợt 11, S1): khớp "chứa", theo collation của cột nên không phân
 * biệt hoa thường và dấu. {@code %}, {@code _}, {@code \} trong chữ tìm được hiểu theo nghĩa đen.
 */
public final class TextSearch {

	/** Ngắn hơn thì không tìm: một chữ cái khớp gần như mọi dòng. */
	public static final int MIN_LENGTH = 2;

	/** Tên tham số của mẫu tìm trong điều kiện do {@link #anyColumnContains} sinh ra. */
	public static final String PARAM = "text";

	private TextSearch() {
	}

	/**
	 * Mẫu {@code LIKE} "chứa" của chữ người dùng gõ.
	 * @return {@code null} khi không tìm: trống, hoặc sau khi cắt hai đầu còn ít hơn {@link #MIN_LENGTH} ký tự
	 */
	public static String pattern(String q) {
		String text = Text.trimToNull(q);
		return (text == null || text.codePointCount(0, text.length()) < MIN_LENGTH) ? null : contains(text);
	}

	/** Có cột nào trong {@code columns} chứa {@code pattern} (đã lấy từ {@link #pattern}). */
	public static SqlCondition anyColumnContains(String pattern, String... columns) {
		StringBuilder sql = new StringBuilder("(");
		for (int i = 0; i < columns.length; i++) {
			sql.append((i == 0) ? "" : " OR ").append(columns[i]).append(" LIKE :").append(PARAM);
		}
		return new SqlCondition(sql.append(')').toString(), Map.of(PARAM, pattern));
	}

	/** Mẫu "chứa" không có giới hạn độ dài, cho phép "chứa" của mục tuỳ biến. */
	static String contains(String text) {
		return "%" + Text.likePrefix(text);
	}

}
