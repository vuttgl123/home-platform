package com.home_platform.registry.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.common.history.ChangeAction;
import com.home_platform.common.history.EntityVersion;
import com.home_platform.registry.item.ItemTestData;
import com.home_platform.registry.support.BusinessDate;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Nhật ký bảo trì của món đồ (spec Đợt 8, L1, L2). */
class MaintenanceIT extends ItemTestData {

	@Autowired
	private BusinessDate businessDate;

	@Test
	void logsAndListsNewestFirst() throws Exception {
		long fridge = item(this.room, "Tủ lạnh");
		long first = entry(fridge, "REPAIR", "2026-03-01", "Thay ron cửa");
		entry(fridge, "SERVICE", "2026-05-10", "Vệ sinh dàn nóng");
		entry(fridge, "REPAIR", "2026-04-02", "Thay bóng đèn");

		this.mvc.perform(asA(get("/maintenance")).param("itemId", String.valueOf(fridge)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[*].work", contains("Vệ sinh dàn nóng", "Thay bóng đèn", "Thay ron cửa")))
			.andExpect(jsonPath("$.items[0].kind").value("SERVICE"))
			.andExpect(jsonPath("$.items[0].version").value(1));
		EntityVersion created = this.history.get(WS_A, "MAINTENANCE", first, 1);
		assertThat(created.action()).isEqualTo(ChangeAction.CREATE);
	}

	@Test
	void validatesFields() throws Exception {
		long fridge = item(this.room, "Tủ lạnh");
		log("{\"itemId\": \"%d\", \"kind\": \"REPAIR\", \"doneOn\": \"2026-03-01\"}".formatted(fridge))
			.andExpect(status().isBadRequest());
		log(body(fridge, "REPAIR", "2026-03-01", "x".repeat(501))).andExpect(status().isBadRequest());
		log(body(fridge, "REPAIR", this.businessDate.today().plusDays(1).toString(), "Sửa"))
			.andExpect(status().isBadRequest());
		log(body(fridge, "REPAIR", this.businessDate.today().toString(), "Sửa")).andExpect(status().isCreated());

		long gone = item(this.room, "Ghế");
		this.mvc.perform(asA(delete("/items/" + gone)).param("version", "1")).andExpect(status().isNoContent());
		log(body(gone, "REPAIR", "2026-03-01", "Sửa")).andExpect(status().isNotFound());
	}

	@Test
	void removedItemStillTakesEntries() throws Exception {
		long chair = item(this.room, "Ghế");
		action(chair, "remove", "{\"version\": 1}").andExpect(status().isOk());
		log(body(chair, "REPAIR", "2026-03-01", "Ghi muộn")).andExpect(status().isCreated());
	}

	@Test
	void markFixedPutsABrokenItemBackInUse() throws Exception {
		long fridge = item("{\"placeId\": \"%d\", \"name\": \"Tủ lạnh\", \"status\": \"BROKEN\"}".formatted(this.room));
		log(fixed(fridge, 1, "Thay block")).andExpect(status().isCreated());

		getItem(fridge).andExpect(jsonPath("$.status").value("IN_USE")).andExpect(jsonPath("$.version").value(2));
		assertThat(version(fridge, 2).note()).startsWith("Đã sửa: Thay block");
		assertThat(version(fridge, 2).action()).isEqualTo(ChangeAction.UPDATE);
	}

	@Test
	void fixedNoteIsCutTo255() throws Exception {
		long fridge = item("{\"placeId\": \"%d\", \"name\": \"Tủ lạnh\", \"status\": \"BROKEN\"}".formatted(this.room));
		log(fixed(fridge, 1, "y".repeat(500))).andExpect(status().isCreated());
		assertThat(version(fridge, 2).note()).hasSize(255);
	}

	@Test
	void staleItemVersionWritesNothing() throws Exception {
		long fridge = item("{\"placeId\": \"%d\", \"name\": \"Tủ lạnh\", \"status\": \"BROKEN\"}".formatted(this.room));
		putItem(fridge, "{\"version\": 1, \"name\": \"Tủ lạnh lớn\", \"status\": \"BROKEN\"}")
			.andExpect(status().isOk());

		log(fixed(fridge, 1, "Thay block")).andExpect(status().isConflict());

		list(fridge).andExpect(jsonPath("$.items", hasSize(0)));
		getItem(fridge).andExpect(jsonPath("$.status").value("BROKEN")).andExpect(jsonPath("$.version").value(2));
	}

	@Test
	void markFixedOnlyForBrokenRepairs() throws Exception {
		long working = item(this.room, "Quạt");
		log(fixed(working, 1, "Tra dầu")).andExpect(status().isBadRequest());

		long broken = item("{\"placeId\": \"%d\", \"name\": \"Tủ lạnh\", \"status\": \"BROKEN\"}".formatted(this.room));
		log("""
				{"itemId": "%d", "kind": "SERVICE", "doneOn": "2026-03-01", "work": "Vệ sinh",
				 "markFixed": true, "itemVersion": 1}""".formatted(broken)).andExpect(status().isBadRequest());
		log("""
				{"itemId": "%d", "kind": "REPAIR", "doneOn": "2026-03-01", "work": "Sửa", "markFixed": true}"""
			.formatted(broken)).andExpect(status().isBadRequest());
		list(broken).andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void editDeleteRestore() throws Exception {
		long fridge = item(this.room, "Tủ lạnh");
		long id = entry(fridge, "REPAIR", "2026-03-01", "Thay ron");

		putEntry(id, """
				{"version": 1, "kind": "REPAIR", "doneOn": "2026-03-02", "work": "Thay ron cửa",
				 "doneBy": "Thợ Minh", "note": "Còn bảo hành"}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.work").value("Thay ron cửa"))
			.andExpect(jsonPath("$.doneBy").value("Thợ Minh"))
			.andExpect(jsonPath("$.version").value(2));
		assertThat(this.history.get(WS_A, "MAINTENANCE", id, 2).action()).isEqualTo(ChangeAction.UPDATE);
		putEntry(id, "{\"version\": 1, \"kind\": \"REPAIR\", \"doneOn\": \"2026-03-02\", \"work\": \"Cũ\"}")
			.andExpect(status().isConflict());

		this.mvc.perform(asA(delete("/maintenance/" + id)).param("version", "2")).andExpect(status().isNoContent());
		list(fridge).andExpect(jsonPath("$.items", hasSize(0)));
		assertThat(this.history.get(WS_A, "MAINTENANCE", id, 3).action()).isEqualTo(ChangeAction.DELETE);

		this.mvc
			.perform(asA(post("/maintenance/" + id + "/restore")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"version\": 3}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(4));
		list(fridge).andExpect(jsonPath("$.items[*].work", contains("Thay ron cửa")));
		assertThat(this.history.get(WS_A, "MAINTENANCE", id, 4).action()).isEqualTo(ChangeAction.RESTORE);
	}

	@Test
	void deletedItemHidesAndLocksItsLog() throws Exception {
		long fridge = item(this.room, "Tủ lạnh");
		long id = entry(fridge, "REPAIR", "2026-03-01", "Thay ron");
		this.mvc.perform(asA(delete("/items/" + fridge)).param("version", "1")).andExpect(status().isNoContent());

		list(fridge).andExpect(status().isNotFound());
		putEntry(id, "{\"version\": 1, \"kind\": \"REPAIR\", \"doneOn\": \"2026-03-02\", \"work\": \"Sửa\"}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail", startsWith("Restore item")));
		this.mvc.perform(asA(delete("/maintenance/" + id)).param("version", "1")).andExpect(status().isConflict());

		this.mvc
			.perform(asA(post("/items/" + fridge + "/restore")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"version\": 2}"))
			.andExpect(status().isOk());
		list(fridge).andExpect(jsonPath("$.items[*].work", contains("Thay ron")));
	}

	@Test
	void onlyServiceEntriesMoveTheNextServiceDate() throws Exception {
		long ac = item("""
				{"placeId": "%d", "name": "Máy lạnh", "placedAt": "2026-01-10", "serviceEveryMonths": 3}"""
			.formatted(this.room));
		entry(ac, "REPAIR", "2026-05-01", "Thay tụ");
		getItem(ac).andExpect(jsonPath("$.nextServiceOn").value("2026-04-10"));

		long service = entry(ac, "SERVICE", "2026-05-01", "Vệ sinh");
		getItem(ac).andExpect(jsonPath("$.nextServiceOn").value("2026-08-01"));

		this.mvc.perform(asA(delete("/maintenance/" + service)).param("version", "1"))
			.andExpect(status().isNoContent());
		getItem(ac).andExpect(jsonPath("$.nextServiceOn").value("2026-04-10"));
	}

	@Test
	void otherWorkspaceIs404() throws Exception {
		long fridge = item(this.room, "Tủ lạnh");
		long id = entry(fridge, "REPAIR", "2026-03-01", "Thay ron");
		this.mvc.perform(asB(get("/maintenance")).param("itemId", String.valueOf(fridge)))
			.andExpect(status().isNotFound());
		this.mvc.perform(asB(delete("/maintenance/" + id)).param("version", "1")).andExpect(status().isNotFound());
	}

	private long entry(long itemId, String kind, String doneOn, String work) throws Exception {
		String response = log(body(itemId, kind, doneOn, work)).andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return Long.parseLong(JsonPath.read(response, "$.id"));
	}

	private static String body(long itemId, String kind, String doneOn, String work) {
		return "{\"itemId\": \"%d\", \"kind\": \"%s\", \"doneOn\": \"%s\", \"work\": \"%s\"}".formatted(itemId, kind,
				doneOn, work);
	}

	private static String fixed(long itemId, int itemVersion, String work) {
		return """
				{"itemId": "%d", "kind": "REPAIR", "doneOn": "2026-03-01", "work": "%s",
				 "markFixed": true, "itemVersion": %d}""".formatted(itemId, work, itemVersion);
	}

	private ResultActions log(String json) throws Exception {
		return this.mvc.perform(asA(post("/maintenance")).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions putEntry(long id, String json) throws Exception {
		return this.mvc.perform(asA(put("/maintenance/" + id)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions list(long itemId) throws Exception {
		return this.mvc.perform(asA(get("/maintenance")).param("itemId", String.valueOf(itemId)));
	}

}
