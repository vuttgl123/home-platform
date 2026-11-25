package com.home_platform.registry.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import com.home_platform.registry.RegistryIT;
import com.home_platform.registry.export.XlsxReader.XlsxSheet;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.itemcategory.ItemCategoryRequest;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.person.PersonRequest;
import com.home_platform.registry.person.PersonService;
import com.home_platform.registry.place.PlaceCreateRequest;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import com.home_platform.registry.support.BusinessDate;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class ExportItemsIT extends RegistryIT {

	private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	PlaceService places;

	@Autowired
	PersonService people;

	@Autowired
	ItemCategoryService categories;

	@Autowired
	FieldDefService fieldDefs;

	@Autowired
	BusinessDate businessDate;

	long type;

	@BeforeEach
	void placeType() {
		this.type = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("T", "T", null, null)).id();
	}

	@Test
	void itemsSheetHasHeadersAndRows() throws Exception {
		long building = place(null, "Toà A");
		long room = place(place(building, "Tầng 3"), "P301");
		long cooling = this.categories.create(CTX_A, new ItemCategoryRequest("Điện lạnh", null)).id();
		long owner = this.people
			.create(CTX_A, new PersonRequest("Nguyễn An", null, null, null, null, null, null, null, null, null, null))
			.id();
		this.fieldDefs.create(CTX_A,
				new FieldDefCreateRequest(FieldTarget.ITEM, null, "brand", "Hãng", FieldDataType.TEXT, null, null, 1));
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.ITEM, cooling, "power", "Công suất",
				FieldDataType.NUMBER, null, null, 2));
		String fridge = this.mvc.perform(asA(post("/items")).contentType(MediaType.APPLICATION_JSON).content("""
				{"placeId": "%d", "ownerPersonId": "%d", "categoryId": "%d", "name": "Tủ lạnh", "quantity": 2,
				 "unit": "cái", "status": "BROKEN", "placedAt": "2025-03-09", "description": "Hai cánh",
				 "attributes": {"brand": "LG", "power": 150}}""".formatted(room, owner, cooling)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		item(room, "Bàn", "IN_USE", null);
		item(building, "Ấm", "IN_USE", null);

		List<XlsxSheet> sheets = XlsxReader.read(export(get("/exports/items.xlsx")));

		assertThat(sheets).hasSize(1);
		XlsxSheet sheet = sheets.get(0);
		assertThat(sheet.name()).isEqualTo("Đồ đạc");
		assertThat(sheet.headers()).containsExactly("Mã", "Tên", "Nhóm", "Ở đâu", "Số lượng", "Đơn vị", "Trạng thái",
				"Ngày đặt", "Ngày gỡ", "Hết bảo hành", "Bảo trì (tháng)", "Chủ", "Mô tả", "Hãng", "Công suất");
		assertThat(sheet.column("Tên")).containsExactly("Ấm", "Bàn", "Tủ lạnh");
		assertThat(sheet.column("Ở đâu")).containsExactly("Toà A", "Toà A / Tầng 3 / P301", "Toà A / Tầng 3 / P301");
		assertThat(sheet.cell(2, "Mã").text()).isEqualTo(JsonPath.<String>read(fridge, "$.code")).isNotEmpty();
		assertThat(sheet.cell(2, "Nhóm").text()).isEqualTo("Điện lạnh");
		assertThat(sheet.cell(2, "Số lượng").isNumber()).isTrue();
		assertThat(sheet.cell(2, "Số lượng").number()).isEqualByComparingTo("2");
		assertThat(sheet.cell(2, "Đơn vị").text()).isEqualTo("cái");
		assertThat(sheet.cell(2, "Trạng thái").text()).isEqualTo("Hỏng");
		assertThat(sheet.cell(2, "Ngày đặt").isDate()).isTrue();
		assertThat(sheet.cell(2, "Ngày đặt").date()).isEqualTo(LocalDate.of(2025, 3, 9));
		assertThat(sheet.cell(2, "Ngày gỡ").isBlank()).isTrue();
		assertThat(sheet.cell(2, "Chủ").text()).isEqualTo("Nguyễn An");
		assertThat(sheet.cell(2, "Mô tả").text()).isEqualTo("Hai cánh");
		assertThat(sheet.cell(2, "Hãng").text()).isEqualTo("LG");
		assertThat(sheet.cell(2, "Công suất").isNumber()).isTrue();
		assertThat(sheet.cell(2, "Công suất").number()).isEqualByComparingTo("150");
		assertThat(sheet.cell(1, "Nhóm").isBlank()).isTrue();
		assertThat(sheet.cell(1, "Chủ").isBlank()).isTrue();
		assertThat(sheet.cell(1, "Trạng thái").text()).isEqualTo("Đang dùng");
		assertThat(sheet.cell(1, "Công suất").isBlank()).isTrue();
	}

	@Test
	void itemsExportFollowsTheListFilter() throws Exception {
		long room = place(null, "P1");
		long chairs = this.categories.create(CTX_A, new ItemCategoryRequest("Ghế", null)).id();
		long tables = this.categories.create(CTX_A, new ItemCategoryRequest("Bàn", null)).id();
		item(room, "A", "IN_USE", chairs);
		item(room, "B", "BROKEN", tables);
		item(room, "S", "STORED", null);
		String removed = item(room, "C", "IN_USE", chairs);
		this.mvc
			.perform(asA(post("/items/" + removed + "/remove")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"version\": 1, \"removedAt\": \"2026-02-01\"}"))
			.andExpect(status().isOk());

		assertThat(names(get("/exports/items.xlsx"))).containsExactly("A", "B", "S");
		assertThat(names(get("/exports/items.xlsx").param("status", "BROKEN"))).containsExactly("B");
		assertThat(names(get("/exports/items.xlsx").param("status", "STORED"))).containsExactly("S");
		assertThat(names(get("/exports/items.xlsx").param("categoryId", String.valueOf(chairs)))).containsExactly("A");
		assertThat(names(get("/exports/items.xlsx").param("status", "REMOVED")
			.param("categoryId", String.valueOf(chairs)))).containsExactly("C");
		XlsxSheet gone = XlsxReader.read(export(get("/exports/items.xlsx").param("status", "REMOVED"))).get(0);
		assertThat(gone.cell(0, "Trạng thái").text()).isEqualTo("Đã gỡ");
		assertThat(gone.cell(0, "Ngày gỡ").isDate()).isTrue();
		assertThat(gone.cell(0, "Ngày gỡ").date()).isEqualTo(LocalDate.of(2026, 2, 1));
	}

	@Test
	void deletedRecordsAreNotExported() throws Exception {
		long room = place(null, "P1");
		item(room, "Còn", "IN_USE", null);
		String gone = item(room, "Đã xoá", "IN_USE", null);
		this.mvc.perform(asA(delete("/items/" + gone)).param("version", "1")).andExpect(status().isNoContent());

		assertThat(names(get("/exports/items.xlsx"))).containsExactly("Còn");
		assertThat(names(get("/exports/items.xlsx").param("status", "IN_USE"))).containsExactly("Còn");
	}

	/** Món đã gỡ nằm ở một địa điểm đã xoá: món vẫn có trong file và vẫn ghi được nó từng ở đâu. */
	@Test
	void removedItemAtADeletedPlaceStillShowsWhere() throws Exception {
		long room = place(null, "Phòng cũ");
		String removed = item(room, "Quạt", "IN_USE", null);
		this.mvc
			.perform(asA(post("/items/" + removed + "/remove")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"version\": 1}"))
			.andExpect(status().isOk());
		this.mvc.perform(asA(delete("/places/" + room)).param("version", "1")).andExpect(status().isNoContent());

		XlsxSheet sheet = XlsxReader.read(export(get("/exports/items.xlsx").param("status", "REMOVED"))).get(0);

		assertThat(sheet.column("Tên")).containsExactly("Quạt");
		assertThat(sheet.cell(0, "Ở đâu").text()).isEqualTo("Phòng cũ");
	}

	/** 501 món cùng tên ở một chỗ: hơn một lô, thứ tự chỉ còn dựa vào id. Mỗi món có mặt đúng một lần. */
	@Test
	void moreThanOneBatchKeepsEveryRowOnce() throws Exception {
		long room = place(null, "Kho");
		StringBuilder sql = new StringBuilder("INSERT INTO reg_item "
				+ "(workspace_id, id, place_id, code, name, status, created_by, updated_by) VALUES ");
		for (int i = 1; i <= 501; i++) {
			sql.append((i > 1) ? ", " : "")
				.append("(:ws, ")
				.append(920000 + i)
				.append(", :place, 'X-")
				.append(i)
				.append("', 'Ghế', 'IN_USE', :user, :user)");
		}
		this.jdbc.sql(sql.toString()).param("ws", WS_A).param("place", room).param("user", USER).update();

		XlsxSheet sheet = XlsxReader.read(export(get("/exports/items.xlsx"))).get(0);

		assertThat(sheet.dataRowCount()).isEqualTo(501);
		assertThat(new HashSet<>(sheet.column("Mã"))).hasSize(501);
		assertThat(sheet.cell(0, "Mã").text()).isEqualTo("X-1");
		assertThat(sheet.cell(500, "Mã").text()).isEqualTo("X-501");
	}

	@Test
	void responseNamesTheFile() throws Exception {
		String name = "home-platform-items-" + this.businessDate.today() + ".xlsx";

		this.mvc.perform(asA(get("/exports/items.xlsx")))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", XLSX))
			.andExpect(header().string("Content-Disposition",
					"attachment; filename=\"" + name + "\"; filename*=UTF-8''" + name));
	}

	/**
	 * Lỗi phải ra trước khi phản hồi được đánh dấu là file tải về. Spring tự thêm
	 * {@code Content-Disposition: inline} cho phản hồi lỗi của đường dẫn có đuôi lạ, nên chỉ kiểm là nó
	 * không phải {@code attachment}.
	 */
	@Test
	void unknownCategoryIs404() throws Exception {
		this.mvc.perform(asA(get("/exports/items.xlsx").param("categoryId", "999")))
			.andExpect(status().isNotFound())
			.andExpect(header().string("Content-Disposition", not(containsString("attachment"))))
			.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void unknownStatusIs400() throws Exception {
		this.mvc.perform(asA(get("/exports/items.xlsx").param("status", "XYZ"))).andExpect(status().isBadRequest());
	}

	@Test
	void otherWorkspaceExportsNothing() throws Exception {
		item(place(null, "P1"), "Tủ", "IN_USE", null);

		XlsxSheet sheet = XlsxReader.read(this.mvc.perform(asB(get("/exports/items.xlsx")))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsByteArray()).get(0);

		assertThat(sheet.headers()).startsWith("Mã", "Tên");
		assertThat(sheet.dataRowCount()).isZero();
	}

	private byte[] export(MockHttpServletRequestBuilder request) throws Exception {
		return this.mvc.perform(asA(request))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsByteArray();
	}

	private List<String> names(MockHttpServletRequestBuilder request) throws Exception {
		return XlsxReader.read(export(request)).get(0).column("Tên");
	}

	private long place(Long parentId, String name) {
		return this.places
			.create(CTX_A, new PlaceCreateRequest(parentId, this.type, name, null, null, null, null, null, null, null))
			.id();
	}

	private String item(long placeId, String name, String status, Long categoryId) throws Exception {
		String category = (categoryId != null) ? "\"" + categoryId + "\"" : "null";
		String body = this.mvc.perform(asA(post("/items")).contentType(MediaType.APPLICATION_JSON).content("""
				{"placeId": "%d", "name": "%s", "status": "%s", "categoryId": %s}"""
			.formatted(placeId, name, status, category)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.id");
	}

}
