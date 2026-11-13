package com.home_platform.registry.fielddef;

import java.util.List;

import com.home_platform.common.context.WorkspaceContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/field-defs")
class FieldDefController {

	private final FieldDefService service;

	private final FieldDefMapper mapper;

	FieldDefController(FieldDefService service, FieldDefMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	List<FieldDefResponse> list(WorkspaceContext ctx, @RequestParam FieldTarget target,
			@RequestParam(required = false) Long typeId) {
		return this.mapper.toResponses(this.service.list(ctx.workspaceId(), target, typeId));
	}

	@GetMapping("/{id}")
	FieldDefResponse get(WorkspaceContext ctx, @PathVariable long id) {
		return this.mapper.toResponse(this.service.get(ctx.workspaceId(), id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	FieldDefResponse create(WorkspaceContext ctx, @Valid @RequestBody FieldDefCreateRequest request) {
		return this.mapper.toResponse(this.service.create(ctx, request));
	}

	@PutMapping("/{id}")
	FieldDefResponse update(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody FieldDefUpdateRequest request) {
		return this.mapper.toResponse(this.service.update(ctx, id, request));
	}

	@PostMapping("/{id}/archive")
	FieldDefResponse archive(WorkspaceContext ctx, @PathVariable long id) {
		return this.mapper.toResponse(this.service.archive(ctx, id));
	}

	@PostMapping("/{id}/unarchive")
	FieldDefResponse unarchive(WorkspaceContext ctx, @PathVariable long id) {
		return this.mapper.toResponse(this.service.unarchive(ctx, id));
	}

}
