package com.home_platform.common.context;

import com.home_platform.common.web.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Lấy người gọi từ hai header {@value #USER_HEADER} và {@value #WORKSPACE_HEADER}.
 */
public class HeaderContextResolver implements ContextResolver {

	public static final String USER_HEADER = "X-User-Id";

	public static final String WORKSPACE_HEADER = "X-Workspace-Id";

	@Override
	public UserContext resolveUser(HttpServletRequest request) {
		return new UserContext(header(request, USER_HEADER));
	}

	@Override
	public WorkspaceContext resolveWorkspace(HttpServletRequest request) {
		return new WorkspaceContext(header(request, WORKSPACE_HEADER), header(request, USER_HEADER));
	}

	private static long header(HttpServletRequest request, String name) {
		String value = request.getHeader(name);
		if (value == null || value.isBlank()) {
			throw new BadRequestException("Missing header " + name);
		}
		try {
			return Long.parseLong(value.trim());
		}
		catch (NumberFormatException ex) {
			throw new BadRequestException("Header " + name + " must be a number");
		}
	}

}
