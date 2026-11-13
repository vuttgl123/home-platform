package com.home_platform.registry.setup;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.home_platform.registry.RegistryIT;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SetupIT extends RegistryIT {

	@Test
	void createsTheDefaults() throws Exception {
		setup(true);

		String types = this.mvc.perform(asA(get("/place-types")))
			.andExpect(jsonPath("$[*].code",
					contains("BUILDING", "FLOOR", "ROOM", "APARTMENT", "HOUSE", "HOTEL", "OTHER")))
			.andExpect(jsonPath("$[0].name").value("Toà nhà"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		this.mvc.perform(asA(get("/item-categories")))
			.andExpect(jsonPath("$[*].name",
					contains("Nội thất", "Điện tử", "Điện lạnh", "Thiết bị vệ sinh", "Đồ gia dụng", "Khác")));

		String hotel = id(types, "HOTEL");
		String apartment = id(types, "APARTMENT");
		this.mvc.perform(asA(get("/field-defs").param("target", "PLACE")))
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[?(@.fieldKey == 'star_rating')].typeId", contains(hotel)))
			.andExpect(jsonPath("$[?(@.fieldKey == 'star_rating')].label", contains("Số sao")))
			.andExpect(jsonPath("$[?(@.fieldKey == 'star_rating')].dataType", contains("NUMBER")))
			.andExpect(jsonPath("$[?(@.fieldKey == 'area_m2')].typeId", contains(apartment)))
			.andExpect(jsonPath("$[?(@.fieldKey == 'area_m2')].label", contains("Diện tích (m²)")))
			.andExpect(jsonPath("$[*].required", contains(false, false)));
	}

	@Test
	void secondCallDoesNothing() throws Exception {
		setup(true);
		setup(false);

		this.mvc.perform(asA(get("/place-types"))).andExpect(jsonPath("$", hasSize(7)));
	}

	@Test
	void doesNotRecreateWhatTheUserRemoved() throws Exception {
		setup(true);
		String types = this.mvc.perform(asA(get("/place-types"))).andReturn().getResponse().getContentAsString();
		this.mvc.perform(asA(delete("/place-types/" + id(types, "OTHER")))).andExpect(status().isNoContent());

		setup(false);

		this.mvc.perform(asA(get("/place-types")))
			.andExpect(jsonPath("$", hasSize(6)))
			.andExpect(jsonPath("$[*].code", not(hasItem("OTHER"))));
	}

	@Test
	void workspaceWithItsOwnTypesIsLeftAlone() throws Exception {
		this.mvc.perform(asA(post("/place-types")).contentType(MediaType.APPLICATION_JSON).content("""
				{"code": "VILLA", "name": "Biệt thự"}""")).andExpect(status().isCreated());

		setup(false);

		this.mvc.perform(asA(get("/place-types"))).andExpect(jsonPath("$", hasSize(1)));
		this.mvc.perform(asA(get("/item-categories"))).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void otherWorkspaceIsUnaffected() throws Exception {
		setup(true);

		this.mvc.perform(asB(get("/place-types"))).andExpect(jsonPath("$", hasSize(0)));
	}

	private void setup(boolean expectedCreated) throws Exception {
		this.mvc.perform(asA(post("/setup")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.created").value(expectedCreated));
	}

	private static String id(String placeTypesJson, String code) {
		List<String> ids = JsonPath.read(placeTypesJson, "$[?(@.code == '" + code + "')].id");
		return ids.get(0);
	}

}
