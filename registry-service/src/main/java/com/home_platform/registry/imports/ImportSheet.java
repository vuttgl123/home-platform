package com.home_platform.registry.imports;

import java.util.List;
import java.util.Set;

/**
 * Một sheet đã đọc, có ít nhất một dòng.
 *
 * @param name tên sheet như trong mẫu
 * @param columns tiêu đề (như trong mẫu) của các cột có trong file
 */
record ImportSheet(SheetKind kind, String name, List<ImportRow> rows, Set<String> columns) {
}
