package com.home_platform.registry.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.home_platform.common.history.ChangeAction;
import com.home_platform.registry.support.BusinessDate;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ItemLifecycleIT extends ItemTestData {

	@Autowired
	BusinessDate businessDate;

	@Test
	void movesToAnotherPlace() throws Exception {
		long other = place(null, "P102");
		long id = item(this.room, "Ghế");

		action(id, "move", "{\"version\": 1, \"placeId\": \"" + other + "\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.placeId").value(String.valueOf(other)))
			.andExpect(jsonPath("$.placeName").value("P102"))
			.andExpect(jsonPath("$.version").value(2));
		assertThat(version(id, 2).changedFields()).contains("placeId");
		assertThat(version(id, 1).snapshot().get("placeId").asLong()).isEqualTo(this.room);
	}

	@Test
	void cannotMoveIntoADeletedPlace() throws Exception {
		long gone = place(null, "Gone");
		this.places.delete(CTX_A, gone, 1, null);
		long id = item(this.room, "Ghế");

		action(id, "move", "{\"version\": 1, \"placeId\": \"" + gone + "\"}").andExpect(status().isNotFound());
	}

	@Test
	void removesAndKeepsTheItemInRemovedList() throws Exception {
		long id = item(this.room, "Ghế");

		action(id, "remove", "{\"version\": 1, \"note\": \"hỏng\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REMOVED"))
			.andExpect(jsonPath("$.removedAt").value(this.businessDate.today().toString()))
			.andExpect(jsonPath("$.version").value(2));
		assertThat(version(id, 2).note()).isEqualTo("hỏng");
		getItem(id).andExpect(status().isOk());
	}

	@Test
	void removalDateCannotPrecedePlacement() throws Exception {
		long id = item("{\"placeId\": \"%d\", \"name\": \"Ghế\", \"placedAt\": \"2025-05-01\"}".formatted(this.room));

		action(id, "remove", "{\"version\": 1, \"removedAt\": \"2025-04-30\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("removedAt must not be before placedAt"));
	}

	@Test
	void removedItemIsReadOnly() throws Exception {
		long other = place(null, "P102");
		long id = item(this.room, "Ghế");
		action(id, "remove", "{\"version\": 1}").andExpect(status().isOk());

		putItem(id, """
				{"version": 2, "name": "x", "status": "IN_USE"}""").andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Item " + id + " is removed"));
		action(id, "move", "{\"version\": 2, \"placeId\": \"" + other + "\"}").andExpect(status().isConflict());
		action(id, "remove", "{\"version\": 2}").andExpect(status().isConflict());
	}

	@Test
	void replacesInOneStep() throws Exception {
		long old = item(this.room, "Tivi cũ");

		String body = action(old, "replace", """
				{"version": 1, "replacement": {"name": "Tivi mới", "code": "SN-2"}}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.removed.status").value("REMOVED"))
			.andExpect(jsonPath("$.replacement.name").value("Tivi mới"))
			.andExpect(jsonPath("$.replacement.placeId").value(String.valueOf(this.room)))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String fresh = JsonPath.read(body, "$.replacement.id");
		getItem(old).andExpect(jsonPath("$.replacedByItemId").value(fresh));
		assertThat(version(old, 2).action()).isEqualTo(ChangeAction.UPDATE);
		assertThat(version(Long.parseLong(fresh), 1).action()).isEqualTo(ChangeAction.CREATE);
	}

	@Test
	void replacementCanGoElsewhere() throws Exception {
		long store = place(null, "Kho");
		long old = item(this.room, "Ghế");

		action(old, "replace", """
				{"version": 1, "replacement": {"placeId": "%d", "name": "Ghế mới"}}""".formatted(store))
			.andExpect(jsonPath("$.replacement.placeName").value("Kho"));
	}

	@Test
	void invalidReplacementLeavesTheOldItemAlone() throws Exception {
		long old = item(this.room, "Ghế");

		action(old, "replace", """
				{"version": 1, "replacement": {"name": "  "}}""").andExpect(status().isBadRequest());
		getItem(old).andExpect(jsonPath("$.status").value("IN_USE")).andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void putsBackARemovedItem() throws Exception {
		long id = item(this.room, "Ghế");
		action(id, "remove", "{\"version\": 1}").andExpect(status().isOk());

		action(id, "put-back", "{\"version\": 2}").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("IN_USE"))
			.andExpect(jsonPath("$.removedAt").value(nullValue()))
			.andExpect(jsonPath("$.version").value(3));
		action(id, "put-back", "{\"version\": 3}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Item " + id + " is not removed"));
	}

	@Test
	void replacedItemCannotBeReturned() throws Exception {
		long old = item(this.room, "Tivi cũ");
		String body = action(old, "replace", """
				{"version": 1, "replacement": {"name": "Tivi mới"}}""").andReturn().getResponse().getContentAsString();
		String fresh = JsonPath.read(body, "$.replacement.id");

		action(old, "put-back", "{\"version\": 2}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Item " + old + " was replaced by item " + fresh));
	}

	@Test
	void putBackNeedsThePlaceAlive() throws Exception {
		long shed = place(null, "Shed");
		long id = item(shed, "Cuốc");
		action(id, "remove", "{\"version\": 1}").andExpect(status().isOk());
		this.places.delete(CTX_A, shed, 1, null);

		action(id, "put-back", "{\"version\": 2}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Restore place " + shed + " first"));
		getItem(id).andExpect(jsonPath("$.status").value("REMOVED"));
	}

	@Test
	void deletesAndRestores() throws Exception {
		long id = item(this.room, "Ghế");

		this.mvc.perform(asA(delete("/items/" + id)).param("version", "1")).andExpect(status().isNoContent());
		getItem(id).andExpect(status().isNotFound());
		assertThat(version(id, 2).action()).isEqualTo(ChangeAction.DELETE);

		action(id, "restore", "{\"version\": 2}").andExpect(status().isOk()).andExpect(jsonPath("$.version").value(3));
		assertThat(version(id, 3).action()).isEqualTo(ChangeAction.RESTORE);
	}

	@Test
	void restoreNeedsThePlaceAlive() throws Exception {
		long shed = place(null, "Shed");
		long id = item(shed, "Cuốc");
		this.mvc.perform(asA(delete("/items/" + id)).param("version", "1")).andExpect(status().isNoContent());
		this.places.delete(CTX_A, shed, 1, null);

		action(id, "restore", "{\"version\": 2}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Restore place " + shed + " first"));
	}

	@Test
	void removedTodayByDefaultUsesTheBusinessDate() throws Exception {
		long id = item(this.room, "Ghế");
		LocalDate today = this.businessDate.today();

		action(id, "remove", "{\"version\": 1}").andExpect(jsonPath("$.removedAt").value(today.toString()));
	}

}
