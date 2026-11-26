package com.home_platform.registry.imports;

import java.util.List;

/**
 * Cả file đã đọc.
 *
 * @param sheets các sheet có dòng, theo thứ tự nhập ({@link SheetKind})
 * @param notices ghi chú không chặn: sheet lạ, cột lạ bị bỏ qua
 */
record ImportWorkbook(List<ImportSheet> sheets, List<String> notices) {
}
