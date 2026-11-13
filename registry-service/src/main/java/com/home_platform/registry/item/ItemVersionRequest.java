package com.home_platform.registry.item;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Thao tác chỉ cần {@code version} và ghi chú: đưa lại, bỏ xoá, khôi phục version. */
public record ItemVersionRequest(@NotNull Integer version, @Size(max = 255) String note) {

	public ItemVersionRequest {
		note = Text.trimToNull(note);
	}

}
