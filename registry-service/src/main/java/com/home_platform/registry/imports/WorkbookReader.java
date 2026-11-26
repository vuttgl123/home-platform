package com.home_platform.registry.imports;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.home_platform.registry.export.ExcelLayout;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

/**
 * Đọc file nhập (spec N1): nhận năm sheet theo tên, cột theo tiêu đề ở dòng 1, bỏ dòng trống. Sheet
 * lạ, cột lạ thành ghi chú; file hỏng, quá nhiều dòng, thiếu cột bắt buộc thành {@link ImportFileException}.
 */
final class WorkbookReader {

	/**
	 * Giới hạn dòng của một file: 3.000 dòng xem trước mất khoảng 40 giây trên database UAT (đo ngày
	 * 2026-11-26; 5.000 dòng mất 66 tới 73 giây, quá mục tiêu 60 giây của spec).
	 */
	static final int MAX_ROWS = 3_000;

	static final String UNREADABLE = "Không đọc được file; hãy lưu lại dưới dạng Excel (.xlsx)";

	static final String TOO_MANY_ROWS = "File có hơn 3.000 dòng; hãy chia thành nhiều file";

	static final String NOTHING_TO_IMPORT = "File không có dòng nào để nhập";

	/** Tổng dung lượng giải nén tối đa: chặn "bom zip" làm hết bộ nhớ. */
	static final long MAX_UNZIPPED_BYTES = 100L * 1024 * 1024;

	/** Số dòng quét tối đa, kể cả dòng trống (định dạng cả cột có thể sinh hàng triệu dòng rỗng). */
	static final int MAX_SCANNED_ROWS = 50_000;

	static final String TOO_MANY_SCANNED_ROWS = "File có quá nhiều dòng (kể cả dòng trống); hãy xoá các dòng thừa";

	/** Đọc cả định dạng của ô; không có nó thì không biết ô số nào là ngày. */
	private static final ReadingOptions WITH_FORMATS = new ReadingOptions(true, false);

	/** File không có phần định dạng: đọc với định dạng thì thư viện báo lỗi, mà file như vậy không có ô ngày. */
	private static final ReadingOptions WITHOUT_FORMATS = new ReadingOptions(false, false);

