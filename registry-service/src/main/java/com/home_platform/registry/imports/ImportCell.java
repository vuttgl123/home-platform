package com.home_platform.registry.imports;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Một ô đã đọc của file nhập: trống, chữ, số, ngày, hay công thức chưa tính. Các lệnh đọc trả
 * {@code null} cho ô trống và ném {@link CellError} khi giá trị không hợp với cột.
 */
final class ImportCell {

	static final String BAD_DATE = "Ngày không hợp lệ; ghi dạng dd/mm/yyyy";

	static final String NOT_INTEGER = "Phải là số nguyên";

	static final String NOT_NUMBER = "Phải là số";

	static final String UNCOMPUTED = "Ô có công thức chưa tính";

	private static final DateTimeFormatter VIETNAMESE_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private static final Pattern DAY_FIRST = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");

	private static final Pattern ISO = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");

	private static final Pattern INTEGER = Pattern.compile("-?\\d+");

	private enum Kind {

		BLANK, TEXT, NUMBER, DATE, UNCOMPUTED

	}

	private final String column;

	private final Kind kind;

	private final String text;

	private final BigDecimal number;

	private final LocalDate date;

	private ImportCell(String column, Kind kind, String text, BigDecimal number, LocalDate date) {
		this.column = column;
		this.kind = kind;
		this.text = text;
		this.number = number;
		this.date = date;
	}

	static ImportCell blank(String column) {
		return new ImportCell(column, Kind.BLANK, null, null, null);
	}

	/** Ô chữ; chữ chỉ có khoảng trắng là ô trống. */
	static ImportCell text(String column, String value) {
		String cleaned = ImportText.clean(value);
		return (cleaned == null) ? blank(column) : new ImportCell(column, Kind.TEXT, cleaned, null, null);
	}

	static ImportCell number(String column, BigDecimal value) {
		return (value == null) ? blank(column) : new ImportCell(column, Kind.NUMBER, null, value, null);
	}

	static ImportCell date(String column, LocalDate value) {
		return (value == null) ? blank(column) : new ImportCell(column, Kind.DATE, null, null, value);
	}

	/** Ô công thức mà file không lưu giá trị đã tính, hoặc ô báo lỗi của Excel ({@code #N/A}). */
	static ImportCell uncomputed(String column) {
		return new ImportCell(column, Kind.UNCOMPUTED, null, null, null);
	}

	String column() {
		return this.column;
	}

	boolean isBlank() {
		return this.kind == Kind.BLANK;
	}

	/**
	 * Chữ của ô. Ô số thành các chữ số như đã gõ (không có {@code .0}, không có {@code E+9}), vì số
	 * điện thoại, số giấy tờ hay bị Excel lưu thành số; ô ngày thành {@code dd/MM/yyyy}.
	 */
	String text() {
		return switch (this.kind) {
			case BLANK -> null;
			case TEXT -> this.text;
			case NUMBER -> plain(this.number);
			case DATE -> VIETNAMESE_DATE.format(this.date);
			case UNCOMPUTED -> throw error(UNCOMPUTED);
		};
	}

	/** Số nguyên từ {@code min} tới {@code max}; nhận ô số nguyên ({@code 12}, {@code 12.0}) và chữ số. */
	Integer integer(int min, int max) {
		BigDecimal value = switch (this.kind) {
			case BLANK -> null;
			case NUMBER -> this.number;
			case TEXT -> INTEGER.matcher(this.text).matches() ? new BigDecimal(this.text) : null;
			case DATE -> null;
			case UNCOMPUTED -> throw error(UNCOMPUTED);
		};
		if (this.kind == Kind.BLANK) {
			return null;
		}
		if (value == null || value.stripTrailingZeros().scale() > 0) {
			throw error(NOT_INTEGER);
		}
		if (value.compareTo(BigDecimal.valueOf(min)) < 0 || value.compareTo(BigDecimal.valueOf(max)) > 0) {
			throw error("Từ " + min + " tới " + max);
		}
		return value.intValueExact();
	}

	/** Số; chữ nhận cả dấu phẩy thập phân ({@code 50,5}) như cách viết ở Việt Nam. */
	BigDecimal decimal() {
		return switch (this.kind) {
			case BLANK -> null;
			case NUMBER -> this.number;
			case TEXT -> parseDecimal(this.text);
			case DATE -> throw error(NOT_NUMBER);
			case UNCOMPUTED -> throw error(UNCOMPUTED);
		};
	}

	/** Ngày: ô ngày của Excel, hoặc chữ {@code dd/mm/yyyy}, {@code d/m/yyyy}, {@code yyyy-mm-dd}. */
	LocalDate date() {
		return switch (this.kind) {
			case BLANK -> null;
			case DATE -> this.date;
			case TEXT -> parseDate(this.text);
			case NUMBER -> throw error(BAD_DATE);
			case UNCOMPUTED -> throw error(UNCOMPUTED);
		};
	}

	CellError error(String message) {
		return new CellError(this.column, message);
	}

	private BigDecimal parseDecimal(String value) {
		String normalized = (value.indexOf(',') >= 0 && value.indexOf('.') < 0) ? value.replace(',', '.') : value;
		try {
			return new BigDecimal(normalized);
		}
		catch (NumberFormatException ex) {
			throw error(NOT_NUMBER);
		}
	}

	private LocalDate parseDate(String value) {
		Matcher dayFirst = DAY_FIRST.matcher(value);
		Matcher iso = ISO.matcher(value);
		try {
			if (dayFirst.matches()) {
				return LocalDate.of(Integer.parseInt(dayFirst.group(3)), Integer.parseInt(dayFirst.group(2)),
						Integer.parseInt(dayFirst.group(1)));
			}
			if (iso.matches()) {
				return LocalDate.of(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)),
						Integer.parseInt(iso.group(3)));
			}
		}
		catch (DateTimeException ex) {
			// ngày không có thật (31/02), rơi xuống lỗi chung bên dưới
		}
		throw error(BAD_DATE);
	}

	private static String plain(BigDecimal value) {
		BigDecimal stripped = value.stripTrailingZeros();
		return ((stripped.scale() < 0) ? stripped.setScale(0) : stripped).toPlainString();
	}

}
