package com.home_platform.registry.ownership;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.home_platform.registry.support.Text;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Sửa một dòng nhập nhầm; người và địa điểm không đổi được. */
public record OwnershipUpdateRequest(@NotNull Integer version,
		@DecimalMin(value = "0", inclusive = false) @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal sharePercent, @NotNull LocalDate startDate, LocalDate endDate,
		@Size(max = 100) String documentNo, @Size(max = 500) String note) {

	public OwnershipUpdateRequest {
		documentNo = Text.trimToNull(documentNo);
		note = Text.trimToNull(note);
	}

}
