package com.home_platform.registry.search;

/**
 * Một điều kiện {@code khoá:phép:giá trị} đã tách, chưa kiểm theo mục tuỳ biến.
 * @param value chữ sau dấu {@code :} thứ hai, nguyên vẹn; rỗng khi phép không cần giá trị
 */
public record AttributeCondition(String key, AttributeOp op, String value) {
}
