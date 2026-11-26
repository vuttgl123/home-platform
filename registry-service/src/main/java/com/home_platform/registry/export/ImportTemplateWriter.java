package com.home_platform.registry.export;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.home_platform.registry.catalog.CatalogStatus;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDef;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.item.ItemStatus;
import com.home_platform.registry.itemcategory.ItemCategory;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.occupancy.Relation;
import com.home_platform.registry.person.Gender;
import com.home_platform.registry.place.PlaceStatus;
import com.home_platform.registry.placetype.PlaceType;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.dhatim.fastexcel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * File mẫu để nhập: sheet "Hướng dẫn", rồi năm sheet chỉ có tiêu đề, cùng cột với file xuất cả không
 * gian (trừ cột "Đang ở", chỉ để xem). Cột mục tuỳ biến theo các mục đang dùng của không gian.
 */
@Service
public class ImportTemplateWriter {

	private static final List<String> GUIDE_HEADERS = List.of("Mục", "Cách ghi");

	/** Số dòng có danh sách chọn ở mỗi cột liệt kê; bằng giới hạn dòng của một lần nhập. */
	private static final int DROPDOWN_ROWS = 3_000;

	private final FieldDefService fieldDefs;

	private final PlaceTypeService placeTypes;

	private final ItemCategoryService categories;

	ImportTemplateWriter(FieldDefService fieldDefs, PlaceTypeService placeTypes, ItemCategoryService categories) {
		this.fieldDefs = fieldDefs;
		this.placeTypes = placeTypes;
		this.categories = categories;
	}

	@Transactional(readOnly = true)
	public void write(long workspaceId, OutputStream out) {
		List<PlaceType> types = this.placeTypes.list(workspaceId);
		List<ItemCategory> categoryList = this.categories.list(workspaceId);
		Map<Long, String> typeNames = new HashMap<>();
		types.forEach(type -> typeNames.put(type.id(), type.name()));
		Map<Long, String> categoryNames = new HashMap<>();
		categoryList.forEach(category -> categoryNames.put(category.id(), category.name()));
		List<FieldDef> defs = this.fieldDefs.listAll(workspaceId);
		AttributeColumns placeColumns = AttributeColumns.of(defs, FieldTarget.PLACE, typeNames);
		AttributeColumns personColumns = AttributeColumns.of(defs, FieldTarget.PERSON, Map.of());
		AttributeColumns itemColumns = AttributeColumns.of(defs, FieldTarget.ITEM, categoryNames);

		Workbook workbook = ExportService.workbook(out);
		SheetWriter guide = new SheetWriter(workbook, ExcelLayout.GUIDE, GUIDE_HEADERS);
		writeGuide(guide, types, categoryList, typeNames, categoryNames, placeColumns, personColumns, itemColumns);
		guide.finish();
		empty(workbook, ExcelLayout.PLACES, ExcelLayout.PLACE_HEADERS, placeColumns,
				Map.of("Trạng thái", labels(PlaceStatus.values(), ExportLabels::placeStatus)));
		List<String> personHeaders = new ArrayList<>(ExcelLayout.PERSON_HEADERS);
		personHeaders.remove(ExcelLayout.LIVING_AT);
		empty(workbook, ExcelLayout.PEOPLE, personHeaders, personColumns,
				Map.of("Giới tính", labels(Gender.values(), ExportLabels::gender)));
		List<String> relations = new ArrayList<>();
		for (Relation relation : Relation.values()) {
			if (relation != Relation.OWNER) {
				relations.add(ExportLabels.relation(relation));
			}
		}
		empty(workbook, ExcelLayout.OCCUPANCIES, ExcelLayout.OCCUPANCY_HEADERS, null, Map.of("Quan hệ", relations));
		empty(workbook, ExcelLayout.OWNERSHIPS, ExcelLayout.OWNERSHIP_HEADERS, null, Map.of());
		empty(workbook, ExcelLayout.ITEMS, ExcelLayout.ITEM_HEADERS, itemColumns,
				Map.of("Trạng thái", labels(ItemStatus.values(), ExportLabels::itemStatus)));
		ExportService.finish(workbook);
	}

	/** Một sheet chỉ có tiêu đề; mỗi cột trong {@code dropdowns} có danh sách chọn. */
	private static void empty(Workbook workbook, String name, List<String> fixed, AttributeColumns attributes,
			Map<String, List<String>> dropdowns) {
		List<String> headers = new ArrayList<>(fixed);
		if (attributes != null) {
			headers.addAll(attributes.headers());
		}
		SheetWriter sheet = new SheetWriter(workbook, name, headers);
		dropdowns.forEach((header, values) -> sheet.dropdown(headers.indexOf(header), values, DROPDOWN_ROWS));
		sheet.finish();
	}

	private static <E> List<String> labels(E[] values, Function<E, String> label) {
		return Arrays.stream(values).map(label).toList();
	}

