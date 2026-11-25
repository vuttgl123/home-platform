package com.home_platform.registry.maintenance;

import jakarta.validation.constraints.NotNull;

/** Lệnh chỉ cần {@code version} đã đọc, ví dụ khôi phục. */
public record MaintenanceVersionRequest(@NotNull Integer version) {
}
