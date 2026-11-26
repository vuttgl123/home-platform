package com.home_platform.registry.imports;

import java.util.List;

/** Lỗi của cả file: không đọc tiếp, không nhập gì. */
final class ImportFileException extends RuntimeException {

	private final List<String> errors;

	ImportFileException(List<String> errors) {
		super(String.join("; ", errors), null, false, false);
		this.errors = List.copyOf(errors);
	}

	ImportFileException(String error) {
		this(List.of(error));
	}

	/** Lời nhắn tiếng Việt cho người dùng. */
	List<String> errors() {
		return this.errors;
	}

}
