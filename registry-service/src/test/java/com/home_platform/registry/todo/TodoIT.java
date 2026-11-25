package com.home_platform.registry.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.history.EntityVersion;
import com.home_platform.registry.occupancy.OccupancyTestData;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.ResultActions;

/** Danh sách việc cần làm (spec Đợt 8, L5). */
class TodoIT extends OccupancyTestData {

	@Autowired
	private EntityHistory history;

	@Autowired
	private JdbcClient jdbc;

	private long room;

	@BeforeEach
	void room() {
		this.room = place(null, "P101");
	}

	@Test
	void brokenItemsAreListed() throws Exception {
		long fridge = item("{\"name\": \"Tủ lạnh\", \"status\": \"BROKEN\"}");
		long removed = item("{\"name\": \"Quạt\", \"status\": \"BROKEN\"}");
		send("/items/" + removed + "/remove", "{\"version\": 1}");
		long deleted = item("{\"name\": \"Đèn\", \"status\": \"BROKEN\"}");
		this.mvc.perform(asA(delete("/items/" + deleted)).param("version", "1")).andExpect(status().isNoContent());
		item("{\"name\": \"Ghế\"}");

		todos().andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.items[0].key").value("BROKEN:" + fridge))
			.andExpect(jsonPath("$.items[0].kind").value("BROKEN"))
			.andExpect(jsonPath("$.items[0].subjectType").value("ITEM"))
			.andExpect(jsonPath("$.items[0].subjectId").value(String.valueOf(fridge)))
			.andExpect(jsonPath("$.items[0].subjectName").value("Tủ lạnh"))
			.andExpect(jsonPath("$.items[0].context").value("P101"))
			.andExpect(jsonPath("$.items[0].isNew").value(true));
	}

	@Test
	void brokenSinceIsWhenStatusBecameBroken() throws Exception {
		long fridge = item("{\"name\": \"Tủ lạnh\", \"status\": \"BROKEN\"}");
		edit("/items/" + fridge, "{\"version\": 1, \"name\": \"Tủ lạnh lớn\", \"status\": \"BROKEN\"}");
		EntityVersion created = this.history.get(WS_A, "ITEM", fridge, 1);

		todos().andExpect(jsonPath("$.items[0].since").value(created.changedAt().toString()))
			.andExpect(jsonPath("$.items[0].dueOn").value(this.today.toString()));
	}

	@Test
	void warrantyWindowEdges() throws Exception {
		long today = withWarranty("Hết hôm nay", this.today);
		long last = withWarranty("Còn 30 ngày", this.today.plusDays(30));
		withWarranty("Hết hôm qua", this.today.minusDays(1));
		withWarranty("Còn 31 ngày", this.today.plusDays(31));

		todos("WARRANTY").andExpect(jsonPath("$.items[*].key", contains("WARRANTY:" + today, "WARRANTY:" + last)))
			.andExpect(jsonPath("$.items[1].dueOn").value(this.today.plusDays(30).toString()));
	}

	/** Chu kỳ 12 tháng: lùi rồi cộng lại một năm cho đúng ngày đó (trừ 29/02), nên hai đầu khung chính xác. */
	@Test
	void serviceDueWithinSevenDaysOrOverdue() throws Exception {
		long inSeven = withService("Bảy ngày", this.today.plusDays(7).minusMonths(12), 12);
		withService("Tám ngày", this.today.plusDays(8).minusMonths(12), 12);
		LocalDate overduePlaced = this.today.minusDays(40).minusMonths(12);
		long overdue = withService("Quá hạn", overduePlaced, 12);

		todos("SERVICE").andExpect(jsonPath("$.items[*].key", contains("SERVICE:" + overdue, "SERVICE:" + inSeven)))
			.andExpect(jsonPath("$.items[0].dueOn").value(overduePlaced.plusMonths(12).toString()));
	}

