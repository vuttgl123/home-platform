package com.home_platform.identity.user;

import java.time.Instant;

public record UserResponse(long id, String email, String displayName, UserStatus status, Instant createdAt,
		Instant updatedAt) {
}
