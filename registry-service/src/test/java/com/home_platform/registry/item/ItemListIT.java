package com.home_platform.registry.item;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class ItemListIT extends ItemTestData {

	@Test
	void currentAndRemovedAtAPlace() throws Exception {
		item(this.room, "Tủ");
		item(this.room, "Bàn");
		long older = item(this.room, "Ghế cũ");
		long newer = item(this.room, "Đèn cũ");
		action(older, "remove", "{\"version\": 1, \"removedAt\": \"2025-01-01\"}").andExpect(status().isOk());
		action(newer, "remove", "{\"version\": 1, \"removedAt\": \"2025-06-01\"}").andExpect(status().isOk());

		this.mvc.perform(asA(get("/items").param("placeId", String.valueOf(this.room))))
			.andExpect(jsonPath("$.items[*].name", contains("Bàn", "Tủ")))
			.andExpect(jsonPath("$.items[0].placeName").value("P101"));
		this.mvc.perform(asA(get("/items").param("placeId", String.valueOf(this.room)).param("removed", "true")))
			.andExpect(jsonPath("$.items[*].name", contains("Đèn cũ", "Ghế cũ")));
	}

	@Test
	void subtreeSkipsRemovedItemsAndDeletedPlaces() throws Exception {
		long building = place(null, "A");
		long r1 = place(building, "R1");
		long r2 = place(building, "R2");
		item(building, "Máy phát");
		item(r1, "Giường");
		long gone = item(r1, "Đã gỡ");
		action(gone, "remove", "{\"version\": 1}").andExpect(status().isOk());
		long hidden = item(r2, "Ẩn");
		this.jdbc.sql("UPDATE reg_place SET deleted_at = UTC_TIMESTAMP(3) WHERE workspace_id = :ws AND id = :id")
			.param("ws", WS_A).param("id", r2).update();
		item(this.room, "Ngoài");

		this.mvc.perform(asA(get("/items").param("subtreeOf", String.valueOf(building))))
			.andExpect(jsonPath("$.items[*].name", contains("Giường", "Máy phát")));
	}

	@Test
	void byCategoryListsCurrentItems() throws Exception {
		long tv = category("Điện tử");
		item("{\"placeId\": \"%d\", \"categoryId\": \"%d\", \"name\": \"Tivi\"}".formatted(this.room, tv));
		long old = item("{\"placeId\": \"%d\", \"categoryId\": \"%d\", \"name\": \"Radio\"}".formatted(this.room, tv));
		action(old, "remove", "{\"version\": 1}").andExpect(status().isOk());
		item(this.room, "Ghế");

		this.mvc.perform(asA(get("/items").param("categoryId", String.valueOf(tv))))
			.andExpect(jsonPath("$.items[*].name", contains("Tivi")))
			.andExpect(jsonPath("$.items[0].categoryName").value("Điện tử"));
	}

	@Test
	void searchByNameIncludesRemovedItems() throws Exception {
		item(this.room, "Ghế gỗ");
		long old = item(this.room, "ghe nhựa");
		action(old, "remove", "{\"version\": 1}").andExpect(status().isOk());
		item(this.room, "Bàn");

		this.mvc.perform(asA(get("/items/search").param("name", "GHE")))
			.andExpect(jsonPath("$.items[*].name", containsInAnyOrder("Ghế gỗ", "ghe nhựa")));
	}

	@Test
	void searchByCodeMatchesExactly() throws Exception {
		item("{\"placeId\": \"%d\", \"name\": \"a\", \"code\": \"SN-1\"}".formatted(this.room));
		item("{\"placeId\": \"%d\", \"name\": \"b\", \"code\": \"SN-10\"}".formatted(this.room));

		this.mvc.perform(asA(get("/items/search").param("code", "sn-1")))
			.andExpect(jsonPath("$.items[*].name", contains("a")));
	}

	@Test
	void exactlyOneFilter() throws Exception {
		this.mvc.perform(asA(get("/items")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Give exactly one of placeId, subtreeOf or categoryId"));
		this.mvc.perform(asA(get("/items/search")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Give exactly one of name or code"));
	}

}
