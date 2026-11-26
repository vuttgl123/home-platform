package com.home_platform.registry.imports;

import java.io.IOException;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.registry.export.ExportService;
import com.home_platform.registry.export.ImportTemplateWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Nhập dữ liệu từ một file Excel: file mẫu, xem trước, nhập. */
@RestController
@RequestMapping("/imports")
class ImportController {

	private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private final ImportTemplateWriter template;

	private final ExportService exports;

	ImportController(ImportTemplateWriter template, ExportService exports) {
		this.template = template;
		this.exports = exports;
	}

	/** File mẫu: sheet Hướng dẫn rồi năm sheet chỉ có tiêu đề, kèm cột mục tuỳ biến của không gian. */
	@GetMapping("/template.xlsx")
	void template(WorkspaceContext ctx, HttpServletResponse response) throws IOException {
		String fileName = this.exports.fileName("import-template");
		response.setContentType(XLSX);
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName);
		this.template.write(ctx.workspaceId(), response.getOutputStream());
	}

}
