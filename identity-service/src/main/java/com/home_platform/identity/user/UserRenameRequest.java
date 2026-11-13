package com.home_platform.identity.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRenameRequest(@NotBlank @Size(max = 150) String displayName) {

	public UserRenameRequest {
		displayName = (displayName != null) ? displayName.strip() : null;
	}

}
