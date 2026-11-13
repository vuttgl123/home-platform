package com.home_platform.identity.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dùng cho cả tạo và đổi tên. */
public record WorkspaceRequest(@NotBlank @Size(max = 150) String name) {

	public WorkspaceRequest {
		name = (name != null) ? name.strip() : null;
	}

}
