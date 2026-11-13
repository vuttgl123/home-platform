package com.home_platform.registry.item;

import java.time.LocalDate;
import java.util.Map;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Tạo một món đồ; cũng là món mới khi thay.
 *
 * @param placeId nơi đặt; bắt buộc khi tạo, bỏ trống khi thay nghĩa là cùng chỗ với món cũ
 * @param status mặc định IN_USE; không được là REMOVED
 * @param note ghi chú cho version được tạo
 */
public record ItemRequest(Long placeId, Long categoryId, @Size(max = 50) String code,
		@NotBlank @Size(max = 255) String name, @Min(1) Integer quantity, @Size(max = 20) String unit,
		ItemStatus status, LocalDate placedAt, @Size(max = 10000) String description, Map<String, Object> attributes,
		Long coverFileId, @Size(max = 255) String note) {

	public ItemRequest {
		code = Text.trimToNull(code);
		name = Text.trimToNull(name);
		quantity = (quantity != null) ? quantity : 1;
		unit = Text.trimToNull(unit);
		status = (status != null) ? status : ItemStatus.IN_USE;
		description = Text.trimToNull(description);
		note = Text.trimToNull(note);
	}

}
