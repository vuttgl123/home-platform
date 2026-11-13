package com.home_platform.registry.occupancy;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class OccupancyListIT extends OccupancyTestData {

	@Test
	void currentUpcomingAndPastAtAPlace() throws Exception {
		long room = place(null, "P");
		occupancy(room, person("Now"), this.today.minusDays(5), null);
		occupancy(room, person("Soon"), this.today.plusDays(5), null);
		occupancy(room, person("Gone"), this.today.minusDays(30), this.today.minusDays(10));

		this.mvc.perform(asA(get("/occupancies").param("placeId", String.valueOf(room))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[*].personFullName", contains("Now")))
			.andExpect(jsonPath("$.items[0].status").value("CURRENT"));
		this.mvc.perform(asA(get("/occupancies").param("placeId", String.valueOf(room)).param("status", "UPCOMING")))
			.andExpect(jsonPath("$.items[*].personFullName", contains("Soon")));
		this.mvc.perform(asA(get("/occupancies").param("placeId", String.valueOf(room)).param("status", "PAST")))
			.andExpect(jsonPath("$.items[*].personFullName", contains("Gone")));
	}

	@Test
	void pastIsNewestFirst() throws Exception {
		long room = place(null, "P");
		occupancy(room, person("Older"), LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31));
		occupancy(room, person("Newer"), LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31));

		this.mvc.perform(asA(get("/occupancies").param("placeId", String.valueOf(room)).param("status", "PAST")))
			.andExpect(jsonPath("$.items[*].personFullName", contains("Newer", "Older")));
	}

	@Test
	void subtreeShowsEveryoneInTheBuilding() throws Exception {
		long building = place(null, "A");
		long p1 = place(building, "P1");
		long p2 = place(building, "P2");
		long elsewhere = place(null, "B");
		occupancy(p1, person("One"), LocalDate.of(2025, 1, 1), null);
		occupancy(p2, person("Two"), LocalDate.of(2025, 2, 1), null);
		occupancy(building, person("Owner"), LocalDate.of(2024, 1, 1), null);
		occupancy(elsewhere, person("Out"), LocalDate.of(2025, 1, 1), null);
		occupancy(p1, person("Left"), LocalDate.of(2020, 1, 1), LocalDate.of(2020, 6, 1));

		this.mvc.perform(asA(get("/occupancies").param("subtreeOf", String.valueOf(building))))
			.andExpect(jsonPath("$.items[*].personFullName", contains("Owner", "One", "Two")))
			.andExpect(jsonPath("$.items[*].placeName", contains("A", "P1", "P2")));
	}

	@Test
	void subtreeOccupantsSkipDeletedPlaces() throws Exception {
		long building = place(null, "A");
		long room = place(building, "P1");
		long resident = person("Ghost");
		long id = occupancy(room, resident, LocalDate.of(2025, 1, 1), null);
		this.mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.delete("/occupancies/" + id).header("X-Workspace-Id", WS_A).header("X-User-Id", USER).param("version", "1"));
		this.jdbc.sql("""
				INSERT INTO reg_occupancy (workspace_id, id, place_id, person_id, relation, start_date, created_by, updated_by)
				VALUES (:ws, 777001, :place, :person, 'RESIDENT', '2025-01-01', :user, :user)""")
			.param("ws", WS_A).param("place", room).param("person", resident).param("user", USER).update();
		this.jdbc.sql("UPDATE reg_place SET deleted_at = UTC_TIMESTAMP(3) WHERE workspace_id = :ws AND id = :id")
			.param("ws", WS_A).param("id", room).update();

		this.mvc.perform(asA(get("/occupancies").param("subtreeOf", String.valueOf(building))))
			.andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void personHistoryIsNewestFirst() throws Exception {
		long an = person("An");
		occupancy(place(null, "Old"), an, LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1));
		occupancy(place(null, "New"), an, LocalDate.of(2023, 1, 1), null);

		this.mvc.perform(asA(get("/occupancies").param("personId", String.valueOf(an))))
			.andExpect(jsonPath("$.items[*].placeName", contains("New", "Old")))
			.andExpect(jsonPath("$.items[*].status", contains("CURRENT", "PAST")));
	}

	@Test
	void personDetailShowsCurrentPlaces() throws Exception {
		long an = person("An");
		occupancy(place(null, "Home"), an, LocalDate.of(2023, 1, 1), null);
		occupancy(place(null, "Office"), an, LocalDate.of(2024, 1, 1), null);
		occupancy(place(null, "Old"), an, LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1));

		this.mvc.perform(asA(get("/people/" + an)))
			.andExpect(jsonPath("$.currentOccupancies[*].placeName", containsInAnyOrder("Home", "Office")));
	}

	@Test
	void exactlyOneFilter() throws Exception {
		this.mvc.perform(asA(get("/occupancies")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Give exactly one of placeId, subtreeOf or personId"));
		this.mvc.perform(asA(get("/occupancies").param("placeId", "1").param("personId", "2")))
			.andExpect(status().isBadRequest());
	}

}
