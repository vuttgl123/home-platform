package com.home_platform.registry.place;

import java.util.List;

import com.home_platform.registry.search.SqlCondition;

/**
 * Bộ lọc của bảng địa điểm (spec Đợt 11, S3); mọi điều kiện cùng đúng.
 * @param text mẫu "chứa" từ {@code TextSearch.pattern}; {@code null} là không tìm chữ
 * @param rootPath chỉ các hậu duệ (không gồm gốc) của địa điểm có {@code path} này
 * @param hasRoom chỉ địa điểm có sức chứa và số người đang ở hôm nay nhỏ hơn
 */
public record PlaceFilter(String text, Long placeTypeId, PlaceStatus status, String rootPath, boolean hasRoom,
		List<SqlCondition> attributes) {

	public PlaceFilter {
		attributes = List.copyOf(attributes);
	}

}
