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
		codeRule("ITEM", 0, "SN", 3, false);
		item(this.room, "a");
		for (int i = 2; i <= 10; i++) {
			item(this.room, "b" + i);
		}

		this.mvc.perform(asA(get("/items/search").param("code", "sn-001")))
			.andExpect(jsonPath("$.items[*].name", contains("a")));
	}

	@Test
	void listsCurrentItemsOfAnOwnerByName() throws Exception {
		long an = person("An");
		long binh = person("Bình");
		item("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"Tủ\"}".formatted(this.room, an));
		item("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"Bàn\"}".formatted(this.room, an));
		long removed = item("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"Ghế\"}".formatted(this.room, an));
		action(removed, "remove", "{\"version\": 1}").andExpect(status().isOk());
		long deleted = item("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"Kệ\"}".formatted(this.room, an));
		this.mvc.perform(asA(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/items/" + deleted))
			.param("version", "1")).andExpect(status().isNoContent());
		item("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"Đèn\"}".formatted(this.room, binh));

		this.mvc.perform(asA(get("/items").param("ownerId", String.valueOf(an))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[*].name", contains("Bàn", "Tủ")))
			.andExpect(jsonPath("$.items[0].ownerName").value("An"))
			.andExpect(jsonPath("$.items[0].placeName").value("P101"));
	}

	@Test
	void ownerListOfAnUnknownPersonIs404() throws Exception {
		this.mvc.perform(asA(get("/items").param("ownerId", "999999"))).andExpect(status().isNotFound());
	}

	@Test
	void everyListCarriesOwnerNames() throws Exception {
		long an = person("An");
		item("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"Tủ\"}".formatted(this.room, an));

		this.mvc.perform(asA(get("/items").param("placeId", String.valueOf(this.room))))
			.andExpect(jsonPath("$.items[0].ownerName").value("An"));
	}

	@Test
	void ownedByUsesAnIndex() throws Exception {
		long an = person("An");
		item("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"Tủ\"}".formatted(this.room, an));
		item(this.room, "Ghế");

		var plan = this.jdbc.sql("EXPLAIN SELECT id FROM reg_item WHERE workspace_id = :ws AND deleted_at IS NULL "
				+ "AND owner_person_id = :owner AND removed_at IS NULL ORDER BY name, id")
			.param("ws", WS_A)
			.param("owner", an)
			.query()
			.listOfRows();
		org.assertj.core.api.Assertions.assertThat(plan).singleElement().satisfies(row -> {
			org.assertj.core.api.Assertions.assertThat(row.get("type")).isNotEqualTo("ALL");
			org.assertj.core.api.Assertions.assertThat(row.get("key")).isEqualTo("idx_reg_item_owner");
		});
	}

	@Test
	void wholeWorkspaceListsCurrentItemsByName() throws Exception {
		long other = place(null, "P102");
		item(this.room, "Tủ");
		item(other, "Bàn");
		long removed = item(this.room, "Ghế");
		action(removed, "remove", "{\"version\": 1}").andExpect(status().isOk());
		long deleted = item(other, "Kệ");
		this.mvc.perform(asA(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/items/" + deleted))
			.param("version", "1")).andExpect(status().isNoContent());

		this.mvc.perform(asA(get("/items")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[*].name", contains("Bàn", "Tủ")))
			.andExpect(jsonPath("$.items[0].placeName").value("P102"));
	}

	@Test
	void statusFilterNarrowsTheWholeWorkspace() throws Exception {
		item("{\"placeId\": \"%d\", \"name\": \"Quạt\", \"status\": \"BROKEN\"}".formatted(this.room));
		item(this.room, "Tủ");

		this.mvc.perform(asA(get("/items").param("status", "BROKEN"))).andExpect(jsonPath("$.items[*].name", contains("Quạt")));
	}

	@Test
	void removedStatusListsRemovedItems() throws Exception {
		long removed = item(this.room, "Ghế");
		action(removed, "remove", "{\"version\": 1}").andExpect(status().isOk());
		item(this.room, "Tủ");

		this.mvc.perform(asA(get("/items").param("status", "REMOVED"))).andExpect(jsonPath("$.items[*].name", contains("Ghế")));
	}

	@Test
	void statusCombinesWithCategory() throws Exception {
		long tv = category("Tivi");
		item("{\"placeId\": \"%d\", \"categoryId\": \"%d\", \"name\": \"Tivi hỏng\", \"status\": \"BROKEN\"}".formatted(this.room, tv));
		item("{\"placeId\": \"%d\", \"categoryId\": \"%d\", \"name\": \"Tivi tốt\"}".formatted(this.room, tv));
		item("{\"placeId\": \"%d\", \"name\": \"Quạt hỏng\", \"status\": \"BROKEN\"}".formatted(this.room));

		this.mvc.perform(asA(get("/items").param("status", "BROKEN").param("categoryId", String.valueOf(tv))))
			.andExpect(jsonPath("$.items[*].name", contains("Tivi hỏng")));
	}

	@Test
	void statusWithAPlaceIs400() throws Exception {
		this.mvc.perform(asA(get("/items").param("placeId", String.valueOf(this.room)).param("status", "BROKEN")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("status can only be combined with categoryId or no scope"));
	}

	@Test
	void twoScopesAre400() throws Exception {
		this.mvc.perform(asA(get("/items").param("placeId", String.valueOf(this.room)).param("ownerId", "1")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Give at most one of placeId, subtreeOf, categoryId or ownerId"));
		this.mvc.perform(asA(get("/items/search")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Give exactly one of name or code"));
	}

	@Test
	void unknownStatusIs400() throws Exception {
		this.mvc.perform(asA(get("/items").param("status", "LOST"))).andExpect(status().isBadRequest());
	}

	@Test
	void wholeWorkspacePagesWithoutSkippingOrRepeating() throws Exception {
		for (String name : new String[] { "a", "b", "c", "d", "e" }) {
			item(this.room, name);
		}
		String first = this.mvc.perform(asA(get("/items").param("limit", "2"))).andReturn().getResponse().getContentAsString();
		String cursor = com.jayway.jsonpath.JsonPath.read(first, "$.nextCursor");
		String second = this.mvc.perform(asA(get("/items").param("limit", "2").param("cursor", cursor)))
			.andReturn().getResponse().getContentAsString();
		String cursor2 = com.jayway.jsonpath.JsonPath.read(second, "$.nextCursor");
		this.mvc.perform(asA(get("/items").param("limit", "2").param("cursor", cursor2)))
			.andExpect(jsonPath("$.items[*].name", contains("e")))
			.andExpect(jsonPath("$.nextCursor").doesNotExist());
		org.assertj.core.api.Assertions.assertThat((java.util.List<String>) com.jayway.jsonpath.JsonPath.read(second, "$.items[*].name"))
			.containsExactly("c", "d");
	}

	@Test
	void otherWorkspaceSeesNothing() throws Exception {
		item(this.room, "Tủ");
		this.mvc.perform(asB(get("/items"))).andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	void wholeWorkspaceListsUseAnIndex() throws Exception {
		long tv = category("Tivi");
		item("{\"placeId\": \"%d\", \"categoryId\": \"%d\", \"name\": \"Tivi\", \"status\": \"BROKEN\"}".formatted(this.room, tv));
		item(this.room, "Tủ");
		for (String where : new String[] { "removed_at IS NULL", "status = 'BROKEN'", "category_id = " + tv + " AND removed_at IS NULL" }) {
			var plan = this.jdbc.sql("EXPLAIN SELECT id FROM reg_item WHERE workspace_id = :ws AND deleted_at IS NULL AND "
					+ where + " ORDER BY name, id LIMIT 21")
				.param("ws", WS_A)
				.query()
				.listOfRows();
			org.assertj.core.api.Assertions.assertThat(plan.get(0).get("type")).as(where).isNotEqualTo("ALL");
		}
	}

}
