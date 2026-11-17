package com.home_platform.registry.export;

import java.time.LocalDate;
import java.util.Map;

import com.home_platform.registry.item.ItemStatus;

/**
 * Một món đồ cho file Excel: các cột của {@code reg_item} cần ghi, kèm đường dẫn {@code id} của địa
 * điểm (để sắp và làm mốc đọc tiếp) và tên chủ.
 *
 * @param placePath cột {@code path} của địa điểm chứa món
 * @param ownerName tên người chủ, kể cả khi người đó đã xoá; {@code null} là món của địa điểm
 */
public record ExportItemRow(long id, long placeId, String placePath, Long categoryId, String code, String name,
		int quantity, String unit, ItemStatus status, LocalDate placedAt, LocalDate removedAt, String ownerName,
		String description, Map<String, Object> attributes) {
}
