package com.home_platform.common.history;

import java.time.Instant;

/**
 * Phát ra ngay sau khi {@link EntityHistory} ghi một version, bên trong transaction của lần
 * sửa. Nơi nghe cần chạy sau khi transaction thành công thì dùng
 * {@code @TransactionalEventListener}. Đây là chỗ để sau này cắm việc đưa dữ liệu sang công cụ
 * tìm kiếm mà không phải sửa code nghiệp vụ.
 */
public record EntityChangedEvent(long workspaceId, String entityType, long entityId, int versionNo,
		ChangeAction action, long changedBy, Instant changedAt) {
}
