package com.home_platform.registry.ownership;

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
@RequestMapping("/ownerships")
class OwnershipController {

	private final OwnershipService service;

	private final OwnershipMapper mapper;

	OwnershipController(OwnershipService service, OwnershipMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	/** Đúng một trong ba: tại một địa điểm (theo trạng thái, mặc định đang sở hữu), trong cả cây con, của một người. */
	@GetMapping
	CursorPage<OwnershipResponse> list(WorkspaceContext ctx, @RequestParam(required = false) Long placeId,
			@RequestParam(required = false) OwnershipStatus status, @RequestParam(required = false) Long subtreeOf,
			@RequestParam(required = false) Long personId, CursorRequest page) {
		if (Stream.of(placeId, subtreeOf, personId).filter(Objects::nonNull).count() != 1) {
			throw new BadRequestException("Give exactly one of placeId, subtreeOf or personId");
		}
		CursorPage<OwnershipView> found = (placeId != null)
				? this.service.atPlace(ctx.workspaceId(), placeId, (status != null) ? status : OwnershipStatus.CURRENT,
						page)
				: (subtreeOf != null) ? this.service.currentInSubtree(ctx.workspaceId(), subtreeOf, page)
						: this.service.ofPerson(ctx.workspaceId(), personId, page);
		return found.map(this.mapper::toResponse);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	OwnershipResponse create(WorkspaceContext ctx, @Valid @RequestBody OwnershipCreateRequest request) {
		return view(ctx, this.service.create(ctx, request).id());
	}

	@GetMapping("/{id}")
	OwnershipResponse get(WorkspaceContext ctx, @PathVariable long id) {
		return view(ctx, id);
	}

	@PutMapping("/{id}")
	OwnershipResponse update(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody OwnershipUpdateRequest request) {
		return view(ctx, this.service.update(ctx, id, request).id());
	}

	@PostMapping("/{id}/end")
	OwnershipResponse end(WorkspaceContext ctx, @PathVariable long id, @Valid @RequestBody OwnershipEndRequest request) {
		return view(ctx, this.service.end(ctx, id, request).id());
	}

	/** Chuyển nhượng; trả cả dòng vừa kết thúc và dòng vừa bắt đầu. */
	@PostMapping("/{id}/transfer")
	OwnershipTransferResponse transfer(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody OwnershipTransferRequest request) {
		OwnershipTransfer done = this.service.transfer(ctx, id, request);
		return new OwnershipTransferResponse(view(ctx, done.ended().id()), view(ctx, done.started().id()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(WorkspaceContext ctx, @PathVariable long id, @RequestParam int version) {
		this.service.delete(ctx, id, version);
	}

	private OwnershipResponse view(WorkspaceContext ctx, long id) {
		return this.mapper.toResponse(this.service.view(ctx.workspaceId(), id));
	}

}
