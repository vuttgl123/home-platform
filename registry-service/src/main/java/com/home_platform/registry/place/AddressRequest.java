package com.home_platform.registry.place;

import java.math.BigDecimal;
import java.util.Locale;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Địa chỉ gửi lên. Mã và tên tỉnh, quận, phường là chữ tự do. Toạ độ phải có cả hai hoặc không
 * có cả hai.
 *
 * @param countryCode hai chữ cái; mặc định {@code VN}
 */
public record AddressRequest(@Pattern(regexp = "[A-Za-z]{2}") String countryCode,
		@Size(max = 10) String provinceCode, @Size(max = 100) String provinceName,
		@Size(max = 10) String districtCode, @Size(max = 100) String districtName, @Size(max = 10) String wardCode,
		@Size(max = 100) String wardName, @Size(max = 255) String street,
		@NotBlank @Size(max = 500) String fullAddress,
		@DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
		@DecimalMin("-180") @DecimalMax("180") BigDecimal longitude) {

	public AddressRequest {
		countryCode = Text.trimToNull(countryCode);
		countryCode = (countryCode != null) ? countryCode.toUpperCase(Locale.ROOT) : "VN";
		provinceCode = Text.trimToNull(provinceCode);
		provinceName = Text.trimToNull(provinceName);
		districtCode = Text.trimToNull(districtCode);
		districtName = Text.trimToNull(districtName);
		wardCode = Text.trimToNull(wardCode);
		wardName = Text.trimToNull(wardName);
		street = Text.trimToNull(street);
		fullAddress = Text.trimToNull(fullAddress);
	}

}
