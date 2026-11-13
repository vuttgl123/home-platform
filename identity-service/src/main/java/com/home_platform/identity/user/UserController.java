package com.home_platform.identity.user;

import com.home_platform.common.context.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
class UserController {

	private final UserService service;

	private final UserMapper mapper;

	UserController(UserService service, UserMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	/** Đăng ký người dùng mới; chưa cần đăng nhập. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	UserResponse create(@Valid @RequestBody UserCreateRequest request) {
		return this.mapper.toResponse(this.service.create(request));
	}

	@GetMapping("/me")
	UserResponse me(UserContext ctx) {
		return this.mapper.toResponse(this.service.get(ctx.userId()));
	}

	@PutMapping("/me")
	UserResponse rename(UserContext ctx, @Valid @RequestBody UserRenameRequest request) {
		return this.mapper.toResponse(this.service.rename(ctx, request));
	}

}
