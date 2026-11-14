package com.home_platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class RequestLogFilterTests {

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PingController())
		.addFilters(new RequestLogFilter())
		.build();

	@AfterEach
	void clearLogContext() {
		MDC.clear();
	}

	@Test
	void generatesAnIdWhenNoneIsSent() throws Exception {
		String id = this.mvc.perform(get("/ping")).andExpect(status().isOk()).andReturn().getResponse()
			.getHeader("X-Request-Id");

		assertThat(id).hasSize(36);
	}

	@Test
	void keepsTheCallersId() throws Exception {
		this.mvc.perform(get("/ping").header("X-Request-Id", "abc-123"))
			.andExpect(header().string("X-Request-Id", "abc-123"));
	}

	@Test
	void replacesAnIdThatIsNotPlain() throws Exception {
		String id = this.mvc.perform(get("/ping").header("X-Request-Id", "a b")).andReturn().getResponse()
			.getHeader("X-Request-Id");

		assertThat(id).isNotEqualTo("a b").hasSize(36);
	}

	@Test
	void theIdIsInTheLogContextDuringTheRequest() throws Exception {
		this.mvc.perform(get("/ping").header("X-Request-Id", "abc-123")).andExpect(content().string("abc-123"));
	}

	@Test
	void logContextIsEmptyAfterTheRequest() throws Exception {
		MDC.put("workspaceId", "12");
		MDC.put("userId", "34");

		this.mvc.perform(get("/ping"));

		assertThat(MDC.get("requestId")).isNull();
		assertThat(MDC.get("workspaceId")).isNull();
		assertThat(MDC.get("userId")).isNull();
	}

	@RestController
	static class PingController {

		@GetMapping("/ping")
		String ping() {
			return String.valueOf(MDC.get("requestId"));
		}

	}

}
