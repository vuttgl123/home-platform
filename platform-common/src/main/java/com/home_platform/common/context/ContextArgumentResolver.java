package com.home_platform.common.context;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Cho phép controller nhận {@link WorkspaceContext} hoặc {@link UserContext} như một tham số.
 */
public class ContextArgumentResolver implements HandlerMethodArgumentResolver {

	private final ContextResolver resolver;

	public ContextArgumentResolver(ContextResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> type = parameter.getParameterType();
		return type == WorkspaceContext.class || type == UserContext.class;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		if (parameter.getParameterType() == WorkspaceContext.class) {
			return this.resolver.resolveWorkspace(request);
		}
		return this.resolver.resolveUser(request);
	}

}
