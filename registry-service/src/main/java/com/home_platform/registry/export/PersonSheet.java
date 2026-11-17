package com.home_platform.registry.export;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.dhatim.fastexcel.Workbook;

/** Sheet "Người": mỗi người một dòng, cột cố định rồi tới cột mục tuỳ biến của người. */
final class PersonSheet {

	static final String NAME = "Người";

	/** Ngăn các nơi trong cột "Đang ở". */
	static final String PLACE_SEPARATOR = "; ";

	private static final List<String> HEADERS = List.of("Họ tên", "Điện thoại", "Email", "Loại giấy tờ", "Số giấy tờ",
			"Ngày sinh", "Giới tính", "Đang ở", "Ghi chú");

	private final SheetWriter writer;

	private final PlacePaths paths;

	private final AttributeColumns attributes;

	PersonSheet(Workbook workbook, PlacePaths paths, AttributeColumns attributes) {
		List<String> headers = new ArrayList<>(HEADERS);
		headers.addAll(attributes.headers());
		this.writer = new SheetWriter(workbook, NAME, headers);
		this.paths = paths;
		this.attributes = attributes;
	}

	/** @param livingAt {@code id} các địa điểm người này đang ở hôm nay */
	void write(ExportPersonRow person, List<Long> livingAt) {
		this.writer.text(person.fullName());
		this.writer.text(person.phone());
		this.writer.text(person.email());
		this.writer.text(person.idType());
		this.writer.text(person.idNumber());
		this.writer.date(person.dateOfBirth());
		this.writer.text(ExportLabels.gender(person.gender()));
		this.writer.text(livingAt.stream()
			.map(this.paths::pathOf)
			.distinct()
			.sorted()
			.collect(Collectors.joining(PLACE_SEPARATOR)));
		this.writer.text(person.note());
		this.attributes.write(this.writer, null, person.attributes());
		this.writer.nextRow();
	}

	void finish() {
		this.writer.finish();
	}

}
