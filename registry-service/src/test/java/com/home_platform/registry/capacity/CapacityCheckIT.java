package com.home_platform.registry.capacity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.registry.occupancy.OccupancyTestData;
import com.home_platform.registry.place.PlaceCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** {@code GET /places/{id}/capacity-check} (spec Đợt 6, L3). */
class CapacityCheckIT extends OccupancyTestData {

	@Test
	void reportsCapacityAndPeak() throws Exception {
		long room = room(2);
		occupancy(room, person("A"), this.today.minusDays(5), null);
		occupancy(room, person("B"), this.today, null);

		check(room, "startDate", this.today.toString()).andExpect(status().isOk())
			.andExpect(jsonPath("$.capacity").value(2))
			.andExpect(jsonPath("$.peak").value(2))
			.andExpect(jsonPath("$.peakDate").value(this.today.toString()));
	}

	@Test
	void editedRowIsNotCountedAgainstItself() throws Exception {
		long room = room(2);
		occupancy(room, person("A"), this.today.minusDays(5), null);
		long edited = occupancy(room, person("B"), this.today, null);

		check(room, "startDate", this.today.toString(), "excludeOccupancyId", Long.toString(edited))
			.andExpect(jsonPath("$.peak").value(1));
	}

	@Test
	void noCapacityStillReportsPeak() throws Exception {
		long room = place(null, "Không giới hạn");
		occupancy(room, person("A"), this.today, null);

		check(room, "startDate", this.today.toString()).andExpect(jsonPath("$.capacity").doesNotExist())
			.andExpect(jsonPath("$.peak").value(1));
	}

	@Test
	void deletedPlaceIs404() throws Exception {
		long room = room(2);
		this.mvc.perform(asA(delete("/places/" + room)).param("version", "1")).andExpect(status().isNoContent());

		check(room, "startDate", this.today.toString()).andExpect(status().isNotFound());
	}

	@Test
	void endBeforeStartIs400() throws Exception {
		check(room(2), "startDate", this.today.toString(), "endDate", this.today.minusDays(1).toString())
			.andExpect(status().isBadRequest());
	}

	private long room(int capacity) {
		return this.places
			.create(CTX_A, new PlaceCreateRequest(null, this.type, "P" + capacity, null, null, capacity, null, null,
					null, null))
			.id();
	}

	private ResultActions check(long placeId, String... params) throws Exception {
		var request = asA(get("/places/" + placeId + "/capacity-check"));
		for (int i = 0; i < params.length; i += 2) {
			request = request.param(params[i], params[i + 1]);
		}
		return this.mvc.perform(request);
	}

}
