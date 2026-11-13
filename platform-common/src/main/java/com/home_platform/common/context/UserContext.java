package com.home_platform.common.context;

/**
 * Người đang gọi API, dùng cho các API không gắn với một không gian nào.
 *
 * @param userId {@code id} của người dùng bên identity
 */
public record UserContext(long userId) {
}
