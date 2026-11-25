package com.home_platform.registry.maintenance;

import java.time.Instant;
import java.time.LocalDate;

/** Nội dung một dòng bảo trì ghi vào version; không có cột kiểm toán và {@code version}. */
public record MaintenanceSnapshot(long id, long itemId, MaintenanceKind kind, LocalDate doneOn, String work,
		String doneBy, String note, Instant deletedAt) {

	static MaintenanceSnapshot of(Maintenance row) {
		return new MaintenanceSnapshot(row.id(), row.itemId(), row.kind(), row.doneOn(), row.work(), row.doneBy(),
				row.note(), row.deletedAt());
	}

}
