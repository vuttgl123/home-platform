package com.home_platform.registry.item;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Thay một món bằng món mới.
 *
 * @param version {@code version} của món cũ
 * @param removedAt ngày gỡ món cũ; bỏ trống là hôm nay
 * @param replacement món mới; {@code placeId} trống là đặt cùng chỗ với món cũ
 */
public record ItemReplaceRequest(@NotNull Integer version, LocalDate removedAt,
		@NotNull @Valid ItemRequest replacement) {
}
