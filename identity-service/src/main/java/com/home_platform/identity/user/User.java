package com.home_platform.identity.user;

import java.time.Instant;

import lombok.With;

/** Một dòng của {@code idn_user}. Các cột phục vụ đăng nhập sẽ thêm khi tích hợp security. */
@With
public record User(long id, String email, String displayName, UserStatus status, Instant createdAt,
		Instant updatedAt) {
}
