package com.home_platform.registry.ownership;

import java.time.LocalDate;

import com.home_platform.registry.support.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Chuyển nhượng một dòng đang sở hữu sang người khác.
 *
 * @param transferDate ngày người nhận bắt đầu sở hữu; trống là hôm nay. Chủ cũ thôi từ hết ngày hôm trước.
 * @param documentNo số giấy tờ của dòng mới
 */
public record OwnershipTransferRequest(@NotNull Integer version, @NotNull Long toPersonId, LocalDate transferDate,
		@Size(max = 100) String documentNo, @Size(max = 500) String note) {

	public OwnershipTransferRequest {
		documentNo = Text.trimToNull(documentNo);
		note = Text.trimToNull(note);
	}

}
