package com.home_platform.common.context;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Cách lấy người gọi từ một request. Bản mặc định đọc header; khi có đăng nhập, service
 * khai báo một bean khác của interface này và controller không phải đổi gì.
 */
public interface ContextResolver {

	UserContext resolveUser(HttpServletRequest request);

	WorkspaceContext resolveWorkspace(HttpServletRequest request);

}
