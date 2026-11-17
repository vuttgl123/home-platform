package com.home_platform.registry.export;

import com.home_platform.registry.item.ItemStatus;

/** Nhãn tiếng Việt của các giá trị liệt kê trong file Excel; trùng với nhãn trên giao diện. */
final class ExportLabels {

	private ExportLabels() {
	}

	static String itemStatus(ItemStatus status) {
		return switch (status) {
			case IN_USE -> "Đang dùng";
			case BROKEN -> "Hỏng";
			case STORED -> "Cất kho";
			case REMOVED -> "Đã gỡ";
		};
	}

	static String yesNo(boolean value) {
		return value ? "Có" : "Không";
	}

}
