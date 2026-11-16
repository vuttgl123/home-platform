package com.home_platform.registry.ownership;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.home_platform.registry.support.Text;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param sharePercent tỷ lệ sở hữu, lớn hơn 0 và không quá 100, tối đa hai chữ số thập phân; trống là không ghi
 * @param endDate ngày cuối cùng còn sở hữu; trống là đang sở hữu
 * @param documentNo số giấy chứng nhận, số hợp đồng
 */
public record OwnershipCreateRequest(@NotNull Long placeId, @NotNull Long personId,
		@DecimalMin(value = "0", inclusive = false) @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal sharePercent, @NotNull LocalDate startDate, LocalDate endDate,
		@Size(max = 100) String documentNo, @Size(max = 500) String note) {

	public OwnershipCreateRequest {
		documentNo = Text.trimToNull(documentNo);
		note = Text.trimToNull(note);
	}

}
