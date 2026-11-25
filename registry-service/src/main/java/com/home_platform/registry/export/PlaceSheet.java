package com.home_platform.registry.export;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dhatim.fastexcel.Workbook;

/** Sheet "Địa điểm": mỗi địa điểm một dòng theo thứ tự của cây, cột cố định rồi tới cột mục tuỳ biến. */
final class PlaceSheet {

	static final String NAME = "Địa điểm";

	private static final List<String> HEADERS = List.of("Mã", "Tên", "Loại", "Sức chứa", "Nằm trong", "Trạng thái",
			"Mô tả");

	private final SheetWriter writer;

	private final PlacePaths paths;

	private final Map<Long, String> typeNames;

	private final AttributeColumns attributes;

	PlaceSheet(Workbook workbook, PlacePaths paths, Map<Long, String> typeNames, AttributeColumns attributes) {
		List<String> headers = new ArrayList<>(HEADERS);
		headers.addAll(attributes.headers());
		this.writer = new SheetWriter(workbook, NAME, headers);
		this.paths = paths;
		this.typeNames = typeNames;
		this.attributes = attributes;
	}

	void write(ExportPlaceRow place) {
		this.writer.text(place.code());
		this.writer.text(place.name());
		this.writer.text(this.typeNames.get(place.placeTypeId()));
		this.writer.number(place.capacity());
		this.writer.text(this.paths.parentPathOf(place.id()));
		this.writer.text(ExportLabels.placeStatus(place.status()));
		this.writer.text(place.description());
		this.attributes.write(this.writer, place.placeTypeId(), place.attributes());
		this.writer.nextRow();
	}

	void finish() {
		this.writer.finish();
	}

}
