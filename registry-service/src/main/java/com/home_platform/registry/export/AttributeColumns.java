package com.home_platform.registry.export;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.registry.catalog.CatalogStatus;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDef;
import com.home_platform.registry.fielddef.FieldTarget;

/**
 * Các cột mục tuỳ biến của một đích (địa điểm, người, đồ đạc): mỗi mục đang dùng là một cột, theo
 * thứ tự hiển thị.
 *
 * <p>Hai mục của hai loại có thể cùng nhãn và cùng khoá. Khi nhãn trùng, cột của mục thuộc một loại
 * mang thêm tên loại; mục áp cho mọi loại giữ nhãn trần.
 */
public final class AttributeColumns {

	private final List<Column> columns;

	private AttributeColumns(List<Column> columns) {
		this.columns = columns;
	}

	/**
	 * @param defs mọi mục của không gian; chỉ mục đang dùng của {@code target} thành cột
	 * @param typeNames tên của loại địa điểm hoặc nhóm đồ theo {@code id}, để phân biệt nhãn trùng
	 */
	public static AttributeColumns of(List<FieldDef> defs, FieldTarget target, Map<Long, String> typeNames) {
		List<FieldDef> active = defs.stream()
			.filter(def -> def.target() == target && def.status() == CatalogStatus.ACTIVE)
			.sorted(Comparator.comparingInt(FieldDef::sortOrder).thenComparingLong(FieldDef::id))
			.toList();
		Map<String, Integer> labelCounts = new HashMap<>();
		for (FieldDef def : active) {
			labelCounts.merge(def.label(), 1, Integer::sum);
		}
		List<Column> columns = new ArrayList<>();
		for (FieldDef def : active) {
			boolean ambiguous = labelCounts.get(def.label()) > 1 && def.typeId() != 0;
			String typeName = typeNames.getOrDefault(def.typeId(), String.valueOf(def.typeId()));
			columns.add(new Column(def, ambiguous ? def.label() + " (" + typeName + ")" : def.label()));
		}
		return new AttributeColumns(withUniqueHeaders(columns));
	}

	/**
	 * Tiêu đề còn trùng sau khi thêm tên loại (ví dụ hai mục chung cùng nhãn) thì thành "nhãn (khoá)":
	 * file mẫu và lớp đọc file nhập nhận cột theo tiêu đề, nên tiêu đề phải khác nhau.
	 */
	private static List<Column> withUniqueHeaders(List<Column> columns) {
		Map<String, Integer> counts = new HashMap<>();
		columns.forEach(column -> counts.merge(column.header(), 1, Integer::sum));
		return columns.stream()
			.map(column -> (counts.get(column.header()) > 1)
					? new Column(column.def(), column.def().label() + " (" + column.def().fieldKey() + ")") : column)
			.toList();
	}

	public List<String> headers() {
		return this.columns.stream().map(Column::header).toList();
	}

	/** Mục của từng cột theo tiêu đề, cùng thứ tự với {@link #headers()}. */
	public Map<String, FieldDef> defsByHeader() {
		Map<String, FieldDef> defs = new LinkedHashMap<>();
		this.columns.forEach(column -> defs.put(column.header(), column.def()));
		return defs;
	}

	/**
	 * Ghi một ô cho mỗi cột. Ô trống khi bản ghi không có giá trị, hoặc khi mục thuộc một loại khác
	 * với {@code typeId} của bản ghi (giá trị cũ còn sót sau khi đổi loại không được ghi ra).
	 * @param typeId loại địa điểm hoặc nhóm đồ của bản ghi; {@code null} là không có
	 */
	void write(SheetWriter row, Long typeId, Map<String, Object> attributes) {
		for (Column column : this.columns) {
			FieldDef def = column.def();
			boolean applies = def.typeId() == 0 || (typeId != null && def.typeId() == typeId);
			Object value = (applies && attributes != null) ? attributes.get(def.fieldKey()) : null;
			writeValue(row, def.dataType(), value);
		}
	}

	/** Giá trị không đúng kiểu của mục (dữ liệu cũ) được ghi nguyên dạng chữ, không bỏ đi. */
	private static void writeValue(SheetWriter row, FieldDataType type, Object value) {
		if (value == null) {
			row.blank();
		}
		else if (type == FieldDataType.NUMBER && value instanceof Number number) {
			row.number(number);
		}
		else if (type == FieldDataType.DATE && parseDate(value) != null) {
			row.date(parseDate(value));
		}
		else if (type == FieldDataType.BOOLEAN && value instanceof Boolean flag) {
			row.text(ExportLabels.yesNo(flag));
		}
		else {
			row.text(String.valueOf(value));
		}
	}

	private static LocalDate parseDate(Object value) {
		try {
			return LocalDate.parse(String.valueOf(value));
		}
		catch (DateTimeParseException ex) {
			return null;
		}
	}

	private record Column(FieldDef def, String header) {
	}

}
