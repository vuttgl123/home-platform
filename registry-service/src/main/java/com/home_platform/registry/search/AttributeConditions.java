package com.home_platform.registry.search;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.home_platform.common.web.BadRequestException;
import com.home_platform.registry.catalog.CatalogStatus;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDef;

/**
 * Lọc danh sách theo mục tuỳ biến (spec Đợt 11, S2): đọc {@code attr=khoá:phép:giá trị}, kiểm
 * theo các mục đang dùng của đích, rồi sinh điều kiện SQL đọc cột JSON {@code attributes}.
 *
 * <p>Một khoá có thể thuộc nhiều mục (mỗi loại một mục, có khi khác kiểu). Điều kiện là "khớp ít
 * nhất một mục": với mỗi mục mà phép và giá trị hợp kiểu, bản ghi phải thuộc loại của mục đó (trừ
 * mục chung) và giá trị phải khớp. {@code unset} là phủ định của {@code set}, nên bản ghi khác loại
 * cũng là trống. Mọi giá trị đi qua tham số; khoá chỉ vào đường dẫn JSON sau khi khớp một mục đang
 * dùng, và khoá của mục luôn đúng mẫu {@code [a-z][a-z0-9_]*}.
 */
public final class AttributeConditions {

	public static final int MAX_CONDITIONS = 3;

	private static final Map<FieldDataType, Set<AttributeOp>> OPS = Map.of(
			FieldDataType.TEXT, EnumSet.of(AttributeOp.HAS, AttributeOp.EQ, AttributeOp.SET, AttributeOp.UNSET),
			FieldDataType.NUMBER,
			EnumSet.of(AttributeOp.EQ, AttributeOp.GTE, AttributeOp.LTE, AttributeOp.SET, AttributeOp.UNSET),
			FieldDataType.DATE, EnumSet.of(AttributeOp.GTE, AttributeOp.LTE, AttributeOp.SET, AttributeOp.UNSET),
			FieldDataType.BOOLEAN, EnumSet.of(AttributeOp.EQ, AttributeOp.SET, AttributeOp.UNSET),
			FieldDataType.SELECT, EnumSet.of(AttributeOp.EQ, AttributeOp.SET, AttributeOp.UNSET));

	private static final String NUMERIC_TYPES = "('INTEGER', 'UNSIGNED INTEGER', 'DECIMAL', 'DOUBLE')";

	private static final String JSON_TRUE = "CAST('true' AS JSON)";

	private AttributeConditions() {
	}

	/**
	 * Tách các giá trị {@code attr}: khoá, phép, rồi phần còn lại là giá trị (có thể chứa {@code :}).
	 * Phép không cần giá trị thì được thiếu dấu {@code :} thứ hai.
	 * @param raw {@code null} hay rỗng là không lọc
	 * @throws BadRequestException quá {@link #MAX_CONDITIONS} điều kiện, hoặc sai dạng
	 */
	public static List<AttributeCondition> parse(List<String> raw) {
		if (raw == null || raw.isEmpty()) {
			return List.of();
		}
		if (raw.size() > MAX_CONDITIONS) {
			throw new BadRequestException("At most " + MAX_CONDITIONS + " attribute conditions");
		}
		List<AttributeCondition> conditions = new ArrayList<>(raw.size());
		for (String text : raw) {
			conditions.add(parseOne((text != null) ? text : ""));
		}
		return List.copyOf(conditions);
	}

	/**
	 * Điều kiện SQL của từng điều kiện, cùng thứ tự; điều kiện thứ {@code i} dùng tham số
	 * {@code a{i}_path}, {@code a{i}_v…}, {@code a{i}_t{j}}.
	 * @param defs mọi mục của đích, mọi trạng thái; chỉ mục {@code ACTIVE} được dùng
	 * @param typeColumn cột loại của bảng ({@code place_type_id}, {@code category_id}); {@code null}
	 * khi đích không có loại
	 * @throws BadRequestException khoá không thuộc mục đang dùng nào, phép không hợp kiểu, giá trị sai kiểu
	 */
	public static List<SqlCondition> toSql(List<AttributeCondition> conditions, List<FieldDef> defs,
			String typeColumn) {
		List<SqlCondition> sql = new ArrayList<>(conditions.size());
		for (int i = 0; i < conditions.size(); i++) {
			sql.add(toSql(i, conditions.get(i), defs, typeColumn));
		}
		return List.copyOf(sql);
	}

	private static AttributeCondition parseOne(String text) {
		int first = text.indexOf(':');
		if (first <= 0) {
			throw malformed();
		}
		int second = text.indexOf(':', first + 1);
		AttributeOp op = AttributeOp.fromCode((second < 0) ? text.substring(first + 1) : text.substring(first + 1, second));
		if (op == null || (second < 0 && op.needsValue())) {
			throw malformed();
		}
		return new AttributeCondition(text.substring(0, first), op, (second < 0) ? "" : text.substring(second + 1));
	}

