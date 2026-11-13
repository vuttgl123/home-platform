package com.home_platform.registry.fielddef;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.registry.RegistryIT;
import com.home_platform.registry.itemcategory.ItemCategoryRequest;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class FieldDefIT extends RegistryIT {

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	ItemCategoryService categories;

	@Autowired
	FieldDefService fieldDefs;

	@Test
	void createsASelectFieldForOnePlaceType() throws Exception {
		long hotel = placeType("HOTEL");

		submit("""
				{"target": "PLACE", "typeId": "%d", "fieldKey": "view", "label": "Hướng", "dataType": "SELECT",
				 "options": [" Biển ", "Phố"], "required": true}""".formatted(hotel))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isString())
			.andExpect(jsonPath("$.typeId").value(String.valueOf(hotel)))
			.andExpect(jsonPath("$.fieldKey").value("view"))
			.andExpect(jsonPath("$.options", contains("Biển", "Phố")))
			.andExpect(jsonPath("$.required").value(true))
			.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void keyMayNotOverlapAcrossScopes() throws Exception {
		long apartment = placeType("APARTMENT");
		long house = placeType("HOUSE");

		created(field("PLACE", 0, "area_m2", "NUMBER"));
		submit(field("PLACE", apartment, "area_m2", "NUMBER")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Field area_m2 already exists for this place"));

		created(field("PLACE", apartment, "rooms", "NUMBER"));
		submit(field("PLACE", 0, "rooms", "NUMBER")).andExpect(status().isConflict());
		created(field("PLACE", house, "rooms", "NUMBER"));
		created(field("PERSON", 0, "area_m2", "NUMBER"));
	}

	@Test
	void optionsMustMatchTheDataType() throws Exception {
		submit(field("PLACE", 0, "a", "SELECT")).andExpect(status().isBadRequest());
		submit("""
				{"target": "PLACE", "fieldKey": "b", "label": "B", "dataType": "SELECT", "options": ["a", "A"]}""")
			.andExpect(status().isBadRequest());
		submit("""
				{"target": "PLACE", "fieldKey": "c", "label": "C", "dataType": "NUMBER", "options": ["1"]}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("options are only allowed for SELECT"));
		submit("""
				{"target": "PLACE", "fieldKey": "d", "label": "D", "dataType": "TEXT", "options": []}""")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.options").value(nullValue()));
	}

	@Test
	void personFieldsApplyToEveryone() throws Exception {
		submit(field("PERSON", 7, "nickname", "TEXT")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("typeId must be 0 for PERSON"));
	}

	@Test
	void typeOfAnotherWorkspaceIsNotFound() throws Exception {
		long otherType = this.placeTypes.create(CTX_B, new PlaceTypeCreateRequest("HOTEL", "Khách sạn", null, null))
			.id();
		long otherCategory = this.categories.create(CTX_B, new ItemCategoryRequest("Nội thất", null)).id();

		submit(field("PLACE", otherType, "stars", "NUMBER")).andExpect(status().isNotFound());
		submit(field("ITEM", otherCategory, "brand", "TEXT")).andExpect(status().isNotFound());
	}

	@Test
	void archivedTypeCannotGetNewFields() throws Exception {
		long hotel = placeType("HOTEL");
		this.placeTypes.archive(CTX_A, hotel);

		submit(field("PLACE", hotel, "stars", "NUMBER")).andExpect(status().isConflict());
	}

	@Test
	void invalidKeyIs400() throws Exception {
		submit(field("PLACE", 0, "Area", "NUMBER")).andExpect(status().isBadRequest());
		submit(field("PLACE", 0, "1area", "NUMBER")).andExpect(status().isBadRequest());
	}

	@Test
	void updateChangesOnlyLabelOptionsRequiredAndSortOrder() throws Exception {
		String id = created("""
				{"target": "PLACE", "fieldKey": "view", "label": "Hướng", "dataType": "SELECT", "options": ["Biển"]}""");

		this.mvc.perform(asA(put("/field-defs/" + id)).contentType(MediaType.APPLICATION_JSON).content("""
				{"label": "Hướng nhìn", "options": ["Biển", "Núi"], "required": true, "sortOrder": 4,
				 "fieldKey": "other", "dataType": "TEXT", "target": "ITEM"}"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.label").value("Hướng nhìn"))
			.andExpect(jsonPath("$.options", contains("Biển", "Núi")))
			.andExpect(jsonPath("$.required").value(true))
			.andExpect(jsonPath("$.sortOrder").value(4))
			.andExpect(jsonPath("$.fieldKey").value("view"))
			.andExpect(jsonPath("$.dataType").value("SELECT"))
			.andExpect(jsonPath("$.target").value("PLACE"));
	}

	@Test
	void listFiltersByTargetAndType() throws Exception {
		long hotel = placeType("HOTEL");
		long house = placeType("HOUSE");
		created(field("PLACE", 0, "a", "TEXT"));
		created(field("PLACE", hotel, "b", "TEXT"));
		created(field("PLACE", house, "c", "TEXT"));
		created(field("ITEM", 0, "d", "TEXT"));

		this.mvc.perform(asA(get("/field-defs").param("target", "PLACE")))
			.andExpect(jsonPath("$[*].fieldKey", containsInAnyOrder("a", "b", "c")));
		this.mvc.perform(asA(get("/field-defs").param("target", "PLACE").param("typeId", String.valueOf(hotel))))
			.andExpect(jsonPath("$[*].fieldKey", containsInAnyOrder("a", "b")));
		this.mvc.perform(asA(get("/field-defs").param("target", "ITEM")))
			.andExpect(jsonPath("$[*].fieldKey", contains("d")));
		this.mvc.perform(asA(get("/field-defs"))).andExpect(status().isBadRequest());
	}

	@Test
	void archiveKeepsTheFieldInListAll() throws Exception {
		long id = Long.parseLong(created(field("PLACE", 0, "a", "TEXT")));

		this.mvc.perform(asA(post("/field-defs/" + id + "/archive"))).andExpect(jsonPath("$.status").value("ARCHIVED"));

		assertThat(this.fieldDefs.listAll(WS_A)).singleElement()
			.satisfies(def -> assertThat(def.status().name()).isEqualTo("ARCHIVED"));
		this.mvc.perform(asA(post("/field-defs/" + id + "/unarchive"))).andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	private long placeType(String code) {
		return this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest(code, code, null, null)).id();
	}

	private static String field(String target, long typeId, String key, String dataType) {
		return """
				{"target": "%s", "typeId": %d, "fieldKey": "%s", "label": "L", "dataType": "%s"}"""
			.formatted(target, typeId, key, dataType);
	}

	private ResultActions submit(String json) throws Exception {
		return this.mvc.perform(asA(post("/field-defs")).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private String created(String json) throws Exception {
		String body = submit(json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

}
