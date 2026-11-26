package com.home_platform.registry.person;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.occupancy.OccupancyTestData;

/** {@code GET /people} có tìm chữ, lọc nơi đang ở và mục tuỳ biến (spec Đợt 11, S4). */
class PersonFilterIT extends OccupancyTestData {

	@Autowired
	private FieldDefService fieldDefs;

	@Test
	void textMatchesNamePhoneEmailIdNumberAndNote() throws Exception {
		create(new PersonRequest("An", "0905123456", null, null, null, null, null, null, null, null, null));
		create(new PersonRequest("Bình", null, "binh@gmail.com", null, null, null, null, null, null, null, null));
		create(new PersonRequest("Chi", null, null, "CCCD", "079012345678", null, null, null, null, null, null));
		create(new PersonRequest("Dũng", null, null, null, null, null, null, "Nuôi mèo", null, null, null));
		person("Em");

		people("q", "0905").andExpect(jsonPath("$.items[*].fullName", contains("An")));
		people("q", "GMAIL").andExpect(jsonPath("$.items[*].fullName", contains("Bình")));
		people("q", "0790").andExpect(jsonPath("$.items[*].fullName", contains("Chi")));
		people("q", "nuoi meo").andExpect(jsonPath("$.items[*].fullName", contains("Dũng")));
		people("q", "dung").andExpect(jsonPath("$.items[*].fullName", contains("Dũng")));
	}

	@Test
	void livingYesAndNo() throws Exception {
		long room = place(null, "P1");
		occupancy(room, person("Đang ở"), this.today.minusDays(1), null);
		occupancy(room, person("Đã đi"), this.today.minusDays(9), this.today.minusDays(1));
		person("Chưa ở");

		people("living", "true").andExpect(jsonPath("$.items[*].fullName", contains("Đang ở")));
		people("living", "false").andExpect(jsonPath("$.items[*].fullName", contains("Chưa ở", "Đã đi")));
	}

	@Test
	void buildingMeansLivingThereOrInside() throws Exception {
		long a = place(null, "Toà A");
		long room = place(a, "P1");
		long b = place(null, "Toà B");
		occupancy(a, person("Ở toà"), this.today, null);
		occupancy(room, person("Ở phòng"), this.today, null);
		occupancy(b, person("Ở B"), this.today, null);

		people("rootId", Long.toString(a)).andExpect(jsonPath("$.items[*].fullName", contains("Ở phòng", "Ở toà")));
		people("rootId", Long.toString(a), "living", "true")
			.andExpect(jsonPath("$.items[*].fullName", contains("Ở phòng", "Ở toà")));
	}

	@Test
	void personLivingInTwoPlacesAppearsOnce() throws Exception {
		long a = place(null, "Toà A");
		long twin = person("Hai nơi");
		occupancy(place(a, "P1"), twin, this.today, null);
		occupancy(place(a, "P2"), twin, this.today, null);
		person("Chưa ở");

		people("living", "true").andExpect(jsonPath("$.items[*].fullName", contains("Hai nơi")));
		people("rootId", Long.toString(a)).andExpect(jsonPath("$.items[*].fullName", contains("Hai nơi")));
	}

	@Test
	void livingFalseWithBuildingIs400() throws Exception {
		long a = place(null, "Toà A");

		people("rootId", Long.toString(a), "living", "false").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("rootId needs living to be true or empty"));
	}

	@Test
	void customFieldsFilterPeople() throws Exception {
		field("que_quan", FieldDataType.TEXT);
		field("cccd_het_han", FieldDataType.DATE);
		create(new PersonRequest("Lan", null, null, null, null, null, null, null,
				Map.of("que_quan", "Huế", "cccd_het_han", "2026-06-30"), null, null));
		create(new PersonRequest("Minh", null, null, null, null, null, null, null,
				Map.of("que_quan", "Hà Nội", "cccd_het_han", "2030-01-01"), null, null));

		people("attr", "que_quan:has:hue").andExpect(jsonPath("$.items[*].fullName", contains("Lan")));
		people("attr", "cccd_het_han:lte:2026-12-31").andExpect(jsonPath("$.items[*].fullName", contains("Lan")));
		people("attr", "que_quan:has:hà", "q", "minh").andExpect(jsonPath("$.items[*].fullName", contains("Minh")));
	}

	private void create(PersonRequest request) {
		this.people.create(CTX_A, request);
	}

	private void field(String key, FieldDataType type) {
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.PERSON, 0L, key, key, type, null, false,
				null, null));
	}

	private ResultActions people(String... params) throws Exception {
		var request = asA(get("/people"));
		for (int i = 0; i < params.length; i += 2) {
			request = request.param(params[i], params[i + 1]);
		}
		return this.mvc.perform(request);
	}

}
