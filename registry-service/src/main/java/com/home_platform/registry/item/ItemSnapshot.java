package com.home_platform.registry.item;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.home_platform.registry.support.PlainNumbers;
import tools.jackson.databind.annotation.JsonSerialize;

/** Nội dung một món đồ ghi vào version; không có cột kiểm toán và {@code version}. */
public record ItemSnapshot(long id, long placeId, Long categoryId, String code, String name, int quantity, String unit,
		ItemStatus status, LocalDate placedAt, LocalDate removedAt, Long replacedByItemId, String description,
		@JsonSerialize(contentUsing = PlainNumbers.class) Map<String, Object> attributes, Long coverFileId,
		Instant deletedAt) {

	public static ItemSnapshot of(Item item) {
		return new ItemSnapshot(item.id(), item.placeId(), item.categoryId(), item.code(), item.name(), item.quantity(),
				item.unit(), item.status(), item.placedAt(), item.removedAt(), item.replacedByItemId(),
				item.description(), item.attributes(), item.coverFileId(), item.deletedAt());
	}

}
