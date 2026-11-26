package com.home_platform.registry.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.registry.export.XlsxReader;
import com.home_platform.registry.export.XlsxReader.XlsxCell;
import com.home_platform.registry.export.XlsxReader.XlsxSheet;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.item.Item;
import com.home_platform.registry.item.ItemRemoveRequest;
import com.home_platform.registry.item.ItemRequest;
import com.home_platform.registry.item.ItemService;
import com.home_platform.registry.item.ItemStatus;
import com.home_platform.registry.itemcategory.ItemCategoryRequest;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.occupancy.OccupancyCreateRequest;
import com.home_platform.registry.occupancy.OccupancyService;
import com.home_platform.registry.occupancy.Relation;
import com.home_platform.registry.ownership.OwnershipCreateRequest;
import com.home_platform.registry.ownership.OwnershipService;
import com.home_platform.registry.person.Gender;
import com.home_platform.registry.person.PersonRequest;
import com.home_platform.registry.person.PersonService;
import com.home_platform.registry.place.PlaceCreateRequest;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.place.PlaceStatus;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** File xuất cả không gian là một file nhập hợp lệ (spec Đợt 9, mục 5): xuất, nhập, xuất lại ra cùng file. */
class ImportRoundTripIT extends ImportTestData {

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	ItemCategoryService categories;

	@Autowired
	FieldDefService fieldDefs;

	@Autowired
	PlaceService places;

	@Autowired
	PersonService people;

	@Autowired
	OccupancyService occupancies;

	@Autowired
	OwnershipService ownerships;

	@Autowired
	ItemService items;

	@Test
	void exportImportExportIsTheSameFile() throws Exception {
		Catalog a = catalog(CTX_A);
		catalog(CTX_B);
		fill(a);

		byte[] first = export(asA(get("/exports/workspace.xlsx")));
		String report = report(commitToB(first));
		byte[] second = export(asB(get("/exports/workspace.xlsx")));

		assertThat((Boolean) read(report, "$.committed")).as(report).isTrue();
		List<XlsxSheet> before = XlsxReader.read(first);
		List<XlsxSheet> after = XlsxReader.read(second);
		assertThat(after).extracting(XlsxSheet::name).isEqualTo(before.stream().map(XlsxSheet::name).toList());
		for (int sheet = 0; sheet < before.size(); sheet++) {
			assertThat(texts(after.get(sheet))).as(before.get(sheet).name()).isEqualTo(texts(before.get(sheet)));
			assertThat(before.get(sheet).dataRowCount()).isPositive();
		}
	}

	private Catalog catalog(WorkspaceContext ctx) {
		long building = this.placeTypes.create(ctx, new PlaceTypeCreateRequest("TOA", "Toà", null, 1)).id();
		long room = this.placeTypes.create(ctx, new PlaceTypeCreateRequest("PHONG", "Phòng", null, 2)).id();
		long appliances = this.categories.create(ctx, new ItemCategoryRequest("Điện máy", 1)).id();
		this.fieldDefs.create(ctx, new FieldDefCreateRequest(FieldTarget.PLACE, room, "area", "Diện tích",
				FieldDataType.NUMBER, null, null, 1, null));
		this.fieldDefs.create(ctx, new FieldDefCreateRequest(FieldTarget.PERSON, null, "home_town", "Quê quán",
				FieldDataType.TEXT, null, null, 1, null));
		this.fieldDefs.create(ctx, new FieldDefCreateRequest(FieldTarget.ITEM, appliances, "power", "Công suất",
				FieldDataType.NUMBER, null, null, 1, null));
		return new Catalog(building, room, appliances);
	}

	private void fill(Catalog c) {
		long a = place(null, c.building(), "Toà A", null, null, Map.of());
		long floor = place(a, c.building(), "Tầng 1", null, null, Map.of());
		long p101 = place(floor, c.room(), "P101", 2, null, Map.of("area", 25));
		long p102 = place(floor, c.room(), "P102", null, PlaceStatus.INACTIVE, Map.of());
		long an = this.people.create(CTX_A, new PersonRequest("Nguyễn An", "0901234567", "an@example.com", "CCCD", "001",
				LocalDate.of(1990, 5, 1), Gender.MALE, "Thuê dài hạn", Map.of(), null, null)).id();
		long binh = this.people.create(CTX_A, new PersonRequest("Trần Bình", null, null, null, null, null, null, null,
				Map.of("home_town", "Huế"), null, null)).id();
		long cuc = this.people.create(CTX_A,
				new PersonRequest("Lê Cúc", null, null, null, null, null, Gender.FEMALE, null, Map.of(), null, null)).id();
		this.occupancies.create(CTX_A,
				new OccupancyCreateRequest(p101, an, Relation.RESIDENT, LocalDate.of(2026, 1, 1), null, "Hợp đồng 1 năm"));
		this.occupancies.create(CTX_A, new OccupancyCreateRequest(p101, binh, Relation.GUEST, LocalDate.of(2025, 1, 1),
				LocalDate.of(2025, 6, 30), null));
		this.ownerships.create(CTX_A, new OwnershipCreateRequest(p102, cuc, new BigDecimal("100"),
				LocalDate.of(2024, 1, 1), null, "GCN-9", null));
		this.items.create(CTX_A, new ItemRequest(p101, an, c.appliances(), "Tủ lạnh", 2, "cái", ItemStatus.IN_USE,
				LocalDate.of(2026, 1, 5), LocalDate.of(2027, 1, 5), 6, "Ngăn đá", Map.of("power", 150), null, null));
		this.items.create(CTX_A, new ItemRequest(floor, null, null, "Quạt", null, null, ItemStatus.BROKEN, null, null,
				null, null, Map.of(), null, null));
		Item washer = this.items.create(CTX_A, new ItemRequest(p102, null, c.appliances(), "Máy giặt", null, null, null,
				LocalDate.of(2025, 1, 1), null, null, null, Map.of(), null, null));
		this.items.remove(CTX_A, washer.id(), new ItemRemoveRequest(washer.version(), LocalDate.of(2026, 10, 10), null));
	}

	private long place(Long parentId, long typeId, String name, Integer capacity, PlaceStatus status,
			Map<String, Object> attributes) {
		return this.places
			.create(CTX_A,
					new PlaceCreateRequest(parentId, typeId, name, null, status, capacity, attributes, null, null, null))
			.id();
	}

	private byte[] export(MockHttpServletRequestBuilder request) throws Exception {
		return this.mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
	}

	private static List<List<String>> texts(XlsxSheet sheet) {
		return sheet.rows().stream().map(row -> row.stream().map(XlsxCell::text).toList()).toList();
	}

	private record Catalog(long building, long room, long appliances) {
	}

}
