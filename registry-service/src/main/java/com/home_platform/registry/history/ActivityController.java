package com.home_platform.registry.history;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ActivityController {

	private final ActivityService service;

	private final ActivityMapper mapper;

	ActivityController(ActivityService service, ActivityMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping("/activity")
	CursorPage<ActivityResponse> list(WorkspaceContext ctx, @RequestParam(required = false) String entityType,
			CursorRequest page) {
		return this.service.list(ctx.workspaceId(), entityType, page).map(this.mapper::toResponse);
	}

}
