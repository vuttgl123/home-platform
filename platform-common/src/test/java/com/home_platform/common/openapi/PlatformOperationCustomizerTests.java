package com.home_platform.common.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.common.web.PagingProperties;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

class PlatformOperationCustomizerTests {

	@Test
	void customizingTwiceAddsEachParameterOnce() throws Exception {
		PlatformOperationCustomizer customizer = new PlatformOperationCustomizer(new PagingProperties(20, 100));
		HandlerMethod method = new HandlerMethod(new Lists(),
				Lists.class.getDeclaredMethod("list", WorkspaceContext.class, CursorRequest.class));
		Operation operation = new Operation();

		customizer.customize(operation, method);
		customizer.customize(operation, method);

		assertThat(operation.getParameters()).extracting(Parameter::getName)
			.containsExactlyInAnyOrder("X-Workspace-Id", "X-User-Id", "cursor", "limit");
	}

	static class Lists {

		String list(WorkspaceContext ctx, CursorRequest page) {
			return "";
		}

	}

}
