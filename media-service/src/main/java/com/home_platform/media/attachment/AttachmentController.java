package com.home_platform.media.attachment;

import java.util.List;

import com.home_platform.common.context.WorkspaceContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	/** Sắp lại mọi ảnh đang hiện của một bản ghi; danh sách cũ (thiếu, thừa, sai id) thì 409. */
	@PutMapping("/order")
	List<AttachmentResponse> reorder(WorkspaceContext ctx, @Valid @RequestBody OrderRequest request) {
		return this.service.reorder(ctx, request);
	}

	/** Đổi chú thích; rỗng là bỏ chú thích. */
	@PutMapping("/{id}")
	AttachmentResponse caption(WorkspaceContext ctx, @PathVariable long id, @RequestBody CaptionRequest request) {
		return this.service.editCaption(ctx, id, request.caption());
	}

	/** Gỡ ảnh; dòng và file đều còn. */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void remove(WorkspaceContext ctx, @PathVariable long id) {
		this.service.remove(ctx, id);
	}

	/** Ảnh đang hiện của một bản ghi, theo thứ tự; tối đa 100 nên không phân trang. */
	@GetMapping
	List<AttachmentResponse> list(WorkspaceContext ctx, @RequestParam OwnerType ownerType, @RequestParam long ownerId) {
		return this.service.list(ctx.workspaceId(), ownerType, ownerId);
	}

}
