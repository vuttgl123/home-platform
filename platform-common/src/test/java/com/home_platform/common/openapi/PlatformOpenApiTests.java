package com.home_platform.common.openapi;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.common.context.UserContext;
import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = PlatformOpenApiTests.App.class, properties = "spring.application.name=sample-service")
@AutoConfigureMockMvc
class PlatformOpenApiTests {

	private static final String THING = "$.paths['/things/{id}'].get";

	private static final String THINGS = "$.paths['/things'].get";

	@Autowired
	MockMvc mvc;

	@Test
	void documentDescribesContextPagingAndIds() throws Exception {
		this.mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.info.title").value("sample-service"))
			.andExpect(jsonPath(THING + ".parameters[*].name")
				.value(containsInAnyOrder("X-Workspace-Id", "X-User-Id", "id")))
			.andExpect(jsonPath(THING + ".parameters[?(@.name == 'X-Workspace-Id')].in").value(contains("header")))
			.andExpect(jsonPath(THING + ".parameters[?(@.name == 'X-Workspace-Id')].required").value(contains(true)))
			.andExpect(jsonPath(THING + ".parameters[?(@.name == 'X-User-Id')].in").value(contains("header")))
			.andExpect(jsonPath(THING + ".parameters[?(@.name == 'X-User-Id')].required").value(contains(true)))
			.andExpect(jsonPath(THING + ".parameters[?(@.name == 'id')].schema.type").value(contains("string")))
			.andExpect(jsonPath(THINGS + ".parameters[*].name")
				.value(containsInAnyOrder("X-Workspace-Id", "X-User-Id", "cursor", "limit")))
			.andExpect(jsonPath(THINGS + ".parameters[?(@.name == 'limit')].schema.type").value(contains("integer")))
			.andExpect(jsonPath(THINGS + ".parameters[?(@.name == 'limit')].schema.default").value(contains(20)))
			.andExpect(jsonPath(THINGS + ".parameters[?(@.name == 'limit')].schema.maximum").value(contains(100)))
			.andExpect(jsonPath(THINGS + ".parameters[?(@.name == 'limit')].required").value(contains(false)))
			.andExpect(jsonPath(THINGS + ".parameters[?(@.name == 'cursor')].required").value(contains(false)))
			.andExpect(jsonPath("$.paths['/me'].get.parameters[*].name").value(contains("X-User-Id")))
			.andExpect(jsonPath("$.components.schemas.ThingResponse.properties.id.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.ThingResponse.properties.count.type").value("integer"))
			.andExpect(jsonPath(THING + ".responses.default.content['application/problem+json'].schema.$ref")
				.value("#/components/schemas/ProblemDetail"))
			.andExpect(jsonPath("$.components.schemas").value(hasKey("ProblemDetail")));
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
	@Import(ThingController.class)
	static class App {

	}

	record ThingResponse(long id, String name, int count) {
	}

	@RestController
	static class ThingController {

		@GetMapping("/things/{id}")
		ThingResponse get(WorkspaceContext ctx, @PathVariable long id) {
			return new ThingResponse(id, "thing", 1);
		}

		@GetMapping("/things")
		CursorPage<ThingResponse> list(WorkspaceContext ctx, CursorRequest page) {
			return new CursorPage<>(java.util.List.of(), null);
		}

		@GetMapping("/me")
		String me(UserContext user) {
			return "me";
		}

	}

}
