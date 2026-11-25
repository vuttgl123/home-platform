package com.home_platform.registry.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import com.home_platform.registry.occupancy.OccupancyTestData;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.ResultActions;

/** Tóm tắt cho chuông và mốc "đã xem" của từng người (spec Đợt 8, L6). */
class TodoSeenIT extends OccupancyTestData {

	private static final long OTHER_USER = 4343L;

	@Autowired
	private JdbcClient jdbc;

	private long room;

	@BeforeEach
	void room() {
		this.room = place(null, "P101");
	}

	@Test
	void everythingIsNewBeforeTheFirstLook() throws Exception {
		item("{\"name\": \"Tủ lạnh\", \"status\": \"BROKEN\"}");
		item("{\"name\": \"Máy giặt\", \"warrantyUntil\": \"%s\"}".formatted(this.today.plusDays(3)));

		summary().andExpect(jsonPath("$.total").value(2))
			.andExpect(jsonPath("$.newCount").value(2))
			.andExpect(jsonPath("$.byKind.BROKEN").value(1))
			.andExpect(jsonPath("$.byKind.WARRANTY").value(1))
			.andExpect(jsonPath("$.byKind.CUSTOM_DATE").value(0))
			.andExpect(jsonPath("$.latest", hasSize(2)))
			.andExpect(jsonPath("$.latest[0].isNew").value(true));
	}

	@Test
	void seenClearsNewAndStaysCleared() throws Exception {
		item("{\"name\": \"Tủ lạnh\", \"status\": \"BROKEN\"}");

		this.mvc.perform(asA(post("/todos/seen"))).andExpect(status().isNoContent());

		summary().andExpect(jsonPath("$.newCount").value(0)).andExpect(jsonPath("$.total").value(1));
		summary().andExpect(jsonPath("$.newCount").value(0));
		this.mvc.perform(asA(get("/todos"))).andExpect(jsonPath("$.items[0].isNew").value(false));
	}

	@Test
	void aFreshlySetWarrantyIsNew() throws Exception {
		long washer = item("{\"name\": \"Máy giặt\"}");
		seenAt(Instant.now().minus(1, ChronoUnit.MINUTES));

		edit(washer, "{\"version\": 1, \"name\": \"Máy giặt\", \"status\": \"IN_USE\", \"warrantyUntil\": \"%s\"}"
			.formatted(this.today.plusDays(20)));

		this.mvc.perform(asA(get("/todos")))
			.andExpect(jsonPath("$.items[0].kind").value("WARRANTY"))
			.andExpect(jsonPath("$.items[0].isNew").value(true));
	}

	@Test
	void renamingDoesNotMakeAWarrantyNew() throws Exception {
		long washer = item("{\"name\": \"Máy giặt\", \"warrantyUntil\": \"%s\"}".formatted(this.today.plusDays(20)));
		backdate("ITEM", washer, Instant.now().minus(10, ChronoUnit.MINUTES));
		seenAt(Instant.now().minus(5, ChronoUnit.MINUTES));

		edit(washer, "{\"version\": 1, \"name\": \"Máy giặt cửa ngang\", \"status\": \"IN_USE\", \"warrantyUntil\": \"%s\"}"
			.formatted(this.today.plusDays(20)));

		summary().andExpect(jsonPath("$.newCount").value(0));
	}

	@Test
	void aFreshlyEnteredDocumentDateIsNew() throws Exception {
		this.mvc.perform(asA(post("/field-defs")).contentType(MediaType.APPLICATION_JSON).content("""
				{"target": "PERSON", "fieldKey": "id_expiry", "label": "CCCD hết hạn", "dataType": "DATE",
				 "remindDays": 30}""")).andExpect(status().isCreated());
		backdateFieldDefs(Instant.now().minus(10, ChronoUnit.MINUTES));
		seenAt(Instant.now().minus(1, ChronoUnit.MINUTES));

		this.mvc.perform(asA(post("/people")).contentType(MediaType.APPLICATION_JSON).content("""
				{"fullName": "An", "attributes": {"id_expiry": "%s"}}""".formatted(this.today.plusDays(20))))
			.andExpect(status().isCreated());

		summary().andExpect(jsonPath("$.newCount").value(1)).andExpect(jsonPath("$.latest[0].kind").value("CUSTOM_DATE"));
	}

	@Test
	void seenIsPerUser() throws Exception {
		item("{\"name\": \"Tủ lạnh\", \"status\": \"BROKEN\"}");
		this.mvc.perform(asA(post("/todos/seen"))).andExpect(status().isNoContent());

		this.mvc
			.perform(get("/todos/summary").header("X-Workspace-Id", WS_A).header("X-User-Id", OTHER_USER))
			.andExpect(jsonPath("$.newCount").value(1));
	}

	@Test
	void latestHoldsTenNewestBySince() throws Exception {
		for (int i = 0; i < 12; i++) {
			item("{\"name\": \"Món %d\", \"status\": \"BROKEN\"}".formatted(i));
		}

		String body = summary().andExpect(jsonPath("$.total").value(12))
			.andExpect(jsonPath("$.latest", hasSize(10)))
			.andReturn()
			.getResponse()
			.getContentAsString();
		List<Instant> since = JsonPath.<List<String>>read(body, "$.latest[*].since")
			.stream()
			.map(Instant::parse)
			.toList();
		assertThat(since).isSortedAccordingTo(Comparator.reverseOrder());
	}

	private long item(String json) throws Exception {
		String body = this.mvc
			.perform(asA(post("/items")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"placeId\": \"%d\", ".formatted(this.room) + json.substring(1)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return Long.parseLong(JsonPath.read(body, "$.id"));
	}

	private void edit(long itemId, String json) throws Exception {
		this.mvc.perform(asA(put("/items/" + itemId)).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isOk());
	}

	private ResultActions summary() throws Exception {
		return this.mvc.perform(asA(get("/todos/summary"))).andExpect(status().isOk());
	}

	/** Ghi thẳng mốc đã xem, để không phụ thuộc vào việc hai lệnh rơi vào cùng một mili giây. */
	private void seenAt(Instant at) {
		this.jdbc.sql("INSERT INTO reg_todo_seen (workspace_id, user_id, seen_at) VALUES (:ws, :user, :at) "
				+ "ON DUPLICATE KEY UPDATE seen_at = :at")
			.param("ws", WS_A)
			.param("user", USER)
			.param("at", utc(at))
			.update();
	}

	/** Lùi mọi version và thời điểm sửa của một bản ghi về {@code at}, như thể nó được ghi từ trước. */
	private void backdate(String entityType, long id, Instant at) {
		this.jdbc.sql("UPDATE reg_entity_version SET changed_at = :at WHERE workspace_id = :ws "
				+ "AND entity_type = :type AND entity_id = :id")
			.param("at", utc(at))
			.param("ws", WS_A)
			.param("type", entityType)
			.param("id", id)
			.update();
		this.jdbc.sql("UPDATE reg_item SET created_at = :at, updated_at = :at WHERE workspace_id = :ws AND id = :id")
			.param("at", utc(at))
			.param("ws", WS_A)
			.param("id", id)
			.update();
	}

	private void backdateFieldDefs(Instant at) {
		this.jdbc.sql("UPDATE reg_field_def SET created_at = :at, updated_at = :at WHERE workspace_id = :ws")
			.param("at", utc(at))
			.param("ws", WS_A)
			.update();
	}

	private static LocalDateTime utc(Instant at) {
		return LocalDateTime.ofInstant(at.truncatedTo(ChronoUnit.MILLIS), ZoneOffset.UTC);
	}

}
