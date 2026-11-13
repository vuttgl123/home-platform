package com.home_platform.registry.fielddef;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.common.web.ApiExceptionHandler;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.registry.catalog.CatalogStatus;

/**
 * Kiểm tra {@code attributes} của một địa điểm, người hoặc món đồ theo các mục tuỳ biến áp
 * dụng cho nó. Không đổi kiểu giá trị: chuỗi {@code "5"} không phải là số.
 *
 * <ul>
 * <li>Khoá không thuộc mục áp dụng nào (kể cả mục đã lưu trữ) bị từ chối.</li>
 * <li>Mục đang dùng được kiểm tra kiểu giá trị và, nếu bắt buộc, phải có giá trị.</li>
 * <li>Mục đã lưu trữ giữ nguyên giá trị cũ, không kiểm tra.</li>
 * <li>Giá trị {@code null} là bỏ trống: khoá đó không được lưu.</li>
 * </ul>
 */
public final class AttributeValidator {

	static final int MAX_TEXT_LENGTH = 1000;

	private AttributeValidator() {
	}

	/**
	 * @param applicable các mục áp dụng cho bản ghi (đã lọc theo đích và loại), mọi trạng thái
	 * @param attributes giá trị gửi lên; {@code null} coi như rỗng
	 * @return giá trị sẽ lưu, đã bỏ các khoá mang {@code null}; {@code null} nếu không còn khoá nào
	 * @throws BadRequestException "Invalid attributes", kèm danh sách {@code errors} theo từng khoá
	 */
	public static Map<String, Object> validate(List<FieldDef> applicable, Map<String, Object> attributes) {
		Map<String, FieldDef> byKey = new LinkedHashMap<>();
		applicable.stream()
			.sorted(Comparator.comparingInt(FieldDef::sortOrder))
			.forEach(def -> byKey.putIfAbsent(def.fieldKey(), def));
		Map<String, Object> values = (attributes != null) ? attributes : Map.of();
		List<Map<String, Object>> errors = new ArrayList<>();
		Map<String, Object> cleaned = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			FieldDef def = byKey.get(entry.getKey());
			if (def == null) {
				errors.add(error(entry.getKey(), "is not a defined field"));
				continue;
			}
			if (entry.getValue() == null) {
				continue;
			}
			String problem = (def.status() == CatalogStatus.ACTIVE) ? check(def, entry.getValue()) : null;
			if (problem != null) {
				errors.add(error(entry.getKey(), problem));
			}
			else {
				cleaned.put(entry.getKey(), entry.getValue());
			}
		}
		for (FieldDef def : byKey.values()) {
			if (def.status() == CatalogStatus.ACTIVE && def.isRequired() && isBlank(values.get(def.fieldKey()))) {
				errors.add(error(def.fieldKey(), "is required"));
			}
		}
		if (!errors.isEmpty()) {
			BadRequestException ex = new BadRequestException("Invalid attributes");
			ex.getBody().setProperty(ApiExceptionHandler.ERRORS_PROPERTY, errors);
			throw ex;
		}
		return cleaned.isEmpty() ? null : cleaned;
	}

	/** Lý do giá trị không hợp lệ với mục, hoặc {@code null} nếu hợp lệ. */
	private static String check(FieldDef def, Object value) {
		return switch (def.dataType()) {
			case TEXT -> (value instanceof String text && text.length() <= MAX_TEXT_LENGTH) ? null
					: "must be text of at most 1000 characters";
			case NUMBER -> (value instanceof Number) ? null : "must be a number";
			case BOOLEAN -> (value instanceof Boolean) ? null : "must be true or false";
			case DATE -> isDate(value) ? null : "must be a date in YYYY-MM-DD format";
			case SELECT -> (value instanceof String text && def.options() != null && def.options().contains(text))
					? null : "must be one of the options";
		};
	}

	private static boolean isDate(Object value) {
		if (!(value instanceof String text)) {
			return false;
		}
		try {
			LocalDate.parse(text);
			return true;
		}
		catch (DateTimeParseException ex) {
			return false;
		}
	}

	private static boolean isBlank(Object value) {
		return value == null || (value instanceof String text && text.isBlank());
	}

	private static Map<String, Object> error(String key, String message) {
		Map<String, Object> error = new LinkedHashMap<>();
		error.put("field", "attributes." + key);
		error.put("message", message);
		return error;
	}

}
