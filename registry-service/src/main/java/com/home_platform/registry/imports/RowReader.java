package com.home_platform.registry.imports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Đọc các ô của một dòng và gom mọi lỗi, cảnh báo của dòng đó, để một lần xem trước báo được hết
 * các ô sai chứ không chỉ ô đầu tiên. Ô sai thì lệnh đọc trả {@code null} và ghi lại lỗi.
 */
final class RowReader {

	private final ImportRow row;

	private final List<RowMessage> messages = new ArrayList<>();

	RowReader(ImportRow row) {
		this.row = row;
	}

	int number() {
		return this.row.number();
	}

	ImportCell cell(String header) {
		return this.row.cell(header);
	}

	/** Chữ đã làm sạch; ô số thành chữ số. */
	String text(String header) {
		return read(() -> this.row.cell(header).text());
	}

	/** Chữ bắt buộc: ô trống là lỗi "Bắt buộc". */
	String requiredText(String header) {
		String text = text(header);
		if (text == null && !hasErrorIn(header)) {
			error(header, ImportMessages.REQUIRED);
		}
		return text;
	}

	Integer integer(String header, int min, int max) {
		return read(() -> this.row.cell(header).integer(min, max));
	}

	BigDecimal decimal(String header) {
		return read(() -> this.row.cell(header).decimal());
	}

	LocalDate date(String header) {
		return read(() -> this.row.cell(header).date());
	}

	/**
	 * Một giá trị liệt kê: khớp {@link ImportText#key} của ô với khoá trong {@code byKey}.
	 * @param labels các nhãn hợp lệ, để ghi vào lời nhắn khi ô không khớp
	 * @return {@code null} khi ô trống hoặc không khớp
	 */
	<E> E choice(String header, Map<String, E> byKey, List<String> labels) {
		String text = text(header);
		if (text == null) {
			return null;
		}
		E value = byKey.get(ImportText.key(text));
		if (value == null) {
			error(header, "Giá trị '" + text + "' không hợp lệ; dùng một trong: " + String.join(", ", labels));
		}
		return value;
	}

	void error(String column, String text) {
		this.messages.add(RowMessage.error(column, text));
	}

	void warning(String column, String text) {
		this.messages.add(RowMessage.warning(column, text));
	}

	void addAll(List<RowMessage> more) {
		this.messages.addAll(more);
	}

	boolean hasErrors() {
		return this.messages.stream().anyMatch(message -> message.level() == MessageLevel.ERROR);
	}

	boolean hasErrorIn(String column) {
		return this.messages.stream()
			.anyMatch(message -> message.level() == MessageLevel.ERROR && column.equals(message.column()));
	}

	RowReport report(String label, String code) {
		RowStatus status = hasErrors() ? RowStatus.ERROR
				: this.messages.isEmpty() ? RowStatus.OK : RowStatus.WARNING;
		return new RowReport(this.row.number(), label, code, status, List.copyOf(this.messages));
	}

	private <T> T read(Supplier<T> read) {
		try {
			return read.get();
		}
		catch (CellError ex) {
			error(ex.column(), ex.text());
			return null;
		}
	}

}
