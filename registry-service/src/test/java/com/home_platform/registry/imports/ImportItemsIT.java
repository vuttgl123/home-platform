package com.home_platform.registry.imports;

import static com.home_platform.registry.imports.XlsxBuilder.workbook;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.item.ItemRequest;
import com.home_platform.registry.item.ItemService;
import com.home_platform.registry.itemcategory.ItemCategoryRequest;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.person.PersonRequest;
import com.home_platform.registry.person.PersonService;
import com.home_platform.registry.place.PlaceCreateRequest;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ImportItemsIT extends ImportTestData {

	private static final String[] HEADERS = { "Mã", "Tên", "Nhóm", "Ở đâu", "Trạng thái", "Ngày đặt", "Ngày gỡ" };

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	PlaceService places;

	@Autowired
	ItemCategoryService categories;

	@Autowired
	ItemService items;

	@Autowired
	PersonService people;

	@Autowired
	FieldDefService fieldDefs;

	long building;

	long p301;

	long appliances;

	@BeforeEach
	void data() {
		long type = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("PHONG", "Phòng", null, 1)).id();
		this.building = this.places
			.create(CTX_A, new PlaceCreateRequest(null, type, "Toà A", null, null, null, null, null, null, null), "TA")
			.id();
		this.p301 = this.places
			.create(CTX_A,
					new PlaceCreateRequest(this.building, type, "P301", null, null, null, null, null, null, null),
					"P301")
			.id();
		this.appliances = this.categories.create(CTX_A, new ItemCategoryRequest("Điện máy", 1)).id();
	}

	@Test
	void itemsLandWithEveryColumn() throws Exception {
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.ITEM, this.appliances, "power", "Công suất",
				FieldDataType.NUMBER, null, null, 1, null));
		long owner = this.people
			.create(CTX_A, new PersonRequest("Trần Bình", null, null, null, null, null, null, null, Map.of(), null, null))
			.id();

		String report = report(commit(workbook().sheet("Đồ đạc", "Mã", "Tên", "Nhóm", "Ở đâu", "Số lượng", "Đơn vị",
				"Trạng thái", "Ngày đặt", "Hết bảo hành", "Bảo trì (tháng)", "Chủ", "Mô tả", "Công suất")
			.row("TL-01", "Tủ lạnh", "điện máy", "Toà A / P301", 2, "cái", "Hỏng", LocalDate.of(2026, 1, 5),
					LocalDate.of(2027, 1, 5), 6, "Trần Bình", "Ngăn đá hỏng", 150)
			.build()));

		assertThat((Boolean) read(report, "$.committed")).isTrue();
		assertThat((String) read(report, "$.sheets[0].items[0].label")).isEqualTo("TL-01 · Tủ lạnh");
		this.mvc.perform(asA(get("/items/" + itemId("Tủ lạnh"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("TL-01"))
			.andExpect(jsonPath("$.categoryId").value(String.valueOf(this.appliances)))
			.andExpect(jsonPath("$.placeId").value(String.valueOf(this.p301)))
			.andExpect(jsonPath("$.quantity").value(2))
			.andExpect(jsonPath("$.unit").value("cái"))
			.andExpect(jsonPath("$.status").value("BROKEN"))
			.andExpect(jsonPath("$.placedAt").value("2026-01-05"))
			.andExpect(jsonPath("$.warrantyUntil").value("2027-01-05"))
			.andExpect(jsonPath("$.serviceEveryMonths").value(6))
			.andExpect(jsonPath("$.ownerPersonId").value(String.valueOf(owner)))
			.andExpect(jsonPath("$.description").value("Ngăn đá hỏng"))
			.andExpect(jsonPath("$.attributes.power").value(150));
	}

	@Test
	void blankQuantityIsOneAndBlankStatusIsInUse() throws Exception {
		report(commit(workbook().sheet("Đồ đạc", "Tên", "Ở đâu").row("Quạt", "P301").build()));

		this.mvc.perform(asA(get("/items/" + itemId("Quạt"))))
			.andExpect(jsonPath("$.quantity").value(1))
			.andExpect(jsonPath("$.status").value("IN_USE"))
			.andExpect(jsonPath("$.ownerPersonId").doesNotExist());
	}

	@Test
	void englishCodesAndAnyCaseAreAccepted() throws Exception {
		report(commit(workbook().sheet("Đồ đạc", "Tên", "Ở đâu", "Trạng thái")
			.row("Quạt", "P301", "BROKEN")
			.row("Đèn", "P301", "cất kho")
			.build()));

		this.mvc.perform(asA(get("/items/" + itemId("Quạt")))).andExpect(jsonPath("$.status").value("BROKEN"));
		this.mvc.perform(asA(get("/items/" + itemId("Đèn")))).andExpect(jsonPath("$.status").value("STORED"));
	}

	@Test
	void removedItemsGetTwoVersions() throws Exception {
		report(commit("do-dac.xlsx", workbook().sheet("Đồ đạc", HEADERS)
			.row(null, "Máy giặt", null, "P301", "Đã gỡ", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 10))
			.build()));

		long id = itemId("Máy giặt");
		this.mvc.perform(asA(get("/items/" + id)))
			.andExpect(jsonPath("$.status").value("REMOVED"))
			.andExpect(jsonPath("$.removedAt").value("2026-10-10"));
		assertThat(versionNote("ITEM", id, 1)).isEqualTo("Nhập từ Excel: do-dac.xlsx");
		assertThat(versionNote("ITEM", id, 2)).isEqualTo("Nhập từ Excel: do-dac.xlsx");
		assertThat(this.jdbc.sql("SELECT action FROM reg_entity_version WHERE workspace_id = :ws AND entity_type = 'ITEM' "
				+ "AND entity_id = :id ORDER BY version_no")
			.param("ws", WS_A)
			.param("id", id)
			.query(String.class)
			.list()).containsExactly("CREATE", "UPDATE");
	}

	@Test
	void removalDateRules() throws Exception {
		String report = report(preview(workbook().sheet("Đồ đạc", HEADERS)
			.row(null, "Quạt", null, "P301", "Đã gỡ", null, null)
			.row(null, "Đèn", null, "P301", "Đang dùng", null, LocalDate.of(2026, 10, 10))
			.row(null, "Tivi", null, "P301", "Đã gỡ", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1))
			.build()));

		assertThat(messages(report, 0)).containsExactly("Bắt buộc khi trạng thái là Đã gỡ");
		assertThat((String) read(report, "$.sheets[0].items[0].messages[0].column")).isEqualTo("Ngày gỡ");
		assertThat(messages(report, 1)).containsExactly("Chỉ ghi khi trạng thái là Đã gỡ");
		assertThat(messages(report, 2)).containsExactly("Ngày gỡ không được trước Ngày đặt");
	}

	@Test
	void categoryAndPlaceErrors() throws Exception {
		long old = this.categories.create(CTX_A, new ItemCategoryRequest("Đồ cũ", 2)).id();
		this.categories.archive(CTX_A, old);

		String report = report(preview(workbook().sheet("Đồ đạc", HEADERS)
			.row(null, "Quạt", "Nội thất", "P301", null, null, null)
			.row(null, "Đèn", "Đồ cũ", "P301", null, null, null)
			.row(null, "Tivi", null, "P999", null, null, null)
			.row(null, "Bàn", null, null, null, null, null)
			.build()));

		assertThat(messages(report, 0)).containsExactly("Chưa có nhóm 'Nội thất' trong Danh mục");
		assertThat(messages(report, 1)).containsExactly("Nhóm 'Đồ cũ' đang ngừng dùng");
		assertThat(messages(report, 2)).containsExactly("Không tìm thấy địa điểm 'P999'");
		assertThat(messages(report, 3)).containsExactly("Bắt buộc");
		assertThat((String) read(report, "$.sheets[0].items[3].messages[0].column")).isEqualTo("Ở đâu");
	}

	@Test
	void sharedFieldsAreCheckedWhenTheCategoryIsWrong() throws Exception {
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.ITEM, null, "supplier", "Nơi mua",
				FieldDataType.TEXT, null, true, 1, null));
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.ITEM, this.appliances, "power", "Công suất",
				FieldDataType.NUMBER, null, null, 2, null));

		String report = report(preview(workbook().sheet("Đồ đạc", "Tên", "Nhóm", "Ở đâu", "Nơi mua", "Công suất")
			.row("Tủ lạnh", "Điện may", "P301", null, 150)
			.build()));

		assertThat(messages(report, 0)).containsExactlyInAnyOrder("Chưa có nhóm 'Điện may' trong Danh mục", "Bắt buộc");
	}

	@Test
	void itemCodesShareTheirOwnSpace() throws Exception {
		this.items.create(CTX_A, new ItemRequest(this.p301, null, null, "Ghế", null, null, null, null, null, null, null,
				null, null, null), "G-01");

		String report = report(preview(workbook().sheet("Đồ đạc", HEADERS)
			.row("TA", "Quạt", null, "P301", null, null, null)
			.row("G-01", "Đèn", null, "P301", null, null, null)
			.build()));

		assertThat(messages(report, 0)).isEmpty();
		assertThat(messages(report, 1)).containsExactly("Mã G-01 đã có");
	}

	@Test
	void sameNameAtSamePlaceWarns() throws Exception {
		this.items.create(CTX_A, new ItemRequest(this.p301, null, null, "Quạt", null, null, null, null, null, null, null,
				null, null, null));

		String report = report(commit(workbook().sheet("Đồ đạc", HEADERS)
			.row(null, "quạt", null, "P301", null, null, null)
			.row(null, "Đèn", null, "Toà A / P301", null, null, null)
			.row(null, "Đèn", null, "P301", null, null, null)
			.build()));

		assertThat(messages(report, 0)).containsExactly("Đã có món 'quạt' ở 'Toà A / P301'");
		assertThat(messages(report, 2)).containsExactly("Trùng với dòng 3");
		assertThat((Boolean) read(report, "$.committed")).isTrue();
	}

	private long itemId(String name) {
		return this.jdbc.sql("SELECT id FROM reg_item WHERE workspace_id = :ws AND name = :name AND deleted_at IS NULL")
			.param("ws", WS_A)
			.param("name", name)
			.query(Long.class)
			.single();
	}

	private static List<String> messages(String report, int row) {
		return read(report, "$.sheets[0].items[" + row + "].messages[*].text");
	}

}
