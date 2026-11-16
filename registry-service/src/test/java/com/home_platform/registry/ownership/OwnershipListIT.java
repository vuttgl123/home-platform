package com.home_platform.registry.ownership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class OwnershipListIT extends OwnershipTestData {

	@Test
	void currentUpcomingAndPastAtAPlace() throws Exception {
		long room = place(null, "P101");
		ownership(room, person("Cũ"), null, LocalDate.of(2020, 1, 1), this.today.minusDays(1));
		ownership(room, person("Nay"), null, LocalDate.of(2021, 1, 1), null);
		ownership(room, person("Sắp"), null, this.today.plusDays(10), null);

		list("placeId", room, "CURRENT").andExpect(jsonPath("$.items[*].personFullName", contains("Nay")));
		list("placeId", room, "UPCOMING").andExpect(jsonPath("$.items[*].personFullName", contains("Sắp")));
		list("placeId", room, "PAST").andExpect(jsonPath("$.items[*].personFullName", contains("Cũ")));
		this.mvc.perform(asA(get("/ownerships").param("placeId", String.valueOf(room))))
			.andExpect(jsonPath("$.items[*].personFullName", contains("Nay")))
			.andExpect(jsonPath("$.items[0].status").value("CURRENT"));
	}

	@Test
	void currentOwnersInAWholeBuilding() throws Exception {
		long building = place(null, "Toà");
		long a = place(building, "A");
		long b = place(building, "B");
		ownership(a, person("An"), null, LocalDate.of(2021, 1, 1), null);
		ownership(b, person("Bình"), null, LocalDate.of(2021, 1, 2), null);
		this.places.delete(CTX_A, b, 1, null);

		this.mvc.perform(asA(get("/ownerships").param("subtreeOf", String.valueOf(building))))
			.andExpect(jsonPath("$.items[*].personFullName", contains("An")));
	}

	@Test
	void everythingAPersonOwnsNewestFirst() throws Exception {
		long an = person("An");
		ownership(place(null, "Cũ"), an, null, LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1));
		ownership(place(null, "Mới"), an, null, LocalDate.of(2023, 1, 1), null);

		this.mvc.perform(asA(get("/ownerships").param("personId", String.valueOf(an))))
			.andExpect(jsonPath("$.items[*].placeName", contains("Mới", "Cũ")));
	}

	@Test
	void exactlyOneFilter() throws Exception {
		this.mvc.perform(asA(get("/ownerships")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Give exactly one of placeId, subtreeOf or personId"));
	}

	@Test
	void listsUseTheirIndexes() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		ownership(room, an, null, LocalDate.of(2021, 1, 1), null);
		ownership(place(null, "P102"), person("Bình"), null, LocalDate.of(2021, 1, 1), null);

		assertThat(key("EXPLAIN SELECT id FROM reg_ownership WHERE workspace_id = :ws AND deleted_at IS NULL "
				+ "AND place_id = :id AND start_date <= :today AND (end_date IS NULL OR end_date >= :today) "
				+ "ORDER BY start_date, id", room)).contains("idx_reg_ownership_place");
		assertThat(key("EXPLAIN SELECT id FROM reg_ownership WHERE workspace_id = :ws AND deleted_at IS NULL "
				+ "AND person_id = :id ORDER BY start_date DESC, id DESC", an)).contains("idx_reg_ownership_person");
	}

	/**
	 * Các index dùng được cho câu truy vấn, sau khi chắc nó không quét cả bảng. Với vài dòng thử, MySQL
	 * có thể chọn khoá chính (cũng bắt đầu bằng workspace_id), nên chỉ kiểm index có trong danh sách.
	 */
	private String key(String sql, long id) {
		List<Map<String, Object>> plan = this.jdbc.sql(sql.replace(":today", "'" + this.today + "'"))
			.param("ws", WS_A)
			.param("id", id)
			.query()
			.listOfRows();
		assertThat(plan.get(0).get("type")).isNotEqualTo("ALL");
		return String.valueOf(plan.get(0).get("possible_keys"));
	}

	private org.springframework.test.web.servlet.ResultActions list(String by, long id, String statusName) throws Exception {
		return this.mvc.perform(asA(get("/ownerships").param(by, String.valueOf(id)).param("status", statusName)));
	}

}
