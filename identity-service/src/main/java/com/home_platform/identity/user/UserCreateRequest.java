package com.home_platform.identity.user;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param email lưu chữ thường; hai email chỉ khác hoa thường là một */
public record UserCreateRequest(@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(max = 150) String displayName) {

	public UserCreateRequest {
		email = (email != null) ? email.strip().toLowerCase(Locale.ROOT) : null;
		displayName = (displayName != null) ? displayName.strip() : null;
	}

}
