package com.home_platform.registry.place;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class PlaceVersionIT extends PlaceTestData {

	@Autowired
	FieldDefService fieldDefs;

	@Test
	void listsVersionsNewestFirst() throws Exception {
		long type = type("T");
		long id = place(null, type, "A");
		update(id, 1, type, "B");
		update(id, 2, type, "C");

		String first = this.mvc.perform(asA(get("/places/" + id + "/versions")).param("limit", "2"))
			.andExpect(jsonPath("$.items[*].versionNo", contains(3, 2)))
			.andExpect(jsonPath("$.items[0].action").value("UPDATE"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		this.mvc
			.perform(asA(get("/places/" + id + "/versions")).param("limit", "2")
				.param("cursor", (String) JsonPath.read(first, "$.nextCursor")))
			.andExpect(jsonPath("$.items[*].versionNo", contains(1)))
			.andExpect(jsonPath("$.nextCursor").value(nullValue()));
	}

	@Test
	void showsOneVersionWithItsSnapshot() throws Exception {
		long id = place(null, type("T"), "Ban đầu");

		this.mvc.perform(asA(get("/places/" + id + "/versions/1")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.action").value("CREATE"))
			.andExpect(jsonPath("$.snapshot.name").value("Ban đầu"))
			.andExpect(jsonPath("$.snapshot.id").value(String.valueOf(id)));
	}

	@Test
	void deletedPlaceKeepsItsHistory() throws Exception {
		long id = place(null, type("T"), "A");
		this.mvc.perform(asA(delete("/places/" + id)).param("version", "1")).andExpect(status().isNoContent());

		this.mvc.perform(asA(get("/places/" + id + "/versions")))
			.andExpect(jsonPath("$.items[*].action", contains("DELETE", "CREATE")));
	}

	@Test
	void restoresContentButNotPosition() throws Exception {
		long type = type("T");
		long p1 = place(null, type, "P1");
		long p2 = place(null, type, "P2");
		long id = place("""
				{"parentId": "%d", "placeTypeId": "%d", "name": "Một", "address": {"fullAddress": "Địa chỉ X"}}"""
			.formatted(p1, type));
		update(id, 1, type, "Hai");
		this.mvc.perform(asA(delete("/places/" + id + "/address")).param("version", "2")).andExpect(status().isOk());
		this.mvc.perform(asA(post("/places/" + id + "/move")).contentType(MediaType.APPLICATION_JSON).content("""
				{"version": 3, "parentId": "%d"}""".formatted(p2))).andExpect(status().isOk());

		restoreVersion(id, 1, 4).andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(5))
			.andExpect(jsonPath("$.name").value("Một"))
			.andExpect(jsonPath("$.address.fullAddress").value("Địa chỉ X"))
			.andExpect(jsonPath("$.parentId").value(String.valueOf(p2)));
		this.mvc.perform(asA(get("/places/" + id + "/versions/5")))
			.andExpect(jsonPath("$.action").value("UPDATE"))
			.andExpect(jsonPath("$.note").value("Restored from version 1"));
	}

	@Test
	void restoreOfInvalidOldContentWritesNothing() throws Exception {
		long t = type("T");
		long u = type("U");
		long id = place(null, t, "A");
		update(id, 1, u, "A");
		this.placeTypes.archive(CTX_A, t);

		restoreVersion(id, 1, 2).andExpect(status().isConflict());
		getPlace(id).andExpect(jsonPath("$.version").value(2));

		long other = place(null, u, "B");
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.PLACE, 0L, "must", "Must",
				FieldDataType.TEXT, null, true, null));
		restoreVersion(other, 1, 1).andExpect(status().isBadRequest());
		getPlace(other).andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void restoringADeletedPlaceIsAConflict() throws Exception {
		long id = place(null, type("T"), "A");
		this.mvc.perform(asA(delete("/places/" + id)).param("version", "1")).andExpect(status().isNoContent());

		restoreVersion(id, 1, 2).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Restore place " + id + " before restoring an old version"));
	}

	@Test
	void unknownVersionIsNotFound() throws Exception {
		long id = place(null, type("T"), "A");

		this.mvc.perform(asA(get("/places/" + id + "/versions/99")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("PLACE " + id + " version 99 not found"));
		this.mvc.perform(asA(get("/places/12345/versions"))).andExpect(status().isNotFound());
	}

	private void update(long id, int version, long type, String name) throws Exception {
		this.mvc.perform(asA(put("/places/" + id)).contentType(MediaType.APPLICATION_JSON).content("""
				{"version": %d, "placeTypeId": "%d", "name": "%s", "status": "ACTIVE"}""".formatted(version, type, name)))
			.andExpect(status().isOk());
	}

	private ResultActions restoreVersion(long id, int versionNo, int currentVersion) throws Exception {
		return this.mvc.perform(asA(post("/places/" + id + "/versions/" + versionNo + "/restore"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"version\": " + currentVersion + "}"));
	}

}
