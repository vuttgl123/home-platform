package com.home_platform.registry.place;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.occupancy.OccupancyTestData;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.jayway.jsonpath.JsonPath;

/** {@code GET /places/list}: bảng địa điểm có tìm chữ và bộ lọc (spec Đợt 11, S1–S3). */
class PlaceFilterIT extends OccupancyTestData {

	@Autowired
	private FieldDefService fieldDefs;

	@Test
	void rowsCarryTheTableColumns() throws Exception {
		long building = place(null, "Toà A");
		long room = place(building, "P101", 3);
		occupancy(room, person("An"), this.today.minusDays(2), null);

		list("q", "P101").andExpect(status().isOk())
			.andExpect(jsonPath("$.items[*].name", contains("P101")))
			.andExpect(jsonPath("$.items[0].placeId").value(Long.toString(room)))
			.andExpect(jsonPath("$.items[0].placeTypeName").value("T"))
			.andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
			.andExpect(jsonPath("$.items[0].ancestors[*].name", contains("Toà A")))
			.andExpect(jsonPath("$.items[0].capacity").value(3))
			.andExpect(jsonPath("$.items[0].occupantCount").value(1));
	}

	@Test
	void textMatchesNameCodeAndDescriptionIgnoringCaseAndAccents() throws Exception {
		codeRule("PLACE", 0, "QX", 3, false);
		place(null, "Nhà Huế");
		String code = this.places.create(CTX_A, request(this.type, "Kho", null, Map.of())).code();
		this.places.create(CTX_A, new PlaceCreateRequest(null, this.type, "Phòng 2", "Có ban công", null, null, null,
				null, null, null));

		list("q", "hue").andExpect(jsonPath("$.items[*].name", contains("Nhà Huế")));
		list("q", code.toLowerCase()).andExpect(jsonPath("$.items[*].name", contains("Kho")));
		list("q", "BAN CONG").andExpect(jsonPath("$.items[*].name", contains("Phòng 2")));
	}

	@Test
	void likeCharactersAreLiteral() throws Exception {
		place(null, "Giảm 50%");
		place(null, "500");

		list("q", "50%").andExpect(jsonPath("$.items[*].name", contains("Giảm 50%")));
	}

	@Test
	void oneLetterIsIgnored() throws Exception {
		place(null, "An");
		place(null, "Bình");

		list("q", " a ").andExpect(jsonPath("$.items[*].name", contains("An", "Bình")));
	}

	@Test
	void filtersByTypeStatusAndBuilding() throws Exception {
		long a = place(null, "Toà A");
		long floor = place(a, "Tầng 1");
		place(floor, "A101");
		long b = place(null, "Toà B");
		place(b, "B1");
		long kiosk = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("KIOSK", "Ki-ốt", null, null)).id();
		this.places.create(CTX_A, request(kiosk, "K1", null, Map.of()));
		this.places.create(CTX_A, request(this.type, "Đóng", PlaceStatus.INACTIVE, Map.of()));

