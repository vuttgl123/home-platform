package com.home_platform.registry.item;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.home_platform.common.jdbc.JsonColumn;
import com.home_platform.registry.support.PlainNumbers;
import lombok.With;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Một dòng của {@code reg_item}. Món đồ chỉ lưu vị trí hiện tại; nó từng nằm ở đâu thì xem
 * qua các version. Món đã gỡ ({@code removedAt}) vẫn ở lại địa điểm, trong danh sách "đã gỡ".
 * {@code ownerPersonId} trống là món của địa điểm; có giá trị là tài sản riêng của người đó.
 * {@code warrantyUntil} là ngày cuối cùng còn bảo hành; {@code serviceEveryMonths} là chu kỳ bảo trì
 * (spec Đợt 8, L3). Cả hai bỏ trống là chưa đặt.
 */
@With
public record Item(long workspaceId, long id, long placeId, Long categoryId, String code, String name, int quantity,
		String unit, ItemStatus status, LocalDate placedAt, LocalDate warrantyUntil,
		Integer serviceEveryMonths, LocalDate removedAt, Long replacedByItemId,
		Long ownerPersonId, String description,
		@JsonSerialize(contentUsing = PlainNumbers.class) @JsonColumn Map<String, Object> attributes,
		Long coverFileId, Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version,
		Instant deletedAt) {

	public boolean removed() {
		return this.removedAt != null;
	}

}