	private static SqlCondition toSql(int index, AttributeCondition condition, List<FieldDef> defs, String typeColumn) {
		List<FieldDef> sameKey = defs.stream()
			.filter(def -> def.status() == CatalogStatus.ACTIVE && def.fieldKey().equals(condition.key()))
			.toList();
		if (sameKey.isEmpty()) {
			throw new BadRequestException("Unknown field: " + condition.key());
		}
		List<FieldDef> fitting = sameKey.stream().filter(def -> OPS.get(def.dataType()).contains(condition.op())).toList();
		if (fitting.isEmpty()) {
			throw new BadRequestException(
					"Operator " + condition.op().code() + " does not apply to field " + condition.key());
		}
		String prefix = "a" + index + "_";
		Map<String, Object> params = new HashMap<>();
		params.put(prefix + "path", "$." + condition.key());
		Map<Object, String> valueParams = new LinkedHashMap<>();
		String json = "JSON_EXTRACT(attributes, :" + prefix + "path)";
		List<String> parts = new ArrayList<>();
		int typed = 0;
		for (FieldDef def : fitting) {
			Value value = (condition.op() == AttributeOp.SET || condition.op() == AttributeOp.UNSET)
					? new Value(present(json), null) : value(def, condition, json);
			if (value == null) {
				continue;
			}
			String test = value.sql();
			if (value.parameter() != null) {
				String name = valueParams.computeIfAbsent(value.parameter(),
						key -> prefix + "v" + (valueParams.isEmpty() ? "" : Integer.toString(valueParams.size() + 1)));
				params.put(name, value.parameter());
				test = test.replace(":v", ":" + name);
			}
			if (typeColumn != null && def.typeId() != 0) {
				String name = prefix + "t" + typed++;
				params.put(name, def.typeId());
				test = typeColumn + " = :" + name + " AND " + test;
			}
			parts.add("(" + test + ")");
		}
		if (parts.isEmpty()) {
			throw new BadRequestException("Invalid value for field " + condition.key() + ": " + condition.value());
		}
		String any = String.join(" OR ", parts);
		return new SqlCondition((condition.op() == AttributeOp.UNSET) ? "NOT (" + any + ")" : "(" + any + ")", params);
	}

	/** Đoạn so giá trị của một mục, với {@code :v} là chỗ của tham số; {@code null} nếu giá trị sai kiểu. */
	private static Value value(FieldDef def, AttributeCondition condition, String json) {
		String text = "JSON_UNQUOTE(" + json + ")";
		String raw = condition.value();
		String compare = switch (condition.op()) {
			case GTE -> ">=";
			case LTE -> "<=";
			default -> "=";
		};
		return switch (def.dataType()) {
			case TEXT -> {
				String cleaned = raw.strip();
				if (cleaned.isEmpty()) {
					yield null;
				}
				// JSON_UNQUOTE trả utf8mb4_bin; CONVERT đưa về collation mặc định của utf8mb4, cũng là của
				// các cột thường, để "hue" khớp "Huế" như khi tìm theo tên.
				String converted = "CONVERT(" + text + " USING utf8mb4)";
				yield (condition.op() == AttributeOp.HAS) ? new Value(converted + " LIKE :v", TextSearch.contains(cleaned))
						: new Value(converted + " = :v", cleaned);
			}
			case NUMBER -> {
				BigDecimal number = number(raw);
				yield (number == null) ? null : new Value("JSON_TYPE(" + json + ") IN " + NUMERIC_TYPES + " AND CAST("
						+ json + " AS DECIMAL(65, 10)) " + compare + " :v", number);
			}
			case DATE -> {
				LocalDate date = date(raw);
				yield (date == null) ? null : new Value(text + " " + compare + " :v", date.toString());
			}
			case BOOLEAN -> switch (raw) {
				case "true" -> new Value("COALESCE(" + json + " = " + JSON_TRUE + ", FALSE)", null);
				case "false" -> new Value("NOT COALESCE(" + json + " = " + JSON_TRUE + ", FALSE)", null);
				default -> null;
			};
			case SELECT -> (def.options() != null && def.options().contains(raw)) ? new Value(text + " = :v", raw) : null;
		};
	}

	private static String present(String json) {
		return json + " IS NOT NULL AND JSON_UNQUOTE(" + json + ") <> ''";
	}

	private static BigDecimal number(String raw) {
		try {
			return new BigDecimal(raw.strip());
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	private static LocalDate date(String raw) {
		try {
			return LocalDate.parse(raw.strip());
		}
		catch (DateTimeParseException ex) {
			return null;
		}
	}

	private static BadRequestException malformed() {
		return new BadRequestException("attr must be key:op:value");
	}

	/** @param parameter giá trị của {@code :v}; {@code null} khi đoạn không có tham số */
	private record Value(String sql, Object parameter) {
	}

}
