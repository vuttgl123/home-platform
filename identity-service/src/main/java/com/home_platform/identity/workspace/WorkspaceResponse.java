package com.home_platform.identity.workspace;

import java.time.Instant;

/** @param role vai trò của người gọi trong không gian này */
public record WorkspaceResponse(long id, String name, WorkspaceStatus status, long ownerUserId, MemberRole role,
		Instant createdAt, Instant updatedAt) {
}
