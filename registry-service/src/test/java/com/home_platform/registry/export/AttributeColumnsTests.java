package com.home_platform.registry.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.registry.catalog.CatalogStatus;
import com.home_platform.registry.export.XlsxReader.XlsxSheet;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDef;
import com.home_platform.registry.fielddef.FieldTarget;
import org.junit.jupiter.api.Test;

class AttributeColumnsTests {

	private static final long ROOM = 10;

	private static final long FLAT = 20;

	private static final Map<Long, String> TYPES = Map.of(ROOM, "Phòng", FLAT, "Căn hộ");

	@Test
	void columnsFollowSortOrder() {
		AttributeColumns columns = AttributeColumns.of(List.of(def(1, 0, "c", "Ba", FieldDataType.TEXT, 2),
				def(3, 0, "b", "Hai", FieldDataType.TEXT, 1), def(2, 0, "a", "Một", FieldDataType.TEXT, 1),
				def(4, 0, "old", "Đã lưu trữ", FieldDataType.TEXT, 0).withStatus(CatalogStatus.ARCHIVED),
				def(5, 0, "item", "Của đồ", FieldDataType.TEXT, 0).withTarget(FieldTarget.ITEM)), FieldTarget.PLACE,
				TYPES);

		assertThat(columns.headers()).containsExactly("Một", "Hai", "Ba");
	}

	/** Hai loại cùng có mục "Diện tích", cùng khoá {@code area}: hai cột, giá trị về đúng cột của loại mình. */
	@Test
	void sameLabelOnTwoTypesGetsTwoColumns() {
		AttributeColumns columns = AttributeColumns.of(
				List.of(def(1, ROOM, "area", "Diện tích", FieldDataType.NUMBER, 1),
						def(2, FLAT, "area", "Diện tích", FieldDataType.NUMBER, 2),
						def(3, 0, "note", "Ghi chú", FieldDataType.TEXT, 3)),
				FieldTarget.PLACE, TYPES);

		assertThat(columns.headers()).containsExactly("Diện tích (Phòng)", "Diện tích (Căn hộ)", "Ghi chú");
		XlsxSheet sheet = SheetWriterTests.sheet(columns.headers(), row -> {
			columns.write(row, ROOM, Map.of("area", 25, "note", "phòng"));
			row.nextRow();
			columns.write(row, FLAT, Map.of("area", 70, "note", "căn hộ"));
			row.nextRow();
		});
		assertThat(sheet.cell(0, "Diện tích (Phòng)").number()).isEqualByComparingTo("25");
		assertThat(sheet.cell(0, "Diện tích (Căn hộ)").isBlank()).isTrue();
		assertThat(sheet.cell(0, "Ghi chú").text()).isEqualTo("phòng");
		assertThat(sheet.cell(1, "Diện tích (Phòng)").isBlank()).isTrue();
		assertThat(sheet.cell(1, "Diện tích (Căn hộ)").number()).isEqualByComparingTo("70");
		assertThat(sheet.cell(1, "Ghi chú").text()).isEqualTo("căn hộ");
	}

	/** Mục chung trùng nhãn với mục của một loại: mục chung giữ nhãn trần, mục của loại thêm tên loại. */
	@Test
	void generalFieldKeepsItsBareLabel() {
		AttributeColumns columns = AttributeColumns.of(List.of(def(1, 0, "area", "Diện tích", FieldDataType.NUMBER, 1),
				def(2, ROOM, "room_area", "Diện tích", FieldDataType.NUMBER, 2),
				def(3, 999, "x", "Diện tích", FieldDataType.NUMBER, 3)), FieldTarget.PLACE, TYPES);

		assertThat(columns.headers()).containsExactly("Diện tích", "Diện tích (Phòng)", "Diện tích (999)");
	}

	/** Bản ghi còn giữ giá trị của một mục thuộc loại khác (ví dụ sau khi đổi loại): cột đó vẫn trống. */
	@Test
	void fieldForAnotherTypeIsBlank() {
		AttributeColumns columns = AttributeColumns
			.of(List.of(def(1, ROOM, "beds", "Số giường", FieldDataType.NUMBER, 1)), FieldTarget.PLACE, TYPES);

		XlsxSheet sheet = SheetWriterTests.sheet(List.of("Số giường", "Sau"), row -> {
			columns.write(row, FLAT, Map.of("beds", 2));
			row.text("x");
			row.nextRow();
			columns.write(row, null, Map.of("beds", 2));
			row.text("y");
			row.nextRow();
			columns.write(row, ROOM, null);
			row.text("z");
			row.nextRow();
		});

		assertThat(sheet.cell(0, "Số giường").isBlank()).isTrue();
		assertThat(sheet.cell(1, "Số giường").isBlank()).isTrue();
		assertThat(sheet.cell(2, "Số giường").isBlank()).isTrue();
		assertThat(sheet.column("Sau")).containsExactly("x", "y", "z");
	}

	@Test
	void valuesGetTheirCellType() {
		AttributeColumns columns = AttributeColumns.of(List.of(def(1, 0, "n", "Số", FieldDataType.NUMBER, 1),
				def(2, 0, "d", "Ngày", FieldDataType.DATE, 2), def(3, 0, "b", "Có không", FieldDataType.BOOLEAN, 3),
				def(4, 0, "s", "Chọn", FieldDataType.SELECT, 4), def(5, 0, "t", "Chữ", FieldDataType.TEXT, 5)),
				FieldTarget.PLACE, Map.of());
		Map<String, Object> missing = new HashMap<>();
		missing.put("n", null);

		XlsxSheet sheet = SheetWriterTests.sheet(columns.headers(), row -> {
			columns.write(row, null, Map.of("n", 12.5, "d", "2026-01-05", "b", true, "s", "Đỏ", "t", "abc"));
			row.nextRow();
			columns.write(row, null, Map.of("b", false, "d", "không phải ngày", "n", "mười"));
			row.nextRow();
			columns.write(row, null, missing);
			row.nextRow();
		});

		assertThat(sheet.cell(0, "Số").isNumber()).isTrue();
		assertThat(sheet.cell(0, "Số").number()).isEqualByComparingTo("12.5");
		assertThat(sheet.cell(0, "Ngày").isDate()).isTrue();
		assertThat(sheet.cell(0, "Ngày").date()).isEqualTo(LocalDate.of(2026, 1, 5));
		assertThat(sheet.cell(0, "Có không").text()).isEqualTo("Có");
		assertThat(sheet.cell(0, "Chọn").text()).isEqualTo("Đỏ");
		assertThat(sheet.cell(0, "Chữ").text()).isEqualTo("abc");
		assertThat(sheet.cell(1, "Có không").text()).isEqualTo("Không");
		assertThat(sheet.cell(1, "Ngày").text()).isEqualTo("không phải ngày");
		assertThat(sheet.cell(1, "Số").text()).isEqualTo("mười");
		assertThat(sheet.cell(1, "Chọn").isBlank()).isTrue();
		assertThat(sheet.cell(2, "Số").isBlank()).isTrue();
	}

	private static FieldDef def(long id, long typeId, String key, String label, FieldDataType type, int sortOrder) {
		return new FieldDef(-1, id, FieldTarget.PLACE, typeId, key, label, type, null, false, sortOrder,
				CatalogStatus.ACTIVE, Instant.EPOCH, Instant.EPOCH);
	}

}
