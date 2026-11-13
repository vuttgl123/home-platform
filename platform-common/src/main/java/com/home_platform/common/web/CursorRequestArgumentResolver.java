package com.home_platform.common.web;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Cho phép controller nhận {@link CursorRequest} như một tham số, lấy từ {@code cursor} và
 * {@code limit} trên URL.
 */
public class CursorRequestArgumentResolver implements HandlerMethodArgumentResolver {

	public static final String CURSOR_PARAMETER = "cursor";

	public static final String LIMIT_PARAMETER = "limit";

	private final PagingProperties properties;

	public CursorRequestArgumentResolver(PagingProperties properties) {
		this.properties = properties;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getParameterType() == CursorRequest.class;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		String cursor = webRequest.getParameter(CURSOR_PARAMETER);
		if (cursor != null && cursor.isBlank()) {
			cursor = null;
		}
		return new CursorRequest(cursor, limit(webRequest.getParameter(LIMIT_PARAMETER)));
	}

	private int limit(String value) {
		if (value == null || value.isBlank()) {
			return this.properties.defaultLimit();
		}
		try {
			int limit = Integer.parseInt(value.trim());
			if (limit >= 1 && limit <= this.properties.maxLimit()) {
				return limit;
			}
		}
		catch (NumberFormatException ex) {
			// Rơi xuống thông báo chung bên dưới.
		}
		throw new BadRequestException("limit must be between 1 and " + this.properties.maxLimit());
	}

}
