package com.home_platform.registry.imports;

import java.util.List;

import com.home_platform.registry.export.ExcelLayout;

/** Năm sheet nhập được, theo thứ tự nhập: hồ sơ được nhắc tới phải có trước dòng nhắc tới nó. */
public enum SheetKind {

	PLACE(ExcelLayout.PLACES, ExcelLayout.PLACE_HEADERS, List.of(List.of("Tên"), List.of("Loại"))),

	PERSON(ExcelLayout.PEOPLE, ExcelLayout.PERSON_HEADERS, List.of(List.of("Họ tên"))),

	OCCUPANCY(ExcelLayout.OCCUPANCIES, ExcelLayout.OCCUPANCY_HEADERS,
			List.of(List.of("Địa điểm", "Mã địa điểm"), List.of("Người"), List.of("Từ ngày"))),

	OWNERSHIP(ExcelLayout.OWNERSHIPS, ExcelLayout.OWNERSHIP_HEADERS,
			List.of(List.of("Địa điểm", "Mã địa điểm"), List.of("Người"), List.of("Từ ngày"))),

	ITEM(ExcelLayout.ITEMS, ExcelLayout.ITEM_HEADERS, List.of(List.of("Tên"), List.of("Ở đâu")));

	private final String sheetName;

	private final List<String> headers;

	private final List<List<String>> required;

	SheetKind(String sheetName, List<String> headers, List<List<String>> required) {
		this.sheetName = sheetName;
		this.headers = headers;
		this.required = required;
	}

	public String sheetName() {
		return this.sheetName;
	}

	/** Tiêu đề cột cố định, như file xuất. */
	List<String> headers() {
		return this.headers;
	}

	/**
	 * Cột bắt buộc khi sheet có dòng: mỗi nhóm cần ít nhất một cột của nó (Cư trú và Sở hữu cần
	 * "Địa điểm" hoặc "Mã địa điểm").
	 */
	List<List<String>> required() {
		return this.required;
	}

}
