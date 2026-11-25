package com.home_platform.registry.item;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.home_platform.registry.support.BusinessDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Ngày hết bảo hành và chu kỳ bảo trì của món đồ (spec Đợt 8, L3). */
class ItemWarrantyIT extends ItemTestData {

	@Autowired
	private BusinessDate businessDate;

	@Test
	void warrantyAndIntervalAreSavedAndVersioned() throws Exception {
		long id = item("""
				{"placeId": "%d", "name": "Máy lạnh", "placedAt": "2026-01-10",
				 "warrantyUntil": "2027-03-12", "serviceEveryMonths": 6}""".formatted(this.room));
		this.mvc.perform(asA(get("/items/" + id)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.warrantyUntil").value("2027-03-12"))
			.andExpect(jsonPath("$.serviceEveryMonths").value(6))
			.andExpect(jsonPath("$.nextServiceOn").value("2026-07-10"));

		putItem(id, """
				{"version": 1, "name": "Máy lạnh", "status": "IN_USE", "placedAt": "2026-01-10",
				 "warrantyUntil": "2027-03-12", "serviceEveryMonths": 12}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.serviceEveryMonths").value(12))
			.andExpect(jsonPath("$.nextServiceOn").value("2027-01-10"));
		this.mvc.perform(asA(get("/items/" + id + "/versions/2")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.snapshot.serviceEveryMonths").value(12))
			.andExpect(jsonPath("$.snapshot.warrantyUntil").value("2027-03-12"));
	}

	@Test
	void intervalCountsFromCreationWithoutPlacedAt() throws Exception {
		long id = item("{\"placeId\": \"%d\", \"name\": \"Bình nóng lạnh\", \"serviceEveryMonths\": 3}"
			.formatted(this.room));
		this.mvc.perform(asA(get("/items/" + id)))
			.andExpect(jsonPath("$.nextServiceOn").value(this.businessDate.today().plusMonths(3).toString()));
	}

	@Test
	void noIntervalHasNoNextService() throws Exception {
		long id = item(this.room, "Ghế");
		this.mvc.perform(asA(get("/items/" + id)))
			.andExpect(jsonPath("$.serviceEveryMonths").doesNotExist())
			.andExpect(jsonPath("$.nextServiceOn").doesNotExist());
	}

	@Test
	void intervalOutOfRangeIs400() throws Exception {
		postItem("{\"placeId\": \"%d\", \"name\": \"A\", \"serviceEveryMonths\": 0}".formatted(this.room))
			.andExpect(status().isBadRequest());
		postItem("{\"placeId\": \"%d\", \"name\": \"A\", \"serviceEveryMonths\": 121}".formatted(this.room))
			.andExpect(status().isBadRequest());
	}

	@Test
	void restoringAVersionBringsThemBack() throws Exception {
		long id = item("""
				{"placeId": "%d", "name": "Tủ lạnh", "warrantyUntil": "2027-05-01", "serviceEveryMonths": 6}"""
			.formatted(this.room));
		putItem(id, "{\"version\": 1, \"name\": \"Tủ lạnh\", \"status\": \"IN_USE\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.warrantyUntil").doesNotExist());
		this.mvc
			.perform(asA(post("/items/" + id + "/versions/1/restore")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"version\": 2}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.warrantyUntil").value(LocalDate.of(2027, 5, 1).toString()))
			.andExpect(jsonPath("$.serviceEveryMonths").value(6));
	}

}
