package com.home_platform.registry.code;

import java.util.Locale;

import com.home_platform.registry.support.Text;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * @param prefix 1 đến 10 ký tự chữ và số, bắt đầu bằng chữ; tự viết hoa
 * @param digits số chữ số của phần số thứ tự, 3 đến 9
 * @param yearly có đặt năm vào mã (và đếm lại từ 1 mỗi năm) không; mặc định không
 */
public record CodeRuleRequest(@NotBlank @Pattern(regexp = "[A-Z][A-Z0-9]{0,9}") String prefix,
		@NotNull @Min(3) @Max(9) Integer digits, Boolean yearly) {

	public CodeRuleRequest {
		prefix = Text.trimToNull(prefix);
		prefix = (prefix != null) ? prefix.toUpperCase(Locale.ROOT) : null;
		yearly = (yearly != null) ? yearly : false;
	}

	CodeFormat format() {
		return new CodeFormat(this.prefix, this.digits, this.yearly);
	}

}
