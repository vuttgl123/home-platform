package com.home_platform.common.context;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.common.web.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ContextArgumentResolverTests {

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ContextController())
		.setCustomArgumentResolvers(new ContextArgumentResolver(new HeaderContextResolver()))
		.setControllerAdvice(new ApiExceptionHandler())
		.build();

	@Test
	void workspaceContextComesFromHeaders() throws Exception {
		this.mvc.perform(get("/ws").header("X-Workspace-Id", "12").header("X-User-Id", "34"))
			.andExpect(status().isOk())
			.andExpect(content().string("12:34"));
	}

	@Test
	void userContextNeedsOnlyUserHeader() throws Exception {
		this.mvc.perform(get("/me").header("X-User-Id", "34"))
			.andExpect(status().isOk())
			.andExpect(content().string("34"));
	}

	@Test
	void negativeIdsAreAccepted() throws Exception {
		this.mvc.perform(get("/ws").header("X-Workspace-Id", "-1").header("X-User-Id", "34"))
			.andExpect(status().isOk())
			.andExpect(content().string("-1:34"));
	}

	@Test
	void missingHeaderIs400NamingTheHeader() throws Exception {
		this.mvc.perform(get("/ws").header("X-User-Id", "34"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Missing header X-Workspace-Id"));
	}

	@Test
	void nonNumericHeaderIs400() throws Exception {
		this.mvc.perform(get("/ws").header("X-Workspace-Id", "12").header("X-User-Id", "abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Header X-User-Id must be a number"));
	}

	@RestController
	static class ContextController {

		@GetMapping("/ws")
		String workspace(WorkspaceContext ctx) {
			return ctx.workspaceId() + ":" + ctx.userId();
		}

		@GetMapping("/me")
		String me(UserContext user) {
			return String.valueOf(user.userId());
		}

	}

}
