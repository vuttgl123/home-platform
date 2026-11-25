package com.home_platform.registry.capacity;

import java.time.LocalDate;

import com.home_platform.common.context.WorkspaceContext;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Sức chứa và chỗ trống của các địa điểm (spec Đợt 6). */
@RestController
@RequestMapping("/places")
class CapacityController {

	private final CapacityService service;

	CapacityController(CapacityService service) {
		this.service = service;
	}

	/** Có vượt sức chứa không nếu thêm một người ở từ {@code startDate} tới {@code endDate}. */
	@GetMapping("/{id}/capacity-check")
	CapacityCheckResponse check(WorkspaceContext ctx, @PathVariable long id,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(required = false) Long excludeOccupancyId) {
		return this.service.check(ctx.workspaceId(), id, startDate, endDate, excludeOccupancyId);
	}

}
