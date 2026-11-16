package com.home_platform.registry.code;

import com.home_platform.common.context.WorkspaceContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Bản ghi chưa có mã: đếm, và cấp mã từng lô. */
@RestController
@RequestMapping("/codes")
class CodeController {

	private final CodeBackfillService service;

	CodeController(CodeBackfillService service) {
		this.service = service;
	}

	@GetMapping("/status")
	CodeStatusResponse status(WorkspaceContext ctx) {
		return this.service.status(ctx.workspaceId());
	}

	@PostMapping("/backfill")
	CodeBackfillResponse backfill(WorkspaceContext ctx) {
		return this.service.backfill(ctx);
	}

}
