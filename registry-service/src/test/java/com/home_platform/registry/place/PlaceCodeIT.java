package com.home_platform.registry.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.home_platform.common.history.ChangeAction;
import com.home_platform.registry.support.BusinessDate;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Mã của địa điểm do hệ thống cấp khi tạo và không đổi được bằng lệnh sửa. */
class PlaceCodeIT extends PlaceTestData {

	@Autowired
	BusinessDate businessDate;

	@Test
	void createIssuesConsecutiveCodes() throws Exception {
		long type = type("T");
		assertThat(codeOf(place(null, type, "A"))).isEqualTo("DD-000001");
		assertThat(codeOf(place(null, type, "B"))).isEqualTo("DD-000002");
	}

	@Test
	void createIgnoresASentCode() throws Exception {
		long type = type("T");
		postPlace("""
				{"placeTypeId": "%d", "name": "A", "code": "X"}""".formatted(type))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value("DD-000001"));
	}

	@Test
	void typeRuleWinsOverTheSharedRule() throws Exception {
		long building = type("BUILDING");
		long room = type("ROOM");
		codeRule("PLACE", 0, "A", 3, false);
		codeRule("PLACE", room, "P", 4, false);

		assertThat(codeOf(place(null, room, "P101"))).isEqualTo("P-0001");
		assertThat(codeOf(place(null, building, "Toà"))).isEqualTo("A-001");
	}

	@Test
	void typesSharingAPrefixShareOneSeries() throws Exception {
		long room = type("ROOM");
		long suite = type("SUITE");
		codeRule("PLACE", room, "P", 4, false);
		codeRule("PLACE", suite, "P", 4, false);

		assertThat(codeOf(place(null, room, "a"))).isEqualTo("P-0001");
		assertThat(codeOf(place(null, suite, "b"))).isEqualTo("P-0002");
	}

	@Test
	void changingThePrefixStartsANewSeriesAndBackContinuesTheOld() throws Exception {
		long type = type("T");
		codeRule("PLACE", 0, "P", 4, false);
		place(null, type, "a");
		place(null, type, "b");
		codeRule("PLACE", 0, "PH", 4, false);
		assertThat(codeOf(place(null, type, "c"))).isEqualTo("PH-0001");
		codeRule("PLACE", 0, "P", 4, false);
		assertThat(codeOf(place(null, type, "d"))).isEqualTo("P-0003");
	}

	@Test
	void counterSkipsCodesAlreadyTaken() throws Exception {
		long type = type("T");
		this.jdbc.sql("""
				INSERT INTO reg_place (workspace_id, id, place_type_id, path, name, code, created_by, updated_by, deleted_at)
				VALUES (:ws, 900001, :type, '/900001/', 'old', 'DD-000001', :user, :user, UTC_TIMESTAMP(3))""")
			.param("ws", WS_A).param("type", type).param("user", USER).update();

		assertThat(codeOf(place(null, type, "new"))).isEqualTo("DD-000002");
	}

	@Test
	void yearlySeriesIgnoresLastYearsCounter() throws Exception {
		long type = type("T");
		int year = this.businessDate.today().getYear();
		codeRule("PLACE", 0, "TS", 4, true);
		this.jdbc.sql("INSERT INTO reg_code_counter (workspace_id, target, series, last_no) VALUES (:ws, 'PLACE', :series, 7)")
			.param("ws", WS_A).param("series", "TS-" + (year - 1) + "-").update();

		assertThat(codeOf(place(null, type, "a"))).isEqualTo("TS-" + year + "-0001");
	}

	@Test
	void updateIgnoresASentCode() throws Exception {
		long type = type("T");
		long id = place(null, type, "A");

		putPlace(id, """
				{"version": 1, "placeTypeId": "%d", "name": "A2", "status": "ACTIVE", "code": "HACK"}""".formatted(type))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("DD-000001"));
		assertThat(version(id, 2).changedFields()).doesNotContain("code");
	}

	@Test
	void changingTheTypeKeepsTheCode() throws Exception {
		long a = type("A");
		long b = type("B");
		codeRule("PLACE", b, "B", 3, false);
		long id = place(null, a, "x");

		putPlace(id, """
				{"version": 1, "placeTypeId": "%d", "name": "x", "status": "ACTIVE"}""".formatted(b))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("DD-000001"));
	}

	@Test
	void otherWorkspaceCountsOnItsOwn() throws Exception {
		long type = type("T");
		place(null, type, "a");
		place(null, type, "b");
		long typeB = this.placeTypes.create(CTX_B, new com.home_platform.registry.placetype.PlaceTypeCreateRequest("T", "T", null, null)).id();

		String body = this.mvc.perform(asB(post("/places")).contentType(MediaType.APPLICATION_JSON)
			.content("{\"placeTypeId\": \"%d\", \"name\": \"c\"}".formatted(typeB)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		assertThat((String) JsonPath.read(body, "$.code")).isEqualTo("DD-000001");
	}

	@Test
	void reissueGivesTheNextCodeAndRecordsIt() throws Exception {
		long type = type("T");
		long id = place(null, type, "A");

		reissue(id, "{\"version\": 1}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("DD-000002"))
			.andExpect(jsonPath("$.version").value(2));
		assertThat(version(id, 2).action()).isEqualTo(ChangeAction.UPDATE);
		assertThat(version(id, 2).changedFields()).containsExactly("code");
		assertThat(version(id, 2).note()).isEqualTo("Code changed from DD-000001");
	}

	@Test
	void reissueOnARecordWithoutCodeNotesCodeAssigned() throws Exception {
		long type = type("T");
		this.jdbc.sql("""
				INSERT INTO reg_place (workspace_id, id, place_type_id, path, name, created_by, updated_by)
				VALUES (:ws, 900002, :type, '/900002/', 'old', :user, :user)""")
			.param("ws", WS_A).param("type", type).param("user", USER).update();

		reissue(900002, "{\"version\": 1}").andExpect(status().isOk()).andExpect(jsonPath("$.code").value("DD-000001"));
		assertThat(version(900002, 2).note()).isEqualTo("Code assigned");
	}

	@Test
	void reissueKeepsTheUsersNote() throws Exception {
		long id = place(null, type("T"), "A");

		reissue(id, "{\"version\": 1, \"note\": \" nhãn bị rách \"}").andExpect(status().isOk());
		assertThat(version(id, 2).note()).isEqualTo("nhãn bị rách");
	}

	@Test
	void reissueFollowsTheCurrentTypesRule() throws Exception {
		long room = type("ROOM");
		long id = place(null, room, "P101");
		codeRule("PLACE", room, "P", 4, false);

		reissue(id, "{\"version\": 1}").andExpect(jsonPath("$.code").value("P-0001"));
	}

	@Test
	void oldCodeNoLongerFindsTheRecord() throws Exception {
		long id = place(null, type("T"), "A");
		reissue(id, "{\"version\": 1}").andExpect(status().isOk());

		this.mvc.perform(asA(get("/places/search").param("code", "DD-000001"))).andExpect(jsonPath("$.items", hasSize(0)));
		this.mvc.perform(asA(get("/places/search").param("code", "DD-000002")))
			.andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.items[0].id").value(String.valueOf(id)));
	}

	@Test
	void oldCodeIsNeverIssuedAgain() throws Exception {
		long type = type("T");
		long id = place(null, type, "A");
		reissue(id, "{\"version\": 1}").andExpect(status().isOk());

		assertThat(codeOf(place(null, type, "B"))).isEqualTo("DD-000003");
	}

	@Test
	void reissueChecksTheVersion() throws Exception {
		long id = place(null, type("T"), "A");

		reissue(id, "{\"version\": 7}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
		assertThat(codeOf(id)).isEqualTo("DD-000001");
	}

	@Test
	void restoringAVersionFromBeforeAReissueKeepsTheNewCode() throws Exception {
		long id = place(null, type("T"), "A");
		reissue(id, "{\"version\": 1}").andExpect(status().isOk());

		this.mvc.perform(asA(post("/places/" + id + "/versions/1/restore")).contentType(MediaType.APPLICATION_JSON)
			.content("{\"version\": 2}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("DD-000002"));
	}

	private org.springframework.test.web.servlet.ResultActions reissue(long id, String json) throws Exception {
		return this.mvc.perform(asA(post("/places/" + id + "/reissue-code")).contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private String codeOf(long id) throws Exception {
		return JsonPath.read(getPlace(id).andReturn().getResponse().getContentAsString(), "$.code");
	}

}
