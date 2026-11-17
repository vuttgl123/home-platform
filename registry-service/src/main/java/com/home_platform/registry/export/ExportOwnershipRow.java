package com.home_platform.registry.export;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Một dòng sở hữu cho file Excel, kèm mã và đường dẫn {@code id} của địa điểm và tên người. Địa điểm
 * và người có thể đã xoá: dòng sở hữu đã kết thúc không mất theo họ.
 *
 * @param sharePercent {@code null} là không ghi tỷ lệ
 */
public record ExportOwnershipRow(long id, long placeId, String placePath, String placeCode, String personName,
		BigDecimal sharePercent, LocalDate startDate, LocalDate endDate, String documentNo, String note) {

	/** Mốc để đọc các dòng đứng sau dòng này. */
	StayCursor cursor() {
		return new StayCursor(this.placePath, this.startDate, this.id);
	}

}
