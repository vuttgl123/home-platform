package com.home_platform.registry.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import com.home_platform.registry.RegistryIT;
import com.home_platform.registry.export.XlsxReader;
import com.home_platform.registry.export.XlsxReader.XlsxCell;
import com.home_platform.registry.export.XlsxReader.XlsxSheet;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.itemcategory.ItemCategoryRequest;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ImportTemplateIT extends RegistryIT {

	private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	ItemCategoryService categories;

	@Autowired
	FieldDefService fieldDefs;

	@Test
	void templateHasGuideThenFiveSheets() throws Exception {
		List<XlsxSheet> sheets = template();

		assertThat(sheets).extracting(XlsxSheet::name)
			.containsExactly("Hướng dẫn", "Địa điểm", "Người", "Cư trú", "Sở hữu", "Đồ đạc");
		assertThat(sheets.subList(1, 6)).allSatisfy(sheet -> assertThat(sheet.dataRowCount()).isZero());
	}

	@Test
	void templateHeadersAreTheExportHeadersWithoutLivingAt() throws Exception {
		long room = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("PHONG", "Phòng", null, 1)).id();
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.PLACE, room, "area", "Diện tích",
				FieldDataType.NUMBER, null, null, 1, null));
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.PERSON, null, "home_town", "Quê quán",
				FieldDataType.TEXT, null, null, 1, null));

		List<XlsxSheet> template = template();
		List<XlsxSheet> export = XlsxReader.read(download("/exports/workspace.xlsx"));

		assertThat(template.get(1).headers()).isEqualTo(export.get(0).headers()).contains("Diện tích");
		List<String> people = new ArrayList<>(export.get(1).headers());
		people.remove("Đang ở");
		assertThat(template.get(2).headers()).isEqualTo(people).contains("Quê quán");
		for (int sheet = 2; sheet < 5; sheet++) {
			assertThat(template.get(sheet + 1).headers()).isEqualTo(export.get(sheet).headers());
		}
	}

	@Test
	void guideListsActiveTypesAndCategories() throws Exception {
		this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("PHONG", "Phòng", null, 1));
		long store = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("KHO", "Kho", null, 2)).id();
		this.placeTypes.archive(CTX_A, store);
		this.categories.create(CTX_A, new ItemCategoryRequest("Điện máy", 1));

		List<String> texts = texts(template().get(0));

		assertThat(texts).contains("Phòng", "Điện máy", "Đang dùng", "Tạm ngưng", "Cất kho", "Người ở")
			.doesNotContain("Kho", "Chủ");
	}

	@Test
	void listColumnsOfferADropdown() throws Exception {
		byte[] file = download("/imports/template.xlsx");

		String places = XlsxReader.sheetXml(file, 2);
		assertThat(places).contains("<dataValidation").contains("Đang dùng,Tạm ngưng");
		String stays = XlsxReader.sheetXml(file, 4);
		assertThat(stays).contains("Người ở,Khách,Nhân viên,Khác").doesNotContain("Chủ,");
	}

	private List<XlsxSheet> template() throws Exception {
		return XlsxReader.read(download("/imports/template.xlsx"));
	}

	private byte[] download(String path) throws Exception {
		return this.mvc.perform(asA(get(path)))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", XLSX))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();
	}

	private static List<String> texts(XlsxSheet sheet) {
		return sheet.rows().stream().flatMap(List::stream).map(XlsxCell::text).toList();
	}

}
