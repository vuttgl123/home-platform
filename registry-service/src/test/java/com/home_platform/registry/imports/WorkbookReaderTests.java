package com.home_platform.registry.imports;

import static com.home_platform.registry.imports.XlsxBuilder.formula;
import static com.home_platform.registry.imports.XlsxBuilder.workbook;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class WorkbookReaderTests {

	@Test
	void readsKnownSheetsInImportOrder() {
		ImportWorkbook book = read(workbook().sheet("Đồ đạc", "Tên", "Ở đâu")
			.row("Quạt", "P301")
			.sheet("Người", "Họ tên")
			.row("Nguyễn An")
			.build());

		assertThat(book.sheets()).extracting(ImportSheet::kind).containsExactly(SheetKind.PERSON, SheetKind.ITEM);
		assertThat(book.sheets().get(1).rows().get(0).cell("Tên").text()).isEqualTo("Quạt");
		assertThat(book.sheets().get(1).name()).isEqualTo("Đồ đạc");
		assertThat(book.notices()).isEmpty();
	}

	@Test
	void unknownSheetsAndColumnsBecomeNotices() {
		ImportWorkbook book = read(workbook().sheet("Hướng dẫn", "Mục", "Cách ghi")
			.row("Thứ tự", "…")
			.sheet("Ghi chép", "Gì đó")
			.row("x")
			.sheet("Người", "Họ tên", "Tầng", "Quê quán")
			.row("Nguyễn An", "3", "Huế")
			.build(), Map.of(SheetKind.PERSON, Set.of("Quê quán")));

		assertThat(book.notices()).containsExactly("Sheet 'Ghi chép' không có trong mẫu, bị bỏ qua",
				"Cột 'Tầng' ở sheet Người không có trong mẫu, bị bỏ qua");
		ImportSheet people = book.sheets().get(0);
		assertThat(people.columns()).containsExactlyInAnyOrder("Họ tên", "Quê quán");
		assertThat(people.rows().get(0).cell("Quê quán").text()).isEqualTo("Huế");
	}

	@Test
	void headersMatchIgnoringCaseSpacesAndComposition() {
		String sheet = Normalizer.normalize("Người", Normalizer.Form.NFD);

		ImportWorkbook book = read(workbook().sheet(sheet, " họ  TÊN ", "điện thoại").row("Nguyễn An", "0901").build());

		ImportRow row = book.sheets().get(0).rows().get(0);
		assertThat(row.cell("Họ tên").text()).isEqualTo("Nguyễn An");
		assertThat(row.cell("Điện thoại").text()).isEqualTo("0901");
		assertThat(row.cell("Email").isBlank()).isTrue();
	}

	@Test
	void blankRowsAreSkippedAndRowNumbersAreExcelRows() {
		ImportWorkbook book = read(workbook().sheet("Người", "Họ tên")
			.row("Nguyễn An")
			.row((Object) null)
			.row("Trần Bình")
			.row("  ")
			.build());

		assertThat(book.sheets().get(0).rows()).extracting(ImportRow::number).containsExactly(2, 4);
	}

	@Test
	void cellTypesAreKept() {
		ImportWorkbook book = read(workbook().sheet("Người", "Họ tên", "Điện thoại", "Ngày sinh")
			.row("Nguyễn An", 901234567, LocalDate.of(1990, 5, 1))
			.row("Trần Bình", 1.203004567E9, "01/02/1991")
			.build());

		ImportRow first = book.sheets().get(0).rows().get(0);
		assertThat(first.cell("Điện thoại").text()).isEqualTo("901234567");
		assertThat(first.cell("Ngày sinh").date()).isEqualTo(LocalDate.of(1990, 5, 1));
		ImportRow second = book.sheets().get(0).rows().get(1);
		assertThat(second.cell("Điện thoại").text()).isEqualTo("1203004567");
		assertThat(second.cell("Ngày sinh").date()).isEqualTo(LocalDate.of(1991, 2, 1));
	}

	@Test
	void formulaUsesTheStoredValueOrFails() {
		ImportWorkbook book = read(workbook().sheet("Đồ đạc", "Tên", "Ở đâu", "Số lượng", "Ngày đặt")
			.row("Quạt", "P301", formula("1+1", 2), formula("DATE(2026,3,5)", LocalDate.of(2026, 3, 5)))
			.row("Đèn", "P301", formula("2+2", null), null)
			.build());

		ImportRow computed = book.sheets().get(0).rows().get(0);
		assertThat(computed.cell("Số lượng").integer(1, 9999)).isEqualTo(2);
		assertThat(computed.cell("Ngày đặt").date()).isEqualTo(LocalDate.of(2026, 3, 5));
		ImportCell uncomputed = book.sheets().get(0).rows().get(1).cell("Số lượng");
		assertThatThrownBy(() -> uncomputed.integer(1, 9999)).isInstanceOf(CellError.class)
			.hasMessage("Ô có công thức chưa tính");
	}

	@Test
	void missingRequiredColumnIsAFileError() {
		assertFileErrors(workbook().sheet("Người", "Điện thoại").row("0901").build(),
				"Sheet Người thiếu cột bắt buộc: Họ tên (dòng 1 phải là tiêu đề cột)");
		assertFileErrors(workbook().sheet("Cư trú", "Người", "Từ ngày").row("Nguyễn An", "01/03/2026").build(),
				"Sheet Cư trú thiếu cột bắt buộc: Địa điểm hoặc Mã địa điểm (dòng 1 phải là tiêu đề cột)");
		assertFileErrors(workbook().sheet("Địa điểm", "Mã").row("P1").sheet("Đồ đạc", "Mã").row("Q1").build(),
				"Sheet Địa điểm thiếu cột bắt buộc: Tên, Loại (dòng 1 phải là tiêu đề cột)",
				"Sheet Đồ đạc thiếu cột bắt buộc: Tên, Ở đâu (dòng 1 phải là tiêu đề cột)");
	}

	@Test
	void sheetWithOnlyHeadersIsLeftOutAndNotChecked() {
		ImportWorkbook book = read(workbook().sheet("Địa điểm", "Mã")
			.sheet("Người", "Họ tên")
			.row("Nguyễn An")
			.build());

		assertThat(book.sheets()).extracting(ImportSheet::kind).containsExactly(SheetKind.PERSON);
	}

	@Test
	void titleRowAboveHeadersExplainsRowOne() {
		assertFileErrors(workbook().sheet("Người").row("DANH SÁCH NGƯỜI Ở").row("Họ tên").row("Nguyễn An").build(),
				"Sheet Người thiếu cột bắt buộc: Họ tên (dòng 1 phải là tiêu đề cột)");
	}

	@Test
	void tooManyRowsIsAFileError() {
		XlsxBuilder file = workbook().sheet("Người", "Họ tên");
		for (int row = 0; row < 2_000; row++) {
			file.row("Người " + row);
		}
		file.sheet("Đồ đạc", "Tên", "Ở đâu");
		for (int row = 0; row < 1_001; row++) {
			file.row("Món " + row, "P301");
		}

		assertFileErrors(file.build(), "File có hơn 3.000 dòng; hãy chia thành nhiều file");
	}

	@Test
	void exactlyTheLimitIsRead() {
		XlsxBuilder file = workbook().sheet("Người", "Họ tên");
		for (int row = 0; row < 3_000; row++) {
			file.row("Người " + row);
		}

		assertThat(read(file.build()).sheets().get(0).rows()).hasSize(3_000);
	}

	@Test
	void notAnXlsxIsAFileError() {
		assertFileErrors("Họ tên\nNguyễn An\n".getBytes(StandardCharsets.UTF_8),
				"Không đọc được file; hãy lưu lại dưới dạng Excel (.xlsx)");
		assertFileErrors(new byte[0], "Không đọc được file; hãy lưu lại dưới dạng Excel (.xlsx)");
	}

	@Test
	void nothingToImportIsAFileError() {
		assertFileErrors(workbook().sheet("Người", "Họ tên").sheet("Đồ đạc", "Tên", "Ở đâu").build(),
				"File không có dòng nào để nhập");
	}

	private static ImportWorkbook read(byte[] file) {
		return read(file, Map.of());
	}

	private static ImportWorkbook read(byte[] file, Map<SheetKind, Set<String>> extraHeaders) {
		return WorkbookReader.read(new ByteArrayInputStream(file), extraHeaders);
	}

	private static void assertFileErrors(byte[] file, String... errors) {
		assertThatThrownBy(() -> read(file)).isInstanceOfSatisfying(ImportFileException.class,
				ex -> assertThat(ex.errors()).containsExactly(errors));
	}

}
