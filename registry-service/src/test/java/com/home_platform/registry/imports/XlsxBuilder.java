package com.home_platform.registry.imports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

/**
 * Dựng file xlsx cho test nhập. Mỗi {@link #sheet} mở một sheet mới, dòng 1 là tiêu đề (nếu có);
 * mỗi {@link #row} là dòng kế tiếp. Ô {@code null} là ô trống; {@code LocalDate} là ô ngày có định
 * dạng; {@link #formula} là ô công thức.
 */
final class XlsxBuilder {

	private final List<SheetData> sheets = new ArrayList<>();

	private boolean withoutStyles;

	private boolean date1904;

	private long extraBytes;

	static XlsxBuilder workbook() {
		return new XlsxBuilder();
	}

	XlsxBuilder sheet(String name, String... headers) {
		SheetData sheet = new SheetData(name);
		if (headers.length > 0) {
			sheet.rows.add(List.of((Object[]) headers));
		}
		this.sheets.add(sheet);
		return this;
	}

	XlsxBuilder row(Object... cells) {
		this.sheets.getLast().rows.add(java.util.Arrays.asList(cells));
		return this;
	}

	/** Bỏ {@code xl/styles.xml} khỏi file, như vài công cụ tạo xlsx tối giản. */
	XlsxBuilder withoutStyles() {
		this.withoutStyles = true;
		return this;
	}

	/** Bật hệ ngày 1904 của workbook (Excel trên Mac cũ). */
	XlsxBuilder date1904() {
		this.date1904 = true;
		return this;
	}

	/** Thêm một mục zip toàn số 0, giải nén ra {@code bytes} byte nhưng nén rất nhỏ ("bom zip"). */
	XlsxBuilder extraEntry(long bytes) {
		this.extraBytes = bytes;
		return this;
	}

	/** Ô ngày ghi bằng số thứ tự ngày cho trước (theo hệ ngày của workbook), định dạng ngày. */
	static DateSerial dateSerial(long serial) {
		return new DateSerial(serial);
	}

	/** Ô số định dạng phần trăm: Excel lưu {@code 0.5} cho ô người dùng gõ "50%". */
	static Percent percent(double value) {
		return new Percent(value);
	}

	/** Ô công thức; {@code cached} {@code null} là chưa tính (như file do thư viện ghi ra). */
	static Formula formula(String expression, Object cached) {
		return new Formula(expression, cached);
	}

	byte[] build() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Map<Integer, Map<String, String>> cachedBySheet = new LinkedHashMap<>();
		try {
			Workbook workbook = new Workbook(out, "test", "1.0");
			for (int index = 0; index < this.sheets.size(); index++) {
				SheetData data = this.sheets.get(index);
				Worksheet sheet = workbook.newWorksheet(data.name);
				Map<String, String> cached = new LinkedHashMap<>();
				for (int row = 0; row < data.rows.size(); row++) {
					List<Object> cells = data.rows.get(row);
					for (int column = 0; column < cells.size(); column++) {
						write(sheet, row, column, cells.get(column), cached);
					}
				}
				sheet.finish();
				cachedBySheet.put(index + 1, cached);
			}
			workbook.finish();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return rewrite(out.toByteArray(), cachedBySheet);
	}

	private static void write(Worksheet sheet, int row, int column, Object value, Map<String, String> cached) {
		switch (value) {
			case null -> {
			}
			case String text -> sheet.value(row, column, text);
			case Number number -> sheet.value(row, column, number);
			case LocalDate date -> {
				sheet.value(row, column, date);
				sheet.style(row, column).format("dd/mm/yyyy").set();
			}
			case DateSerial serial -> {
				sheet.value(row, column, serial.value());
				sheet.style(row, column).format("dd/mm/yyyy").set();
			}
			case Percent percent -> {
				sheet.value(row, column, percent.value());
				sheet.style(row, column).format("0%").set();
			}
			case Formula formula -> {
				sheet.formula(row, column, formula.expression());
				if (formula.cached() instanceof LocalDate date) {
					sheet.style(row, column).format("dd/mm/yyyy").set();
					cached.put(formula.expression(), String.valueOf(date.toEpochDay() + 25569));
				}
				else if (formula.cached() instanceof Number number) {
					cached.put(formula.expression(), new BigDecimal(number.toString()).toPlainString());
				}
				else if (formula.cached() != null) {
					throw new IllegalArgumentException("Cached value must be a number or a date");
				}
			}
			default -> throw new IllegalArgumentException("Unsupported cell " + value);
		}
	}

