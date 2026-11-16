package com.home_platform.registry.item;

import java.util.Objects;
import java.util.stream.Stream;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.history.EntityVersion;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.registry.code.CodeReissueRequest;
import com.home_platform.registry.support.Text;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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

	/**
	 * Nhiều nhất một phạm vi: ở một địa điểm (chưa gỡ, hoặc đã gỡ với {@code removed=true}), trong cả
	 * cây con, theo nhóm, của một người. Không có phạm vi là cả không gian. {@code status} chỉ đi được
	 * với cả không gian hoặc với nhóm; trống là mọi món chưa gỡ.
	 */
	@GetMapping
	CursorPage<ItemResponse> list(WorkspaceContext ctx, @RequestParam(required = false) Long placeId,
			@RequestParam(defaultValue = "false") boolean removed, @RequestParam(required = false) Long subtreeOf,
			@RequestParam(required = false) Long categoryId, @RequestParam(required = false) Long ownerId,
			@RequestParam(required = false) ItemStatus status, CursorRequest page) {
		if (Stream.of(placeId, subtreeOf, categoryId, ownerId).filter(Objects::nonNull).count() > 1) {
			throw new BadRequestException("Give at most one of placeId, subtreeOf, categoryId or ownerId");
		}
		if (status != null && (placeId != null || subtreeOf != null || ownerId != null)) {
			throw new BadRequestException("status can only be combined with categoryId or no scope");
		}
		CursorPage<ItemView> found = (placeId != null) ? this.service.atPlace(ctx.workspaceId(), placeId, removed, page)
				: (subtreeOf != null) ? this.service.inSubtree(ctx.workspaceId(), subtreeOf, page)
						: (ownerId != null) ? this.service.ownedBy(ctx.workspaceId(), ownerId, page)
								: this.service.all(ctx.workspaceId(), status, categoryId, page);
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
		ItemReplacement replaced = this.service.replace(ctx, id, request);
		return new ItemReplaceResponse(detail(ctx, replaced.removed().id()), detail(ctx, replaced.replacement().id()));
	}

	/** Cấp mã mới; mã cũ không còn tìm ra món này. */
	@PostMapping("/{id}/reissue-code")
	ItemDetailResponse reissueCode(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody CodeReissueRequest request) {
		return detail(ctx, this.service.reissueCode(ctx, id, request).id());
	}

	/** Đổi chủ; {@code ownerPersonId} trống là trả món về cho địa điểm. */
	@PostMapping("/{id}/owner")
	ItemDetailResponse changeOwner(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody ItemOwnerRequest request) {
		return detail(ctx, this.service.changeOwner(ctx, id, request).id());
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
			@RequestParam(required = false) @Size(max = 255) String note) {
		this.service.delete(ctx, id, version, Text.trimToNull(note));
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