		list("rootId", Long.toString(a)).andExpect(jsonPath("$.items[*].name", contains("A101", "Tầng 1")));
		list("typeId", Long.toString(kiosk)).andExpect(jsonPath("$.items[*].name", contains("K1")));
		list("status", "INACTIVE").andExpect(jsonPath("$.items[*].name", contains("Đóng")));
		list("rootId", "999999").andExpect(status().isNotFound());
	}

	@Test
	void hasRoomNeedsCapacityAndAFreeSeat() throws Exception {
		long room = place(null, "Còn", 2);
		occupancy(room, person("A"), this.today, null);
		long full = place(null, "Đầy", 1);
		occupancy(full, person("B"), this.today, null);
		place(null, "Chưa đặt");
		place(null, "Kho", 0);

		list("hasRoom", "true").andExpect(jsonPath("$.items[*].name", contains("Còn")));
	}

	@Test
	void numberDateBooleanSelectAndTextFilters() throws Exception {
		field("dien_tich", FieldDataType.NUMBER, 0);
		field("ngay_sua", FieldDataType.DATE, 0);
		field("co_ban_cong", FieldDataType.BOOLEAN, 0);
		field("huong", FieldDataType.SELECT, 0, "Đông", "Tây");
		field("ghi_chu", FieldDataType.TEXT, 0);
		this.places.create(CTX_A, request(this.type, "Lớn", null, Map.of("dien_tich", 30, "ngay_sua", "2026-03-01",
				"co_ban_cong", true, "huong", "Đông", "ghi_chu", "Gần Huế, bờ sông")));
		this.places.create(CTX_A, request(this.type, "Nhỏ", null, Map.of("dien_tich", new BigDecimal("12.5"),
				"ngay_sua", "2025-01-01", "co_ban_cong", false, "huong", "Tây")));
		place(null, "Trống");

		attr("dien_tich:gte:20").andExpect(jsonPath("$.items[*].name", contains("Lớn")));
		attr("dien_tich:lte:12.5").andExpect(jsonPath("$.items[*].name", contains("Nhỏ")));
		attr("dien_tich:eq:30").andExpect(jsonPath("$.items[*].name", contains("Lớn")));
		attr("ngay_sua:gte:2026-01-01").andExpect(jsonPath("$.items[*].name", contains("Lớn")));
		attr("ngay_sua:lte:2025-12-31").andExpect(jsonPath("$.items[*].name", contains("Nhỏ")));
		attr("co_ban_cong:eq:true").andExpect(jsonPath("$.items[*].name", contains("Lớn")));
		attr("co_ban_cong:eq:false").andExpect(jsonPath("$.items[*].name", contains("Nhỏ", "Trống")));
		attr("huong:eq:Tây").andExpect(jsonPath("$.items[*].name", contains("Nhỏ")));
		attr("ghi_chu:has:hue").andExpect(jsonPath("$.items[*].name", contains("Lớn")));
		attr("ghi_chu:eq:gần huế, bờ sông").andExpect(jsonPath("$.items[*].name", contains("Lớn")));
		attr("ghi_chu:has:huế, bờ").andExpect(jsonPath("$.items[*].name", contains("Lớn")));
		attr("ghi_chu:set").andExpect(jsonPath("$.items[*].name", contains("Lớn")));
		attr("ghi_chu:unset").andExpect(jsonPath("$.items[*].name", contains("Nhỏ", "Trống")));
	}

	@Test
	void typedFieldOnlyMatchesItsType() throws Exception {
		long shelfType = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("KE", "Kệ", null, null)).id();
		field("so_tang", FieldDataType.NUMBER, shelfType);
		this.places.create(CTX_A, request(shelfType, "Kệ 1", null, Map.of("so_tang", 3)));
		place(null, "Phòng");

		attr("so_tang:set").andExpect(jsonPath("$.items[*].name", contains("Kệ 1")));
		attr("so_tang:gte:1").andExpect(jsonPath("$.items[*].name", contains("Kệ 1")));
		attr("so_tang:unset").andExpect(jsonPath("$.items[*].name", contains("Phòng")));
	}

	@Test
	void sameKeyOnTwoTypesIsJudgedByEachType() throws Exception {
		long store = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("KHO", "Kho", null, null)).id();
		field("dien_tich", FieldDataType.NUMBER, this.type);
		field("dien_tich", FieldDataType.TEXT, store);
		this.places.create(CTX_A, request(this.type, "P1", null, Map.of("dien_tich", 25)));
		this.places.create(CTX_A, request(this.type, "P2", null, Map.of("dien_tich", 10)));
		this.places.create(CTX_A, request(store, "Kho lớn", null, Map.of("dien_tich", "25 m2")));

		attr("dien_tich:gte:20").andExpect(status().isOk()).andExpect(jsonPath("$.items[*].name", contains("P1")));
		attr("dien_tich:has:m2").andExpect(jsonPath("$.items[*].name", contains("Kho lớn")));
		attr("dien_tich:set").andExpect(jsonPath("$.items[*].name", containsInAnyOrder("P1", "P2", "Kho lớn")));
	}

	@Test
	void conditionsCombineWithAnd() throws Exception {
		field("dien_tich", FieldDataType.NUMBER, 0);
		field("huong", FieldDataType.SELECT, 0, "Đông", "Tây");
		this.places.create(CTX_A, request(this.type, "Cả hai", null, Map.of("dien_tich", 30, "huong", "Đông")));
		this.places.create(CTX_A, request(this.type, "Chỉ rộng", null, Map.of("dien_tich", 30, "huong", "Tây")));
		this.places.create(CTX_A, request(this.type, "Chỉ hướng", null, Map.of("dien_tich", 10, "huong", "Đông")));

		attr("dien_tich:gte:20", "huong:eq:Đông").andExpect(jsonPath("$.items[*].name", contains("Cả hai")));
		this.mvc.perform(asA(get("/places/list")).param("q", "chỉ").param("attr", "dien_tich:gte:20"))
			.andExpect(jsonPath("$.items[*].name", contains("Chỉ rộng")));
	}

	@Test
	void badFiltersAre400() throws Exception {
		field("dien_tich", FieldDataType.NUMBER, 0);

		attr("x:set").andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Unknown field: x"));
		attr("dien_tich:gte:abc").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Invalid value for field dien_tich: abc"));
		attr("dien_tich:set", "dien_tich:set", "dien_tich:set", "dien_tich:set").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("At most 3 attribute conditions"));
	}

	@Test
	void pagesByNameAndHidesDeleted() throws Exception {
		place(null, "R3");
		place(null, "R1");
		place(null, "R2");
		long gone = place(null, "R0");
		this.mvc.perform(asA(delete("/places/" + gone)).param("version", "1")).andExpect(status().isNoContent());

		String first = list("limit", "2").andExpect(jsonPath("$.items[*].name", contains("R1", "R2")))
			.andReturn()
			.getResponse()
			.getContentAsString();
		list("limit", "2", "cursor", JsonPath.read(first, "$.nextCursor"))
			.andExpect(jsonPath("$.items[*].name", contains("R3")));
	}

	private void field(String key, FieldDataType type, long typeId, String... options) {
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.PLACE, typeId, key, key, type,
				(options.length > 0) ? List.of(options) : null, false, null, null));
	}

	private static PlaceCreateRequest request(long typeId, String name, PlaceStatus status,
			Map<String, Object> attributes) {
		return new PlaceCreateRequest(null, typeId, name, null, status, null, attributes.isEmpty() ? null : attributes,
				null, null, null);
	}

	private ResultActions attr(String... conditions) throws Exception {
		return this.mvc.perform(asA(get("/places/list")).param("attr", conditions));
	}

	private ResultActions list(String... params) throws Exception {
		var request = asA(get("/places/list"));
		for (int i = 0; i < params.length; i += 2) {
			request = request.param(params[i], params[i + 1]);
		}
		return this.mvc.perform(request);
	}

}
