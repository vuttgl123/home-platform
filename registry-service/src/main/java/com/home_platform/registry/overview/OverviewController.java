package com.home_platform.registry.overview;

import com.home_platform.common.context.WorkspaceContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class OverviewController {

	private final OverviewService service;

	OverviewController(OverviewService service) {
		this.service = service;
	}

	@GetMapping("/overview")
	OverviewResponse get(WorkspaceContext ctx) {
		return this.service.get(ctx.workspaceId());
	}

}
