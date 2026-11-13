package com.home_platform.registry.occupancy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class OccupancyIT extends OccupancyTestData {

	@Test
	void recordsWhoLivesWhere() throws Exception {
		long room = place(null, "P101");
		long an = person("An");

		postOccupancy(room, an, LocalDate.of(2025, 1, 1), null).andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isString())
			.andExpect(jsonPath("$.placeId").value(String.valueOf(room)))
			.andExpect(jsonPath("$.placeName").value("P101"))
			.andExpect(jsonPath("$.personFullName").value("An"))
			.andExpect(jsonPath("$.relation").value("RESIDENT"))
			.andExpect(jsonPath("$.startDate").value("2025-01-01"))
			.andExpect(jsonPath("$.status").value("CURRENT"))
			.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void statusFollowsTheDates() throws Exception {
		long room = place(null, "P");

		postOccupancy(room, person("A"), this.today.plusDays(1), null).andExpect(jsonPath("$.status").value("UPCOMING"));
		postOccupancy(room, person("B"), this.today.minusDays(10), this.today.minusDays(1))
			.andExpect(jsonPath("$.status").value("PAST"));
		postOccupancy(room, person("C"), this.today.minusDays(10), this.today)
			.andExpect(jsonPath("$.status").value("CURRENT"));
	}

	@Test
	void endBeforeStartIs400() throws Exception {
		postOccupancy(place(null, "P"), person("A"), LocalDate.of(2025, 2, 1), LocalDate.of(2025, 1, 31))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("endDate must not be before startDate"));
	}

	@Test
	void overlappingStaysAtTheSamePlaceConflict() throws Exception {
		long room = place(null, "P");
		long other = place(null, "Q");
		long an = person("An");
		occupancy(room, an, LocalDate.of(2025, 1, 1), null);

		postOccupancy(room, an, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 7, 1)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Person " + an + " already has an overlapping occupancy at this place"));
		postOccupancy(other, an, LocalDate.of(2025, 6, 1), null).andExpect(status().isCreated());
		postOccupancy(room, an, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)).andExpect(status().isCreated());
		postOccupancy(room, an, LocalDate.of(2024, 12, 31), LocalDate.of(2024, 12, 31)).andExpect(status().isConflict());
	}

	@Test
	void editingADateDoesNotConflictWithItself() throws Exception {
		long id = occupancy(place(null, "P"), person("A"), LocalDate.of(2025, 1, 1), null);

		this.mvc.perform(asA(put("/occupancies/" + id)).contentType(MediaType.APPLICATION_JSON).content("""
				{"version": 1, "relation": "OWNER", "startDate": "2024-06-01", "note": "chủ nhà"}"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.startDate").value("2024-06-01"))
			.andExpect(jsonPath("$.relation").value("OWNER"))
			.andExpect(jsonPath("$.note").value("chủ nhà"))
			.andExpect(jsonPath("$.version").value(2));
	}

	@Test
	void endingSetsTheLastDay() throws Exception {
		long id = occupancy(place(null, "P"), person("A"), LocalDate.of(2025, 1, 1), null);

		this.mvc.perform(asA(post("/occupancies/" + id + "/end")).contentType(MediaType.APPLICATION_JSON).content("""
				{"version": 1}""")).andExpect(status().isOk())
			.andExpect(jsonPath("$.endDate").value(this.today.toString()))
			.andExpect(jsonPath("$.version").value(2));
		this.mvc.perform(asA(post("/occupancies/" + id + "/end")).contentType(MediaType.APPLICATION_JSON).content("""
				{"version": 2}""")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Occupancy " + id + " has already ended"));
	}

	@Test
	void deletedPlaceOrPersonIsNotFound() throws Exception {
		long room = place(null, "P");
		long gone = person("Gone");
		this.people.delete(CTX_A, gone, 1, null);
		long ghostPlace = place(null, "Ghost");
		this.places.delete(CTX_A, ghostPlace, 1, null);

		postOccupancy(room, gone, LocalDate.of(2025, 1, 1), null).andExpect(status().isNotFound());
		postOccupancy(ghostPlace, person("A"), LocalDate.of(2025, 1, 1), null).andExpect(status().isNotFound());
	}

	@Test
	void deleteIsSoftAndChecksVersion() throws Exception {
		long id = occupancy(place(null, "P"), person("A"), LocalDate.of(2025, 1, 1), null);

		this.mvc.perform(asA(delete("/occupancies/" + id)).param("version", "9")).andExpect(status().isConflict());
		this.mvc.perform(asA(delete("/occupancies/" + id)).param("version", "1")).andExpect(status().isNoContent());
		this.mvc.perform(asA(get("/occupancies/" + id))).andExpect(status().isNotFound());
	}

	@Test
	void otherWorkspaceSeesNothing() throws Exception {
		long id = occupancy(place(null, "P"), person("A"), LocalDate.of(2025, 1, 1), null);

		this.mvc.perform(asB(get("/occupancies/" + id))).andExpect(status().isNotFound());
	}

}
