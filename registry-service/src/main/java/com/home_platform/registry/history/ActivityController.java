package com.home_platform.registry.history;

import java.time.Instant;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.history.ChangeAction;
import com.home_platform.common.history.ChangeFilter;
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
			@RequestParam(required = false) ChangeAction action, @RequestParam(required = false) Long changedBy,
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
			CursorRequest page) {
		return this.service.list(ctx.workspaceId(), new ChangeFilter(entityType, action, changedBy, from, to), page)
			.map(this.mapper::toResponse);
	}

}
