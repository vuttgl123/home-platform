package com.home_platform.registry.export;

import com.home_platform.registry.item.ItemStatus;
import com.home_platform.registry.occupancy.Relation;
import com.home_platform.registry.person.Gender;
import com.home_platform.registry.place.PlaceStatus;

/** Nhãn tiếng Việt của các giá trị liệt kê trong file Excel; trùng với nhãn trên giao diện. */
public final class ExportLabels {

	private ExportLabels() {
	}

	public static String itemStatus(ItemStatus status) {
		return switch (status) {
			case IN_USE -> "Đang dùng";
			case BROKEN -> "Hỏng";
			case STORED -> "Cất kho";
			case REMOVED -> "Đã gỡ";
		};
	}

	public static String placeStatus(PlaceStatus status) {
		return switch (status) {
			case ACTIVE -> "Đang dùng";
			case INACTIVE -> "Tạm ngưng";
		};
	}

	public static String relation(Relation relation) {
		return switch (relation) {
			case OWNER -> "Chủ";
			case RESIDENT -> "Người ở";
			case GUEST -> "Khách";
			case STAFF -> "Nhân viên";
			case OTHER -> "Khác";
		};
	}

	/** {@code null} (không ghi giới tính) vẫn là {@code null}, để ô trống. */
	public static String gender(Gender gender) {
		if (gender == null) {
			return null;
		}
		return switch (gender) {
			case MALE -> "Nam";
			case FEMALE -> "Nữ";
			case OTHER -> "Khác";
		};
	}

	public static String yesNo(boolean value) {
		return value ? "Có" : "Không";
	}

}
