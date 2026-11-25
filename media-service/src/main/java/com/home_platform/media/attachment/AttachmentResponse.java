package com.home_platform.media.attachment;

import java.time.Instant;

import com.home_platform.media.file.MediaFile;

/**
 * Một ảnh đã gắn vào một địa điểm, người hay món đồ. Nội dung ảnh đọc ở
 * {@code /workspaces/{ws}/files/{fileId}} và {@code /thumbnail}.
 */
public record AttachmentResponse(long id, OwnerType ownerType, long ownerId, long fileId, String caption,
		int sortOrder, long addedBy, Instant addedAt, FileInfo file) {

	static AttachmentResponse of(Attachment attachment, MediaFile file) {
		return new AttachmentResponse(attachment.id(), attachment.ownerType(), attachment.ownerId(), attachment.fileId(),
				attachment.caption(), attachment.sortOrder(), attachment.addedBy(), attachment.addedAt(),
				new FileInfo(file.originalName(), file.contentType(), file.sizeBytes(),
						(file.width() != null) ? file.width() : 0, (file.height() != null) ? file.height() : 0));
	}

}
