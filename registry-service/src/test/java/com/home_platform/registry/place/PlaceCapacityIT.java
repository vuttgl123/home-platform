package com.home_platform.registry.place;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.registry.occupancy.OccupancyTestData;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Sức chứa của một địa điểm và số người đang ở đó (spec Đợt 6, L1 và L2). */
class PlaceCapacityIT extends OccupancyTestData {

	@Test
	void capacityIsSavedAndVersioned() throws Exception {
		long id = created(createWith("4"));
		this.mvc.perform(asA(get("/places/" + id)))
			.andExpect(jsonPath("$.capacity").value(4))
			.andExpect(jsonPath("$.occupantCount").value(0));

		update(id, 1, "2").andExpect(status().isOk()).andExpect(jsonPath("$.capacity").value(2));
		this.mvc.perform(asA(get("/places/" + id + "/versions/2"))).andExpect(jsonPath("$.snapshot.capacity").value(2));

		update(id, 2, "null").andExpect(status().isOk()).andExpect(jsonPath("$.capacity").doesNotExist());
	}

	@Test
	void capacityOutOfRangeIs400() throws Exception {
		createWith("-1").andExpect(status().isBadRequest());
		createWith("10000").andExpect(status().isBadRequest());
		long id = created(createWith("9999"));
		update(id, 1, "0").andExpect(status().isOk()).andExpect(jsonPath("$.capacity").value(0));
	}

	@Test
	void restoringAVersionBringsCapacityBack() throws Exception {
		long id = created(createWith("3"));
		update(id, 1, "5").andExpect(status().isOk());

		this.mvc.perform(asA(post("/places/" + id + "/versions/1/restore")).contentType(MediaType.APPLICATION_JSON)
			.content("{\"version\": 2}")).andExpect(status().isOk()).andExpect(jsonPath("$.capacity").value(3));
	}

	@Test
	void occupantCountCountsEveryRelationLivingToday() throws Exception {
		long room = place(null, "P101");
		stay(room, person("Ở"), "RESIDENT", this.today.minusDays(10), null);
		stay(room, person("Khách"), "GUEST", this.today, this.today.plusDays(2));
		stay(room, person("Bảo vệ"), "STAFF", this.today.minusDays(1), null);
		occupancy(room, person("Đi hôm nay"), this.today.minusDays(5), this.today);
		occupancy(room, person("Đi hôm qua"), this.today.minusDays(5), this.today.minusDays(1));
		occupancy(room, person("Đến mai"), this.today.plusDays(1), null);
		long ghost = person("Đã xoá");
		occupancy(room, ghost, this.today.minusDays(3), null);
		this.jdbc.sql("UPDATE reg_person SET deleted_at = UTC_TIMESTAMP(3) WHERE workspace_id = :ws AND id = :id")
			.param("ws", WS_A)
			.param("id", ghost)
			.update();

		this.mvc.perform(asA(get("/places/" + room))).andExpect(jsonPath("$.occupantCount").value(4));
	}

	@Test
	void lowerCapacityThanOccupantsIsAllowed() throws Exception {
		long id = created(createWith("3"));
		occupancy(id, person("A"), this.today, null);
		occupancy(id, person("B"), this.today, null);

		update(id, 1, "1").andExpect(status().isOk())
			.andExpect(jsonPath("$.capacity").value(1))
			.andExpect(jsonPath("$.occupantCount").value(2));
	}

	private ResultActions createWith(String capacity) throws Exception {
		return this.mvc.perform(asA(post("/places")).contentType(MediaType.APPLICATION_JSON).content("""
				{"placeTypeId": "%d", "name": "Phòng", "capacity": %s}""".formatted(this.type, capacity)));
	}

	private ResultActions update(long id, int version, String capacity) throws Exception {
		return this.mvc.perform(asA(put("/places/" + id)).contentType(MediaType.APPLICATION_JSON).content("""
				{"version": %d, "placeTypeId": "%d", "name": "Phòng", "status": "ACTIVE", "capacity": %s}"""
			.formatted(version, this.type, capacity)));
	}

	private static long created(ResultActions result) throws Exception {
		String body = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return Long.parseLong(JsonPath.read(body, "$.id"));
	}

	private void stay(long placeId, long personId, String relation, java.time.LocalDate start,
			java.time.LocalDate end) throws Exception {
		String endJson = (end != null) ? "\"" + end + "\"" : "null";
		this.mvc.perform(asA(post("/occupancies")).contentType(MediaType.APPLICATION_JSON).content("""
				{"placeId": "%d", "personId": "%d", "relation": "%s", "startDate": "%s", "endDate": %s}"""
			.formatted(placeId, personId, relation, start, endJson))).andExpect(status().isCreated());
	}

}
