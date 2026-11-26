package com.home_platform.registry.export;

import java.util.List;

import org.dhatim.fastexcel.Workbook;

/** Sheet "Cư trú": mọi dòng cư trú chưa xoá, cả dòng đã kết thúc. */
final class OccupancySheet {

	static final String NAME = ExcelLayout.OCCUPANCIES;

	private static final List<String> HEADERS = ExcelLayout.OCCUPANCY_HEADERS;

	private final SheetWriter writer;

	private final PlacePaths paths;

	OccupancySheet(Workbook workbook, PlacePaths paths) {
		this.writer = new SheetWriter(workbook, NAME, HEADERS);
		this.paths = paths;
	}

	void write(ExportOccupancyRow stay) {
		this.writer.text(this.paths.pathOf(stay.placeId()));
		this.writer.text(stay.placeCode());
		this.writer.text(stay.personName());
		this.writer.text(ExportLabels.relation(stay.relation()));
		this.writer.date(stay.startDate());
		this.writer.date(stay.endDate());
		this.writer.text(stay.note());
		this.writer.nextRow();
	}

	void finish() {
		this.writer.finish();
	}

}
