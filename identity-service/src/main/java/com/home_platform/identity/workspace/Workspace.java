package com.home_platform.identity.workspace;

import java.time.Instant;

import lombok.With;

/** Một dòng của {@code idn_workspace}. {@code id} chính là {@code workspace_id} ở mọi bảng khác. */
@With
public record Workspace(long id, long ownerUserId, String name, WorkspaceStatus status, Instant createdAt,
		Instant updatedAt) {
}
