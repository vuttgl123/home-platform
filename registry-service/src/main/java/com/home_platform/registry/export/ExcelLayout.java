package com.home_platform.registry.export;

import java.util.List;

/**
 * Bố cục của file Excel: tên sheet và tiêu đề cột cố định. File xuất, file mẫu nhập và lớp đọc file
 * nhập cùng lấy từ đây, nên ba thứ không lệch nhau. Cột mục tuỳ biến đứng sau các cột này
 * ({@link AttributeColumns}).
 */
public final class ExcelLayout {

	public static final String PLACES = "Địa điểm";

	public static final String PEOPLE = "Người";

	public static final String OCCUPANCIES = "Cư trú";

	public static final String OWNERSHIPS = "Sở hữu";

	public static final String ITEMS = "Đồ đạc";

	/** Sheet đầu của file mẫu nhập; khi nhập thì bỏ qua. */
	public static final String GUIDE = "Hướng dẫn";

	/** Cột của sheet Người chỉ để xem: nơi người đó đang ở hôm nay, tính từ cư trú. */
	public static final String LIVING_AT = "Đang ở";

	public static final List<String> PLACE_HEADERS = List.of("Mã", "Tên", "Loại", "Sức chứa", "Nằm trong",
			"Trạng thái", "Mô tả");

	public static final List<String> PERSON_HEADERS = List.of("Họ tên", "Điện thoại", "Email", "Loại giấy tờ",
			"Số giấy tờ", "Ngày sinh", "Giới tính", LIVING_AT, "Ghi chú");

	public static final List<String> OCCUPANCY_HEADERS = List.of("Địa điểm", "Mã địa điểm", "Người", "Quan hệ",
			"Từ ngày", "Đến ngày", "Ghi chú");

	public static final List<String> OWNERSHIP_HEADERS = List.of("Địa điểm", "Mã địa điểm", "Người", "Tỷ lệ (%)",
			"Từ ngày", "Đến ngày", "Số giấy tờ", "Ghi chú");

	public static final List<String> ITEM_HEADERS = List.of("Mã", "Tên", "Nhóm", "Ở đâu", "Số lượng", "Đơn vị",
			"Trạng thái", "Ngày đặt", "Ngày gỡ", "Hết bảo hành", "Bảo trì (tháng)", "Chủ", "Mô tả");

	private ExcelLayout() {
	}

}
