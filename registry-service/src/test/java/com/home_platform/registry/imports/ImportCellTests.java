package com.home_platform.registry.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ImportCellTests {

	private static final String COLUMN = "Cột";

	@Test
	void numericCellInATextColumnBecomesPlainDigits() {
		assertThat(ImportCell.number(COLUMN, new BigDecimal("901234567")).text()).isEqualTo("901234567");
		assertThat(ImportCell.number(COLUMN, new BigDecimal("1.203004567E9")).text()).isEqualTo("1203004567");
		assertThat(ImportCell.number(COLUMN, new BigDecimal("12.50")).text()).isEqualTo("12.5");
		assertThat(ImportCell.number(COLUMN, new BigDecimal("12.0")).text()).isEqualTo("12");
	}

	@Test
	void textIsCleanedAndDatesAreWrittenTheVietnameseWay() {
		assertThat(ImportCell.text(COLUMN, " Toà A ").text()).isEqualTo("Toà A");
		assertThat(ImportCell.date(COLUMN, LocalDate.of(2026, 3, 5)).text()).isEqualTo("05/03/2026");
		assertThat(ImportCell.blank(COLUMN).text()).isNull();
	}

	@Test
	void integerAcceptsWholeNumbersAndDigitText() {
		assertThat(ImportCell.number(COLUMN, new BigDecimal("12")).integer(0, 9999)).isEqualTo(12);
		assertThat(ImportCell.number(COLUMN, new BigDecimal("12.0")).integer(0, 9999)).isEqualTo(12);
		assertThat(ImportCell.text(COLUMN, "12").integer(0, 9999)).isEqualTo(12);
		assertThat(ImportCell.blank(COLUMN).integer(0, 9999)).isNull();
		assertError(() -> ImportCell.number(COLUMN, new BigDecimal("12.5")).integer(0, 9999), "Phải là số nguyên");
		assertError(() -> ImportCell.text(COLUMN, "mười").integer(0, 9999), "Phải là số nguyên");
		assertError(() -> ImportCell.number(COLUMN, new BigDecimal("10000")).integer(0, 9999), "Từ 0 tới 9999");
	}

	@Test
	void decimalAcceptsNumbersAndBothSeparators() {
		assertThat(ImportCell.number(COLUMN, new BigDecimal("50.5")).decimal()).isEqualByComparingTo("50.5");
		assertThat(ImportCell.text(COLUMN, "50,5").decimal()).isEqualByComparingTo("50.5");
		assertThat(ImportCell.text(COLUMN, "50.5").decimal()).isEqualByComparingTo("50.5");
		assertError(() -> ImportCell.text(COLUMN, "một nửa").decimal(), "Phải là số");
	}

	@Test
	void datesFromExcelAndThreeTextForms() {
		LocalDate expected = LocalDate.of(2026, 3, 5);

		assertThat(ImportCell.date(COLUMN, expected).date()).isEqualTo(expected);
		assertThat(ImportCell.text(COLUMN, "05/03/2026").date()).isEqualTo(expected);
		assertThat(ImportCell.text(COLUMN, "5/3/2026").date()).isEqualTo(expected);
		assertThat(ImportCell.text(COLUMN, "2026-03-05").date()).isEqualTo(expected);
		assertThat(ImportCell.blank(COLUMN).date()).isNull();
	}

	@Test
	void impossibleDateIsAnError() {
		for (String text : new String[] { "31/02/2026", "2026-13-01", "hôm qua", "5/3/26" }) {
			assertError(() -> ImportCell.text(COLUMN, text).date(), "Ngày không hợp lệ; ghi dạng dd/mm/yyyy");
		}
		assertError(() -> ImportCell.number(COLUMN, new BigDecimal("46000")).date(),
				"Ngày không hợp lệ; ghi dạng dd/mm/yyyy");
	}

	@Test
	void uncomputedFormulaFailsEveryRead() {
		ImportCell cell = ImportCell.uncomputed(COLUMN);

		assertThat(cell.isBlank()).isFalse();
		assertError(cell::text, "Ô có công thức chưa tính");
		assertError(cell::date, "Ô có công thức chưa tính");
	}

	private static void assertError(Runnable read, String message) {
		assertThatThrownBy(read::run).isInstanceOfSatisfying(CellError.class, error -> {
			assertThat(error.column()).isEqualTo(COLUMN);
			assertThat(error.text()).isEqualTo(message);
		});
	}

}