	@Test
	void loggingAServiceTakesItOffTheList() throws Exception {
		long ac = withService("Máy lạnh", this.today.minusMonths(2), 1);
		send("/maintenance", """
				{"itemId": "%d", "kind": "SERVICE", "doneOn": "%s", "work": "Vệ sinh"}""".formatted(ac, this.today));

		todos("SERVICE").andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void moveOutMatchesTheOverview() throws Exception {
		long an = person("An");
		long binh = person("Bình");
		long chi = person("Chi");
		long leaving = occupancy(this.room, an, this.today.minusDays(10), this.today.plusDays(30));
		occupancy(this.room, binh, this.today.minusDays(10), this.today.plusDays(31));
		occupancy(this.room, chi, this.today.minusDays(10), null);

		todos("MOVE_OUT").andExpect(jsonPath("$.items[*].key", contains("MOVE_OUT:" + leaving)))
			.andExpect(jsonPath("$.items[0].subjectType").value("PERSON"))
			.andExpect(jsonPath("$.items[0].subjectId").value(String.valueOf(an)))
			.andExpect(jsonPath("$.items[0].subjectName").value("An"))
			.andExpect(jsonPath("$.items[0].context").value("P101"));
		this.mvc.perform(asA(get("/overview"))).andExpect(jsonPath("$.endingSoon.total").value(1));
	}

	@Test
	void customDateWindowEdges() throws Exception {
		fieldDef("PERSON", "id_expiry", "CCCD hết hạn", 10);
		long soon = personWith("Sớm", "id_expiry", this.today.plusDays(10));
		personWith("Xa", "id_expiry", this.today.plusDays(11));
		long lapsed = personWith("Quá", "id_expiry", this.today.minusDays(30));
		personWith("Quá lâu", "id_expiry", this.today.minusDays(31));

		todos("CUSTOM_DATE")
			.andExpect(jsonPath("$.items[*].key",
					contains("CUSTOM_DATE:PERSON:" + lapsed + ":id_expiry", "CUSTOM_DATE:PERSON:" + soon + ":id_expiry")))
			.andExpect(jsonPath("$.items[0].subjectType").value("PERSON"))
			.andExpect(jsonPath("$.items[0].context").value("CCCD hết hạn"));
	}

	@Test
	void customDatesNeedAnActiveReminder() throws Exception {
		String archived = fieldDef("PERSON", "passport", "Hộ chiếu", 30);
		fieldDef("PERSON", "visa", "Thị thực", null);
		personWith("An", "passport", this.today.plusDays(5));
		personWith("Bình", "visa", this.today.plusDays(5));
		send("/field-defs/" + archived + "/archive", "{}");

		todos("CUSTOM_DATE").andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void customDatesOfItemsAndPlaces() throws Exception {
		fieldDef("PLACE", "fire_check", "Kiểm định PCCC", 30);
		fieldDef("ITEM", "inspection", "Kiểm định", 30);
		long building = placeWith("Toà A", "fire_check", this.today.plusDays(3));
		long lift = item("{\"name\": \"Thang máy\", \"attributes\": {\"inspection\": \"%s\"}}"
			.formatted(this.today.plusDays(4)));

		todos("CUSTOM_DATE")
			.andExpect(jsonPath("$.items[*].key", contains("CUSTOM_DATE:PLACE:" + building + ":fire_check",
					"CUSTOM_DATE:ITEM:" + lift + ":inspection")))
			.andExpect(jsonPath("$.items[*].subjectType", contains("PLACE", "ITEM")));
	}

	@Test
	void twoDateFieldsOnOnePersonAreTwoTodos() throws Exception {
		fieldDef("PERSON", "id_expiry", "CCCD hết hạn", 30);
		fieldDef("PERSON", "passport", "Hộ chiếu", 30);
		long an = this.json(send("/people", """
				{"fullName": "An", "attributes": {"id_expiry": "%s", "passport": "%s"}}"""
			.formatted(this.today.plusDays(5), this.today.plusDays(5))));

		todos("CUSTOM_DATE").andExpect(jsonPath("$.items", hasSize(2)))
			.andExpect(jsonPath("$.items[*].subjectId", contains(String.valueOf(an), String.valueOf(an))))
			.andExpect(jsonPath("$.items[*].context", containsInAnyOrder("CCCD hết hạn", "Hộ chiếu")));

		String first = page("CUSTOM_DATE", null, 1);
		String cursor = JsonPath.read(first, "$.nextCursor");
		String second = page("CUSTOM_DATE", cursor, 1);
		List<String> keys = List.of(JsonPath.<String>read(first, "$.items[0].key"),
				JsonPath.<String>read(second, "$.items[0].key"));
		assertThat(keys).doesNotHaveDuplicates();
		assertThat(JsonPath.<Object>read(second, "$.nextCursor")).isNull();
	}

	@Test
	void malformedCustomDateIsSkipped() throws Exception {
		fieldDef("PERSON", "id_expiry", "CCCD hết hạn", 30);
		long an = personWith("An", "id_expiry", this.today.plusDays(5));
		long binh = person("Bình");
		this.jdbc.sql("UPDATE reg_person SET attributes = JSON_OBJECT('id_expiry', '31/12/2026') "
				+ "WHERE workspace_id = :ws AND id = :id")
			.param("ws", WS_A)
			.param("id", binh)
			.update();

		todos("CUSTOM_DATE").andExpect(status().isOk())
			.andExpect(jsonPath("$.items[*].subjectId", contains(String.valueOf(an))));
	}

	@Test
	void deletedPlacesTakeTheirItemsOut() throws Exception {
		long attic = place(null, "Gác");
		item(attic, "{\"name\": \"Đèn\", \"status\": \"BROKEN\"}");
		// Món hỏng còn ở một địa điểm đã xoá (ghi thẳng: API không cho xoá chỗ còn đồ).
		this.jdbc.sql("UPDATE reg_place SET deleted_at = UTC_TIMESTAMP(3) WHERE workspace_id = :ws AND id = :id")
			.param("ws", WS_A)
			.param("id", attic)
			.update();

		todos().andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void filtersByKindAndPages() throws Exception {
		item("{\"name\": \"Tủ\", \"status\": \"BROKEN\"}");
		withWarranty("A", this.today.plusDays(1));
		withWarranty("B", this.today.plusDays(2));
		withWarranty("C", this.today.plusDays(3));

		todos("WARRANTY").andExpect(jsonPath("$.items", hasSize(3)))
			.andExpect(jsonPath("$.items[*].kind", contains("WARRANTY", "WARRANTY", "WARRANTY")));
		todos().andExpect(jsonPath("$.items", hasSize(4)));

		String first = page(null, null, 2);
		String second = page(null, JsonPath.read(first, "$.nextCursor"), 2);
		assertThat(JsonPath.<List<String>>read(first, "$.items[*].subjectName")).containsExactly("Tủ", "A");
		assertThat(JsonPath.<List<String>>read(second, "$.items[*].subjectName")).containsExactly("B", "C");
	}

	private long withWarranty(String name, LocalDate until) throws Exception {
		return item("{\"name\": \"%s\", \"warrantyUntil\": \"%s\"}".formatted(name, until));
	}

	private long withService(String name, LocalDate placedAt, int months) throws Exception {
		return item("{\"name\": \"%s\", \"placedAt\": \"%s\", \"serviceEveryMonths\": %d}".formatted(name, placedAt,
				months));
	}

	private long item(String json) throws Exception {
		return item(this.room, json);
	}

	private long item(long placeId, String json) throws Exception {
		return json(send("/items", "{\"placeId\": \"%d\", ".formatted(placeId) + json.substring(1)));
	}

	private String fieldDef(String target, String key, String label, Integer remindDays) throws Exception {
		String remind = (remindDays != null) ? ", \"remindDays\": " + remindDays : "";
		String body = send("/field-defs", """
				{"target": "%s", "fieldKey": "%s", "label": "%s", "dataType": "DATE"%s}"""
			.formatted(target, key, label, remind)).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private long personWith(String name, String key, LocalDate value) throws Exception {
		return json(send("/people", "{\"fullName\": \"%s\", \"attributes\": {\"%s\": \"%s\"}}".formatted(name, key,
				value)));
	}

	private long placeWith(String name, String key, LocalDate value) throws Exception {
		return json(send("/places", "{\"placeTypeId\": \"%d\", \"name\": \"%s\", \"attributes\": {\"%s\": \"%s\"}}"
			.formatted(this.type, name, key, value)));
	}

	private ResultActions send(String path, String json) throws Exception {
		return this.mvc.perform(asA(post(path)).contentType(MediaType.APPLICATION_JSON)
			.content((json != null) ? json : "{}"));
	}

	private void edit(String path, String json) throws Exception {
		this.mvc.perform(asA(put(path)).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isOk());
	}

	private long json(ResultActions created) throws Exception {
		String body = created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return Long.parseLong(JsonPath.read(body, "$.id"));
	}

	private ResultActions todos() throws Exception {
		return this.mvc.perform(asA(get("/todos"))).andExpect(status().isOk());
	}

	private ResultActions todos(String kind) throws Exception {
		return this.mvc.perform(asA(get("/todos")).param("kind", kind)).andExpect(status().isOk());
	}

	private String page(String kind, String cursor, int limit) throws Exception {
		var request = asA(get("/todos")).param("limit", String.valueOf(limit));
		if (kind != null) {
			request.param("kind", kind);
		}
		if (cursor != null) {
			request.param("cursor", cursor);
		}
		return this.mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
	}

}
