package com.home_platform.registry.itemcategory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.common.web.ConflictException;
import com.home_platform.registry.RegistryIT;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class ItemCategoryIT extends RegistryIT {

	@Autowired
	ItemCategoryService categories;

	@Test
	void createsListsUpdatesAndDeletes() throws Exception {
		create("""
				{"name": "Điện tử", "sortOrder": 2}""");
		String furniture = create("""
				{"name": " Nội thất ", "sortOrder": 1}""");

		this.mvc.perform(asA(get("/item-categories")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name", contains("Nội thất", "Điện tử")));

		this.mvc.perform(asA(put("/item-categories/" + furniture)).contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "Bàn ghế", "sortOrder": 3}"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Bàn ghế"))
			.andExpect(jsonPath("$.sortOrder").value(3));
		this.mvc.perform(asA(get("/item-categories"))).andExpect(jsonPath("$[*].name", contains("Điện tử", "Bàn ghế")));

		this.mvc.perform(asA(delete("/item-categories/" + furniture))).andExpect(status().isNoContent());
		this.mvc.perform(asA(get("/item-categories/" + furniture))).andExpect(status().isNotFound());
	}

	@Test
	void namesDifferingOnlyInCaseOrAccentsConflict() throws Exception {
		create("""
				{"name": "Nội thất"}""");

		this.mvc.perform(asA(post("/item-categories")).contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "noi that"}"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Item category noi that already exists"));
		this.mvc.perform(asA(post("/item-categories")).contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "  "}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field", hasItem("name")));
	}

	@Test
	void archivedCategoryIsNotActive() throws Exception {
		long id = Long.parseLong(create("""
				{"name": "Nội thất"}"""));

		this.mvc.perform(asA(post("/item-categories/" + id + "/archive")))
			.andExpect(jsonPath("$.status").value("ARCHIVED"));
		assertThatThrownBy(() -> this.categories.requireActive(WS_A, id)).isInstanceOf(ConflictException.class);

		this.mvc.perform(asA(post("/item-categories/" + id + "/unarchive")))
			.andExpect(jsonPath("$.status").value("ACTIVE"));
		assertThat(this.categories.requireActive(WS_A, id).name()).isEqualTo("Nội thất");
	}

	@Test
	void usedCategoryCannotBeDeleted() throws Exception {
		String id = create("""
				{"name": "Nội thất"}""");
		long type = this.jdbc.sql("SELECT 900100").query(Long.class).single();
		this.jdbc.sql("""
				INSERT INTO reg_place_type (workspace_id, id, code, name) VALUES (:ws, :type, 'T', 'T')""")
			.param("ws", WS_A)
			.param("type", type)
			.update();
		this.jdbc.sql("""
				INSERT INTO reg_place (workspace_id, id, place_type_id, path, name, created_by, updated_by)
				VALUES (:ws, 900001, :type, '/900001/', 'P', :user, :user)""")
			.param("ws", WS_A)
			.param("type", type)
			.param("user", USER)
			.update();
		this.jdbc.sql("""
				INSERT INTO reg_item (workspace_id, id, place_id, category_id, name, created_by, updated_by)
				VALUES (:ws, 900002, 900001, :category, 'Ghế', :user, :user)""")
			.param("ws", WS_A)
			.param("category", Long.parseLong(id))
			.param("user", USER)
			.update();

		this.mvc.perform(asA(delete("/item-categories/" + id)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("ItemCategory " + id + " is used by items; archive it instead"));
	}

	@Test
	void otherWorkspaceSeesNothing() throws Exception {
		String id = create("""
				{"name": "Nội thất"}""");

		this.mvc.perform(asB(get("/item-categories/" + id))).andExpect(status().isNotFound());
		this.mvc.perform(asB(get("/item-categories"))).andExpect(jsonPath("$", hasSize(0)));
		this.mvc.perform(asB(put("/item-categories/" + id)).contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "X"}""")).andExpect(status().isNotFound());
		this.mvc.perform(asB(delete("/item-categories/" + id))).andExpect(status().isNotFound());
	}

	private String create(String json) throws Exception {
		String body = this.mvc
			.perform(asA(post("/item-categories")).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isString())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.id");
	}

}
