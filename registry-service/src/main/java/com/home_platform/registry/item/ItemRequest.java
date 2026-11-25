package com.home_platform.registry.item;

import java.time.LocalDate;
import java.util.Map;

import com.home_platform.registry.support.Text;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Tạo một món đồ; cũng là món mới khi thay. Không có {@code code}: mã do hệ thống cấp.
 *
 * @param placeId nơi đặt; bắt buộc khi tạo, bỏ trống khi thay nghĩa là cùng chỗ với món cũ
 * @param ownerPersonId chủ riêng của món; trống là món của địa điểm (khi thay: cùng chủ với món cũ)
 * @param status mặc định IN_USE; không được là REMOVED
 * @param note ghi chú cho version được tạo
 */
public record ItemRequest(Long placeId, Long ownerPersonId, Long categoryId,
		@NotBlank @Size(max = 255) String name, @Min(1) Integer quantity, @Size(max = 20) String unit,
		ItemStatus status, LocalDate placedAt, LocalDate warrantyUntil,
		@Min(1) @Max(120) Integer serviceEveryMonths, @Size(max = 10000) String description,
		Map<String, Object> attributes, Long coverFileId, @Size(max = 255) String note) {

	/** Món thay cho {@code old}: chỗ đặt và chủ trống thì lấy của món cũ. */
	ItemRequest inheriting(Item old) {
		return new ItemRequest((this.placeId != null) ? this.placeId : old.placeId(),
				(this.ownerPersonId != null) ? this.ownerPersonId : old.ownerPersonId(), this.categoryId, this.name,
				this.quantity, this.unit, this.status, this.placedAt, this.warrantyUntil, this.serviceEveryMonths,
				this.description, this.attributes, this.coverFileId, this.note);
	}

	public ItemRequest {
		name = Text.trimToNull(name);
		quantity = (quantity != null) ? quantity : 1;
		unit = Text.trimToNull(unit);
		status = (status != null) ? status : ItemStatus.IN_USE;
		description = Text.trimToNull(description);
		note = Text.trimToNull(note);
	}

}
