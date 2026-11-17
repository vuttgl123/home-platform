package com.home_platform.registry.export;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

/**
 * Ghi một sheet theo từng dòng, từ trái sang phải: mỗi lần gọi {@link #text}, {@link #number},
 * {@link #date} hay {@link #blank} là một ô, {@link #nextRow} kết thúc dòng.
 *
 * <p>Dòng đầu là tiêu đề, in đậm và được cố định. Cứ {@link #FLUSH_EVERY} dòng thì đẩy ra luồng, nên
 * bộ nhớ không lớn theo số dòng. Chữ ghi dạng chuỗi trong ô (không qua bảng chuỗi dùng chung của
 * file), vì bảng đó nằm trong bộ nhớ tới khi file xong.
 */
final class SheetWriter {

	static final int FLUSH_EVERY = 200;

	/** Giới hạn của Excel cho một ô. */
	private static final int MAX_CELL_CHARS = 32_767;

	/** Mã định dạng của Excel; hiển thị là ngày/tháng/năm. */
	private static final String DATE_FORMAT = "dd/mm/yyyy";

	private static final int MIN_WIDTH = 12;

	private final Worksheet sheet;

	private final int columns;

	private int row = 1;

	private int column;

	SheetWriter(Workbook workbook, String name, List<String> headers) {
		this.sheet = workbook.newWorksheet(name);
		this.columns = headers.size();
		for (int index = 0; index < headers.size(); index++) {
			String header = headers.get(index);
			this.sheet.inlineString(0, index, header);
			this.sheet.style(0, index).bold().set();
			this.sheet.width(index, Math.max(MIN_WIDTH, header.length() + 4));
		}
		this.sheet.freezePane(0, 1);
	}

	/** Ô chữ; {@code null} và chuỗi rỗng là ô trống. Chữ dài hơn giới hạn của Excel thì bị cắt. */
	void text(String value) {
		if (value != null && !value.isEmpty()) {
			this.sheet.inlineString(this.row, this.column, cut(value));
		}
		this.column++;
	}

	/** Ô số; {@code null} là ô trống. */
	void number(Number value) {
		if (value != null) {
			this.sheet.value(this.row, this.column, value);
		}
		this.column++;
	}

	/** Ô ngày, Excel lọc và sắp theo ngày được; {@code null} là ô trống. */
	void date(LocalDate value) {
		if (value != null) {
			this.sheet.value(this.row, this.column, value);
			this.sheet.style(this.row, this.column).format(DATE_FORMAT).set();
		}
		this.column++;
	}

	void blank() {
		this.column++;
	}

	/** Kết thúc dòng đang ghi. Dòng nào cũng phải kết thúc bằng lời gọi này. */
	void nextRow() {
		if (this.row % FLUSH_EVERY == 0) {
			flush();
		}
		this.row++;
		this.column = 0;
	}

	/** Bật lọc tự động trên tiêu đề và mọi dòng đã ghi, rồi đóng sheet. Phải gọi trước khi mở sheet kế. */
	void finish() {
		this.sheet.setAutoFilter(0, 0, this.row - 1, this.columns - 1);
		try {
			this.sheet.finish();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private void flush() {
		try {
			this.sheet.flush();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/** Cắt ở giới hạn của ô, không cắt đôi một ký tự ghép từ hai {@code char}. */
	private static String cut(String value) {
		if (value.length() <= MAX_CELL_CHARS) {
			return value;
		}
		int end = Character.isHighSurrogate(value.charAt(MAX_CELL_CHARS - 1)) ? MAX_CELL_CHARS - 1 : MAX_CELL_CHARS;
		return value.substring(0, end);
	}

}
