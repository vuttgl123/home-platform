package com.home_platform.registry.placetype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.registry.RegistryIT;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.common.web.ConflictException;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.ErrorResponse;

class PlaceTypeIT extends RegistryIT {

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	FieldDefService fieldDefs;

	@Test
	void createsAndReadsBack() throws Exception {
		String id = create("""
				{"code": " hotel ", "name": " Khách sạn ", "sortOrder": 2}""");

		this.mvc.perform(asA(get("/place-types/" + id)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.code").value("HOTEL"))
			.andExpect(jsonPath("$.name").value("Khách sạn"))
			.andExpect(jsonPath("$.sortOrder").value(2))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.description").value(nullValue()));
	}

	@Test
	void codeIsUppercasedAndDuplicatesConflict() throws Exception {
		create("""
				{"code": "HOTEL", "name": "Khách sạn"}""");

		this.mvc.perform(asA(post("/place-types")).contentType(MediaType.APPLICATION_JSON).content("""
				{"code": "hotel", "name": "Khác"}"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Place type code HOTEL already exists"));
	}

	@Test
	void invalidInputIs400WithFieldErrors() throws Exception {
		expect400("""
				{"code": "HO-TEL", "name": "A"}""", "code");
		expect400("""
				{"code": "HOTEL", "name": "   "}""", "name");
		expect400("{\"code\": \"" + "A".repeat(51) + "\", \"name\": \"A\"}", "code");
	}

	@Test
	void listIsOrderedBySortOrderThenName() throws Exception {
		create("""
				{"code": "B", "name": "B", "sortOrder": 0}""");
		create("""
				{"code": "A", "name": "A", "sortOrder": 0}""");
		create("""
				{"code": "C", "name": "C", "sortOrder": -1}""");

		this.mvc.perform(asA(get("/place-types")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name", contains("C", "A", "B")));
	}

	@Test
	void listReflectsWritesImmediately() throws Exception {
		this.mvc.perform(asA(get("/place-types"))).andExpect(jsonPath("$", hasSize(0)));

		String id = create("""
				{"code": "ROOM", "name": "Phòng"}""");
		this.mvc.perform(asA(get("/place-types"))).andExpect(jsonPath("$", hasSize(1)));

		this.mvc.perform(asA(put("/place-types/" + id)).contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "Phòng ngủ"}""")).andExpect(status().isOk());
		this.mvc.perform(asA(get("/place-types"))).andExpect(jsonPath("$[0].name").value("Phòng ngủ"));

		this.mvc.perform(asA(delete("/place-types/" + id))).andExpect(status().isNoContent());
		this.mvc.perform(asA(get("/place-types"))).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void updateChangesNameDescriptionSortOrderButNotCode() throws Exception {
		String id = create("""
				{"code": "ROOM", "name": "Phòng", "description": "cũ"}""");

		this.mvc.perform(asA(put("/place-types/" + id)).contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "X", "description": "  ", "sortOrder": 5, "code": "OTHER"}"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("X"))
			.andExpect(jsonPath("$.description").value(nullValue()))
			.andExpect(jsonPath("$.sortOrder").value(5))
			.andExpect(jsonPath("$.code").value("ROOM"));
	}

	@Test
	void archiveAndUnarchive() throws Exception {
		String id = create("""
				{"code": "ROOM", "name": "Phòng"}""");

		this.mvc.perform(asA(post("/place-types/" + id + "/archive")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ARCHIVED"));
		assertThatThrownBy(() -> this.placeTypes.requireActive(WS_A, Long.parseLong(id)))
			.isInstanceOf(ConflictException.class)
			.satisfies(ex -> assertThat(((ErrorResponse) ex).getBody().getDetail())
				.isEqualTo("PlaceType " + id + " is archived"));

		this.mvc.perform(asA(post("/place-types/" + id + "/unarchive")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACTIVE"));
		assertThat(this.placeTypes.requireActive(WS_A, Long.parseLong(id)).code()).isEqualTo("ROOM");
	}

	@Test
	void deleteUnusedTypeAndRefuseUsedOne() throws Exception {
		String unused = create("""
				{"code": "ROOM", "name": "Phòng"}""");
		String used = create("""
				{"code": "HOUSE", "name": "Nhà"}""");
		this.jdbc.sql("""
				INSERT INTO reg_place (workspace_id, id, place_type_id, path, name, created_by, updated_by)
				VALUES (:ws, 900001, :type, '/900001/', 'P', :user, :user)""")
			.param("ws", WS_A)
			.param("type", Long.parseLong(used))
			.param("user", USER)
			.update();

		this.mvc.perform(asA(delete("/place-types/" + unused))).andExpect(status().isNoContent());
		this.mvc.perform(asA(get("/place-types/" + unused))).andExpect(status().isNotFound());

		this.mvc.perform(asA(delete("/place-types/" + used)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("PlaceType " + used + " is used by places; archive it instead"));
	}

	@Test
	void deletingATypeRemovesItsOwnFields() throws Exception {
		long id = Long.parseLong(create("""
				{"code": "VILLA", "name": "Biệt thự"}"""));
		long field = this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.PLACE, id, "pool", "Hồ bơi",
				FieldDataType.BOOLEAN, null, null, null)).id();
		this.fieldDefs.listAll(WS_A);

		this.mvc.perform(asA(delete("/place-types/" + id))).andExpect(status().isNoContent());

		this.mvc.perform(asA(get("/field-defs/" + field))).andExpect(status().isNotFound());
		assertThat(this.fieldDefs.listAll(WS_A)).isEmpty();
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.PLACE, 0L, "pool", "Hồ bơi",
				FieldDataType.BOOLEAN, null, null, null));
	}

	@Test
	void otherWorkspaceSeesNothing() throws Exception {
		String id = create("""
				{"code": "ROOM", "name": "Phòng"}""");

		this.mvc.perform(asB(get("/place-types/" + id))).andExpect(status().isNotFound());
		this.mvc.perform(asB(get("/place-types"))).andExpect(jsonPath("$", hasSize(0)));
		this.mvc.perform(asB(put("/place-types/" + id)).contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "X"}""")).andExpect(status().isNotFound());
		this.mvc.perform(asB(delete("/place-types/" + id))).andExpect(status().isNotFound());
		this.mvc.perform(asA(get("/place-types/" + id))).andExpect(jsonPath("$.name").value("Phòng"));
	}

	private String create(String json) throws Exception {
		String body = this.mvc.perform(asA(post("/place-types")).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isString())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private void expect400(String json, String field) throws Exception {
		this.mvc.perform(asA(post("/place-types")).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field", hasItem(field)));
	}

}
