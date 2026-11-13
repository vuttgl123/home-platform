package com.home_platform.registry.item;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class ItemVersionIT extends ItemTestData {

	@Test
	void listsAndShowsVersions() throws Exception {
		long id = item(this.room, "Ghế");
		putItem(id, """
				{"version": 1, "name": "Ghế gỗ", "status": "IN_USE"}""").andExpect(status().isOk());

		this.mvc.perform(asA(get("/items/" + id + "/versions")))
			.andExpect(jsonPath("$.items[*].versionNo", contains(2, 1)));
		this.mvc.perform(asA(get("/items/" + id + "/versions/1")))
			.andExpect(jsonPath("$.snapshot.name").value("Ghế"))
			.andExpect(jsonPath("$.snapshot.placeId").value(String.valueOf(this.room)));
	}

	@Test
	void restoresContentButNotPositionOrRemoval() throws Exception {
		long other = place(null, "P102");
		long id = item(this.room, "Ghế");
		putItem(id, """
				{"version": 1, "name": "Ghế gỗ", "quantity": 3, "status": "BROKEN"}""").andExpect(status().isOk());
		action(id, "move", "{\"version\": 2, \"placeId\": \"" + other + "\"}").andExpect(status().isOk());

		restoreVersion(id, 1, 3).andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Ghế"))
			.andExpect(jsonPath("$.quantity").value(1))
			.andExpect(jsonPath("$.status").value("IN_USE"))
			.andExpect(jsonPath("$.placeId").value(String.valueOf(other)))
			.andExpect(jsonPath("$.version").value(4));
		this.mvc.perform(asA(get("/items/" + id + "/versions/4")))
			.andExpect(jsonPath("$.note").value("Restored from version 1"));
	}

	@Test
	void restoringARemovedItemIsAConflict() throws Exception {
		long id = item(this.room, "Ghế");
		action(id, "remove", "{\"version\": 1}").andExpect(status().isOk());

		restoreVersion(id, 1, 2).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Item " + id + " is removed"));
	}

	@Test
	void restoringADeletedItemIsAConflict() throws Exception {
		long id = item(this.room, "Ghế");
		this.mvc.perform(asA(delete("/items/" + id)).param("version", "1")).andExpect(status().isNoContent());

		restoreVersion(id, 1, 2).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Restore item " + id + " before restoring an old version"));
	}

	private ResultActions restoreVersion(long id, int versionNo, int currentVersion) throws Exception {
		return action(id, "versions/" + versionNo + "/restore", "{\"version\": " + currentVersion + "}");
	}

}
