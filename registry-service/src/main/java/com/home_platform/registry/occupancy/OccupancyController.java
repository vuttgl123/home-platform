package com.home_platform.registry.occupancy;

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
@RequestMapping("/occupancies")
class OccupancyController {

	private final OccupancyService service;

	private final OccupancyMapper mapper;

	OccupancyController(OccupancyService service, OccupancyMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	OccupancyResponse create(WorkspaceContext ctx, @Valid @RequestBody OccupancyCreateRequest request) {
		return view(ctx, this.service.create(ctx, request).id());
	}

	@GetMapping("/{id}")
	OccupancyResponse get(WorkspaceContext ctx, @PathVariable long id) {
		return view(ctx, id);
	}

	@PutMapping("/{id}")
	OccupancyResponse update(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody OccupancyUpdateRequest request) {
		return view(ctx, this.service.update(ctx, id, request).id());
	}

	@PostMapping("/{id}/end")
	OccupancyResponse end(WorkspaceContext ctx, @PathVariable long id, @Valid @RequestBody OccupancyEndRequest request) {
		return view(ctx, this.service.end(ctx, id, request).id());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(WorkspaceContext ctx, @PathVariable long id, @RequestParam int version) {
		this.service.delete(ctx, id, version);
	}

	private OccupancyResponse view(WorkspaceContext ctx, long id) {
		return this.mapper.toResponse(this.service.view(ctx.workspaceId(), id));
	}

}
