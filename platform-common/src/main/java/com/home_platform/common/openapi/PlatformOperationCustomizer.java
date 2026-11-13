package com.home_platform.common.openapi;

import com.home_platform.common.context.HeaderContextResolver;
import com.home_platform.common.context.UserContext;
import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.common.web.PagingProperties;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.HandlerMethod;

/**
 * Mô tả trong tài liệu API những tham số mà controller nhận qua phần nền chứ không qua chú
 * thích của Spring: người gọi ({@link WorkspaceContext}, {@link UserContext}) thành các
 * header, {@link CursorRequest} thành hai tham số {@code cursor} và {@code limit}. Mọi API
 * cũng được ghi là có thể trả lỗi dạng Problem Details.
 */
public class PlatformOperationCustomizer implements GlobalOperationCustomizer {

	static final String PROBLEM_JSON = "application/problem+json";

	private final PagingProperties paging;

	public PlatformOperationCustomizer(PagingProperties paging) {
		this.paging = paging;
	}

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
			Class<?> type = parameter.getParameterType();
			if (type == WorkspaceContext.class) {
				addHeader(operation, HeaderContextResolver.WORKSPACE_HEADER, "Id of the workspace the call works in");
				addHeader(operation, HeaderContextResolver.USER_HEADER, "Id of the calling user");
			}
			else if (type == UserContext.class) {
				addHeader(operation, HeaderContextResolver.USER_HEADER, "Id of the calling user");
			}
			else if (type == CursorRequest.class) {
				addPaging(operation);
			}
		}
		addProblemResponse(operation);
		return operation;
	}

	private void addHeader(Operation operation, String name, String description) {
		if (hasParameter(operation, name)) {
			return;
		}
		operation.addParametersItem(new Parameter().in("header")
			.name(name)
			.required(true)
			.description(description)
			.schema(new StringSchema()));
	}

	private void addPaging(Operation operation) {
		operation.addParametersItem(new Parameter().in("query")
			.name("cursor")
			.required(false)
			.description("nextCursor of the previous page; omit for the first page")
			.schema(new StringSchema()));
		Schema<Number> limit = new IntegerSchema()._default(this.paging.defaultLimit())
			.minimum(java.math.BigDecimal.ONE)
			.maximum(java.math.BigDecimal.valueOf(this.paging.maxLimit()));
		operation.addParametersItem(new Parameter().in("query")
			.name("limit")
			.required(false)
			.description("Maximum number of items in the page")
			.schema(limit));
	}

	private static void addProblemResponse(Operation operation) {
		if (operation.getResponses() == null) {
			operation.setResponses(new ApiResponses());
		}
		if (operation.getResponses().containsKey("default")) {
			return;
		}
		Schema<?> ref = new Schema<>().$ref(ProblemDetailCustomizer.REF);
		operation.getResponses()
			.addApiResponse("default", new ApiResponse().description("Error, described as Problem Details")
				.content(new Content().addMediaType(PROBLEM_JSON, new MediaType().schema(ref))));
	}

	private static boolean hasParameter(Operation operation, String name) {
		return operation.getParameters() != null
				&& operation.getParameters().stream().anyMatch(p -> name.equals(p.getName()));
	}

}
