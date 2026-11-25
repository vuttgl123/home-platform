package com.home_platform.registry.maintenance;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.registry.item.ItemTestData;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Dòng bảo trì trong Hoạt động, kèm món đồ của nó để dẫn link (spec Đợt 8, mục 4.2). */
class MaintenanceActivityIT extends ItemTestData {

	@Test
	void maintenanceShowsUpWithItsItem() throws Exception {
		long fridge = item(this.room, "Tủ lạnh");
		this.mvc.perform(asA(post("/maintenance")).contentType(MediaType.APPLICATION_JSON).content("""
				{"itemId": "%d", "kind": "REPAIR", "doneOn": "2026-03-01", "work": "Thay ron cửa"}"""
			.formatted(fridge))).andExpect(status().isCreated());

		this.mvc.perform(asA(get("/activity")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].entityType").value("MAINTENANCE"))
			.andExpect(jsonPath("$.items[0].entityName").value("Tủ lạnh: Thay ron cửa"))
			.andExpect(jsonPath("$.items[0].parentId").value(String.valueOf(fridge)))
			.andExpect(jsonPath("$.items[1].entityType").value("ITEM"))
			.andExpect(jsonPath("$.items[1].parentId").doesNotExist());

		this.mvc.perform(asA(get("/activity")).param("entityType", "MAINTENANCE"))
			.andExpect(jsonPath("$.items[*].entityType", contains("MAINTENANCE")))
			.andExpect(jsonPath("$.items[*].entityName", everyItem(is("Tủ lạnh: Thay ron cửa"))));
	}

}
