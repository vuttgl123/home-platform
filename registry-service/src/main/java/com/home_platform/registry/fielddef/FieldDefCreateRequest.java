package com.home_platform.registry.fielddef;

import java.util.List;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param typeId 0 hoặc bỏ trống là áp dụng cho mọi bản ghi của {@code target}
 * @param options bắt buộc với SELECT, phải trống với kiểu khác
 */
public record FieldDefCreateRequest(@NotNull FieldTarget target, Long typeId,
		@NotBlank @Size(max = 50) @Pattern(regexp = "[a-z][a-z0-9_]*") String fieldKey,
		@NotBlank @Size(max = 100) String label, @NotNull FieldDataType dataType, List<String> options,
		Boolean required, Integer sortOrder) {

	public FieldDefCreateRequest {
		typeId = (typeId != null) ? typeId : 0L;
		fieldKey = Text.trimToNull(fieldKey);
		label = Text.trimToNull(label);
		options = FieldOptions.trim(options);
		required = (required != null) ? required : Boolean.FALSE;
		sortOrder = (sortOrder != null) ? sortOrder : 0;
	}

}
