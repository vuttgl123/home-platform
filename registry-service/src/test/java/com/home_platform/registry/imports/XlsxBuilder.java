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
		return withCachedValues(out.toByteArray(), cachedBySheet);
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

	/** Thư viện không ghi giá trị đã tính của công thức; thêm {@code <v>} vào XML của sheet. */
	private static byte[] withCachedValues(byte[] file, Map<Integer, Map<String, String>> cachedBySheet) {
		if (cachedBySheet.values().stream().allMatch(Map::isEmpty)) {
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
					byte[] content = in.getInputStream(entry).readAllBytes();
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

	private static final class SheetData {

		private final String name;

		private final List<List<Object>> rows = new ArrayList<>();

		private SheetData(String name) {
			this.name = name;
		}

	}

}
