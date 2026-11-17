package com.home_platform.registry.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import com.home_platform.registry.export.XlsxReader.XlsxSheet;
import org.dhatim.fastexcel.Workbook;
import org.junit.jupiter.api.Test;

class SheetWriterTests {

	/** Ghi một sheet "S" với tiêu đề {@code headers} rồi đọc lại. */
	static byte[] write(List<String> headers, Consumer<SheetWriter> rows) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (Workbook workbook = new Workbook(out, "home-platform", "1.0")) {
			SheetWriter sheet = new SheetWriter(workbook, "S", headers);
			rows.accept(sheet);
			sheet.finish();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		return out.toByteArray();
	}

	static XlsxSheet sheet(List<String> headers, Consumer<SheetWriter> rows) {
		return XlsxReader.read(write(headers, rows)).get(0);
	}

	@Test
	void writesHeadersThenTypedCells() {
		XlsxSheet sheet = sheet(List.of("Tên", "Số", "Ngày", "Trống", "Cuối"), row -> {
			row.text("Tủ lạnh");
			row.number(3);
			row.date(LocalDate.of(2026, 1, 5));
			row.blank();
			row.text("x");
			row.nextRow();
			row.text(null);
			row.number(null);
			row.date(null);
			row.text("");
			row.number(new BigDecimal("12.50"));
			row.nextRow();
		});

		assertThat(sheet.name()).isEqualTo("S");
		assertThat(sheet.headers()).containsExactly("Tên", "Số", "Ngày", "Trống", "Cuối");
		assertThat(sheet.dataRowCount()).isEqualTo(2);
		assertThat(sheet.cell(0, "Tên").isText()).isTrue();
		assertThat(sheet.cell(0, "Tên").text()).isEqualTo("Tủ lạnh");
		assertThat(sheet.cell(0, "Số").isNumber()).isTrue();
		assertThat(sheet.cell(0, "Số").number()).isEqualByComparingTo("3");
		assertThat(sheet.cell(0, "Ngày").isDate()).isTrue();
		assertThat(sheet.cell(0, "Ngày").date()).isEqualTo(LocalDate.of(2026, 1, 5));
		assertThat(sheet.cell(0, "Ngày").format()).isEqualToIgnoringCase("dd/mm/yyyy");
		assertThat(sheet.cell(0, "Trống").isBlank()).isTrue();
		assertThat(sheet.cell(0, "Cuối").text()).isEqualTo("x");
		assertThat(sheet.cell(1, "Tên").isBlank()).isTrue();
		assertThat(sheet.cell(1, "Số").isBlank()).isTrue();
		assertThat(sheet.cell(1, "Ngày").isBlank()).isTrue();
		assertThat(sheet.cell(1, "Trống").isBlank()).isTrue();
		assertThat(sheet.cell(1, "Cuối").number()).isEqualByComparingTo("12.5");
	}

	@Test
	void headerRowIsFrozenAndFilterable() {
		byte[] file = write(List.of("A", "B", "C"), row -> {
			for (int i = 0; i < 4; i++) {
				row.text("x");
				row.nextRow();
			}
		});

		String xml = XlsxReader.sheetXml(file, 1);
		assertThat(xml).contains("<pane").contains("ySplit=\"1\"");
		assertThat(xml).contains("<autoFilter ref=\"A1:C5\"");
	}

	@Test
	void sheetWithoutRowsStillHasHeaders() {
		byte[] file = write(List.of("A", "B"), row -> {
		});

		assertThat(XlsxReader.read(file).get(0).headers()).containsExactly("A", "B");
		assertThat(XlsxReader.read(file).get(0).dataRowCount()).isZero();
		assertThat(XlsxReader.sheetXml(file, 1)).contains("<autoFilter ref=\"A1:B1\"");
	}

	/** Chữ bắt đầu bằng "=" vẫn là chữ, không thành công thức. */
	@Test
	void textThatLooksLikeAFormulaStaysText() {
		XlsxSheet sheet = sheet(List.of("A"), row -> {
			row.text("=1+1");
			row.nextRow();
		});

		assertThat(sheet.cell(0, "A").isText()).isTrue();
		assertThat(sheet.cell(0, "A").text()).isEqualTo("=1+1");
	}

	/** Một ô Excel chứa tối đa 32.767 ký tự; dài hơn thì Excel báo file hỏng. */
	@Test
	void longTextIsCutToTheCellLimit() {
		XlsxSheet sheet = sheet(List.of("A"), row -> {
			row.text("a".repeat(40000));
			row.nextRow();
		});

		assertThat(sheet.cell(0, "A").text()).hasSize(32767);
	}

	/** Ký tự điều khiển không hợp lệ trong XML không được làm hỏng file. */
	@Test
	void controlCharactersDoNotBreakTheFile() {
		XlsxSheet sheet = sheet(List.of("A", "B"), row -> {
			row.text("a\u0001b\u000Bc");
			row.text("dòng 1\ndòng 2");
			row.nextRow();
		});

		assertThat(sheet.cell(0, "A").text()).isEqualTo("abc");
		assertThat(sheet.cell(0, "B").text()).isEqualTo("dòng 1\ndòng 2");
	}

	/** Nhiều dòng hơn một lần đẩy ra luồng: không mất, không lặp, đúng thứ tự. */
	@Test
	void manyRowsSurviveFlushing() {
		int count = SheetWriter.FLUSH_EVERY * 2 + 7;
		XlsxSheet sheet = sheet(List.of("N"), row -> {
			for (int i = 0; i < count; i++) {
				row.number(i);
				row.nextRow();
			}
		});

		assertThat(sheet.dataRowCount()).isEqualTo(count);
		assertThat(sheet.cell(0, "N").number()).isEqualByComparingTo("0");
		assertThat(sheet.cell(count - 1, "N").number()).isEqualByComparingTo(String.valueOf(count - 1));
	}

}
