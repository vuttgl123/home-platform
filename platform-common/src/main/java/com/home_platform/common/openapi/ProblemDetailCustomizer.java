package com.home_platform.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;

/**
 * Thêm vào tài liệu API schema {@code ProblemDetail} (RFC 9457), dạng lỗi mà mọi API trả về.
 */
public class ProblemDetailCustomizer implements GlobalOpenApiCustomizer {

	static final String NAME = "ProblemDetail";

	static final String REF = "#/components/schemas/" + NAME;

	@Override
	public void customise(OpenAPI openApi) {
		if (openApi.getComponents() == null) {
			openApi.setComponents(new Components());
		}
		openApi.getComponents()
			.addSchemas(NAME, new ObjectSchema().description("Error response (RFC 9457 Problem Details)")
				.addProperty("type", new StringSchema().format("uri"))
				.addProperty("title", new StringSchema())
				.addProperty("status", new IntegerSchema())
				.addProperty("detail", new StringSchema())
				.addProperty("instance", new StringSchema().format("uri")));
	}

}
