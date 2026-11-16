package com.home_platform.registry.trash;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TrashController {

	private final TrashService service;

	TrashController(TrashService service) {
		this.service = service;
	}

	/** Một trang các bản ghi đang bị xoá của một loại, mới xoá trước. */
	@GetMapping("/trash")
	CursorPage<TrashResponse> list(WorkspaceContext ctx, @RequestParam TrashType entityType, CursorRequest page) {
		return this.service.list(ctx.workspaceId(), entityType, page).map(TrashResponse::of);
	}

}
