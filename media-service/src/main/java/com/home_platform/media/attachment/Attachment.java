package com.home_platform.media.attachment;

import java.time.Instant;

import lombok.With;

/**
 * Một dòng của {@code med_attachment}: gắn một file vào một địa điểm, người hoặc món đồ. Gỡ ảnh
 * là ghi {@code removedBy}, {@code removedAt}; dòng không bị xoá.
 *
 * <p>TODO(media): repository với SQL tay (cột chuẩn của bảng này mang tên khác:
 * {@code added_*}, {@code removed_*}); danh sách ảnh của một bản ghi theo index
 * (workspace_id, owner_type, owner_id, removed_at, sort_order). Media không kiểm tra được
 * {@code ownerId} có tồn tại bên registry không. Xem {@code docs/superpowers/todo/media.md}.
 *
 * @param removedAt {@code null} nghĩa là ảnh còn hiển thị
 */
@With
public record Attachment(long workspaceId, long id, OwnerType ownerType, long ownerId, long fileId, String caption,
		int sortOrder, long addedBy, Instant addedAt, Long removedBy, Instant removedAt) {
}
