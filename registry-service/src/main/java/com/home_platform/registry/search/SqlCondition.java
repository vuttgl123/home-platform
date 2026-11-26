package com.home_platform.registry.search;

import java.util.Map;

/**
 * Một đoạn điều kiện SQL cùng giá trị của các tham số có tên trong nó, để đưa vào
 * {@code KeysetQuery.where}. Không bao giờ chứa chữ của người dùng ngoài tham số.
 */
public record SqlCondition(String sql, Map<String, Object> params) {

	public SqlCondition {
		params = Map.copyOf(params);
	}

}
