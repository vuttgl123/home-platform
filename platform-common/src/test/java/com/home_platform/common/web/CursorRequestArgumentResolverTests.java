package com.home_platform.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class CursorRequestArgumentResolverTests {

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ListController())
		.setCustomArgumentResolvers(new CursorRequestArgumentResolver(new PagingProperties(20, 100)))
		.setControllerAdvice(new ApiExceptionHandler())
		.build();

	@Test
	void defaultsApplyWhenNothingIsSent() throws Exception {
		this.mvc.perform(get("/list")).andExpect(status().isOk()).andExpect(content().string("null:20"));
	}

	@Test
	void cursorAndLimitAreRead() throws Exception {
		this.mvc.perform(get("/list").param("cursor", "abc").param("limit", "50"))
			.andExpect(status().isOk())
			.andExpect(content().string("abc:50"));
	}

	@Test
	void blankCursorMeansFirstPage() throws Exception {
		this.mvc.perform(get("/list").param("cursor", "")).andExpect(content().string("null:20"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "101", "abc", "-5" })
	void limitOutOfRangeOrNotANumberIs400(String limit) throws Exception {
		this.mvc.perform(get("/list").param("limit", limit))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("limit must be between 1 and 100"));
	}

	@RestController
	static class ListController {

		@GetMapping("/list")
		String list(CursorRequest page) {
			return page.cursor() + ":" + page.limit();
		}

	}

}
