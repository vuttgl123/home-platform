package com.home_platform.common.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ApiExceptionHandlerTests {

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailingController())
		.setControllerAdvice(new ApiExceptionHandler())
		.build();

	@Test
	void notFoundIsReportedAsProblemDetail() throws Exception {
		this.mvc.perform(get("/missing"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.detail").value("Place 42 not found"));
	}

	@Test
	void conflictIsReportedAsProblemDetail() throws Exception {
		this.mvc.perform(get("/conflict"))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").value("Place 42 was changed by someone else"));
	}

	@Test
	void unexpectedErrorIsReportedAs500WithoutItsMessage() throws Exception {
		this.mvc.perform(get("/boom"))
			.andExpect(status().isInternalServerError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.detail").value("Unexpected error"))
			.andExpect(content().string(not(containsString("internal detail"))));
	}

	@Test
	void malformedRequestBodyIsReportedAsProblemDetail() throws Exception {
		this.mvc.perform(post("/echo").contentType(MediaType.APPLICATION_JSON).content("{\"a\":"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400));
	}

	@RestController
	static class FailingController {

		@GetMapping("/missing")
		void missing() {
			throw new NotFoundException("Place 42 not found");
		}

		@GetMapping("/conflict")
		void conflict() {
			throw new ConflictException("Place 42 was changed by someone else");
		}

		@GetMapping("/boom")
		void boom() {
			throw new IllegalStateException("internal detail");
		}

		@PostMapping("/echo")
		Map<String, Object> echo(@RequestBody Map<String, Object> body) {
			return body;
		}

	}

}
