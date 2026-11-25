package com.home_platform.media.attachment;

import java.time.Instant;

import lombok.With;

/**
 * Một dòng của {@code med_attachment}: gắn một file vào một địa điểm, người hoặc món đồ. Gỡ ảnh
 * là ghi {@code removedBy}, {@code removedAt}; dòng không bị xoá.
 *
 * <p>Đọc, ghi qua {@link AttachmentRepository} (SQL tay, vì cột chuẩn của bảng này mang tên khác:
 * {@code added_*}, {@code removed_*}). Media không kiểm tra được {@code ownerId} có tồn tại bên
 * registry không.
 *
 * @param removedAt {@code null} nghĩa là ảnh còn hiển thị
 */
@With
public record Attachment(long workspaceId, long id, OwnerType ownerType, long ownerId, long fileId, String caption,
		int sortOrder, long addedBy, Instant addedAt, Long removedBy, Instant removedAt) {
}
