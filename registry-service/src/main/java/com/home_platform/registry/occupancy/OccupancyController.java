package com.home_platform.registry.occupancy;

import java.util.Objects;
import java.util.stream.Stream;

import com.home_platform.common.context.WorkspaceContext;
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
@RequestMapping("/occupancies")
class OccupancyController {

	private final OccupancyService service;

	private final OccupancyMapper mapper;

	OccupancyController(OccupancyService service, OccupancyMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	/** Đúng một trong ba: tại một địa điểm (theo trạng thái, mặc định đang ở), trong cả cây con, của một người. */
	@GetMapping
	CursorPage<OccupancyResponse> list(WorkspaceContext ctx, @RequestParam(required = false) Long placeId,
			@RequestParam(required = false) OccupancyStatus status, @RequestParam(required = false) Long subtreeOf,
			@RequestParam(required = false) Long personId, CursorRequest page) {
		if (Stream.of(placeId, subtreeOf, personId).filter(Objects::nonNull).count() != 1) {
			throw new BadRequestException("Give exactly one of placeId, subtreeOf or personId");
		}
		CursorPage<OccupancyView> found = (placeId != null)
				? this.service.atPlace(ctx.workspaceId(), placeId,
						(status != null) ? status : OccupancyStatus.CURRENT, page)
				: (subtreeOf != null) ? this.service.currentInSubtree(ctx.workspaceId(), subtreeOf, page)
						: this.service.ofPerson(ctx.workspaceId(), personId, page);
		return found.map(this.mapper::toResponse);
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
