package com.home_platform.registry.export;

import java.time.LocalDate;

import com.home_platform.registry.occupancy.Relation;

/**
 * Một dòng cư trú cho file Excel, kèm mã và đường dẫn {@code id} của địa điểm và tên người. Địa điểm
 * và người có thể đã xoá: dòng cư trú đã kết thúc không mất theo họ.
 */
public record ExportOccupancyRow(long id, long placeId, String placePath, String placeCode, String personName,
		Relation relation, LocalDate startDate, LocalDate endDate, String note) {

	/** Mốc để đọc các dòng đứng sau dòng này. */
	StayCursor cursor() {
		return new StayCursor(this.placePath, this.startDate, this.id);
	}

}
