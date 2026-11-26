package com.home_platform.registry.person;

import java.util.List;

import com.home_platform.registry.search.SqlCondition;

/**
 * Bộ lọc của danh sách người (spec Đợt 11, S4); mọi điều kiện cùng đúng.
 * @param text mẫu "chứa" từ {@code TextSearch.pattern}; {@code null} là không tìm chữ
 * @param living {@code true}: có một dòng cư trú hôm nay; {@code false}: không có; {@code null}: mọi người
 * @param rootPath đang ở tại địa điểm có {@code path} này hay một nơi bên trong nó; khác {@code null} thì
 * {@code living} không được là {@code false}
 */
public record PersonFilter(String text, Boolean living, String rootPath, List<SqlCondition> attributes) {

	public PersonFilter {
		attributes = List.copyOf(attributes);
	}

}
