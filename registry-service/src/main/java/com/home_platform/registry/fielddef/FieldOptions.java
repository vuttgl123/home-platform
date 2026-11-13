package com.home_platform.registry.fielddef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.home_platform.common.web.BadRequestException;
import com.home_platform.registry.catalog.Text;

/** Luật cho danh sách lựa chọn của một mục tuỳ biến. */
final class FieldOptions {

	static final int MAX_OPTIONS = 100;

	static final int MAX_LENGTH = 100;

	private FieldOptions() {
	}

	/** Cắt khoảng trắng từng phần tử; phần tử rỗng thành {@code null} để bị bắt ở {@link #check}. */
	static List<String> trim(List<String> options) {
		if (options == null) {
			return null;
		}
		List<String> trimmed = new ArrayList<>(options.size());
		options.forEach(option -> trimmed.add(Text.trimToNull(option)));
		return trimmed;
	}

	/**
	 * Danh sách lựa chọn sẽ lưu cho một mục kiểu {@code dataType}.
	 * @return danh sách không đổi được với SELECT; {@code null} với kiểu khác
	 * @throws BadRequestException nếu danh sách không hợp với kiểu
	 */
	static List<String> check(FieldDataType dataType, List<String> options) {
		boolean empty = options == null || options.isEmpty();
		if (dataType != FieldDataType.SELECT) {
			if (!empty) {
				throw new BadRequestException("options are only allowed for SELECT");
			}
			return null;
		}
		if (empty || options.size() > MAX_OPTIONS) {
			throw invalid();
		}
		Set<String> seen = new HashSet<>();
		for (String option : options) {
			if (option == null || option.length() > MAX_LENGTH || !seen.add(option.toLowerCase(Locale.ROOT))) {
				throw invalid();
			}
		}
		return List.copyOf(options);
	}

	private static BadRequestException invalid() {
		return new BadRequestException(
				"options must be 1 to 100 distinct non-blank values of at most 100 characters");
	}

}
