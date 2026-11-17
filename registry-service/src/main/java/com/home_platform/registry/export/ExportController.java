package com.home_platform.registry.export;

import java.io.IOException;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.registry.item.ItemStatus;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tải dữ liệu về dạng file Excel. File được ghi thẳng vào phản hồi trong lúc đọc dữ liệu, nên mọi
 * kiểm tra phải xong trước khi đặt header của file.
 */
@RestController
@RequestMapping("/exports")
class ExportController {

	private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private final ExportService service;

	ExportController(ExportService service) {
		this.service = service;
	}

	/** Đồ đạc của cả không gian; {@code status} và {@code categoryId} lọc như {@code GET /items}. */
	@GetMapping("/items.xlsx")
	void items(WorkspaceContext ctx, @RequestParam(required = false) ItemStatus status,
			@RequestParam(required = false) Long categoryId, HttpServletResponse response) throws IOException {
		this.service.requireItemFilter(ctx.workspaceId(), categoryId);
		attach(response, this.service.fileName("items"));
		this.service.writeItems(ctx.workspaceId(), status, categoryId, response.getOutputStream());
	}

	/** Mọi người chưa xoá của không gian. */
	@GetMapping("/people.xlsx")
	void people(WorkspaceContext ctx, HttpServletResponse response) throws IOException {
		attach(response, this.service.fileName("people"));
		this.service.writePeople(ctx.workspaceId(), response.getOutputStream());
	}

	/** Cả không gian trong một file: địa điểm, người, cư trú, sở hữu, đồ đạc. */
	@GetMapping("/workspace.xlsx")
	void workspace(WorkspaceContext ctx, HttpServletResponse response) throws IOException {
		attach(response, this.service.fileName("workspace"));
		this.service.writeWorkspace(ctx.workspaceId(), response.getOutputStream());
	}

	private static void attach(HttpServletResponse response, String fileName) {
		response.setContentType(XLSX);
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName);
	}

}
