package com.home_platform.media.attachment;

import java.util.List;

import com.home_platform.common.context.WorkspaceContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Ảnh gắn vào địa điểm, người, món đồ. */
@RestController
@RequestMapping("/attachments")
class AttachmentController {

	private final AttachmentService service;

	AttachmentController(AttachmentService service) {
		this.service = service;
	}

	/** Tải một ảnh lên và gắn vào bản ghi; ảnh mới đứng cuối. */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	AttachmentResponse upload(WorkspaceContext ctx, @RequestParam OwnerType ownerType, @RequestParam long ownerId,
			@RequestPart("file") MultipartFile file, @RequestParam(required = false) String caption) {
		return this.service.upload(ctx, ownerType, ownerId, file, caption);
	}

	/** Ảnh đang hiện của một bản ghi, theo thứ tự; tối đa 100 nên không phân trang. */
	@GetMapping
	List<AttachmentResponse> list(WorkspaceContext ctx, @RequestParam OwnerType ownerType, @RequestParam long ownerId) {
		return this.service.list(ctx.workspaceId(), ownerType, ownerId);
	}

}
