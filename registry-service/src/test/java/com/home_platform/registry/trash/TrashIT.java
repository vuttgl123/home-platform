package com.home_platform.registry.trash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.registry.RegistryIT;
import com.home_platform.registry.item.ItemRequest;
import com.home_platform.registry.item.ItemService;
import com.home_platform.registry.person.PersonRequest;
import com.home_platform.registry.person.PersonService;
import com.home_platform.registry.place.PlaceCreateRequest;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class TrashIT extends RegistryIT {

	@Autowired
	PlaceService places;

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	PersonService people;

	@Autowired
	ItemService items;

	long type;

	@BeforeEach
	void placeType() {
		this.type = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("T", "T", null, null)).id();
	}

	@Test
	void listsDeletedPlacesNewestFirst() throws Exception {
		long a = place(null, "A");
		long b = place(null, "B");
		this.places.delete(CTX_A, a, 1, null);
		deletedAt("reg_place", a, "2026-01-01 00:00:01");
		this.places.delete(CTX_A, b, 1, null);
		deletedAt("reg_place", b, "2026-01-01 00:00:02");

		trash("PLACE").andExpect(status().isOk())
			.andExpect(jsonPath("$.items[*].name", contains("B", "A")))
			.andExpect(jsonPath("$.items[0].entityType").value("PLACE"))
			.andExpect(jsonPath("$.items[0].id").value(String.valueOf(b)))
			.andExpect(jsonPath("$.items[0].code").value("DD-000002"))
			.andExpect(jsonPath("$.items[0].deletedAt").value("2026-01-01T00:00:02Z"))
			.andExpect(jsonPath("$.items[0].deletedBy").value(String.valueOf(USER)))
			.andExpect(jsonPath("$.items[0].version").value(2));
	}

	@Test
	void deletedChildShowsItsParent() throws Exception {
		long building = place(null, "Toà A");
		long room = place(building, "P101");
		this.places.delete(CTX_A, room, 1, null);

		trash("PLACE").andExpect(jsonPath("$.items[0].context").value("Toà A"));
	}

	@Test
	void topLevelPlaceHasNoContext() throws Exception {
		long a = place(null, "A");
		this.places.delete(CTX_A, a, 1, null);

		trash("PLACE").andExpect(jsonPath("$.items[0].context").value(nullValue()));
	}

	@Test
	void deletedItemShowsItsPlace() throws Exception {
		long room = place(null, "P101");
		long tv = this.items.create(CTX_A, new ItemRequest(room, null, null, "Tivi", null, null, null, null, null, null, null, null)).id();
		this.items.delete(CTX_A, tv, 1, null);

		trash("ITEM").andExpect(jsonPath("$.items[0].name").value("Tivi"))
			.andExpect(jsonPath("$.items[0].context").value("P101"))
			.andExpect(jsonPath("$.items[0].code").value("TS-000001"));
	}

	@Test
	void deletedPersonHasNeitherCodeNorContext() throws Exception {
		long an = person("An");
		this.people.delete(CTX_A, an, 1, null);

		trash("PERSON").andExpect(jsonPath("$.items[0].name").value("An"))
			.andExpect(jsonPath("$.items[0].code").value(nullValue()))
			.andExpect(jsonPath("$.items[0].context").value(nullValue()));
	}

	@Test
	void liveRecordsAreNotInTheTrash() throws Exception {
		place(null, "A");
		person("An");

		trash("PLACE").andExpect(jsonPath("$.items", hasSize(0)));
		trash("PERSON").andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void restoredRecordLeavesTheTrash() throws Exception {
		long a = place(null, "A");
		this.places.delete(CTX_A, a, 1, null);
		restorePlace(a, 2).andExpect(status().isOk());

		trash("PLACE").andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void versionFromTheTrashRestoresTheRecord() throws Exception {
		long a = place(null, "A");
		this.places.delete(CTX_A, a, 1, null);
		String body = trash("PLACE").andReturn().getResponse().getContentAsString();
		int version = JsonPath.read(body, "$.items[0].version");

		restorePlace(a, version).andExpect(status().isOk());
	}

	@Test
	void typesAreListedSeparately() throws Exception {
		long a = place(null, "A");
		long an = person("An");
		this.places.delete(CTX_A, a, 1, null);
		this.people.delete(CTX_A, an, 1, null);

		trash("PLACE").andExpect(jsonPath("$.items[*].name", contains("A")));
		trash("PERSON").andExpect(jsonPath("$.items[*].name", contains("An")));
	}

	@Test
	void entityTypeIsRequiredAndChecked() throws Exception {
		this.mvc.perform(asA(get("/trash"))).andExpect(status().isBadRequest());
		this.mvc.perform(asA(get("/trash").param("entityType", "OCCUPANCY"))).andExpect(status().isBadRequest());
	}

	@Test
	void trashPages() throws Exception {
		long a = place(null, "A");
		long b = place(null, "B");
		this.places.delete(CTX_A, a, 1, null);
		this.places.delete(CTX_A, b, 1, null);

		String first = this.mvc.perform(asA(get("/trash").param("entityType", "PLACE").param("limit", "1")))
			.andExpect(jsonPath("$.items", hasSize(1)))
			.andReturn().getResponse().getContentAsString();
		String cursor = JsonPath.read(first, "$.nextCursor");
		assertThat(cursor).isNotNull();
		this.mvc.perform(asA(get("/trash").param("entityType", "PLACE").param("limit", "1").param("cursor", cursor)))
			.andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.nextCursor").value(nullValue()));
	}

	@Test
	void otherWorkspaceSeesAnEmptyTrash() throws Exception {
		long a = place(null, "A");
		this.places.delete(CTX_A, a, 1, null);

		this.mvc.perform(asB(get("/trash").param("entityType", "PLACE"))).andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void trashListsUseAnIndex() throws Exception {
		long a = place(null, "A");
		place(null, "B");
		this.places.delete(CTX_A, a, 1, null);
		for (String table : new String[] { "reg_place", "reg_person", "reg_item" }) {
			var plan = this.jdbc.sql("EXPLAIN SELECT id FROM " + table + " WHERE workspace_id = :ws AND deleted_at IS NOT NULL "
					+ "ORDER BY deleted_at DESC, id DESC LIMIT 21")
				.param("ws", WS_A)
				.query()
				.listOfRows();
			assertThat(plan.get(0).get("type")).as(table).isNotEqualTo("ALL");
		}
	}

	private long place(Long parentId, String name) {
		return this.places.create(CTX_A, new PlaceCreateRequest(parentId, this.type, name, null, null, null, null, null, null))
			.id();
	}

	private long person(String fullName) {
		return this.people.create(CTX_A, new PersonRequest(fullName, null, null, null, null, null, null, null, null, null, null))
			.id();
	}

	private void deletedAt(String table, long id, String at) {
		this.jdbc.sql("UPDATE " + table + " SET deleted_at = :at WHERE workspace_id = :ws AND id = :id")
			.param("at", at).param("ws", WS_A).param("id", id).update();
	}

	private ResultActions trash(String entityType) throws Exception {
		return this.mvc.perform(asA(get("/trash").param("entityType", entityType)));
	}

	private ResultActions restorePlace(long id, int version) throws Exception {
		return this.mvc.perform(asA(post("/places/" + id + "/restore")).contentType(MediaType.APPLICATION_JSON)
			.content("{\"version\": " + version + "}"));
	}

}
