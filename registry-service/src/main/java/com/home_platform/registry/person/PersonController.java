package com.home_platform.registry.person;

import com.home_platform.common.context.WorkspaceContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/people")
class PersonController {

	private final PersonService service;

	private final PersonMapper mapper;

	PersonController(PersonService service, PersonMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	PersonDetailResponse create(WorkspaceContext ctx, @Valid @RequestBody PersonRequest request) {
		return this.mapper.toDetail(this.service.create(ctx, request));
	}

	@GetMapping("/{id}")
	PersonDetailResponse get(WorkspaceContext ctx, @PathVariable long id) {
		return this.mapper.toDetail(this.service.get(ctx.workspaceId(), id));
	}

	@PutMapping("/{id}")
	PersonDetailResponse update(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody PersonUpdateRequest request) {
		return this.mapper.toDetail(this.service.update(ctx, id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(WorkspaceContext ctx, @PathVariable long id, @RequestParam int version,
			@RequestParam(required = false) String note) {
		this.service.delete(ctx, id, version, note);
	}

	@PostMapping("/{id}/restore")
	PersonDetailResponse restore(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody PersonRestoreRequest request) {
		return this.mapper.toDetail(this.service.restore(ctx, id, request));
	}

}
