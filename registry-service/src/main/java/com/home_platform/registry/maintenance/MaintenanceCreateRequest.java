package com.home_platform.registry.maintenance;

import java.time.LocalDate;

import com.home_platform.registry.support.Text;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ghi một lần sửa hay bảo trì.
 * @param markFixed với {@code REPAIR} của món đang hỏng: cùng lúc chuyển món về {@code IN_USE}
 * @param itemVersion {@code version} của món đã đọc; bắt buộc khi {@code markFixed}
 */
public record MaintenanceCreateRequest(@NotNull Long itemId, @NotNull MaintenanceKind kind, @NotNull LocalDate doneOn,
		@NotBlank @Size(max = 500) String work, @Size(max = 150) String doneBy, @Size(max = 1000) String note,
		Boolean markFixed, Integer itemVersion) {

	public MaintenanceCreateRequest {
		work = Text.trimToNull(work);
		doneBy = Text.trimToNull(doneBy);
		note = Text.trimToNull(note);
		markFixed = Boolean.TRUE.equals(markFixed);
	}

}