	/**
	 * Sửa file sau khi thư viện ghi: thêm {@code <v>} cho công thức (thư viện không ghi giá trị đã tính),
	 * bỏ {@code styles.xml}, bật hệ ngày 1904, thêm mục zip lớn.
	 */
	private byte[] rewrite(byte[] file, Map<Integer, Map<String, String>> cachedBySheet) {
		if (cachedBySheet.values().stream().allMatch(Map::isEmpty) && !this.withoutStyles && !this.date1904
				&& this.extraBytes == 0) {
			return file;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Path temp = null;
		try {
			// File ghi dạng luồng chỉ đọc đúng được qua mục lục của zip, nên phải mở bằng ZipFile.
			temp = Files.createTempFile("import", ".xlsx");
			Files.write(temp, file);
			try (ZipFile in = new ZipFile(temp.toFile()); ZipOutputStream zip = new ZipOutputStream(out)) {
				for (ZipEntry entry : java.util.Collections.list(in.entries())) {
					if (this.withoutStyles && entry.getName().equals("xl/styles.xml")) {
						continue;
					}
					byte[] content = in.getInputStream(entry).readAllBytes();
					if (this.withoutStyles && (entry.getName().endsWith(".rels") || entry.getName().equals("[Content_Types].xml"))) {
						content = new String(content, StandardCharsets.UTF_8)
							.replaceAll("<Relationship [^>]*styles\\.xml\"/>", "")
							.replaceAll("<Override [^>]*styles\\.xml\"[^>]*/>", "")
							.getBytes(StandardCharsets.UTF_8);
					}
					if (this.date1904 && entry.getName().equals("xl/workbook.xml")) {
						String xml = new String(content, StandardCharsets.UTF_8);
						if (xml.contains("date1904=\"")) {
							xml = xml.replaceFirst("date1904=\"[^\"]*\"", "date1904=\"1\"");
						}
						else {
							xml = xml.contains("<workbookPr") ? xml.replaceFirst("<workbookPr", "<workbookPr date1904=\"1\"")
									: xml.replaceFirst("(<workbook[^>]*>)", "$1<workbookPr date1904=\"1\"/>");
						}
						content = xml.getBytes(StandardCharsets.UTF_8);
					}
					for (Map.Entry<Integer, Map<String, String>> sheet : cachedBySheet.entrySet()) {
						if (entry.getName().equals("xl/worksheets/sheet" + sheet.getKey() + ".xml")) {
							String xml = new String(content, StandardCharsets.UTF_8);
							for (Map.Entry<String, String> formula : sheet.getValue().entrySet()) {
								String tag = "<f>" + formula.getKey() + "</f>";
								xml = xml.replace(tag, tag + "<v>" + formula.getValue() + "</v>");
							}
							content = xml.getBytes(StandardCharsets.UTF_8);
						}
					}
					zip.putNextEntry(new ZipEntry(entry.getName()));
					zip.write(content);
					zip.closeEntry();
				}
				if (this.extraBytes > 0) {
					zip.putNextEntry(new ZipEntry("xl/media/zeros.bin"));
					byte[] chunk = new byte[1 << 16];
					for (long written = 0; written < this.extraBytes; written += chunk.length) {
						zip.write(chunk, 0, (int) Math.min(chunk.length, this.extraBytes - written));
					}
					zip.closeEntry();
				}
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		finally {
			if (temp != null) {
				temp.toFile().delete();
			}
		}
		return out.toByteArray();
	}

	record Formula(String expression, Object cached) {
	}

	record Percent(double value) {
	}

	record DateSerial(long value) {
	}

	private static final class SheetData {

		private final String name;

		private final List<List<Object>> rows = new ArrayList<>();

		private SheetData(String name) {
			this.name = name;
		}

	}

}
