package com.home_platform.registry.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.common.history.ChangeAction;
import com.home_platform.common.history.EntityVersion;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class PlaceIT extends PlaceTestData {

	@Autowired
	FieldDefService fieldDefs;

	@Test
	void createsATopLevelPlace() throws Exception {
		long building = type("BUILDING");

		String body = postPlace("""
				{"placeTypeId": "%d", "name": " Toà A ", "code": "A"}""".formatted(building))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isString())
			.andExpect(jsonPath("$.parentId").value(nullValue()))
			.andExpect(jsonPath("$.depth").value(0))
			.andExpect(jsonPath("$.name").value("Toà A"))
			.andExpect(jsonPath("$.code").value("A"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.version").value(1))
			.andExpect(jsonPath("$.ancestors", hasSize(0)))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
		assertThat((String) com.jayway.jsonpath.JsonPath.read(body, "$.path")).isEqualTo("/" + id + "/");
	}

	@Test
	void createsAChildWithPathAndAncestors() throws Exception {
		long type = type("T");
		long a = place(null, type, "Toà A");
		long b = place(a, type, "Tầng 1");
		long c = place(b, type, "P101");

		getPlace(c).andExpect(status().isOk())
			.andExpect(jsonPath("$.depth").value(2))
			.andExpect(jsonPath("$.path").value("/" + a + "/" + b + "/" + c + "/"))
			.andExpect(jsonPath("$.parentId").value(String.valueOf(b)))
			.andExpect(jsonPath("$.ancestors[*].id", contains(String.valueOf(a), String.valueOf(b))))
			.andExpect(jsonPath("$.ancestors[*].name", contains("Toà A", "Tầng 1")));
	}

	@Test
	void nestingStopsAtTwentyLevels() throws Exception {
		long type = type("T");
		Long parent = null;
		for (int level = 0; level < 20; level++) {
			parent = place(parent, type, "L" + level);
		}
		getPlace(parent).andExpect(jsonPath("$.depth").value(19));

		postPlace("""
				{"parentId": "%d", "placeTypeId": "%d", "name": "too deep"}""".formatted(parent, type))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Places can be nested at most 20 levels deep"));
	}

	@Test
	void parentMustExistInThisWorkspace() throws Exception {
		long type = type("T");
		long otherType = this.placeTypes
			.create(CTX_B, new com.home_platform.registry.placetype.PlaceTypeCreateRequest("T", "T", null, null))
			.id();
		long other = this.mvc
			.perform(asB(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/places"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"placeTypeId": "%d", "name": "B"}""".formatted(otherType)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString()
			.transform(json -> Long.parseLong(com.jayway.jsonpath.JsonPath.read(json, "$.id")));

		postPlace("""
				{"parentId": "999", "placeTypeId": "%d", "name": "x"}""".formatted(type))
			.andExpect(status().isNotFound());
		postPlace("""
				{"parentId": "%d", "placeTypeId": "%d", "name": "x"}""".formatted(other, type))
			.andExpect(status().isNotFound());
	}

	@Test
	void archivedTypeIsRejectedForNewPlaces() throws Exception {
		long type = type("T");
		this.placeTypes.archive(CTX_A, type);

		postPlace("""
				{"placeTypeId": "%d", "name": "x"}""".formatted(type))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("PlaceType " + type + " is archived"));
	}

	@Test
	void attributesAreValidated() throws Exception {
		long type = type("T");
		this.fieldDefs.create(CTX_A,
				new FieldDefCreateRequest(FieldTarget.PLACE, 0L, "area", "Area", FieldDataType.NUMBER, null, null, null));

		postPlace("""
				{"placeTypeId": "%d", "name": "x", "attributes": {"area": "x"}}""".formatted(type))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("attributes.area"));
		long id = place("""
				{"placeTypeId": "%d", "name": "x", "attributes": {"area": 42}}""".formatted(type));
		getPlace(id).andExpect(jsonPath("$.attributes.area").value(42));
	}

	@Test
	void largeNumberAttributesRoundTrip() throws Exception {
		long type = type("T");
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.PLACE, 0L, "price", "Giá",
				FieldDataType.NUMBER, null, null, null));
		long id = place("""
				{"placeTypeId": "%d", "name": "Căn hộ", "attributes": {"price": 3500000000}}""".formatted(type));

		String body = getPlace(id).andExpect(jsonPath("$.attributes.price").isNumber())
			.andExpect(jsonPath("$.attributes.price").value(3500000000L))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String attributes = new com.fasterxml.jackson.databind.ObjectMapper()
			.writeValueAsString(com.jayway.jsonpath.JsonPath.read(body, "$.attributes"));
		putPlace(id, """
				{"version": 1, "placeTypeId": "%d", "name": "Căn hộ", "status": "ACTIVE", "attributes": %s}"""
			.formatted(type, attributes)).andExpect(status().isOk());
		this.mvc.perform(asA(get("/places/" + id + "/versions/1")))
			.andExpect(jsonPath("$.snapshot.attributes.price").isNumber());
	}

	@Test
	void createRecordsVersionOne() throws Exception {
		long id = place(null, type("T"), "Toà A");

		EntityVersion v1 = version(id, 1);
		assertThat(v1.action()).isEqualTo(ChangeAction.CREATE);
		assertThat(v1.changedBy()).isEqualTo(USER);
		assertThat(v1.snapshot().get("name").asString()).isEqualTo("Toà A");
		assertThat(v1.snapshot().get("path").asString()).isEqualTo("/" + id + "/");
		assertThat(v1.snapshot().has("createdAt")).isFalse();
		assertThat(v1.snapshot().has("updatedAt")).isFalse();
		assertThat(v1.snapshot().has("version")).isFalse();
	}

	@Test
	void updateWritesFieldsAndBumpsVersion() throws Exception {
		long type = type("T");
		long id = place(null, type, "A");

		putPlace(id, """
				{"version": 1, "placeTypeId": "%d", "name": "B", "status": "INACTIVE", "note": "đổi tên"}"""
			.formatted(type))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(2))
			.andExpect(jsonPath("$.name").value("B"))
			.andExpect(jsonPath("$.status").value("INACTIVE"));

		EntityVersion v2 = version(id, 2);
		assertThat(v2.action()).isEqualTo(ChangeAction.UPDATE);
		assertThat(v2.changedFields()).contains("name", "status");
		assertThat(v2.note()).isEqualTo("đổi tên");
	}

	@Test
	void updateWithStaleVersionIsAConflict() throws Exception {
		long type = type("T");
		long id = place(null, type, "A");
		String json = """
				{"version": 1, "placeTypeId": "%d", "name": "B", "status": "ACTIVE"}""".formatted(type);

		putPlace(id, json).andExpect(status().isOk());
		putPlace(id, json).andExpect(status().isConflict());
	}

	@Test
	void updateNeverMovesThePlace() throws Exception {
		long type = type("T");
		long a = place(null, type, "A");
		long b = place(null, type, "B");

		putPlace(b, """
				{"version": 1, "placeTypeId": "%d", "name": "B2", "status": "ACTIVE",
				 "parentId": "%d", "path": "/x/", "depth": 5}""".formatted(type, a))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.parentId").value(nullValue()))
			.andExpect(jsonPath("$.path").value("/" + b + "/"))
			.andExpect(jsonPath("$.depth").value(0));
	}

	@Test
	void keepingAnArchivedTypeIsAllowedButSwitchingToOneIsNot() throws Exception {
		long x = type("X");
		long y = type("Y");
		long id = place(null, x, "A");
		this.placeTypes.archive(CTX_A, x);
		this.placeTypes.archive(CTX_A, y);

		putPlace(id, """
				{"version": 1, "placeTypeId": "%d", "name": "A2", "status": "ACTIVE"}""".formatted(x))
			.andExpect(status().isOk());
		putPlace(id, """
				{"version": 2, "placeTypeId": "%d", "name": "A3", "status": "ACTIVE"}""".formatted(y))
			.andExpect(status().isConflict());
	}

	@Test
	void otherWorkspaceCannotSeeOrEdit() throws Exception {
		long type = type("T");
		long id = place(null, type, "A");

		this.mvc.perform(asB(get("/places/" + id))).andExpect(status().isNotFound());
		this.mvc.perform(asB(put("/places/" + id)).contentType(MediaType.APPLICATION_JSON).content("""
				{"version": 1, "placeTypeId": "%d", "name": "B", "status": "ACTIVE"}""".formatted(type)))
			.andExpect(status().isNotFound());
	}

}
