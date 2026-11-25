package com.home_platform.registry.maintenance;

import com.home_platform.common.context.WorkspaceContext;
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
@RequestMapping("/maintenance")
class MaintenanceController {

	private final MaintenanceService service;

	private final MaintenanceMapper mapper;

	MaintenanceController(MaintenanceService service, MaintenanceMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	/** Nhật ký của một món, mới làm trước. */
	@GetMapping
	CursorPage<MaintenanceResponse> list(WorkspaceContext ctx, @RequestParam long itemId, CursorRequest page) {
		return this.service.ofItem(ctx.workspaceId(), itemId, page).map(this.mapper::toResponse);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	MaintenanceResponse create(WorkspaceContext ctx, @Valid @RequestBody MaintenanceCreateRequest request) {
		return this.mapper.toResponse(this.service.create(ctx, request));
	}

	@PutMapping("/{id}")
	MaintenanceResponse update(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody MaintenanceUpdateRequest request) {
		return this.mapper.toResponse(this.service.update(ctx, id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(WorkspaceContext ctx, @PathVariable long id, @RequestParam int version) {
		this.service.delete(ctx, id, version);
	}

	@PostMapping("/{id}/restore")
	MaintenanceResponse restore(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody MaintenanceVersionRequest request) {
		return this.mapper.toResponse(this.service.restore(ctx, id, request.version()));
	}

}
