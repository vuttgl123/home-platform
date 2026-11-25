package com.home_platform.media.file;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nội dung ảnh cho {@code <img src>}. Thẻ này không gửi được header, nên id không gian nằm trên
 * đường dẫn thay vì {@code X-Workspace-Id}. Nội dung một file không bao giờ đổi, nên trình duyệt
 * được giữ một năm. Chưa có kiểm tra quyền: tới Đợt 10 (đăng nhập bằng cookie).
 */
@RestController
@RequestMapping("/workspaces/{workspaceId}/files/{fileId}")
class FileContentController {

	static final String CACHE_CONTROL = "private, max-age=31536000, immutable";

	private final FileContentService service;

	FileContentController(FileContentService service) {
		this.service = service;
	}

	@GetMapping
	ResponseEntity<InputStreamResource> original(@PathVariable long workspaceId, @PathVariable long fileId) {
		return respond(this.service.original(workspaceId, fileId));
	}

	@GetMapping("/thumbnail")
	ResponseEntity<InputStreamResource> thumbnail(@PathVariable long workspaceId, @PathVariable long fileId) {
		return respond(this.service.thumbnail(workspaceId, fileId));
	}

	private static ResponseEntity<InputStreamResource> respond(FileContent content) {
		ResponseEntity.BodyBuilder response = ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(content.contentType()))
			.header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
			.header("X-Content-Type-Options", "nosniff");
		if (content.sizeBytes() >= 0) {
			response.contentLength(content.sizeBytes());
		}
		return response.body(new InputStreamResource(content.stream()));
	}

}
