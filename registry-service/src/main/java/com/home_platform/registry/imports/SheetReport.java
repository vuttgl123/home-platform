package com.home_platform.registry.imports;

import java.util.List;

/**
 * Kết quả của một sheet.
 *
 * @param name tên sheet như trong mẫu
 * @param errors số dòng có lỗi
 * @param warnings số dòng chỉ có cảnh báo
 * @param items mọi dòng đã đọc, theo thứ tự trong file
 */
public record SheetReport(SheetKind sheet, String name, int rows, int errors, int warnings, List<RowReport> items) {
}
