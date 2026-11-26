package com.home_platform.registry.imports;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.registry.export.AttributeColumns;
import com.home_platform.registry.export.ExportLabels;
import com.home_platform.registry.fielddef.FieldDef;

/** Đọc các cột mục tuỳ biến của một dòng thành {@code attributes} của request (spec N6). */
final class AttributeCells {

	/** Giới hạn của mục kiểu chữ, như {@code AttributeValidator}. */
	static final int MAX_TEXT = 1000;

	private static final Map<String, Boolean> YES_NO = Map.of(ImportText.key(ExportLabels.yesNo(true)), true,
			ImportText.key(ExportLabels.yesNo(false)), false, "true", true, "false", false);

	private static final List<String> YES_NO_LABELS = List.of(ExportLabels.yesNo(true), ExportLabels.yesNo(false));

	private AttributeCells() {
	}

	/**
	 * @param typeId loại địa điểm hay nhóm đồ của dòng; {@code null} là không có
	 * @param typeName tên của loại (nhóm) đó, cho lời nhắn khi một mục không áp cho nó
	 * @return giá trị theo {@code fieldKey}; ô trống không có trong kết quả
	 */
	static Map<String, Object> read(RowReader reader, AttributeColumns columns, Long typeId, String typeName) {
		Map<String, Object> values = new LinkedHashMap<>();
		columns.defsByHeader().forEach((header, def) -> {
			boolean applies = def.typeId() == 0 || (typeId != null && def.typeId() == typeId);
			if (!applies) {
				if (!reader.cell(header).isBlank()) {
					reader.error(header, "Mục '" + def.label() + "' không áp cho loại " + typeName);
				}
				return;
			}
			Object value = value(reader, header, def);
			if (value != null) {
				values.put(def.fieldKey(), value);
			}
			else if (def.isRequired() && !reader.hasErrorIn(header)) {
				reader.error(header, ImportMessages.REQUIRED);
			}
		});
		return values;
	}

	/**
	 * Chỉ các mục áp cho mọi loại ({@code typeId} 0), khi loại (nhóm) của dòng không tìm được: vẫn báo
	 * lỗi của chúng trong cùng lần xem trước; cột của các mục theo loại thì bỏ qua, vì chưa biết loại.
	 */
	static Map<String, Object> readShared(RowReader reader, AttributeColumns columns) {
		Map<String, Object> values = new LinkedHashMap<>();
		columns.defsByHeader().forEach((header, def) -> {
			if (def.typeId() != 0) {
				return;
			}
			Object value = value(reader, header, def);
			if (value != null) {
				values.put(def.fieldKey(), value);
			}
			else if (def.isRequired() && !reader.hasErrorIn(header)) {
				reader.error(header, ImportMessages.REQUIRED);
			}
		});
		return values;
	}

	private static Object value(RowReader reader, String header, FieldDef def) {
		return switch (def.dataType()) {
			case TEXT -> {
				String text = reader.text(header);
				if (text != null && text.length() > MAX_TEXT) {
					reader.error(header, "Dài quá " + MAX_TEXT + " ký tự");
					yield null;
				}
				yield text;
			}
			case NUMBER -> reader.decimal(header);
			case DATE -> {
				var date = reader.date(header);
				yield (date != null) ? date.toString() : null;
			}
			case BOOLEAN -> reader.choice(header, YES_NO, YES_NO_LABELS);
			case SELECT -> {
				Map<String, String> options = new LinkedHashMap<>();
				List<String> labels = (def.options() != null) ? def.options() : List.of();
				labels.forEach(option -> options.put(ImportText.key(option), option));
				yield reader.choice(header, options, labels);
			}
		};
	}

}
