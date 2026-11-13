package com.home_platform.identity.workspace;

import com.home_platform.common.context.UserContext;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workspaces")
class WorkspaceController {

	private final WorkspaceService service;

	private final WorkspaceMapper mapper;

	WorkspaceController(WorkspaceService service, WorkspaceMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	WorkspaceResponse create(UserContext ctx, @Valid @RequestBody WorkspaceRequest request) {
		return this.mapper.toResponse(this.service.create(ctx, request));
	}

	/** Các không gian của người gọi. */
	@GetMapping
	CursorPage<WorkspaceResponse> mine(UserContext ctx, CursorRequest page) {
		return this.service.mine(ctx, page).map(this.mapper::toResponse);
	}

	@GetMapping("/{id}")
	WorkspaceResponse get(UserContext ctx, @PathVariable long id) {
		return this.mapper.toResponse(this.service.get(ctx, id));
	}

	@PutMapping("/{id}")
	WorkspaceResponse rename(UserContext ctx, @PathVariable long id, @Valid @RequestBody WorkspaceRequest request) {
		return this.mapper.toResponse(this.service.rename(ctx, id, request));
	}

}