	private static void writeGuide(SheetWriter guide, List<PlaceType> types, List<ItemCategory> categories,
			Map<Long, String> typeNames, Map<Long, String> categoryNames, AttributeColumns placeColumns,
			AttributeColumns personColumns, AttributeColumns itemColumns) {
		line(guide, "Thứ tự nhập", "Địa điểm, Người, Cư trú, Sở hữu, Đồ đạc. Một dòng có thể nhắc tới hồ sơ đã có "
				+ "hoặc một dòng khác trong file. Thứ tự dòng trong sheet Địa điểm không quan trọng.");
		line(guide, "Tất cả hoặc không",
				"Còn một dòng lỗi thì không dòng nào được nhập. Sửa file rồi chọn lại file. Cảnh báo không chặn.");
		line(guide, "Dòng 1", "Dòng 1 của mỗi sheet là tiêu đề cột, giữ đúng chữ như mẫu. Cột lạ bị bỏ qua.");
		line(guide, "Cột bắt buộc", null);
		line(guide, ExcelLayout.PLACES, "Tên, Loại");
		line(guide, ExcelLayout.PEOPLE, "Họ tên");
		line(guide, ExcelLayout.OCCUPANCIES, "Người, Từ ngày, và Địa điểm hoặc Mã địa điểm");
		line(guide, ExcelLayout.OWNERSHIPS, "Người, Từ ngày, và Địa điểm hoặc Mã địa điểm");
		line(guide, ExcelLayout.ITEMS, "Tên, Ở đâu");
		line(guide, "Mã", "Để trống thì hệ thống cấp theo mẫu mã. Ghi mã thì mã được giữ; mã không được trùng, "
				+ "kể cả với hồ sơ đã xoá.");
		line(guide, "Cách ghi địa điểm",
				"Mã của địa điểm, hoặc đường dẫn tên từ cấp cao nhất, ví dụ: Toà A / Tầng 3 / P301.");
		line(guide, "Cách ghi người", "Họ tên. Có hai người trùng tên thì ghi số điện thoại hoặc số giấy tờ.");
		line(guide, "Cách ghi ngày", "dd/mm/yyyy, ví dụ 05/03/2026, hoặc ô ngày của Excel.");

		line(guide, "Loại địa điểm đang dùng", "Ghi tên hoặc mã ở cột Loại");
		types.stream()
			.filter(type -> type.status() == CatalogStatus.ACTIVE)
			.forEach(type -> line(guide, type.name(), "mã " + type.code()));
		line(guide, "Nhóm đồ đang dùng", "Ghi tên ở cột Nhóm; trống là không nhóm");
		categories.stream()
			.filter(category -> category.status() == CatalogStatus.ACTIVE)
			.forEach(category -> line(guide, category.name(), null));
		line(guide, "Trạng thái địa điểm", "Trống là Đang dùng");
		for (PlaceStatus status : PlaceStatus.values()) {
			line(guide, ExportLabels.placeStatus(status), null);
		}
		line(guide, "Trạng thái đồ đạc", "Trống là Đang dùng; Đã gỡ cần Ngày gỡ");
		for (ItemStatus status : ItemStatus.values()) {
			line(guide, ExportLabels.itemStatus(status), null);
		}
		line(guide, "Quan hệ cư trú", "Trống là Người ở; chủ sở hữu ghi ở sheet Sở hữu");
		for (Relation relation : Relation.values()) {
			if (relation != Relation.OWNER) {
				line(guide, ExportLabels.relation(relation), null);
			}
		}
		line(guide, "Giới tính", "Trống là không ghi");
		for (Gender gender : Gender.values()) {
			line(guide, ExportLabels.gender(gender), null);
		}

		line(guide, "Mục tuỳ biến", null);
		fields(guide, ExcelLayout.PLACES, placeColumns, typeNames, "loại");
		fields(guide, ExcelLayout.PEOPLE, personColumns, Map.of(), null);
		fields(guide, ExcelLayout.ITEMS, itemColumns, categoryNames, "nhóm");
	}

	private static void fields(SheetWriter guide, String sheet, AttributeColumns columns, Map<Long, String> typeNames,
			String typeWord) {
		columns.defsByHeader().forEach((header, def) -> {
			List<String> parts = new ArrayList<>();
			parts.add("sheet " + sheet);
			parts.add(typeLabel(def.dataType()));
			if (def.dataType() == FieldDataType.SELECT && def.options() != null) {
				parts.add("chọn một trong: " + String.join(", ", def.options()));
			}
			if (def.isRequired()) {
				parts.add("bắt buộc");
			}
			if (def.typeId() != 0 && typeWord != null) {
				parts.add("chỉ cho " + typeWord + " " + typeNames.getOrDefault(def.typeId(), String.valueOf(def.typeId())));
			}
			line(guide, header, String.join(" · ", parts));
		});
	}

	private static String typeLabel(FieldDataType type) {
		return switch (type) {
			case TEXT -> "chữ";
			case NUMBER -> "số";
			case BOOLEAN -> "Có hoặc Không";
			case DATE -> "ngày";
			case SELECT -> "một lựa chọn";
		};
	}

	private static void line(SheetWriter guide, String item, String how) {
		guide.text(item);
		guide.text(how);
		guide.nextRow();
	}

}
