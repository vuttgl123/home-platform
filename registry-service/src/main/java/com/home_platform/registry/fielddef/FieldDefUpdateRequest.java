package com.home_platform.registry.fielddef;

import java.util.List;

import com.home_platform.registry.support.Text;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Phần sửa được của một mục; khoá, đích, phạm vi và kiểu không đổi sau khi tạo. */
public record FieldDefUpdateRequest(@NotBlank @Size(max = 100) String label, List<String> options, Boolean required,
		Integer sortOrder, @Min(1) @Max(365) Integer remindDays) {

	public FieldDefUpdateRequest {
		label = Text.trimToNull(label);
		options = FieldOptions.trim(options);
		required = (required != null) ? required : Boolean.FALSE;
		sortOrder = (sortOrder != null) ? sortOrder : 0;
	}

}
