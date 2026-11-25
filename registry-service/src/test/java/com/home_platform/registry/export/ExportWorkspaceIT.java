package com.home_platform.registry.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import com.home_platform.registry.RegistryIT;
import com.home_platform.registry.export.XlsxReader.XlsxSheet;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.occupancy.Occupancy;
import com.home_platform.registry.occupancy.OccupancyCreateRequest;
import com.home_platform.registry.occupancy.OccupancyService;
import com.home_platform.registry.occupancy.Relation;
import com.home_platform.registry.ownership.Ownership;
import com.home_platform.registry.ownership.OwnershipCreateRequest;
import com.home_platform.registry.ownership.OwnershipService;
import com.home_platform.registry.person.Gender;
import com.home_platform.registry.person.PersonRequest;
import com.home_platform.registry.person.PersonService;
import com.home_platform.registry.place.Place;
import com.home_platform.registry.place.PlaceCreateRequest;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.place.PlaceStatus;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import com.home_platform.registry.support.BusinessDate;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class ExportWorkspaceIT extends RegistryIT {

	private static final int PLACES = 0;

	private static final int PEOPLE = 1;

	private static final int STAYS = 2;

	private static final int OWNERSHIPS = 3;

	private static final int ITEMS = 4;

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	PlaceService places;

	@Autowired
	PersonService people;

	@Autowired
	OccupancyService occupancies;

	@Autowired
	OwnershipService ownerships;

	@Autowired
	FieldDefService fieldDefs;

	@Autowired
	BusinessDate businessDate;

	long building;

	long room;

	LocalDate today;

	@BeforeEach
	void types() {
		this.building = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("TOA", "Toà", null, 1)).id();
		this.room = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("PHONG", "Phòng", null, 2)).id();
		this.today = this.businessDate.today();
	}

	@Test
	void workspaceFileHasFiveSheetsInOrder() throws Exception {
		List<XlsxSheet> sheets = workspace();

		assertThat(sheets).extracting(XlsxSheet::name)
			.containsExactly("Địa điểm", "Người", "Cư trú", "Sở hữu", "Đồ đạc");
		assertThat(sheets.get(PLACES).headers()).containsExactly("Mã", "Tên", "Loại", "Sức chứa", "Nằm trong",
				"Trạng thái", "Mô tả");
		assertThat(sheets.get(PEOPLE).headers()).containsExactly("Họ tên", "Điện thoại", "Email", "Loại giấy tờ",
				"Số giấy tờ", "Ngày sinh", "Giới tính", "Đang ở", "Ghi chú");
		assertThat(sheets.get(STAYS).headers()).containsExactly("Địa điểm", "Mã địa điểm", "Người", "Quan hệ",
				"Từ ngày", "Đến ngày", "Ghi chú");
		assertThat(sheets.get(OWNERSHIPS).headers()).containsExactly("Địa điểm", "Mã địa điểm", "Người", "Tỷ lệ (%)",
				"Từ ngày", "Đến ngày", "Số giấy tờ", "Ghi chú");
		assertThat(sheets.get(ITEMS).headers()).containsExactly("Mã", "Tên", "Nhóm", "Ở đâu", "Số lượng", "Đơn vị",
				"Trạng thái", "Ngày đặt", "Ngày gỡ", "Chủ", "Mô tả");
		assertThat(sheets).allSatisfy(sheet -> assertThat(sheet.dataRowCount()).isZero());
	}

	@Test
	void placesSheetShowsParentPathAndAttributes() throws Exception {
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.PLACE, this.room, "area", "Diện tích",
				FieldDataType.NUMBER, null, null, 1));
		long a = place(this.building, null, "Toà A").id();
		long floor = place(this.building, a, "Tầng 3").id();
		Place p301 = this.places.create(CTX_A, new PlaceCreateRequest(floor, this.room, "P301", "Hướng nam",
				PlaceStatus.INACTIVE, 2, Map.of("area", 25), null, null, null));
		place(this.building, null, "Toà B");

		XlsxSheet sheet = workspace().get(PLACES);

		assertThat(sheet.headers()).containsExactly("Mã", "Tên", "Loại", "Sức chứa", "Nằm trong", "Trạng thái",
				"Mô tả", "Diện tích");
		assertThat(sheet.column("Tên")).containsExactly("Toà A", "Tầng 3", "P301", "Toà B");
		assertThat(sheet.column("Nằm trong")).containsExactly("", "Toà A", "Toà A / Tầng 3", "");
		assertThat(sheet.column("Loại")).containsExactly("Toà", "Toà", "Phòng", "Toà");
		assertThat(sheet.column("Trạng thái")).containsExactly("Đang dùng", "Đang dùng", "Tạm ngưng", "Đang dùng");
		assertThat(sheet.cell(2, "Mã").text()).isEqualTo(p301.code()).isNotEmpty();
		assertThat(sheet.cell(2, "Mô tả").text()).isEqualTo("Hướng nam");
		assertThat(sheet.cell(2, "Diện tích").isNumber()).isTrue();
		assertThat(sheet.cell(2, "Diện tích").number()).isEqualByComparingTo("25");
		assertThat(sheet.cell(0, "Diện tích").isBlank()).isTrue();
		assertThat(sheet.cell(2, "Sức chứa").number()).isEqualByComparingTo("2");
		assertThat(sheet.cell(0, "Sức chứa").isBlank()).isTrue();
		assertThat(sheet.cell(0, "Mô tả").isBlank()).isTrue();
	}

	@Test
	void peopleSheetListsWhereEachPersonLivesToday() throws Exception {
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.PERSON, null, "job", "Nghề nghiệp",
				FieldDataType.TEXT, null, null, 1));
		long a = place(this.building, null, "Toà A").id();
		long p1 = place(this.room, a, "P1").id();
		long p2 = place(this.room, a, "P2").id();
		long p3 = place(this.room, a, "P3").id();
		long an = this.people
			.create(CTX_A,
					new PersonRequest("Trần An", "0901234567", "an@example.com", "CCCD", "012345678901",
							LocalDate.of(1990, 5, 20), Gender.FEMALE, "Hay đi công tác", Map.of("job", "Kỹ sư"), null,
							null))
			.id();
		long binh = person("Lê Bình");
		stay(p2, an, Relation.RESIDENT, this.today.minusDays(5), null, null);
		stay(p1, an, Relation.RESIDENT, this.today.minusDays(9), this.today, null);
		stay(p3, an, Relation.GUEST, this.today.minusDays(30), this.today.minusDays(1), null);
		stay(p3, binh, Relation.GUEST, this.today.plusDays(1), null, null);

		XlsxSheet sheet = workspace().get(PEOPLE);

		assertThat(sheet.headers()).endsWith("Ghi chú", "Nghề nghiệp");
		assertThat(sheet.column("Họ tên")).containsExactly("Lê Bình", "Trần An");
		assertThat(sheet.cell(1, "Đang ở").text()).isEqualTo("Toà A / P1; Toà A / P2");
		assertThat(sheet.cell(0, "Đang ở").isBlank()).isTrue();
		assertThat(sheet.cell(1, "Điện thoại").isText()).isTrue();
		assertThat(sheet.cell(1, "Điện thoại").text()).isEqualTo("0901234567");
		assertThat(sheet.cell(1, "Email").text()).isEqualTo("an@example.com");
		assertThat(sheet.cell(1, "Loại giấy tờ").text()).isEqualTo("CCCD");
		assertThat(sheet.cell(1, "Số giấy tờ").isText()).isTrue();
		assertThat(sheet.cell(1, "Số giấy tờ").text()).isEqualTo("012345678901");
		assertThat(sheet.cell(1, "Ngày sinh").isDate()).isTrue();
		assertThat(sheet.cell(1, "Ngày sinh").date()).isEqualTo(LocalDate.of(1990, 5, 20));
		assertThat(sheet.cell(1, "Giới tính").text()).isEqualTo("Nữ");
		assertThat(sheet.cell(1, "Ghi chú").text()).isEqualTo("Hay đi công tác");
		assertThat(sheet.cell(1, "Nghề nghiệp").text()).isEqualTo("Kỹ sư");
		assertThat(sheet.cell(0, "Giới tính").isBlank()).isTrue();
		assertThat(sheet.cell(0, "Ngày sinh").isBlank()).isTrue();
	}

	@Test
	void occupancySheetUsesVietnameseRelations() throws Exception {
		Place a = place(this.building, null, "Toà A");
		Place p1 = place(this.room, a.id(), "P1");
		long an = person("An");
		long binh = person("Bình");
		stay(p1.id(), binh, Relation.RESIDENT, this.today.minusDays(3), null, "Hợp đồng 12 tháng");
		stay(p1.id(), an, Relation.GUEST, this.today.minusDays(20), this.today.minusDays(10), null);
		stay(a.id(), an, Relation.STAFF, this.today.minusDays(1), null, null);

		XlsxSheet sheet = workspace().get(STAYS);

		assertThat(sheet.column("Địa điểm")).containsExactly("Toà A", "Toà A / P1", "Toà A / P1");
		assertThat(sheet.column("Người")).containsExactly("An", "An", "Bình");
		assertThat(sheet.column("Quan hệ")).containsExactly("Nhân viên", "Khách", "Người ở");
		assertThat(sheet.cell(1, "Mã địa điểm").text()).isEqualTo(p1.code()).isNotEmpty();
		assertThat(sheet.cell(1, "Từ ngày").isDate()).isTrue();
		assertThat(sheet.cell(1, "Từ ngày").date()).isEqualTo(this.today.minusDays(20));
		assertThat(sheet.cell(1, "Đến ngày").isDate()).isTrue();
		assertThat(sheet.cell(1, "Đến ngày").date()).isEqualTo(this.today.minusDays(10));
		assertThat(sheet.cell(2, "Đến ngày").isBlank()).isTrue();
		assertThat(sheet.cell(2, "Ghi chú").text()).isEqualTo("Hợp đồng 12 tháng");
		assertThat(sheet.cell(1, "Ghi chú").isBlank()).isTrue();
	}

	@Test
	void ownershipSheetHasShareAsNumber() throws Exception {
		Place a = place(this.building, null, "Toà A");
		long an = person("An");
		long binh = person("Bình");
		own(a.id(), an, new BigDecimal("60.5"), this.today.minusDays(100), null, "GCN-01", "Mua lại");
		own(a.id(), binh, null, this.today.minusDays(50), this.today.minusDays(2), null, null);

		XlsxSheet sheet = workspace().get(OWNERSHIPS);

		assertThat(sheet.column("Người")).containsExactly("An", "Bình");
		assertThat(sheet.column("Địa điểm")).containsExactly("Toà A", "Toà A");
		assertThat(sheet.cell(0, "Mã địa điểm").text()).isEqualTo(a.code()).isNotEmpty();
		assertThat(sheet.cell(0, "Tỷ lệ (%)").isNumber()).isTrue();
		assertThat(sheet.cell(0, "Tỷ lệ (%)").number()).isEqualByComparingTo("60.5");
		assertThat(sheet.cell(1, "Tỷ lệ (%)").isBlank()).isTrue();
		assertThat(sheet.cell(0, "Từ ngày").isDate()).isTrue();
		assertThat(sheet.cell(0, "Từ ngày").date()).isEqualTo(this.today.minusDays(100));
		assertThat(sheet.cell(0, "Đến ngày").isBlank()).isTrue();
		assertThat(sheet.cell(1, "Đến ngày").date()).isEqualTo(this.today.minusDays(2));
		assertThat(sheet.cell(0, "Số giấy tờ").text()).isEqualTo("GCN-01");
		assertThat(sheet.cell(0, "Ghi chú").text()).isEqualTo("Mua lại");
	}

	@Test
	void deletedRecordsAreNotExported() throws Exception {
		long a = place(this.building, null, "Toà A").id();
		long gonePlace = place(this.room, null, "Đã xoá").id();
		long kept = person("Còn");
		long gonePerson = person("Đã xoá");
		stay(a, kept, Relation.RESIDENT, this.today.minusDays(3), null, null);
		Occupancy goneStay = stay(a, kept, Relation.GUEST, this.today.minusDays(30), this.today.minusDays(20), null);
		own(a, kept, null, this.today.minusDays(3), null, "giữ", null);
		Ownership goneOwn = own(a, kept, null, this.today.minusDays(30), this.today.minusDays(20), "bỏ", null);
		this.places.delete(CTX_A, gonePlace, 1, null);
		this.people.delete(CTX_A, gonePerson, 1, null);
		this.occupancies.delete(CTX_A, goneStay.id(), 1);
		this.ownerships.delete(CTX_A, goneOwn.id(), 1);

		List<XlsxSheet> sheets = workspace();

		assertThat(sheets.get(PLACES).column("Tên")).containsExactly("Toà A");
		assertThat(sheets.get(PEOPLE).column("Họ tên")).containsExactly("Còn");
		assertThat(sheets.get(STAYS).column("Quan hệ")).containsExactly("Người ở");
		assertThat(sheets.get(OWNERSHIPS).column("Số giấy tờ")).containsExactly("giữ");
		assertThat(XlsxReader.read(export("/exports/people.xlsx")).get(0).column("Họ tên")).containsExactly("Còn");
	}

	/**
	 * Dòng cư trú đã kết thúc không bị xoá khi người và địa điểm của nó vào thùng rác: nó vẫn có trong file,
	 * vẫn ghi tên người và tên địa điểm, dù hai sheet kia không còn liệt kê họ.
	 */
	@Test
	void pastStayAtADeletedPlaceOfADeletedPersonIsKept() throws Exception {
		long oldRoom = place(this.room, null, "Phòng cũ").id();
		long oldTenant = person("Người cũ");
		stay(oldRoom, oldTenant, Relation.RESIDENT, this.today.minusDays(30), this.today.minusDays(1), null);
		own(oldRoom, oldTenant, null, this.today.minusDays(30), this.today.minusDays(1), null, null);
		this.people.delete(CTX_A, oldTenant, 1, null);
		this.places.delete(CTX_A, oldRoom, 1, null);

		List<XlsxSheet> sheets = workspace();

		assertThat(sheets.get(PLACES).dataRowCount()).isZero();
		assertThat(sheets.get(PEOPLE).dataRowCount()).isZero();
		assertThat(sheets.get(STAYS).column("Địa điểm")).containsExactly("Phòng cũ");
		assertThat(sheets.get(STAYS).column("Người")).containsExactly("Người cũ");
		assertThat(sheets.get(OWNERSHIPS).column("Địa điểm")).containsExactly("Phòng cũ");
		assertThat(sheets.get(OWNERSHIPS).column("Người")).containsExactly("Người cũ");
	}

	/** File cả không gian ghi mọi món chưa xoá, kể cả món đã gỡ; trạng thái ghi rõ. */
	@Test
	void workspaceItemsSheetIncludesRemovedItems() throws Exception {
		long a = place(this.building, null, "Toà A").id();
		item(a, "Bàn");
		String removed = item(a, "Ghế");
		String deleted = item(a, "Tủ");
		this.mvc
			.perform(asA(post("/items/" + removed + "/remove")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"version\": 1}"))
			.andExpect(status().isOk());
		this.mvc.perform(asA(delete("/items/" + deleted)).param("version", "1")).andExpect(status().isNoContent());

		XlsxSheet sheet = workspace().get(ITEMS);

		assertThat(sheet.column("Tên")).containsExactly("Bàn", "Ghế");
		assertThat(sheet.column("Trạng thái")).containsExactly("Đang dùng", "Đã gỡ");
		assertThat(sheet.cell(1, "Ngày gỡ").isDate()).isTrue();
	}

	@Test
	void peopleExportIsOneSheet() throws Exception {
		person("An");

		List<XlsxSheet> sheets = XlsxReader.read(export("/exports/people.xlsx"));

		assertThat(sheets).hasSize(1);
		assertThat(sheets.get(0).name()).isEqualTo("Người");
		assertThat(sheets.get(0).headers()).startsWith("Họ tên", "Điện thoại");
		assertThat(sheets.get(0).column("Họ tên")).containsExactly("An");
	}

	@Test
	void responsesNameTheFiles() throws Exception {
		for (String kind : List.of("people", "workspace")) {
			String name = "home-platform-" + kind + "-" + this.today + ".xlsx";
			this.mvc.perform(asA(get("/exports/" + kind + ".xlsx")))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition",
						"attachment; filename=\"" + name + "\"; filename*=UTF-8''" + name));
		}
	}

	/**
	 * 501 dòng ở mỗi sheet, giống nhau ở mọi cột dùng để sắp (cùng tên, cùng địa điểm, cùng ngày): hơn một
	 * lô, thứ tự chỉ còn dựa vào id. Mỗi dòng có mặt đúng một lần.
	 */
	@Test
	void everySheetSurvivesMoreThanOneBatch() throws Exception {
		Place root = place(this.building, null, "Kho");
		insert("reg_place (workspace_id, id, parent_id, place_type_id, path, depth, code, name, created_by, "
				+ "updated_by)", i -> "(:ws, " + (930000 + i) + ", " + root.id() + ", " + this.room + ", '"
						+ root.path() + (930000 + i) + "/', 1, 'P-" + i + "', 'P', :user, :user)");
		insert("reg_person (workspace_id, id, full_name, phone, created_by, updated_by)",
				i -> "(:ws, " + (940000 + i) + ", 'Người', '09" + String.format("%08d", i) + "', :user, :user)");
		insert("reg_occupancy (workspace_id, id, place_id, person_id, relation, start_date, note, created_by, "
				+ "updated_by)", i -> "(:ws, " + (950000 + i) + ", " + root.id() + ", " + (940000 + i)
						+ ", 'RESIDENT', '2020-01-01', 'n-" + i + "', :user, :user)");
		insert("reg_ownership (workspace_id, id, place_id, person_id, start_date, document_no, created_by, "
				+ "updated_by)", i -> "(:ws, " + (960000 + i) + ", " + root.id() + ", " + (940000 + i)
						+ ", '2020-01-01', 'd-" + i + "', :user, :user)");

		List<XlsxSheet> sheets = workspace();

		assertThat(sheets.get(PLACES).dataRowCount()).isEqualTo(502);
		assertThat(new HashSet<>(sheets.get(PLACES).column("Mã"))).hasSize(502);
		assertThat(sheets.get(PEOPLE).dataRowCount()).isEqualTo(501);
		assertThat(new HashSet<>(sheets.get(PEOPLE).column("Điện thoại"))).hasSize(501);
		assertThat(sheets.get(PEOPLE).cell(0, "Điện thoại").text()).isEqualTo("0900000001");
		assertThat(sheets.get(PEOPLE).cell(500, "Điện thoại").text()).isEqualTo("0900000501");
		assertThat(sheets.get(PEOPLE).column("Đang ở")).containsOnly("Kho");
		assertThat(sheets.get(STAYS).dataRowCount()).isEqualTo(501);
		assertThat(new HashSet<>(sheets.get(STAYS).column("Ghi chú"))).hasSize(501);
		assertThat(sheets.get(STAYS).cell(0, "Ghi chú").text()).isEqualTo("n-1");
		assertThat(sheets.get(STAYS).cell(500, "Ghi chú").text()).isEqualTo("n-501");
		assertThat(sheets.get(OWNERSHIPS).dataRowCount()).isEqualTo(501);
		assertThat(new HashSet<>(sheets.get(OWNERSHIPS).column("Số giấy tờ"))).hasSize(501);
		assertThat(sheets.get(OWNERSHIPS).cell(500, "Số giấy tờ").text()).isEqualTo("d-501");
	}

	@Test
	void otherWorkspaceExportsNothing() throws Exception {
		long a = place(this.building, null, "Toà A").id();
		long an = person("An");
		stay(a, an, Relation.RESIDENT, this.today.minusDays(3), null, null);
		own(a, an, null, this.today.minusDays(3), null, null, null);
		item(a, "Bàn");

		List<XlsxSheet> sheets = XlsxReader.read(this.mvc.perform(asB(get("/exports/workspace.xlsx")))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsByteArray());
		XlsxSheet people = XlsxReader.read(this.mvc.perform(asB(get("/exports/people.xlsx")))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsByteArray()).get(0);

		assertThat(sheets).hasSize(5).allSatisfy(sheet -> assertThat(sheet.dataRowCount()).isZero());
		assertThat(people.dataRowCount()).isZero();
	}

	private List<XlsxSheet> workspace() throws Exception {
		return XlsxReader.read(export("/exports/workspace.xlsx"));
	}

	private byte[] export(String path) throws Exception {
		return this.mvc.perform(asA(get(path)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsByteArray();
	}

	/** Chèn thẳng 501 dòng vào {@code WS_A}; {@code values} trả về phần {@code (...)} của dòng thứ i (từ 1). */
	private void insert(String tableAndColumns, IntFunction<String> values) {
		StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableAndColumns).append(" VALUES ");
		for (int i = 1; i <= 501; i++) {
			sql.append((i > 1) ? ", " : "").append(values.apply(i));
		}
		this.jdbc.sql(sql.toString()).param("ws", WS_A).param("user", USER).update();
	}

	private Place place(long type, Long parentId, String name) {
		return this.places.create(CTX_A,
				new PlaceCreateRequest(parentId, type, name, null, null, null, null, null, null, null));
	}

	private long person(String fullName) {
		return this.people
			.create(CTX_A, new PersonRequest(fullName, null, null, null, null, null, null, null, null, null, null))
			.id();
	}

	private Occupancy stay(long placeId, long personId, Relation relation, LocalDate start, LocalDate end,
			String note) {
		return this.occupancies.create(CTX_A,
				new OccupancyCreateRequest(placeId, personId, relation, start, end, note));
	}

	private Ownership own(long placeId, long personId, BigDecimal share, LocalDate start, LocalDate end,
			String documentNo, String note) {
		return this.ownerships.create(CTX_A,
				new OwnershipCreateRequest(placeId, personId, share, start, end, documentNo, note));
	}

	private String item(long placeId, String name) throws Exception {
		String body = this.mvc.perform(asA(post("/items")).contentType(MediaType.APPLICATION_JSON).content("""
				{"placeId": "%d", "name": "%s", "status": "IN_USE"}""".formatted(placeId, name)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.id");
	}

}
