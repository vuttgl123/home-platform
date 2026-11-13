package com.home_platform.registry.setup;

import java.util.List;

/** Dữ liệu mặc định của một không gian mới, theo thứ tự hiển thị. */
final class DefaultCatalog {

	/** Mã và tên của các loại địa điểm mặc định. */
	static final List<String[]> PLACE_TYPES = List.of(new String[] { "BUILDING", "Toà nhà" },
			new String[] { "FLOOR", "Tầng" }, new String[] { "ROOM", "Phòng" },
			new String[] { "APARTMENT", "Căn hộ" }, new String[] { "HOUSE", "Nhà ở" },
			new String[] { "HOTEL", "Khách sạn" }, new String[] { "OTHER", "Khác" });

	static final List<String> ITEM_CATEGORIES = List.of("Nội thất", "Điện tử", "Điện lạnh", "Thiết bị vệ sinh",
			"Đồ gia dụng", "Khác");

	/** Mục tuỳ biến mẫu: mã loại địa điểm, khoá, nhãn; đều là NUMBER, không bắt buộc. */
	static final List<String[]> PLACE_FIELDS = List.of(new String[] { "HOTEL", "star_rating", "Số sao" },
			new String[] { "APARTMENT", "area_m2", "Diện tích (m²)" });

	/** Khoảng cách giữa hai giá trị sortOrder liên tiếp, để người dùng chèn vào giữa. */
	static final int SORT_STEP = 10;

	private DefaultCatalog() {
	}

}
