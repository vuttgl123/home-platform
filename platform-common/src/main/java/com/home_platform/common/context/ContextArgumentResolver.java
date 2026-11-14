package com.home_platform.common.context;

import com.home_platform.common.web.RequestLogFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Cho phép controller nhận {@link WorkspaceContext} hoặc {@link UserContext} như một tham số.
 *
 * <p>Người gọi vừa xác định được cũng được ghi vào log context (khoá
 * {@link RequestLogFilter#WORKSPACE_ID} và {@link RequestLogFilter#USER_ID}), để mọi dòng log sau đó
 * của request cho biết nó thuộc không gian nào, của ai. Ghi ở đây chứ không đọc header trong
 * filter: khi {@link ContextResolver} được thay bằng đăng nhập thì log vẫn đúng.
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
			WorkspaceContext workspace = this.resolver.resolveWorkspace(request);
			MDC.put(RequestLogFilter.WORKSPACE_ID, String.valueOf(workspace.workspaceId()));
			MDC.put(RequestLogFilter.USER_ID, String.valueOf(workspace.userId()));
			return workspace;
		}
		UserContext user = this.resolver.resolveUser(request);
		MDC.put(RequestLogFilter.USER_ID, String.valueOf(user.userId()));
		return user;
	}

}
