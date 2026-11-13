package com.home_platform.common.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.emptyString;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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

	@Test
	void badRequestIsReportedAsProblemDetail() throws Exception {
		this.mvc.perform(get("/bad"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.detail").value("Invalid cursor"));
	}

	@Test
	void notFoundFactoryBuildsMessage() {
		assertThat(NotFoundException.of("Place", 42).getBody().getDetail()).isEqualTo("Place 42 not found");
	}

	@Test
	void invalidBodyListsEachField() throws Exception {
		this.mvc.perform(post("/people").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"age\":-1}"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors.length()").value(2))
			.andExpect(jsonPath("$.errors[?(@.field=='name')].message", hasSize(1)))
			.andExpect(jsonPath("$.errors[?(@.field=='age')].message", hasSize(1)));
	}

	@Test
	void invalidParameterListsTheParameter() throws Exception {
		this.mvc.perform(get("/search").param("size", "0"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors[0].field").value("size"))
			.andExpect(jsonPath("$.errors[0].message").value(not(emptyString())));
	}

	@Test
	void duplicateKeyIsReportedAsConflictWithoutSqlText() throws Exception {
		this.mvc.perform(get("/duplicate"))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Duplicate value"))
			.andExpect(content().string(not(containsString("uk_reg"))));
	}

	record Person(@NotBlank String name, @Min(0) int age) {
	}

	@RestController
	static class FailingController {

		@GetMapping("/bad")
		void bad() {
			throw new BadRequestException("Invalid cursor");
		}

		@PostMapping("/people")
		Person people(@Valid @RequestBody Person person) {
			return person;
		}

		@GetMapping("/search")
		int search(@RequestParam @Min(1) int size) {
			return size;
		}

		@GetMapping("/duplicate")
		void duplicate() {
			throw new DuplicateKeyException("Duplicate entry 'x' for key 'uk_reg_place_type_code'");
		}

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
