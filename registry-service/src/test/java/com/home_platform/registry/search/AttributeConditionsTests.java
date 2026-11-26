package com.home_platform.registry.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.home_platform.common.web.BadRequestException;
import com.home_platform.registry.catalog.CatalogStatus;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDef;
import com.home_platform.registry.fielddef.FieldTarget;

class AttributeConditionsTests {

	@Test
	void parsesKeyOpAndValue() {
		assertThat(AttributeConditions.parse(List.of("dien_tich:gte:20")))
			.containsExactly(new AttributeCondition("dien_tich", AttributeOp.GTE, "20"));
	}

	@Test
	void valueKeepsItsColons() {
		assertThat(AttributeConditions.parse(List.of("gio_mo:has:8:30")))
			.containsExactly(new AttributeCondition("gio_mo", AttributeOp.HAS, "8:30"));
	}

	@Test
	void setNeedsNoValue() {
		AttributeCondition expected = new AttributeCondition("cccd_het_han", AttributeOp.SET, "");
		assertThat(AttributeConditions.parse(List.of("cccd_het_han:set:"))).containsExactly(expected);
		assertThat(AttributeConditions.parse(List.of("cccd_het_han:set"))).containsExactly(expected);
	}

	@Test
	void nothingParsesToNoConditions() {
		assertThat(AttributeConditions.parse(null)).isEmpty();
		assertThat(AttributeConditions.parse(List.of())).isEmpty();
	}

	@Test
	void fourConditionsAreRejected() {
		assertThatThrownBy(() -> AttributeConditions.parse(List.of("a:set", "b:set", "c:set", "d:set")))
			.isInstanceOf(BadRequestException.class)
			.satisfies(ex -> assertThat(detail(ex)).isEqualTo("At most 3 attribute conditions"));
	}

	@Test
	void malformedIsRejected() {
		for (String raw : List.of("dien_tich", "dien_tich:zz:1", ":gte:1", "dien_tich:gte")) {
			assertThatThrownBy(() -> AttributeConditions.parse(List.of(raw))).as(raw)
				.isInstanceOf(BadRequestException.class)
				.satisfies(ex -> assertThat(detail(ex)).isEqualTo("attr must be key:op:value"));
		}
	}

	@Test
	void unknownOrArchivedKeyIsRejected() {
		List<FieldDef> defs = List.of(def("cu", FieldDataType.TEXT, 0).withStatus(CatalogStatus.ARCHIVED));

		assertThatThrownBy(() -> toSql("x:set", defs)).isInstanceOf(BadRequestException.class)
			.satisfies(ex -> assertThat(detail(ex)).isEqualTo("Unknown field: x"));
		assertThatThrownBy(() -> toSql("cu:set", defs)).isInstanceOf(BadRequestException.class)
			.satisfies(ex -> assertThat(detail(ex)).isEqualTo("Unknown field: cu"));
	}

	@Test
	void operatorMustFitTheType() {
		List<FieldDef> defs = List.of(def("dien_tich", FieldDataType.NUMBER, 0));

		assertThatThrownBy(() -> toSql("dien_tich:has:20", defs)).isInstanceOf(BadRequestException.class)
			.satisfies(ex -> assertThat(detail(ex)).isEqualTo("Operator has does not apply to field dien_tich"));
	}

	@Test
	void valuesAreCheckedByType() {
		List<FieldDef> defs = List.of(def("so", FieldDataType.NUMBER, 0), def("ngay", FieldDataType.DATE, 0),
				def("co", FieldDataType.BOOLEAN, 0), def("chon", FieldDataType.SELECT, 0, "Đỏ", "Xanh"),
				def("chu", FieldDataType.TEXT, 0));
		for (String raw : List.of("so:eq:abc", "ngay:gte:2026-13-01", "co:eq:maybe", "chon:eq:Vàng", "chu:has:")) {
			String key = raw.substring(0, raw.indexOf(':'));
			String value = raw.substring(raw.indexOf(':', raw.indexOf(':') + 1) + 1);
			assertThatThrownBy(() -> toSql(raw, defs)).as(raw)
				.isInstanceOf(BadRequestException.class)
				.satisfies(ex -> assertThat(detail(ex)).isEqualTo("Invalid value for field " + key + ": " + value));
		}
	}

	@Test
	void numberBecomesADecimalParameter() {
		SqlCondition condition = toSql("dien_tich:gte:20.5", List.of(def("dien_tich", FieldDataType.NUMBER, 0)));

		assertThat(condition.params()).containsEntry("a0_path", "$.dien_tich")
			.containsEntry("a0_v", new BigDecimal("20.5"))
			.doesNotContainKey("a0_t0");
	}

	@Test
	void typedFieldRestrictsByType() {
		SqlCondition condition = toSql("dien_tich:set", List.of(def("dien_tich", FieldDataType.NUMBER, 7)));

		assertThat(condition.params()).containsEntry("a0_t0", 7L);
		assertThat(condition.sql()).contains("place_type_id = :a0_t0");
	}

	@Test
	void textHasEscapesLikeCharacters() {
		SqlCondition condition = toSql("ghi_chu:has:5%", List.of(def("ghi_chu", FieldDataType.TEXT, 0)));

		assertThat(condition.params()).containsEntry("a0_v", "%5\\%%");
	}

	@Test
	void sameKeyKeepsOnlyDefsTheValueFits() {
		List<FieldDef> defs = List.of(def("dien_tich", FieldDataType.NUMBER, 7), def("dien_tich", FieldDataType.TEXT, 8));

		assertThat(toSql("dien_tich:gte:20", defs).params()).containsEntry("a0_t0", 7L).doesNotContainKey("a0_t1");
		assertThat(toSql("dien_tich:has:abc", defs).params()).containsEntry("a0_t0", 8L).doesNotContainKey("a0_t1");
	}

	@Test
	void eachConditionGetsItsOwnParameters() {
		List<FieldDef> defs = List.of(def("a", FieldDataType.TEXT, 0), def("b", FieldDataType.TEXT, 0));

		List<SqlCondition> conditions = AttributeConditions
			.toSql(AttributeConditions.parse(List.of("a:eq:x", "b:eq:y")), defs, "place_type_id");

		assertThat(conditions).hasSize(2);
		assertThat(conditions.get(0).params()).containsEntry("a0_v", "x");
		assertThat(conditions.get(1).params()).containsEntry("a1_v", "y");
	}

	private static SqlCondition toSql(String raw, List<FieldDef> defs) {
		List<SqlCondition> conditions = AttributeConditions.toSql(AttributeConditions.parse(List.of(raw)), defs,
				"place_type_id");
		assertThat(conditions).hasSize(1);
		return conditions.get(0);
	}

	private static FieldDef def(String key, FieldDataType type, long typeId, String... options) {
		return new FieldDef(-1, key.hashCode(), FieldTarget.PLACE, typeId, key, key, type,
				(options.length > 0) ? Arrays.asList(options) : null, false, null, 0, CatalogStatus.ACTIVE,
				Instant.EPOCH, Instant.EPOCH);
	}

	private static String detail(Throwable ex) {
		return ((BadRequestException) ex).getBody().getDetail();
	}

}
