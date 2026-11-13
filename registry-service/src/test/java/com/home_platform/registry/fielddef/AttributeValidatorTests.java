package com.home_platform.registry.fielddef;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.common.web.BadRequestException;
import com.home_platform.registry.catalog.CatalogStatus;
import org.junit.jupiter.api.Test;
import org.springframework.web.ErrorResponse;

class AttributeValidatorTests {

	private static final List<FieldDef> DEFS = List.of(def("note", FieldDataType.TEXT),
			def("area", FieldDataType.NUMBER), def("floors", FieldDataType.NUMBER), def("pet", FieldDataType.BOOLEAN),
			def("built", FieldDataType.DATE),
			def("view", FieldDataType.SELECT).withOptions(List.of("Biển", "Phố")));

	@Test
	void acceptsValidValuesOfEveryType() {
		Map<String, Object> attributes = map("note", "x", "area", 42.5, "floors", 3, "pet", true, "built",
				"2020-01-31", "view", "Biển");

		assertThat(AttributeValidator.validate(DEFS, attributes)).isEqualTo(attributes);
	}

	@Test
	void valuesOfTheWrongJsonTypeAreRejected() {
		assertThat(errors(DEFS, map("area", "5", "pet", "true", "built", "2026-02-30", "view", "biển", "note", 7)))
			.containsExactly(error("area", "must be a number"), error("pet", "must be true or false"),
					error("built", "must be a date in YYYY-MM-DD format"),
					error("view", "must be one of the options"),
					error("note", "must be text of at most 1000 characters"));
		assertThat(errors(DEFS, map("built", 20200131)))
			.containsExactly(error("built", "must be a date in YYYY-MM-DD format"));
	}

	@Test
	void unknownKeyIsRejected() {
		assertThat(errors(DEFS, map("color", "red"))).containsExactly(error("color", "is not a defined field"));
	}

	@Test
	void requiredFieldsMustBePresent() {
		List<FieldDef> defs = List.of(def("view", FieldDataType.TEXT).withRequired(true));
		Map<String, Object> withNull = new HashMap<>();
		withNull.put("view", null);

		assertThat(errors(defs, map())).containsExactly(error("view", "is required"));
		assertThat(errors(defs, withNull)).containsExactly(error("view", "is required"));
		assertThat(errors(defs, map("view", "  "))).containsExactly(error("view", "is required"));
	}

	@Test
	void archivedFieldIsKeptButNotChecked() {
		List<FieldDef> defs = List.of(
				def("old", FieldDataType.NUMBER).withRequired(true).withStatus(CatalogStatus.ARCHIVED));

		assertThat(AttributeValidator.validate(defs, map("old", "text"))).isEqualTo(map("old", "text"));
		assertThat(AttributeValidator.validate(defs, map())).isNull();
	}

	@Test
	void nullValuesAreDroppedAndEmptyBecomesNull() {
		Map<String, Object> withNull = new HashMap<>();
		withNull.put("note", null);

		assertThat(AttributeValidator.validate(DEFS, withNull)).isNull();
		assertThat(AttributeValidator.validate(DEFS, null)).isNull();
		assertThat(AttributeValidator.validate(DEFS, map())).isNull();
		withNull.put("area", 1);
		assertThat(AttributeValidator.validate(DEFS, withNull)).isEqualTo(map("area", 1));
	}

	@Test
	void textLongerThan1000IsRejected() {
		assertThat(AttributeValidator.validate(DEFS, map("note", "a".repeat(1000)))).containsKey("note");
		assertThat(errors(DEFS, map("note", "a".repeat(1001))))
			.containsExactly(error("note", "must be text of at most 1000 characters"));
	}

	@Test
	void errorsAreAttachedToTheProblemDetail() {
		assertThatThrownBy(() -> AttributeValidator.validate(DEFS, map("color", "red")))
			.isInstanceOf(BadRequestException.class)
			.satisfies(ex -> {
				ErrorResponse response = (ErrorResponse) ex;
				assertThat(response.getBody().getDetail()).isEqualTo("Invalid attributes");
				assertThat(response.getBody().getProperties()).containsKey("errors");
			});
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> errors(List<FieldDef> defs, Map<String, Object> attributes) {
		try {
			AttributeValidator.validate(defs, attributes);
		}
		catch (BadRequestException ex) {
			return (List<Map<String, Object>>) ex.getBody().getProperties().get("errors");
		}
		throw new AssertionError("Expected the attributes to be rejected");
	}

	private static Map<String, Object> error(String key, String message) {
		return Map.of("field", "attributes." + key, "message", message);
	}

	private static Map<String, Object> map(Object... keysAndValues) {
		Map<String, Object> map = new LinkedHashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			map.put((String) keysAndValues[i], keysAndValues[i + 1]);
		}
		return map;
	}

	private static FieldDef def(String key, FieldDataType dataType) {
		return new FieldDef(-1, key.hashCode(), FieldTarget.PLACE, 0, key, key, dataType, null, false, 0,
				CatalogStatus.ACTIVE, null, null);
	}

}