	/** Mốc ngày của Excel (hệ 1900): số 1 là 01/01/1900, và Excel coi 1900 là năm nhuận. */
	private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);

	/** Mốc của hệ ngày 1904: số 0 là 01/01/1904. */
	private static final LocalDate EPOCH_1904 = LocalDate.of(1904, 1, 1);

	/** Đếm của một lần đọc, và mốc ngày của workbook. */
	private static final class Counts {

		private final LocalDate epoch;

		private int rows;

		private int scanned;

		private Counts(LocalDate epoch) {
			this.epoch = epoch;
		}

	}

	private WorkbookReader() {
	}

	/**
	 * @param extraHeaders tiêu đề cột mục tuỳ biến của từng sheet; cột có tiêu đề này không bị coi là
	 *     cột lạ
	 */
	static ImportWorkbook read(byte[] file, Map<SheetKind, Set<String>> extraHeaders) {
		ZipFacts facts = ZipFacts.scan(file, MAX_UNZIPPED_BYTES);
		List<String> notices = new ArrayList<>();
		List<String> errors = new ArrayList<>();
		List<ImportSheet> sheets = new ArrayList<>();
		Counts rowCount = new Counts(facts.date1904() ? EPOCH_1904 : EXCEL_EPOCH);
		ReadingOptions options = facts.hasStyles() ? WITH_FORMATS : WITHOUT_FORMATS;
		try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(file), options)) {
			for (Sheet sheet : workbook.getSheets().toList()) {
				SheetKind kind = kindOf(sheet.getName());
				if (kind == null) {
					if (!ImportText.key(ExcelLayout.GUIDE).equals(ImportText.key(sheet.getName()))) {
						notices.add("Sheet '" + ImportText.clean(sheet.getName()) + "' không có trong mẫu, bị bỏ qua");
					}
					continue;
				}
				ImportSheet read = readSheet(sheet, kind, extraHeaders.getOrDefault(kind, Set.of()), notices,
						rowCount);
				if (read != null) {
					List<String> missing = missingColumns(read);
					if (!missing.isEmpty()) {
						errors.add("Sheet " + kind.sheetName() + " thiếu cột bắt buộc: " + String.join(", ", missing)
								+ " (dòng 1 phải là tiêu đề cột)");
					}
					sheets.add(read);
				}
			}
		}
		catch (ImportFileException ex) {
			throw ex;
		}
		catch (IOException | RuntimeException ex) {
			throw new ImportFileException(UNREADABLE);
		}
		if (!errors.isEmpty()) {
			throw new ImportFileException(errors);
		}
		if (sheets.isEmpty()) {
			throw new ImportFileException(NOTHING_TO_IMPORT);
		}
		sheets.sort(Comparator.comparing(ImportSheet::kind));
		return new ImportWorkbook(List.copyOf(sheets), List.copyOf(notices));
	}

	private static SheetKind kindOf(String sheetName) {
		String key = ImportText.key(sheetName);
		for (SheetKind kind : SheetKind.values()) {
			if (ImportText.key(kind.sheetName()).equals(key)) {
				return kind;
			}
		}
		return null;
	}

	/** {@code null} khi sheet không có dòng dữ liệu nào: sheet đó coi như không có trong file. */
	private static ImportSheet readSheet(Sheet sheet, SheetKind kind, Set<String> extraHeaders, List<String> notices,
			Counts rowCount) throws IOException {
		Map<String, String> known = new HashMap<>();
		Stream.concat(kind.headers().stream(), extraHeaders.stream())
			.forEach(header -> known.put(ImportText.key(header), header));
		Map<Integer, String> columnHeaders = new HashMap<>();
		List<String> sheetNotices = new ArrayList<>();
		List<ImportRow> rows = new ArrayList<>();
		try (Stream<Row> stream = sheet.openStream()) {
			for (Row row : (Iterable<Row>) stream::iterator) {
				if (++rowCount.scanned > MAX_SCANNED_ROWS) {
					throw new ImportFileException(TOO_MANY_SCANNED_ROWS);
				}
				if (row.getRowNum() == 1) {
					readHeaders(row, kind, known, columnHeaders, sheetNotices);
					continue;
				}
				Map<String, ImportCell> cells = new LinkedHashMap<>();
				boolean blank = true;
				for (Map.Entry<Integer, String> column : columnHeaders.entrySet()) {
					Cell cell = (column.getKey() < row.getCellCount()) ? row.getCell(column.getKey()) : null;
					ImportCell value = toCell(column.getValue(), cell, rowCount.epoch);
					cells.put(column.getValue(), value);
					blank &= value.isBlank();
				}
				if (blank) {
					continue;
				}
				if (++rowCount.rows > MAX_ROWS) {
					throw new ImportFileException(TOO_MANY_ROWS);
				}
				rows.add(new ImportRow(row.getRowNum(), cells));
			}
		}
		if (rows.isEmpty() && !hasDataBelowHeaders(sheet)) {
			return null;
		}
		notices.addAll(sheetNotices);
		return new ImportSheet(kind, kind.sheetName(), List.copyOf(rows),
				new LinkedHashSet<>(columnHeaders.values()));
	}

	/**
	 * Sheet không có cột nào nhận ra được mà vẫn có chữ dưới dòng 1 (ví dụ dòng 1 là tên bảng, tiêu đề
	 * ở dòng 2): phải báo thiếu cột, không được coi như sheet trống.
	 */
	private static boolean hasDataBelowHeaders(Sheet sheet) throws IOException {
		try (Stream<Row> stream = sheet.openStream()) {
			return stream.anyMatch(row -> row.getRowNum() > 1 && hasText(row));
		}
	}

	private static boolean hasText(Row row) {
		for (int column = 0; column < row.getCellCount(); column++) {
			Cell cell = row.getCell(column);
			if (cell != null && cell.getType() != CellType.EMPTY && ImportText.clean(cell.getText()) != null) {
				return true;
			}
		}
		return false;
	}

	private static void readHeaders(Row row, SheetKind kind, Map<String, String> known,
			Map<Integer, String> columnHeaders, List<String> notices) {
		for (int column = 0; column < row.getCellCount(); column++) {
			Cell cell = row.getCell(column);
			String text = (cell != null) ? ImportText.clean(cell.getText()) : null;
			if (text == null) {
				continue;
			}
			String header = known.get(ImportText.key(text));
			if (header != null && !columnHeaders.containsValue(header)) {
				columnHeaders.put(column, header);
			}
			else {
				notices.add("Cột '" + text + "' ở sheet " + kind.sheetName() + " không có trong mẫu, bị bỏ qua");
			}
		}
	}

	private static List<String> missingColumns(ImportSheet sheet) {
		List<String> missing = new ArrayList<>();
		for (List<String> group : sheet.kind().required()) {
			if (group.stream().noneMatch(sheet.columns()::contains)) {
				missing.add(String.join(" hoặc ", group));
			}
		}
		return missing;
	}

	private static ImportCell toCell(String column, Cell cell, LocalDate epoch) {
		if (cell == null) {
			return ImportCell.blank(column);
		}
		return switch (cell.getType()) {
			case EMPTY -> ImportCell.blank(column);
			case STRING -> ImportCell.text(column, cell.getText());
			case BOOLEAN -> ImportCell.text(column, Boolean.TRUE.equals(cell.asBoolean()) ? "Có" : "Không");
			case NUMBER -> isDateFormatted(cell) ? ImportCell.date(column, fromExcel(cell.asNumber(), epoch))
					: ImportCell.number(column, asShown(cell, cell.asNumber()));
			case FORMULA -> fromFormula(column, cell, epoch);
			case ERROR -> ImportCell.uncomputed(column);
		};
	}

	/** Ô công thức: dùng giá trị Excel đã lưu cùng file. */
	private static ImportCell fromFormula(String column, Cell cell, LocalDate epoch) {
		String raw = ImportText.clean(cell.getRawValue());
		if (raw == null) {
			return ImportCell.uncomputed(column);
		}
		BigDecimal number;
		try {
			number = new BigDecimal(raw);
		}
		catch (NumberFormatException ex) {
			return ImportCell.text(column, raw);
		}
		return isDateFormatted(cell) ? ImportCell.date(column, fromExcel(number, epoch))
				: ImportCell.number(column, asShown(cell, number));
	}

	/**
	 * Định dạng ngày: các mã dựng sẵn của Excel cho ngày (14 tới 17, 22), hoặc một định dạng riêng có
	 * {@code y} hay {@code d} ngoài phần chữ trong ngoặc kép và ngoặc vuông.
	 */
	private static boolean isDateFormatted(Cell cell) {
		Integer id = cell.getDataFormatId();
		if (id != null && ((id >= 14 && id <= 17) || id == 22)) {
			return true;
		}
		String format = cell.getDataFormatString();
		if (format == null) {
			return false;
		}
		String bare = format.replaceAll("\"[^\"]*\"", "").replaceAll("\\[[^\\]]*\\]", "").toLowerCase();
		return bare.contains("y") || bare.contains("d");
	}

	/**
	 * Số như người dùng thấy: ô định dạng phần trăm lưu {@code 0.5} cho "50%", nên nhân 100. Không có
	 * bước này, "50%" ở cột "Tỷ lệ (%)" thành 0,5% mà không ai hay.
	 */
	private static BigDecimal asShown(Cell cell, BigDecimal value) {
		Integer id = cell.getDataFormatId();
		String format = cell.getDataFormatString();
		boolean percent = (id != null && (id == 9 || id == 10))
				|| (format != null && format.replaceAll("\"[^\"]*\"", "").replaceAll("\\[[^\\]]*\\]", "").contains("%"));
		return percent ? value.movePointRight(2) : value;
	}

	private static LocalDate fromExcel(BigDecimal serial, LocalDate epoch) {
		return epoch.plusDays(serial.longValue());
	}

}
