package com.home_platform.registry.placetype;

import java.util.Locale;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param code mã ổn định, tự viết hoa; không đổi được sau khi tạo
 * @param sortOrder thứ tự hiển thị; mặc định 0
 */
public record PlaceTypeCreateRequest(@NotBlank @Size(max = 50) @Pattern(regexp = "[A-Z0-9_]+") String code,
		@NotBlank @Size(max = 100) String name, @Size(max = 500) String description, Integer sortOrder) {

	public PlaceTypeCreateRequest {
		code = Text.trimToNull(code);
		code = (code != null) ? code.toUpperCase(Locale.ROOT) : null;
		name = Text.trimToNull(name);
		description = Text.trimToNull(description);
		sortOrder = (sortOrder != null) ? sortOrder : 0;
	}

}
