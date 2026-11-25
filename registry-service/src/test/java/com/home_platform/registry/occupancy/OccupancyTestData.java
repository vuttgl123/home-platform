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
public abstract class OccupancyTestData extends RegistryIT {

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
			.create(CTX_A, new PlaceCreateRequest(parentId, this.type, name, null, null, null, null, null, null, null))
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

	/** Thêm một dòng cư trú với quan hệ {@code relation} (RESIDENT, GUEST, STAFF, OTHER). */
	protected void stay(long placeId, long personId, String relation, LocalDate start, LocalDate end)
			throws Exception {
		String endJson = (end != null) ? "\"" + end + "\"" : "null";
		this.mvc.perform(asA(post("/occupancies")).contentType(MediaType.APPLICATION_JSON).content("""
				{"placeId": "%d", "personId": "%d", "relation": "%s", "startDate": "%s", "endDate": %s}"""
			.formatted(placeId, personId, relation, start, endJson))).andExpect(status().isCreated());
	}

	/** Một địa điểm loại {@link #type} có sức chứa {@code capacity} ({@code null} là chưa đặt). */
	protected long place(Long parentId, String name, Integer capacity) {
		return this.places
			.create(CTX_A, new PlaceCreateRequest(parentId, this.type, name, null, null, capacity, null, null, null,
					null))
			.id();
	}

	protected long occupancy(long placeId, long personId, LocalDate start, LocalDate end) throws Exception {
		String body = postOccupancy(placeId, personId, start, end).andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return Long.parseLong(JsonPath.read(body, "$.id"));
	}

}
