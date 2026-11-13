package com.home_platform.registry.place;

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
@RequestMapping("/places")
class PlaceController {

	private final PlaceService service;

	private final PlaceMapper mapper;

	PlaceController(PlaceService service, PlaceMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	PlaceDetailResponse create(WorkspaceContext ctx, @Valid @RequestBody PlaceCreateRequest request) {
		return detail(ctx, this.service.create(ctx, request).id());
	}

	@GetMapping("/{id}")
	PlaceDetailResponse get(WorkspaceContext ctx, @PathVariable long id) {
		return detail(ctx, id);
	}

	@PutMapping("/{id}")
	PlaceDetailResponse update(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody PlaceUpdateRequest request) {
		return detail(ctx, this.service.update(ctx, id, request).id());
	}

	@PostMapping("/{id}/move")
	PlaceDetailResponse move(WorkspaceContext ctx, @PathVariable long id, @Valid @RequestBody PlaceMoveRequest request) {
		return detail(ctx, this.service.move(ctx, id, request).id());
	}

	@PutMapping("/{id}/address")
	PlaceDetailResponse setAddress(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody PlaceAddressRequest request) {
		return detail(ctx, this.service.setAddress(ctx, id, request).id());
	}

	@DeleteMapping("/{id}/address")
	PlaceDetailResponse removeAddress(WorkspaceContext ctx, @PathVariable long id, @RequestParam int version,
			@RequestParam(required = false) String note) {
		return detail(ctx, this.service.removeAddress(ctx, id, version, note).id());
	}

	private PlaceDetailResponse detail(WorkspaceContext ctx, long id) {
		return this.mapper.toDetail(this.service.view(ctx.workspaceId(), id));
	}

}
