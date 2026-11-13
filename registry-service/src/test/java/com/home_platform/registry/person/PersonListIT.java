package com.home_platform.registry.person;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

class PersonListIT extends PersonTestData {

	@Test
	void listsByNameAndPages() throws Exception {
		named("Cường");
		named("An");
		named("Bình");
		long gone = named("Ẩn");
		this.mvc.perform(asA(delete("/people/" + gone)).param("version", "1")).andExpect(status().isNoContent());

		String first = this.mvc.perform(asA(get("/people")).param("limit", "2"))
			.andExpect(jsonPath("$.items[*].fullName", contains("An", "Bình")))
			.andExpect(jsonPath("$.items[0].id").isString())
			.andReturn()
			.getResponse()
			.getContentAsString();
		this.mvc.perform(asA(get("/people")).param("limit", "2").param("cursor", (String) JsonPath.read(first, "$.nextCursor")))
			.andExpect(jsonPath("$.items[*].fullName", contains("Cường")))
			.andExpect(jsonPath("$.nextCursor").value(nullValue()));
	}

	@Test
	void searchByNamePrefixIgnoresCaseAndAccents() throws Exception {
		named("Nguyễn Văn A");
		named("nguyen thi B");
		named("Trần C");

		this.mvc.perform(asA(get("/people/search").param("name", "NGUYEN")))
			.andExpect(jsonPath("$.items[*].fullName", containsInAnyOrder("Nguyễn Văn A", "nguyen thi B")));
	}

	@Test
	void phoneSearchIgnoresSpacesAndDots() throws Exception {
		person("""
				{"fullName": "A", "phone": "0901234567"}""");
		person("""
				{"fullName": "B", "phone": "0911111111"}""");

		this.mvc.perform(asA(get("/people/search").param("phone", "090 123")))
			.andExpect(jsonPath("$.items[*].fullName", contains("A")));
		this.mvc.perform(asA(get("/people/search").param("phone", "090.1"))).andExpect(jsonPath("$.items", hasSize(1)));
	}

	@Test
	void idNumberSearchMatchesExactly() throws Exception {
		person("""
				{"fullName": "A", "idType": "CCCD", "idNumber": "001"}""");
		person("""
				{"fullName": "B", "idType": "CCCD", "idNumber": "0012"}""");

		this.mvc.perform(asA(get("/people/search").param("idNumber", "001")))
			.andExpect(jsonPath("$.items[*].fullName", contains("A")));
	}

	@Test
	void searchNeedsExactlyOneCriterion() throws Exception {
		this.mvc.perform(asA(get("/people/search")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Give exactly one of name, phone or idNumber"));
		this.mvc.perform(asA(get("/people/search").param("name", "a").param("phone", "1")))
			.andExpect(status().isBadRequest());
	}

	@Test
	void otherWorkspaceSeesNothing() throws Exception {
		named("A");

		this.mvc.perform(asB(get("/people"))).andExpect(jsonPath("$.items", hasSize(0)));
	}

}
