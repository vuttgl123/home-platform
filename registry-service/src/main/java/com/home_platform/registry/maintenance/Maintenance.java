package com.home_platform.registry.maintenance;

import java.time.Instant;
import java.time.LocalDate;

import lombok.With;

/**
 * Một dòng của {@code reg_maintenance}: một lần sửa hay bảo trì món {@code itemId} vào ngày {@code doneOn}.
 * {@code doneBy} là chữ tự do (thợ ngoài thường không có hồ sơ người). Món của dòng không đổi sau khi tạo.
 */
@With
public record Maintenance(long workspaceId, long id, long itemId, MaintenanceKind kind, LocalDate doneOn, String work,
		String doneBy, String note, Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version,
		Instant deletedAt) {
}
