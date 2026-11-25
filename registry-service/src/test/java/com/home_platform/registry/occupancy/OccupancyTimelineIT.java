package com.home_platform.registry.occupancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** {@code GET /occupancies/timeline}: mọi lần ở của một địa điểm hay một người (spec Đợt 6, 4.2). */
class OccupancyTimelineIT extends OccupancyTestData {

	@Autowired
	private OccupancyService service;

	@Test
	void placeTimelineHasEveryStatus() throws Exception {
		long room = place(null, "P101");
		long past = occupancy(room, person("Cũ"), this.today.minusDays(60), this.today.minusDays(30));
		long current = occupancy(room, person("Đang ở"), this.today.minusDays(10), null);
		long upcoming = occupancy(room, person("Sắp đến"), this.today.plusDays(5), null);

		this.mvc.perform(asA(get("/occupancies/timeline")).param("placeId", Long.toString(room)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rows[*].id", contains(Long.toString(upcoming), Long.toString(current),
					Long.toString(past))))
			.andExpect(jsonPath("$.rows[*].status", contains("UPCOMING", "CURRENT", "PAST")))
			.andExpect(jsonPath("$.rows[0].personFullName").value("Sắp đến"))
			.andExpect(jsonPath("$.truncated").value(false));
	}

	@Test
	void personTimelineSpansPlaces() throws Exception {
		long an = person("An");
		occupancy(place(null, "Nhà cũ"), an, this.today.minusDays(400), this.today.minusDays(100));
		occupancy(place(null, "Nhà mới"), an, this.today.minusDays(99), null);

		this.mvc.perform(asA(get("/occupancies/timeline")).param("personId", Long.toString(an)))
			.andExpect(jsonPath("$.rows[*].placeName", contains("Nhà mới", "Nhà cũ")));
	}

	@Test
	void deletedRowsAreLeftOut() throws Exception {
		long room = place(null, "P101");
		long mistake = occupancy(room, person("Nhập nhầm"), this.today, null);
		occupancy(room, person("Thật"), this.today, null);
		this.mvc.perform(asA(delete("/occupancies/" + mistake)).param("version", "1"))
			.andExpect(status().isNoContent());

		this.mvc.perform(asA(get("/occupancies/timeline")).param("placeId", Long.toString(room)))
			.andExpect(jsonPath("$.rows[*].personFullName", contains("Thật")));
	}

	@Test
	void exactlyOneFilter() throws Exception {
		this.mvc.perform(asA(get("/occupancies/timeline"))).andExpect(status().isBadRequest());
		this.mvc.perform(asA(get("/occupancies/timeline")).param("placeId", "1").param("personId", "2"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void truncatesAfterTheLimit() throws Exception {
		long room = place(null, "P101");
		occupancy(room, person("A"), this.today.minusDays(3), null);
		long b = occupancy(room, person("B"), this.today.minusDays(2), null);
		long c = occupancy(room, person("C"), this.today.minusDays(1), null);

		OccupancyService.Timeline timeline = this.service.timelineAtPlace(WS_A, room, 2);

		assertThat(timeline.rows()).extracting(view -> view.occupancy().id()).containsExactly(c, b);
		assertThat(timeline.truncated()).isTrue();
	}

}
