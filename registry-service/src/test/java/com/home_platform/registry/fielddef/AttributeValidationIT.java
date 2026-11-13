package com.home_platform.registry.fielddef;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import com.home_platform.common.web.BadRequestException;
import com.home_platform.registry.RegistryIT;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AttributeValidationIT extends RegistryIT {

	@Autowired
	AttributeValidation validation;

	@Autowired
	FieldDefService fieldDefs;

	@Autowired
	PlaceTypeService placeTypes;

	@Test
	void usesFieldsOfTheTargetAndTypeOnly() {
		long hotel = placeType("HOTEL");
		long house = placeType("HOUSE");
		field(FieldTarget.PLACE, 0, "a", FieldDataType.NUMBER);
		field(FieldTarget.PLACE, hotel, "b", FieldDataType.TEXT);
		field(FieldTarget.ITEM, 0, "c", FieldDataType.TEXT);

		assertThat(this.validation.validate(WS_A, FieldTarget.PLACE, hotel, Map.of("a", 1, "b", "x")))
			.isEqualTo(Map.of("a", 1, "b", "x"));
		assertThatThrownBy(() -> this.validation.validate(WS_A, FieldTarget.PLACE, house, Map.of("b", "x")))
			.isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> this.validation.validate(WS_A, FieldTarget.PLACE, hotel, Map.of("c", "x")))
			.isInstanceOf(BadRequestException.class);
		assertThat(this.validation.validate(WS_A, FieldTarget.ITEM, 0, Map.of("c", "x"))).isEqualTo(Map.of("c", "x"));
	}

	@Test
	void seesAFieldRightAfterItIsCreated() {
		assertThatThrownBy(() -> this.validation.validate(WS_A, FieldTarget.PERSON, 0, Map.of("nickname", "Bo")))
			.isInstanceOf(BadRequestException.class);

		field(FieldTarget.PERSON, 0, "nickname", FieldDataType.TEXT);

		assertThat(this.validation.validate(WS_A, FieldTarget.PERSON, 0, Map.of("nickname", "Bo")))
			.isEqualTo(Map.of("nickname", "Bo"));
	}

	@Test
	void otherWorkspacesFieldsDoNotApply() {
		this.fieldDefs.create(CTX_B, new FieldDefCreateRequest(FieldTarget.PERSON, 0L, "nickname", "Biệt danh",
				FieldDataType.TEXT, null, null, null));

		assertThatThrownBy(() -> this.validation.validate(WS_A, FieldTarget.PERSON, 0, Map.of("nickname", "Bo")))
			.isInstanceOf(BadRequestException.class);
	}

	private long placeType(String code) {
		return this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest(code, code, null, null)).id();
	}

	private void field(FieldTarget target, long typeId, String key, FieldDataType dataType) {
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(target, typeId, key, key, dataType, null, null, null));
	}

}
