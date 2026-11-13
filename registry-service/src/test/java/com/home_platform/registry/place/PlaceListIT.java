package com.home_platform.registry.place;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class PlaceListIT extends PlaceTestData {

	@Test
	void topLevelAndChildrenAreSortedByNameAndPaged() throws Exception {
		long type = type("T");
		place(null, type, "B");
		long a = place(null, type, "A");
		place(null, type, "C");
		place(a, type, "P2");
		place(a, type, "P1");

		String first = list(get("/places").param("limit", "2")).andExpect(jsonPath("$.items[*].name", contains("A", "B")))
			.andReturn()
			.getResponse()
			.getContentAsString();
		list(get("/places").param("limit", "2").param("cursor", (String) JsonPath.read(first, "$.nextCursor")))
			.andExpect(jsonPath("$.items[*].name", contains("C")))
			.andExpect(jsonPath("$.nextCursor").value(nullValue()));
		list(get("/places/" + a + "/children")).andExpect(jsonPath("$.items[*].name", contains("P1", "P2")))
			.andExpect(jsonPath("$.items[0].id").isString());
	}

	@Test
	void subtreeListsDescendantsInPathOrderWithoutTheRoot() throws Exception {
		long type = type("T");
		long a = place(null, type, "A");
		long x = place(a, type, "X");
		place(x, type, "Y");
		place(a, type, "Z");
		place(null, type, "Outside");

		list(get("/places/" + a + "/subtree")).andExpect(jsonPath("$.items[*].name", contains("X", "Y", "Z")));
	}

	@Test
	void deletedPlacesAreHidden() throws Exception {
		long type = type("T");
		long a = place(null, type, "A");
		long gone = place(a, type, "Gone");
		place(a, type, "Kept");
		this.mvc.perform(asA(delete("/places/" + gone)).param("version", "1")).andExpect(status().isNoContent());

		list(get("/places/" + a + "/children")).andExpect(jsonPath("$.items[*].name", contains("Kept")));
		list(get("/places/" + a + "/subtree")).andExpect(jsonPath("$.items[*].name", contains("Kept")));
		list(get("/places/search").param("name", "Gone")).andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void byTypeFiltersOnType() throws Exception {
		long room = type("ROOM");
		long floor = type("FLOOR");
		place(null, room, "R2");
		place(null, floor, "F1");
		place(null, room, "R1");

		list(get("/places/by-type/" + room)).andExpect(jsonPath("$.items[*].name", contains("R1", "R2")));
	}

	@Test
	void searchByNamePrefixIgnoresCaseAndAccents() throws Exception {
		long type = type("T");
		place(null, type, "Phòng 101");
		place(null, type, "phong 102");
		place(null, type, "Kho");

		list(get("/places/search").param("name", " PHONG ")).andExpect(jsonPath("$.items[*].name",
				containsInAnyOrder("Phòng 101", "phong 102")));
	}

	@Test
	void searchTreatsWildcardsLiterally() throws Exception {
		long type = type("T");
		place(null, type, "50% off");
		place(null, type, "500 m");
		place(null, type, "5_x");
		place(null, type, "5ax");

		list(get("/places/search").param("name", "50%")).andExpect(jsonPath("$.items[*].name", contains("50% off")));
		list(get("/places/search").param("name", "5_")).andExpect(jsonPath("$.items[*].name", contains("5_x")));
	}

	@Test
	void searchByCodeMatchesExactly() throws Exception {
		long type = type("T");
		place("""
				{"placeTypeId": "%d", "name": "a", "code": "P101"}""".formatted(type));
		place("""
				{"placeTypeId": "%d", "name": "b", "code": "P1010"}""".formatted(type));

		list(get("/places/search").param("code", "p101")).andExpect(jsonPath("$.items[*].code", contains("P101")));
	}

	@Test
	void searchNeedsExactlyOneCriterion() throws Exception {
		this.mvc.perform(asA(get("/places/search")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Give exactly one of name or code"));
		this.mvc.perform(asA(get("/places/search").param("name", "a").param("code", "b")))
			.andExpect(status().isBadRequest());
		this.mvc.perform(asA(get("/places/search").param("name", "  "))).andExpect(status().isBadRequest());
	}

	@Test
	void byAreaUsesTheAddressTable() throws Exception {
		long type = type("T");
		long a = place(null, type, "A");
		long b = place(null, type, "B");
		long c = place(null, type, "C");
		address(a, "01", "001");
		address(b, "01", "002");
		address(c, "79", "100");

		list(get("/places/by-area").param("province", "01"))
			.andExpect(jsonPath("$.items[*].name", containsInAnyOrder("A", "B")));
		list(get("/places/by-area").param("province", "01").param("ward", "002"))
			.andExpect(jsonPath("$.items[*].name", contains("B")));
		String first = list(get("/places/by-area").param("province", "01").param("limit", "1")).andReturn()
			.getResponse()
			.getContentAsString();
		list(get("/places/by-area").param("province", "01")
			.param("limit", "1")
			.param("cursor", (String) JsonPath.read(first, "$.nextCursor"))).andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.nextCursor").value(nullValue()));
		this.mvc.perform(asA(get("/places/by-area"))).andExpect(status().isBadRequest());
	}

	@Test
	void otherWorkspaceIsInvisible() throws Exception {
		long type = type("T");
		long a = place(null, type, "A");
		place(a, type, "B");

		this.mvc.perform(asB(get("/places"))).andExpect(jsonPath("$.items", hasSize(0)));
		this.mvc.perform(asB(get("/places/search").param("name", "A"))).andExpect(jsonPath("$.items", hasSize(0)));
		this.mvc.perform(asB(get("/places/" + a + "/children"))).andExpect(status().isNotFound());
	}

	private ResultActions list(MockHttpServletRequestBuilder request) throws Exception {
		return this.mvc.perform(asA(request)).andExpect(status().isOk());
	}

	private void address(long placeId, String province, String ward) throws Exception {
		this.mvc.perform(asA(put("/places/" + placeId + "/address")).contentType(MediaType.APPLICATION_JSON).content("""
				{"version": 1, "address": {"fullAddress": "x", "provinceCode": "%s", "wardCode": "%s"}}"""
			.formatted(province, ward))).andExpect(status().isOk());
	}

}
