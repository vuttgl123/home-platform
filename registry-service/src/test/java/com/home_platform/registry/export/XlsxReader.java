package com.home_platform.registry.export;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;

/** Đọc lại một file xlsx trong test: tên sheet, tiêu đề, giá trị và kiểu của từng ô. */
final class XlsxReader {

	/** Đọc cả định dạng của ô; không có nó thì không phân biệt được ô ngày với ô số. */
	private static final ReadingOptions WITH_FORMATS = new ReadingOptions(true, false);

	private XlsxReader() {
	}

	static List<XlsxSheet> read(byte[] bytes) {
		try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(bytes), WITH_FORMATS)) {
			List<XlsxSheet> sheets = new ArrayList<>();
			for (var sheet : workbook.getSheets().toList()) {
				List<List<XlsxCell>> rows = new ArrayList<>();
				for (Row row : sheet.read()) {
					while (rows.size() < row.getRowNum() - 1) {
						rows.add(List.of());
					}
					List<XlsxCell> cells = new ArrayList<>();
					for (int column = 0; column < row.getCellCount(); column++) {
						cells.add(new XlsxCell(row.getCell(column)));
					}
					rows.add(cells);
				}
				sheets.add(new XlsxSheet(sheet.getName(), rows));
			}
			return sheets;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * XML thô của sheet thứ {@code number} (từ 1), để kiểm thứ mà bộ đọc không đưa ra: khung cố định, lọc
	 * tự động. Mở qua một file tạm vì file xlsx ghi dạng luồng chỉ đọc đúng được qua mục lục của zip.
	 */
	static String sheetXml(byte[] bytes, int number) {
		Path file = null;
		try {
			file = Files.createTempFile("export", ".xlsx");
			Files.write(file, bytes);
			try (ZipFile zip = new ZipFile(file.toFile())) {
				ZipEntry entry = zip.getEntry("xl/worksheets/sheet" + number + ".xml");
				if (entry == null) {
					throw new IllegalArgumentException("No sheet " + number);
				}
				return new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		finally {
			if (file != null) {
				file.toFile().delete();
			}
		}
	}

	/**
	 * Một sheet: dòng đầu là tiêu đề, các dòng sau là dữ liệu. Dòng không có ô nào thì không có trong
	 * file: ở giữa nó là một dòng rỗng, ở cuối nó không được đếm.
	 */
	record XlsxSheet(String name, List<List<XlsxCell>> rows) {

		List<String> headers() {
			return this.rows.get(0).stream().map(XlsxCell::text).toList();
		}

		int dataRowCount() {
			return this.rows.size() - 1;
		}

		/**
		 * Ô của dòng dữ liệu thứ {@code row} (từ 0) ở cột có tiêu đề {@code header}; ô hay dòng không được
		 * ghi là ô trống.
		 */
		XlsxCell cell(int row, String header) {
			int column = headers().indexOf(header);
			if (column < 0) {
				throw new IllegalArgumentException("No column " + header + " in " + headers());
			}
			List<XlsxCell> cells = (row + 1 < this.rows.size()) ? this.rows.get(row + 1) : List.of();
			return (column < cells.size()) ? cells.get(column) : new XlsxCell(null);
		}

		/** Chữ của mọi dòng dữ liệu ở một cột. */
		List<String> column(String header) {
			List<String> texts = new ArrayList<>();
			for (int row = 0; row < dataRowCount(); row++) {
				texts.add(cell(row, header).text());
			}
			return texts;
		}

	}

	record XlsxCell(Cell cell) {

		boolean isBlank() {
			return this.cell == null || this.cell.getType() == CellType.EMPTY;
		}

		boolean isText() {
			return this.cell != null && this.cell.getType() == CellType.STRING;
		}

		/** Ô số không mang định dạng ngày. */
		boolean isNumber() {
			return this.cell != null && this.cell.getType() == CellType.NUMBER && !isDate();
		}

		/** Ô số mang định dạng ngày: Excel hiện là một ngày và lọc, sắp theo ngày được. */
		boolean isDate() {
			return this.cell != null && this.cell.getType() == CellType.NUMBER && format() != null
					&& format().toLowerCase().contains("yy");
		}

		String format() {
			return (this.cell != null) ? this.cell.getDataFormatString() : null;
		}

		String text() {
			return isBlank() ? "" : this.cell.getText();
		}

		BigDecimal number() {
			return this.cell.asNumber();
		}

		LocalDate date() {
			return this.cell.asDate().toLocalDate();
		}

	}

}
