package com.home_platform.registry.capacity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import com.home_platform.registry.occupancy.OccupancyTestData;
import com.home_platform.registry.place.PlaceCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** {@code GET /places/vacancies}: địa điểm còn chỗ hôm nay (spec Đợt 6, L4). */
class VacancyIT extends OccupancyTestData {

	@Test
	void placesWithRoomAreListed() throws Exception {
		long building = place(null, "Toà A");
		long floor = place(building, "Tầng 1");
		long room = place(floor, "P101", 3);
		occupancy(room, person("An"), this.today.minusDays(2), null);

		vacancies().andExpect(status().isOk())
			.andExpect(jsonPath("$.items[*].name", contains("P101")))
			.andExpect(jsonPath("$.items[0].placeId").value(Long.toString(room)))
			.andExpect(jsonPath("$.items[0].capacity").value(3))
			.andExpect(jsonPath("$.items[0].occupantCount").value(1))
			.andExpect(jsonPath("$.items[0].free").value(2))
			.andExpect(jsonPath("$.items[0].placeTypeName").value("T"))
			.andExpect(jsonPath("$.items[0].ancestors[*].name", contains("Toà A", "Tầng 1")));
	}

	@Test
	void fullOrOverfullPlacesAreNotVacant() throws Exception {
		long full = place(null, "Đủ", 1);
		occupancy(full, person("A"), this.today, null);
		long over = place(null, "Quá", 1);
		occupancy(over, person("B"), this.today, null);
		occupancy(over, person("C"), this.today, null);
		place(null, "Kho", 0);

		vacancies().andExpect(jsonPath("$.items", empty()));
	}

	@Test
	void emptyLeafWithoutCapacityIsVacant() throws Exception {
		long floor = place(null, "Tầng 2");
		place(floor, "P201");

		vacancies().andExpect(jsonPath("$.items[*].name", contains("P201")))
			.andExpect(jsonPath("$.items[0].capacity").doesNotExist())
			.andExpect(jsonPath("$.items[0].free").doesNotExist())
			.andExpect(jsonPath("$.items[0].occupantCount").value(0));
	}

	@Test
	void anyRelationTakesTheLeafOutOfVacancies() throws Exception {
		long guarded = place(null, "Phòng bảo vệ");
		stay(guarded, person("Bảo vệ"), "STAFF", this.today.minusDays(1), null);
		long left = place(null, "Phòng đã trả");
		occupancy(left, person("Đã đi"), this.today.minusDays(9), this.today.minusDays(1));

		vacancies().andExpect(jsonPath("$.items[*].name", contains("Phòng đã trả")));
	}

	@Test
	void filtersByTypeAndBuilding() throws Exception {
		long a = place(null, "Toà A");
		place(a, "A1");
		long b = place(null, "Toà B");
		place(b, "B1");
		long kiosk = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("KIOSK", "Ki-ốt", null, null)).id();
		this.places.create(CTX_A, new PlaceCreateRequest(null, kiosk, "K1", null, null, null, null, null, null, null));

		vacancies("rootId", Long.toString(a)).andExpect(jsonPath("$.items[*].name", contains("A1")));
		vacancies("placeTypeId", Long.toString(kiosk)).andExpect(jsonPath("$.items[*].name", contains("K1")));
	}

	@Test
	void pagesByName() throws Exception {
		place(null, "R3");
		place(null, "R1");
		place(null, "R2");

		String first = vacancies("limit", "2").andExpect(jsonPath("$.items[*].name", contains("R1", "R2")))
			.andExpect(jsonPath("$.nextCursor").exists())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String cursor = JsonPath.read(first, "$.nextCursor");
		vacancies("limit", "2", "cursor", cursor).andExpect(jsonPath("$.items[*].name", contains("R3")));
	}

	@Test
	void deletedPlacesAreHidden() throws Exception {
		long gone = place(null, "Đã xoá");
		place(null, "Còn");
		this.mvc.perform(asA(delete("/places/" + gone)).param("version", "1")).andExpect(status().isNoContent());

		vacancies().andExpect(jsonPath("$.items[*].name", contains("Còn")));
	}

	private ResultActions vacancies(String... params) throws Exception {
		var request = asA(get("/places/vacancies"));
		for (int i = 0; i < params.length; i += 2) {
			request = request.param(params[i], params[i + 1]);
		}
		return this.mvc.perform(request);
	}

}
