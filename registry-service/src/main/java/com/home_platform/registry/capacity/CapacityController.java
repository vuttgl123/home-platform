package com.home_platform.registry.capacity;

import java.time.LocalDate;

import jakarta.servlet.http.HttpServletRequest;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.registry.place.PlaceStatus;
import com.home_platform.registry.search.AttributeFilter;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
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

	/** Địa điểm còn chỗ hôm nay, theo tên; lọc theo loại và theo cây con của một địa điểm. */
	@GetMapping("/vacancies")
	CursorPage<VacancyResponse> vacancies(WorkspaceContext ctx, @RequestParam(required = false) Long placeTypeId,
			@RequestParam(required = false) Long rootId, CursorRequest page) {
		return this.service.vacancies(ctx.workspaceId(), placeTypeId, rootId, page);
	}

	/**
	 * Bảng địa điểm (spec Đợt 11, S3), theo tên: tìm chữ trong tên, mã, mô tả; lọc theo loại, trạng
	 * thái, hậu duệ của một địa điểm, còn chỗ, và mục tuỳ biến ({@code attr} lặp, tối đa 3).
	 */
	@GetMapping("/list")
	@Parameter(name = AttributeFilter.PARAM, in = ParameterIn.QUERY,
			description = "khoá:phép:giá trị (has, eq, gte, lte, set, unset); lặp tối đa 3 lần",
			array = @ArraySchema(schema = @Schema(type = "string")))
	CursorPage<VacancyResponse> list(WorkspaceContext ctx, @RequestParam(required = false) String q,
			@RequestParam(required = false) Long typeId, @RequestParam(required = false) PlaceStatus status,
			@RequestParam(required = false) Long rootId, @RequestParam(defaultValue = "false") boolean hasRoom,
			CursorRequest page, HttpServletRequest request) {
		return this.service.list(ctx.workspaceId(), q, typeId, status, rootId, hasRoom, AttributeFilter.values(request),
				page);
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
