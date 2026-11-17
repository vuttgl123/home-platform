package com.home_platform.registry.export;

import java.util.List;

import org.dhatim.fastexcel.Workbook;

/** Sheet "Sở hữu": mọi dòng sở hữu chưa xoá, cả dòng đã kết thúc. */
final class OwnershipSheet {

	static final String NAME = "Sở hữu";

	private static final List<String> HEADERS = List.of("Địa điểm", "Mã địa điểm", "Người", "Tỷ lệ (%)", "Từ ngày",
			"Đến ngày", "Số giấy tờ", "Ghi chú");

	private final SheetWriter writer;

	private final PlacePaths paths;

	OwnershipSheet(Workbook workbook, PlacePaths paths) {
		this.writer = new SheetWriter(workbook, NAME, HEADERS);
		this.paths = paths;
	}

	void write(ExportOwnershipRow ownership) {
		this.writer.text(this.paths.pathOf(ownership.placeId()));
		this.writer.text(ownership.placeCode());
		this.writer.text(ownership.personName());
		this.writer.number(ownership.sharePercent());
		this.writer.date(ownership.startDate());
		this.writer.date(ownership.endDate());
		this.writer.text(ownership.documentNo());
		this.writer.text(ownership.note());
		this.writer.nextRow();
	}

	void finish() {
		this.writer.finish();
	}

}
