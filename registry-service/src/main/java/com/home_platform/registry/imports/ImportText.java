package com.home_platform.registry.imports;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Cách so chữ khi nhập. Chữ Việt có thể đến ở dạng tổ hợp (dấu tách rời, từ một số bảng mã), với
 * khoảng trắng không ngắt (chép từ web), hoa thường lẫn lộn; mọi phép so đi qua đây.
 */
final class ImportText {

	/** Mọi khoảng trắng, kể cả khoảng trắng Unicode như NBSP (U+00A0). */
	private static final Pattern SPACES = Pattern.compile("[\\s\\p{Z}]+");

	private static final Pattern MARKS = Pattern.compile("\\p{M}+");

	private ImportText() {
	}

	/** NFC, gộp khoảng trắng liền nhau, bỏ hai đầu; rỗng là {@code null}. */
	static String clean(String value) {
		if (value == null) {
			return null;
		}
		String cleaned = SPACES.matcher(Normalizer.normalize(value, Normalizer.Form.NFC)).replaceAll(" ").strip();
		return cleaned.isEmpty() ? null : cleaned;
	}

	/** {@link #clean} rồi viết thường: khoá để so tên, nhãn, tiêu đề. */
	static String key(String value) {
		String cleaned = clean(value);
		return (cleaned != null) ? cleaned.toLowerCase(Locale.ROOT) : null;
	}

	/**
	 * {@link #key} rồi bỏ dấu ({@code đ} thành {@code d}): khoá để so mã, gần với cách database so mã
	 * ({@code utf8mb4_0900_ai_ci}, không phân biệt hoa thường và dấu).
	 */
	static String codeKey(String value) {
		String key = key(value);
		if (key == null) {
			return null;
		}
		return MARKS.matcher(Normalizer.normalize(key, Normalizer.Form.NFD)).replaceAll("").replace('đ', 'd');
	}

}
