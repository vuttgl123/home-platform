package com.home_platform.registry.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import com.home_platform.registry.RegistryIT;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class ActivityIT extends RegistryIT {

	@Autowired
	PlaceTypeService placeTypes;

	@Test
	void listsChangesNewestFirstAcrossPlaces() throws Exception {
		long type = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("T", "T", null, null)).id();
		String a = create(type, "A");
		Thread.sleep(5);
		String b = create(type, "B");
		Thread.sleep(5);
		this.mvc.perform(asA(put("/places/" + a)).contentType(MediaType.APPLICATION_JSON).content("""
				{"version": 1, "placeTypeId": "%d", "name": "A2", "status": "ACTIVE"}""".formatted(type)))
			.andExpect(status().isOk());

		this.mvc.perform(asA(get("/activity")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[*].entityId", contains(a, b, a)))
			.andExpect(jsonPath("$.items[*].versionNo", contains(2, 1, 1)))
			.andExpect(jsonPath("$.items[0].action").value("UPDATE"))
			.andExpect(jsonPath("$.items[0].entityType").value("PLACE"));
	}

	@Test
	void filtersByEntityType() throws Exception {
		long type = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("T", "T", null, null)).id();
		create(type, "A");

		this.mvc.perform(asA(get("/activity").param("entityType", "PLACE"))).andExpect(jsonPath("$.items", hasSize(1)));
		this.mvc.perform(asA(get("/activity").param("entityType", "ITEM"))).andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void activityPagesWithoutGapsWhenTimesAreEqual() throws Exception {
		int[][] rows = { { 1, 1 }, { 1, 2 }, { 2, 1 }, { 3, 1 }, { 3, 2 }, { 3, 3 }, { 4, 1 } };
		for (int[] row : rows) {
			this.jdbc.sql("""
					INSERT INTO reg_entity_version (workspace_id, entity_type, entity_id, version_no, action, snapshot,
					  changed_by, changed_at)
					VALUES (:ws, 'PLACE', :entity, :version, 'UPDATE', '{}', :user, '2026-01-01 00:00:00.000')""")
				.param("ws", WS_A)
				.param("entity", row[0])
				.param("version", row[1])
				.param("user", USER)
				.update();
		}

		List<String> seen = new ArrayList<>();
		String cursor = null;
		for (int guard = 0; guard < 10; guard++) {
			var request = get("/activity").param("limit", "2");
			if (cursor != null) {
				request = request.param("cursor", cursor);
			}
			String body = this.mvc.perform(asA(request)).andReturn().getResponse().getContentAsString();
			List<String> ids = JsonPath.read(body, "$.items[*].entityId");
			List<Integer> versions = JsonPath.read(body, "$.items[*].versionNo");
			for (int i = 0; i < ids.size(); i++) {
				seen.add(ids.get(i) + "/" + versions.get(i));
			}
			cursor = JsonPath.read(body, "$.nextCursor");
			if (cursor == null) {
				break;
			}
		}

		assertThat(seen).containsExactly("4/1", "3/3", "3/2", "3/1", "2/1", "1/2", "1/1");
	}

	@Test
	void otherWorkspaceIsInvisible() throws Exception {
		long type = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("T", "T", null, null)).id();
		create(type, "A");

		this.mvc.perform(asB(get("/activity"))).andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void badCursorIs400() throws Exception {
		this.mvc.perform(asA(get("/activity").param("cursor", "garbage"))).andExpect(status().isBadRequest());
	}

	private String create(long type, String name) throws Exception {
		String body = this.mvc.perform(asA(post("/places")).contentType(MediaType.APPLICATION_JSON).content("""
				{"placeTypeId": "%d", "name": "%s"}""".formatted(type, name)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.id");
	}

}
