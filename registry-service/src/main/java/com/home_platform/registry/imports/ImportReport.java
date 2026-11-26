package com.home_platform.registry.imports;

import java.util.List;

/**
 * Kết quả một lần xem trước hay nhập (spec Đợt 9, mục 4.2). Chữ do backend viết bằng tiếng Việt,
 * như nhãn của file xuất.
 *
 * @param committed {@code true} chỉ khi dữ liệu đã được lưu
 * @param fileErrors lỗi của cả file; có lỗi thì {@code sheets} rỗng
 * @param notices ghi chú không chặn: sheet lạ, cột lạ bị bỏ qua
 * @param sheets các sheet có dòng, theo thứ tự nhập
 */
public record ImportReport(String fileName, boolean committed, List<String> fileErrors, List<String> notices,
		Totals totals, List<SheetReport> sheets) {

	/**
	 * @param errors số dòng có lỗi
	 * @param warnings số dòng chỉ có cảnh báo
	 */
	public record Totals(int rows, int errors, int warnings) {
	}

	boolean hasErrors() {
		return !this.fileErrors.isEmpty() || this.totals.errors() > 0;
	}

	ImportReport asCommitted() {
		return new ImportReport(this.fileName, true, this.fileErrors, this.notices, this.totals, this.sheets);
	}

}
