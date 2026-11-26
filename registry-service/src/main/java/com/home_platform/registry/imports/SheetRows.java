package com.home_platform.registry.imports;

import java.util.List;

/** Nhập các dòng của một loại sheet. */
interface SheetRows {

	SheetKind kind();

	/** @return kết quả của mọi dòng, theo thứ tự trong file */
	List<RowReport> importAll(ImportSheet sheet, ImportRun run);

}
