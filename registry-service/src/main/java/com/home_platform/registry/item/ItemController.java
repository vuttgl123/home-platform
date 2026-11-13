package com.home_platform.registry.item;

import com.home_platform.common.context.WorkspaceContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

	ItemDetailResponse detail(WorkspaceContext ctx, long id) {
		return this.mapper.toDetail(this.service.view(ctx.workspaceId(), id));
	}

}
