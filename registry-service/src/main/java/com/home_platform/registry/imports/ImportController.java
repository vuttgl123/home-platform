package com.home_platform.registry.imports;

import java.io.IOException;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.registry.export.ExportService;
import com.home_platform.registry.export.ImportTemplateWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Nhập dữ liệu từ một file Excel: file mẫu, xem trước, nhập. */
@RestController
@RequestMapping("/imports")
class ImportController {

	static final long MAX_BYTES = 5L * 1024 * 1024;

	private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private final ImportTemplateWriter template;

	private final ExportService exports;

	private final ImportService imports;

	ImportController(ImportTemplateWriter template, ExportService exports, ImportService imports) {
		this.template = template;
		this.exports = exports;
		this.imports = imports;
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

	/** Chạy thử cả file rồi huỷ; trả kết quả từng dòng. Lỗi trong file là kết quả bình thường, vẫn 200. */
	@PostMapping(path = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ImportReport preview(WorkspaceContext ctx, @RequestParam("file") MultipartFile file) throws IOException {
		return this.imports.preview(ctx, fileName(file), bytes(file));
	}

	/** Nhập cả file, chỉ khi không có lỗi nào; {@code committed} cho biết đã lưu hay chưa. */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ImportReport commit(WorkspaceContext ctx, @RequestParam("file") MultipartFile file) throws IOException {
		return this.imports.commit(ctx, fileName(file), bytes(file));
	}

	private static byte[] bytes(MultipartFile file) throws IOException {
		if (file.getSize() > MAX_BYTES) {
			throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "File must be at most 5 MB");
		}
		return file.getBytes();
	}

	/** Tên file không kèm đường dẫn: vài trình duyệt cũ gửi cả "C:\\…\\nha.xlsx". */
	private static String fileName(MultipartFile file) {
		String name = ImportText.clean(file.getOriginalFilename());
		if (name != null) {
			name = ImportText.clean(name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1));
		}
		return (name != null) ? name : "import.xlsx";
	}

}
