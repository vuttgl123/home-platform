package com.home_platform.registry.place;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.history.EntityVersion;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
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

	@GetMapping
	CursorPage<PlaceResponse> topLevel(WorkspaceContext ctx, CursorRequest page) {
		return this.service.topLevel(ctx.workspaceId(), page).map(this.mapper::toResponse);
	}

	@GetMapping("/{id}/children")
	CursorPage<PlaceResponse> children(WorkspaceContext ctx, @PathVariable long id, CursorRequest page) {
		return this.service.children(ctx.workspaceId(), id, page).map(this.mapper::toResponse);
	}

	@GetMapping("/{id}/subtree")
	CursorPage<PlaceResponse> subtree(WorkspaceContext ctx, @PathVariable long id, CursorRequest page) {
		return this.service.subtree(ctx.workspaceId(), id, page).map(this.mapper::toResponse);
	}

	@GetMapping("/by-type/{typeId}")
	CursorPage<PlaceResponse> byType(WorkspaceContext ctx, @PathVariable long typeId, CursorRequest page) {
		return this.service.byType(ctx.workspaceId(), typeId, page).map(this.mapper::toResponse);
	}

	@GetMapping("/by-area")
	CursorPage<PlaceResponse> byArea(WorkspaceContext ctx, @RequestParam String province,
			@RequestParam(required = false) String ward, CursorRequest page) {
		return this.service.byArea(ctx.workspaceId(), province, ward, page).map(this.mapper::toResponse);
	}

	/** Tìm theo tên bắt đầu bằng ({@code name}) hoặc đúng mã ({@code code}); đúng một trong hai. */
	@GetMapping("/search")
	CursorPage<PlaceResponse> search(WorkspaceContext ctx, @RequestParam(required = false) String name,
			@RequestParam(required = false) String code, CursorRequest page) {
		if ((name == null) == (code == null)) {
			throw new BadRequestException("Give exactly one of name or code");
		}
		CursorPage<Place> found = (name != null) ? this.service.searchByName(ctx.workspaceId(), name, page)
				: this.service.findByCode(ctx.workspaceId(), code, page);
		return found.map(this.mapper::toResponse);
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

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(WorkspaceContext ctx, @PathVariable long id, @RequestParam int version,
			@RequestParam(required = false) String note) {
		this.service.delete(ctx, id, version, note);
	}

	@PostMapping("/{id}/restore")
	PlaceDetailResponse restore(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody PlaceRestoreRequest request) {
		return detail(ctx, this.service.restore(ctx, id, request).id());
	}

	@GetMapping("/{id}/versions")
	CursorPage<PlaceVersionSummaryResponse> versions(WorkspaceContext ctx, @PathVariable long id, CursorRequest page) {
		return this.service.versions(ctx.workspaceId(), id, page).map(this.mapper::toVersionSummary);
	}

	@GetMapping("/{id}/versions/{versionNo}")
	PlaceVersionResponse version(WorkspaceContext ctx, @PathVariable long id, @PathVariable int versionNo) {
		EntityVersion version = this.service.version(ctx.workspaceId(), id, versionNo);
		return this.mapper.toVersion(version, this.service.snapshotOf(version));
	}

	@PostMapping("/{id}/versions/{versionNo}/restore")
	PlaceDetailResponse restoreVersion(WorkspaceContext ctx, @PathVariable long id, @PathVariable int versionNo,
			@Valid @RequestBody PlaceVersionRestoreRequest request) {
		return detail(ctx, this.service.restoreVersion(ctx, id, versionNo, request).id());
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
