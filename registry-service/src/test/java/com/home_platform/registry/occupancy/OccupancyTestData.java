package com.home_platform.registry.occupancy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.home_platform.registry.RegistryIT;
import com.home_platform.registry.person.PersonRequest;
import com.home_platform.registry.person.PersonService;
import com.home_platform.registry.place.PlaceCreateRequest;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import com.home_platform.registry.support.BusinessDate;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Hàm phụ dùng chung cho các test cư trú. */
abstract class OccupancyTestData extends RegistryIT {

	@Autowired
	protected PlaceService places;

	@Autowired
	protected PersonService people;

	@Autowired
	protected PlaceTypeService placeTypes;

	@Autowired
	protected BusinessDate businessDate;

	protected long type;

	protected LocalDate today;

	@BeforeEach
	void placeType() {
		this.type = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("T", "T", null, null)).id();
		this.today = this.businessDate.today();
	}

	protected long place(Long parentId, String name) {
		return this.places
			.create(CTX_A, new PlaceCreateRequest(parentId, this.type, null, name, null, null, null, null, null, null))
			.id();
	}

	protected long person(String fullName) {
		return this.people
			.create(CTX_A, new PersonRequest(fullName, null, null, null, null, null, null, null, null, null, null))
			.id();
	}

	protected ResultActions postOccupancy(long placeId, long personId, LocalDate start, LocalDate end)
			throws Exception {
		String endJson = (end != null) ? "\"" + end + "\"" : "null";
		return this.mvc.perform(asA(post("/occupancies")).contentType(MediaType.APPLICATION_JSON).content("""
				{"placeId": "%d", "personId": "%d", "relation": "RESIDENT", "startDate": "%s", "endDate": %s}"""
			.formatted(placeId, personId, start, endJson)));
	}

	protected long occupancy(long placeId, long personId, LocalDate start, LocalDate end) throws Exception {
		String body = postOccupancy(placeId, personId, start, end).andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return Long.parseLong(JsonPath.read(body, "$.id"));
	}

}
