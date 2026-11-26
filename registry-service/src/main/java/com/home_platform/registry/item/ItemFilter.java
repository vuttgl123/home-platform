package com.home_platform.registry.item;

import java.util.List;

import com.home_platform.registry.search.SqlCondition;

/**
 * Bộ lọc của danh sách đồ cả không gian (spec Đợt 11, S5); mọi điều kiện cùng đúng.
 * @param status {@code null} là mọi món chưa gỡ
 * @param text mẫu "chứa" từ {@code TextSearch.pattern}; {@code null} là không tìm chữ
 * @param rootPath món nằm tại địa điểm có {@code path} này hay một nơi bên trong nó
 */
public record ItemFilter(ItemStatus status, Long categoryId, String text, String rootPath,
		List<SqlCondition> attributes) {

	public ItemFilter {
		attributes = List.copyOf(attributes);
	}

}
