package com.home_platform.identity.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import com.home_platform.identity.IdentityIT;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class WorkspaceIT extends IdentityIT {

	@Test
	void createMakesTheCallerOwner() throws Exception {
		long owner = user("An");

		String id = JsonPath.read(create(owner, " Nhà của An ").andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isString())
			.andExpect(jsonPath("$.name").value("Nhà của An"))
			.andExpect(jsonPath("$.ownerUserId").value(String.valueOf(owner)))
			.andExpect(jsonPath("$.role").value("OWNER"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andReturn()
			.getResponse()
			.getContentAsString(), "$.id");

		String role = this.jdbc.sql("SELECT role FROM idn_workspace_member WHERE workspace_id = :ws AND user_id = :user")
			.param("ws", Long.parseLong(id))
			.param("user", owner)
			.query(String.class)
			.single();
		assertThat(role).isEqualTo("OWNER");
	}

	@Test
	void unknownOrDisabledUserCannotCreate() throws Exception {
		create(-12345, "X").andExpect(status().isNotFound());

		long disabled = user("Off");
		this.jdbc.sql("UPDATE idn_user SET status = 'DISABLED' WHERE id = :id").param("id", disabled).update();
		create(disabled, "X").andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("User " + disabled + " is disabled"));
	}

	@Test
	void myWorkspacesAreSortedByNameAndPaged() throws Exception {
		long owner = user("An");
		long other = user("Bình");
		for (String name : List.of("Nhà C", "Nhà A", "Nhà B", "Nhà A", "Kho")) {
			create(owner, name).andExpect(status().isCreated());
		}
		create(other, "Của người khác").andExpect(status().isCreated());

		List<String> names = new ArrayList<>();
		List<String> ids = new ArrayList<>();
		String cursor = null;
		for (int guard = 0; guard < 10; guard++) {
			var request = get("/workspaces").param("limit", "2");
			if (cursor != null) {
				request = request.param("cursor", cursor);
			}
			String body = this.mvc.perform(as(owner, request)).andReturn().getResponse().getContentAsString();
			names.addAll(JsonPath.read(body, "$.items[*].name"));
			ids.addAll(JsonPath.read(body, "$.items[*].id"));
			cursor = JsonPath.read(body, "$.nextCursor");
			if (cursor == null) {
				break;
			}
		}

		assertThat(names).containsExactly("Kho", "Nhà A", "Nhà A", "Nhà B", "Nhà C");
		assertThat(ids).doesNotHaveDuplicates();
	}

	@Test
	void outsidersCannotSeeOrRename() throws Exception {
		long owner = user("An");
		long outsider = user("Bình");
		String id = JsonPath.read(create(owner, "Nhà").andReturn().getResponse().getContentAsString(), "$.id");

		this.mvc.perform(as(outsider, get("/workspaces/" + id))).andExpect(status().isNotFound());
		rename(outsider, id, "Của tôi").andExpect(status().isNotFound());
		this.mvc.perform(as(owner, get("/workspaces/" + id))).andExpect(jsonPath("$.name").value("Nhà"));
	}

	@Test
	void renameChangesTheName() throws Exception {
		long owner = user("An");
		String id = JsonPath.read(create(owner, "Nhà").andReturn().getResponse().getContentAsString(), "$.id");

		rename(owner, id, " Nhà mới ").andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Nhà mới"));
	}

	@Test
	void blankNameIs400() throws Exception {
		create(user("An"), "  ").andExpect(status().isBadRequest());
	}

	private ResultActions create(long userId, String name) throws Exception {
		return this.mvc.perform(as(userId, post("/workspaces")).contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\": \"" + name + "\"}"));
	}

	private ResultActions rename(long userId, String id, String name) throws Exception {
		return this.mvc.perform(as(userId, put("/workspaces/" + id)).contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\": \"" + name + "\"}"));
	}

}
