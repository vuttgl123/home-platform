package com.home_platform.registry.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.ResultActions;

class ActivityIT extends RegistryIT {

	private static final long OTHER_USER = 4242L;

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
	void activityCarriesEntityNames() throws Exception {
		long type = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("T", "T", null, null)).id();
		String place = create(type, "Toà A");
		String person = id(this.mvc.perform(asA(post("/people")).contentType(MediaType.APPLICATION_JSON).content("""
				{"fullName": "Nguyễn An"}""")).andExpect(status().isCreated()));
		String item = id(this.mvc.perform(asA(post("/items")).contentType(MediaType.APPLICATION_JSON).content("""
				{"placeId": "%s", "name": "Tủ lạnh"}""".formatted(place))).andExpect(status().isCreated()));

		this.mvc.perform(asA(get("/activity")))
			.andExpect(jsonPath("$.items[?(@.entityId == '" + place + "')].entityName", contains("Toà A")))
			.andExpect(jsonPath("$.items[?(@.entityId == '" + person + "')].entityName", contains("Nguyễn An")))
			.andExpect(jsonPath("$.items[?(@.entityId == '" + item + "')].entityName", contains("Tủ lạnh")));
	}

	@Test
	void activityNamesDeletedRecords() throws Exception {
		long type = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("T", "T", null, null)).id();
		String place = create(type, "Gone");
		this.mvc.perform(asA(delete("/places/" + place)).param("version", "1")).andExpect(status().isNoContent());

		this.mvc.perform(asA(get("/activity")))
			.andExpect(jsonPath("$.items[*].action", contains("DELETE", "CREATE")))
			.andExpect(jsonPath("$.items[*].entityName", contains("Gone", "Gone")));
	}

	@Test
	void activityAfterRenameShowsTheCurrentName() throws Exception {
		long type = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("T", "T", null, null)).id();
		String place = create(type, "Old");
		this.mvc.perform(asA(put("/places/" + place)).contentType(MediaType.APPLICATION_JSON).content("""
				{"version": 1, "placeTypeId": "%d", "name": "New", "status": "ACTIVE"}""".formatted(type)))
			.andExpect(status().isOk());

		this.mvc.perform(asA(get("/activity"))).andExpect(jsonPath("$.items[*].entityName", contains("New", "New")));
	}

	@Test
	void recordsThatNoLongerExistHaveNoName() throws Exception {
		this.jdbc.sql("""
				INSERT INTO reg_entity_version (workspace_id, entity_type, entity_id, version_no, action, snapshot,
				  changed_by, changed_at)
				VALUES (:ws, 'PLACE', 1, 1, 'CREATE', '{}', :user, '2026-01-01 00:00:00.000')""")
			.param("ws", WS_A)
			.param("user", USER)
			.update();

		this.mvc.perform(asA(get("/activity"))).andExpect(jsonPath("$.items[0].entityName").value(nullValue()));
	}

	private static String id(ResultActions created) throws Exception {
		return JsonPath.read(created.andReturn().getResponse().getContentAsString(), "$.id");
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
	void filtersByAction() throws Exception {
		insert(1, 1, "CREATE", USER, "2026-01-01 08:00:00.000");
		insert(1, 2, "DELETE", USER, "2026-01-02 08:00:00.000");

		this.mvc.perform(asA(get("/activity").param("action", "DELETE")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[*].versionNo", contains(2)));
	}

	@Test
	void filtersByChangedBy() throws Exception {
		insert(1, 1, "CREATE", USER, "2026-01-01 08:00:00.000");
		insert(2, 1, "CREATE", OTHER_USER, "2026-01-01 09:00:00.000");

		this.mvc.perform(asA(get("/activity").param("changedBy", String.valueOf(OTHER_USER))))
			.andExpect(jsonPath("$.items[*].entityId", contains("2")));
		this.mvc.perform(asA(get("/activity").param("changedBy", String.valueOf(USER))))
			.andExpect(jsonPath("$.items[*].entityId", contains("1")));
	}

	@Test
	void filtersByTimeRange() throws Exception {
		insert(1, 1, "CREATE", USER, "2026-01-01 08:00:00.000");
		insert(1, 2, "UPDATE", USER, "2026-01-02 08:00:00.000");
		insert(1, 3, "UPDATE", USER, "2026-01-03 08:00:00.000");

		this.mvc
			.perform(asA(get("/activity").param("from", "2026-01-02T00:00:00Z").param("to", "2026-01-03T00:00:00Z")))
			.andExpect(jsonPath("$.items[*].versionNo", contains(2)));
		this.mvc.perform(asA(get("/activity").param("from", "2026-01-02T08:00:00Z")))
			.andExpect(jsonPath("$.items[*].versionNo", contains(3, 2)));
		this.mvc.perform(asA(get("/activity").param("to", "2026-01-02T08:00:00Z")))
			.andExpect(jsonPath("$.items[*].versionNo", contains(1)));
	}

	@Test
	void filtersCombine() throws Exception {
		insert(1, 1, "CREATE", USER, "2026-01-01 08:00:00.000");
		insert(1, 2, "UPDATE", OTHER_USER, "2026-01-02 08:00:00.000");
		insert(1, 3, "UPDATE", USER, "2026-01-03 08:00:00.000");
		insert(1, 4, "UPDATE", USER, "2026-01-05 08:00:00.000");

		this.mvc
			.perform(asA(get("/activity").param("entityType", "PLACE")
				.param("action", "UPDATE")
				.param("changedBy", String.valueOf(USER))
				.param("from", "2026-01-02T00:00:00Z")
				.param("to", "2026-01-04T00:00:00Z")))
			.andExpect(jsonPath("$.items[*].versionNo", contains(3)));
	}

	/** Năm dòng khớp xen giữa các dòng không khớp, trang 2 dòng: đi hết ba trang không mất, không lặp. */
	@Test
	void filteredListPagesWithoutGaps() throws Exception {
		for (int v = 1; v <= 10; v++) {
			insert(1, v, (v % 2 == 0) ? "UPDATE" : "CREATE", USER, "2026-01-01 08:00:00.000");
		}

		List<Integer> seen = new ArrayList<>();
		String cursor = null;
		int pages = 0;
		for (int guard = 0; guard < 10; guard++) {
			var request = get("/activity").param("action", "UPDATE").param("limit", "2");
			if (cursor != null) {
				request = request.param("cursor", cursor);
			}
			String body = this.mvc.perform(asA(request)).andReturn().getResponse().getContentAsString();
			seen.addAll(JsonPath.<List<Integer>>read(body, "$.items[*].versionNo"));
			pages++;
			cursor = JsonPath.read(body, "$.nextCursor");
			if (cursor == null) {
				break;
			}
		}

		assertThat(seen).containsExactly(10, 8, 6, 4, 2);
		assertThat(pages).isEqualTo(3);
	}

	@Test
	void fromAfterToIs400() throws Exception {
		this.mvc
			.perform(asA(get("/activity").param("from", "2026-01-03T00:00:00Z").param("to", "2026-01-02T00:00:00Z")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("from must be before to"));
		this.mvc
			.perform(asA(get("/activity").param("from", "2026-01-02T00:00:00Z").param("to", "2026-01-02T00:00:00Z")))
			.andExpect(status().isBadRequest());
	}

	@Test
	void unknownActionIs400() throws Exception {
		this.mvc.perform(asA(get("/activity").param("action", "XYZ"))).andExpect(status().isBadRequest());
	}

	@Test
	void badInstantIs400() throws Exception {
		this.mvc.perform(asA(get("/activity").param("from", "17-11-2026"))).andExpect(status().isBadRequest());
	}

	private void insert(long entity, int version, String action, long user, String at) {
		this.jdbc.sql("""
				INSERT INTO reg_entity_version (workspace_id, entity_type, entity_id, version_no, action, snapshot,
				  changed_by, changed_at)
				VALUES (:ws, 'PLACE', :entity, :version, :action, '{}', :user, :at)""")
			.param("ws", WS_A)
			.param("entity", entity)
			.param("version", version)
			.param("action", action)
			.param("user", user)
			.param("at", at)
			.update();
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
