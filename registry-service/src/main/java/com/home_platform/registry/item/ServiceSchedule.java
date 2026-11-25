package com.home_platform.registry.item;

import java.time.LocalDate;

/** Hạn bảo trì kế tiếp của một món (spec Đợt 8, L3); dùng chung cho trang món đồ và Việc cần làm. */
public final class ServiceSchedule {

	private ServiceSchedule() {
	}

	/**
	 * Lần bảo trì gần nhất (không có thì ngày đặt, không có nữa thì ngày tạo) cộng {@code everyMonths}
	 * tháng. Cộng tháng rơi vào ngày không có (31/01 + 1 tháng) thì lấy ngày cuối của tháng đó.
	 * @param lastServiceOn ngày của dòng {@code SERVICE} chưa xoá muộn nhất; chỉ bảo trì định kỳ làm lùi
	 * hạn, một lần sửa chữa thì không
	 * @return {@code null} khi chưa đặt chu kỳ
	 */
	public static LocalDate nextServiceOn(LocalDate lastServiceOn, LocalDate placedAt, LocalDate createdOn,
			Integer everyMonths) {
		if (everyMonths == null) {
			return null;
		}
		LocalDate from = (lastServiceOn != null) ? lastServiceOn : (placedAt != null) ? placedAt : createdOn;
		return from.plusMonths(everyMonths);
	}

}
