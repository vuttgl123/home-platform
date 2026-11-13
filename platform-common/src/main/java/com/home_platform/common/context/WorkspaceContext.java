package com.home_platform.common.context;

/**
 * Người đang gọi API và không gian họ đang làm việc. Controller khai báo một tham số kiểu
 * này rồi truyền nó xuống service và repository như một tham số bình thường; không có
 * {@code ThreadLocal} nào giữ hộ, nên quên truyền thì không biên dịch được.
 *
 * @param workspaceId không gian đang làm việc
 * @param userId {@code id} của người dùng bên identity
 */
public record WorkspaceContext(long workspaceId, long userId) {
}
