package com.home_platform.registry.itemcategory;

import java.util.List;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/item-categories")
class ItemCategoryController {

	private final ItemCategoryService service;

	private final ItemCategoryMapper mapper;

	ItemCategoryController(ItemCategoryService service, ItemCategoryMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	List<ItemCategoryResponse> list(WorkspaceContext ctx) {
		return this.mapper.toResponses(this.service.list(ctx.workspaceId()));
	}

	@GetMapping("/{id}")
	ItemCategoryResponse get(WorkspaceContext ctx, @PathVariable long id) {
		return this.mapper.toResponse(this.service.get(ctx.workspaceId(), id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ItemCategoryResponse create(WorkspaceContext ctx, @Valid @RequestBody ItemCategoryRequest request) {
		return this.mapper.toResponse(this.service.create(ctx, request));
	}

	@PutMapping("/{id}")
	ItemCategoryResponse update(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody ItemCategoryRequest request) {
		return this.mapper.toResponse(this.service.update(ctx, id, request));
	}

	@PostMapping("/{id}/archive")
	ItemCategoryResponse archive(WorkspaceContext ctx, @PathVariable long id) {
		return this.mapper.toResponse(this.service.archive(ctx, id));
	}

	@PostMapping("/{id}/unarchive")
	ItemCategoryResponse unarchive(WorkspaceContext ctx, @PathVariable long id) {
		return this.mapper.toResponse(this.service.unarchive(ctx, id));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(WorkspaceContext ctx, @PathVariable long id) {
		this.service.delete(ctx, id);
	}

}
