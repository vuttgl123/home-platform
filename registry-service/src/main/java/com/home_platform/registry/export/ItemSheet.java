package com.home_platform.registry.export;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dhatim.fastexcel.Workbook;

/** Sheet "Đồ đạc": mỗi món một dòng, cột cố định rồi tới cột mục tuỳ biến của đồ đạc. */
final class ItemSheet {

	static final String NAME = ExcelLayout.ITEMS;

	private static final List<String> HEADERS = ExcelLayout.ITEM_HEADERS;

	private final SheetWriter writer;

	private final PlacePaths paths;

	private final Map<Long, String> categoryNames;

	private final AttributeColumns attributes;

	ItemSheet(Workbook workbook, PlacePaths paths, Map<Long, String> categoryNames, AttributeColumns attributes) {
		List<String> headers = new ArrayList<>(HEADERS);
		headers.addAll(attributes.headers());
		this.writer = new SheetWriter(workbook, NAME, headers);
		this.paths = paths;
		this.categoryNames = categoryNames;
		this.attributes = attributes;
	}

	void write(ExportItemRow item) {
		this.writer.text(item.code());
		this.writer.text(item.name());
		this.writer.text((item.categoryId() != null) ? this.categoryNames.get(item.categoryId()) : null);
		this.writer.text(this.paths.pathOf(item.placeId()));
		this.writer.number(item.quantity());
		this.writer.text(item.unit());
		this.writer.text(ExportLabels.itemStatus(item.status()));
		this.writer.date(item.placedAt());
		this.writer.date(item.removedAt());
		this.writer.date(item.warrantyUntil());
		this.writer.number(item.serviceEveryMonths());
		this.writer.text(item.ownerName());
		this.writer.text(item.description());
		this.attributes.write(this.writer, item.categoryId(), item.attributes());
		this.writer.nextRow();
	}

	void finish() {
		this.writer.finish();
	}

}
