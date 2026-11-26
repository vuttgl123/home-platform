package com.home_platform.registry.imports;

import java.util.Map;

/**
 * Một dòng dữ liệu của một sheet.
 *
 * @param number số dòng trong Excel (dòng dữ liệu đầu là 2)
 * @param cells ô theo tiêu đề cột như trong mẫu; cột không có trong file thì không có ở đây
 */
record ImportRow(int number, Map<String, ImportCell> cells) {

	/** Ô ở cột {@code header}; cột không có trong file là ô trống. */
	ImportCell cell(String header) {
		ImportCell cell = this.cells.get(header);
		return (cell != null) ? cell : ImportCell.blank(header);
	}

}
