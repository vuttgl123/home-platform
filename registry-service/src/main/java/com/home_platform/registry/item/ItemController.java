package com.home_platform.registry.item;

import java.util.Objects;
import java.util.stream.Stream;

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
@RequestMapping("/items")
class ItemController {

	private final ItemService service;

	private final ItemMapper mapper;

	ItemController(ItemService service, ItemMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	/** Đúng một trong ba: ở một địa điểm (chưa gỡ, hoặc đã gỡ với {@code removed=true}), trong cả cây con, theo nhóm. */
	@GetMapping
	CursorPage<ItemResponse> list(WorkspaceContext ctx, @RequestParam(required = false) Long placeId,
			@RequestParam(defaultValue = "false") boolean removed, @RequestParam(required = false) Long subtreeOf,
			@RequestParam(required = false) Long categoryId, CursorRequest page) {
		if (Stream.of(placeId, subtreeOf, categoryId).filter(Objects::nonNull).count() != 1) {
			throw new BadRequestException("Give exactly one of placeId, subtreeOf or categoryId");
		}
		CursorPage<ItemView> found = (placeId != null) ? this.service.atPlace(ctx.workspaceId(), placeId, removed, page)
				: (subtreeOf != null) ? this.service.inSubtree(ctx.workspaceId(), subtreeOf, page)
						: this.service.byCategory(ctx.workspaceId(), categoryId, page);
		return found.map(this.mapper::toResponse);
	}

	/** Tìm theo tên bắt đầu bằng ({@code name}, gồm cả đồ đã gỡ) hoặc đúng mã ({@code code}); đúng một trong hai. */
	@GetMapping("/search")
	CursorPage<ItemResponse> search(WorkspaceContext ctx, @RequestParam(required = false) String name,
			@RequestParam(required = false) String code, CursorRequest page) {
		if ((name == null) == (code == null)) {
			throw new BadRequestException("Give exactly one of name or code");
		}
		CursorPage<ItemView> found = (name != null) ? this.service.searchByName(ctx.workspaceId(), name, page)
				: this.service.findByCode(ctx.workspaceId(), code, page);
		return found.map(this.mapper::toResponse);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ItemDetailResponse create(WorkspaceContext ctx, @Valid @RequestBody ItemRequest request) {
		return detail(ctx, this.service.create(ctx, request).id());
	}

	@GetMapping("/{id}")
	ItemDetailResponse get(WorkspaceContext ctx, @PathVariable long id) {
		return detail(ctx, id);
	}

	@PutMapping("/{id}")
	ItemDetailResponse update(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody ItemUpdateRequest request) {
		return detail(ctx, this.service.update(ctx, id, request).id());
	}

	@PostMapping("/{id}/move")
	ItemDetailResponse move(WorkspaceContext ctx, @PathVariable long id, @Valid @RequestBody ItemMoveRequest request) {
		return detail(ctx, this.service.move(ctx, id, request).id());
	}

	@PostMapping("/{id}/remove")
	ItemDetailResponse remove(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody ItemRemoveRequest request) {
		return detail(ctx, this.service.remove(ctx, id, request).id());
	}

	@PostMapping("/{id}/replace")
	ItemReplaceResponse replace(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody ItemReplaceRequest request) {
		Item[] items = this.service.replace(ctx, id, request);
		return new ItemReplaceResponse(detail(ctx, items[0].id()), detail(ctx, items[1].id()));
	}

	/** Đưa lại một món đã gỡ. */
	@PostMapping("/{id}/put-back")
	ItemDetailResponse putBack(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody ItemVersionRequest request) {
		return detail(ctx, this.service.putBack(ctx, id, request).id());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(WorkspaceContext ctx, @PathVariable long id, @RequestParam int version,
			@RequestParam(required = false) String note) {
		this.service.delete(ctx, id, version, note);
	}

	@PostMapping("/{id}/restore")
	ItemDetailResponse restore(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody ItemVersionRequest request) {
		return detail(ctx, this.service.restore(ctx, id, request).id());
	}

	@GetMapping("/{id}/versions")
	CursorPage<ItemVersionSummaryResponse> versions(WorkspaceContext ctx, @PathVariable long id, CursorRequest page) {
		return this.service.versions(ctx.workspaceId(), id, page).map(this.mapper::toVersionSummary);
	}

	@GetMapping("/{id}/versions/{versionNo}")
	ItemVersionResponse version(WorkspaceContext ctx, @PathVariable long id, @PathVariable int versionNo) {
		EntityVersion version = this.service.version(ctx.workspaceId(), id, versionNo);
		return this.mapper.toVersion(version, this.service.snapshotOf(version));
	}

	@PostMapping("/{id}/versions/{versionNo}/restore")
	ItemDetailResponse restoreVersion(WorkspaceContext ctx, @PathVariable long id, @PathVariable int versionNo,
			@Valid @RequestBody ItemVersionRequest request) {
		return detail(ctx, this.service.restoreVersion(ctx, id, versionNo, request).id());
	}

	ItemDetailResponse detail(WorkspaceContext ctx, long id) {
		return this.mapper.toDetail(this.service.view(ctx.workspaceId(), id));
	}

}
