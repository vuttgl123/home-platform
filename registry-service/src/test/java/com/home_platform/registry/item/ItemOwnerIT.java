package com.home_platform.registry.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.common.history.ChangeAction;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.registry.person.PersonRequest;
import org.junit.jupiter.api.Test;

/** Chủ của món đồ: đặt khi tạo, đổi bằng thao tác riêng, không đổi theo lệnh sửa hay khôi phục version. */
class ItemOwnerIT extends ItemTestData {

	@Test
	void createsAnItemOwnedByAPerson() throws Exception {
		long an = person("An");

		postItem("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"Tủ lạnh\"}".formatted(this.room, an))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.ownerPersonId").value(String.valueOf(an)))
			.andExpect(jsonPath("$.ownerName").value("An"));
	}

	@Test
	void itemWithoutOwnerHasNullOwner() throws Exception {
		getItem(item(this.room, "Ghế")).andExpect(jsonPath("$.ownerPersonId").value(nullValue()))
			.andExpect(jsonPath("$.ownerName").value(nullValue()));
	}

	@Test
	void ownerMustBeALivePersonOfThisWorkspace() throws Exception {
		long gone = person("Gone");
		this.people.delete(CTX_A, gone, 1, null);
		long other = this.people
			.create(CTX_B, new PersonRequest("B", null, null, null, null, null, null, null, null, null, null))
			.id();

		for (long owner : new long[] { 999_999, gone, other }) {
			postItem("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"x\"}".formatted(this.room, owner))
				.andExpect(status().isNotFound());
		}
		assertThat(this.jdbc.sql("SELECT COUNT(*) FROM reg_item WHERE workspace_id = :ws").param("ws", WS_A)
			.query(Integer.class).single()).isZero();
	}

	@Test
	void changeOwnerRecordsAVersion() throws Exception {
		long id = item(this.room, "Tủ");
		long an = person("An");

		action(id, "owner", "{\"version\": 1, \"ownerPersonId\": \"%d\"}".formatted(an))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(2))
			.andExpect(jsonPath("$.ownerName").value("An"));
		assertThat(version(id, 2).action()).isEqualTo(ChangeAction.UPDATE);
		assertThat(version(id, 2).changedFields()).containsExactly("ownerPersonId");
	}

	@Test
	void clearingTheOwnerGivesTheItemBackToThePlace() throws Exception {
		long an = person("An");
		long id = item("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"Tủ\"}".formatted(this.room, an));

		action(id, "owner", "{\"version\": 1, \"ownerPersonId\": null}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ownerPersonId").value(nullValue()));
	}

	@Test
	void changingToTheSameOwnerWritesNothing() throws Exception {
		long an = person("An");
		long id = item("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"Tủ\"}".formatted(this.room, an));

		action(id, "owner", "{\"version\": 1, \"ownerPersonId\": \"%d\"}".formatted(an))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(1));
		assertThat(this.history.list(WS_A, "ITEM", id, new CursorRequest(null, 10)).items()).hasSize(1);
	}

	@Test
	void changeOwnerChecksTheVersion() throws Exception {
		long id = item(this.room, "Tủ");

		action(id, "owner", "{\"version\": 5, \"ownerPersonId\": \"%d\"}".formatted(person("An")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
	}

	@Test
	void removedItemCannotChangeOwner() throws Exception {
		long id = item(this.room, "Tủ");
		action(id, "remove", "{\"version\": 1}").andExpect(status().isOk());

		action(id, "owner", "{\"version\": 2, \"ownerPersonId\": \"%d\"}".formatted(person("An")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Item " + id + " is removed"));
	}

	@Test
	void updateIgnoresASentOwner() throws Exception {
		long id = item(this.room, "Tủ");

		putItem(id, "{\"version\": 1, \"name\": \"Tủ 2\", \"status\": \"IN_USE\", \"ownerPersonId\": \"%d\"}"
			.formatted(person("An"))).andExpect(status().isOk()).andExpect(jsonPath("$.ownerPersonId").value(nullValue()));
	}

	@Test
	void restoringAnOldVersionKeepsTheOwner() throws Exception {
		long id = item(this.room, "Tủ");
		long an = person("An");
		action(id, "owner", "{\"version\": 1, \"ownerPersonId\": \"%d\"}".formatted(an)).andExpect(status().isOk());

		action(id, "versions/1/restore", "{\"version\": 2}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ownerPersonId").value(String.valueOf(an)));
	}

	@Test
	void movingKeepsTheOwner() throws Exception {
		long an = person("An");
		long id = item("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"Tủ\"}".formatted(this.room, an));

		action(id, "move", "{\"version\": 1, \"placeId\": \"%d\"}".formatted(place(null, "P102")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ownerPersonId").value(String.valueOf(an)));
	}

	@Test
	void replacementInheritsTheOwner() throws Exception {
		long an = person("An");
		long id = item("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"Tủ\"}".formatted(this.room, an));

		action(id, "replace", "{\"version\": 1, \"replacement\": {\"name\": \"Tủ mới\"}}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.replacement.ownerPersonId").value(String.valueOf(an)));
	}

	@Test
	void replacementCanNameAnotherOwner() throws Exception {
		long an = person("An");
		long binh = person("Bình");
		long id = item("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"Tủ\"}".formatted(this.room, an));

		action(id, "replace", "{\"version\": 1, \"replacement\": {\"name\": \"Tủ mới\", \"ownerPersonId\": \"%d\"}}"
			.formatted(binh))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.replacement.ownerName").value("Bình"));
	}

	@Test
	void putBackIsRefusedWhileTheOwnerIsDeleted() throws Exception {
		long an = person("An");
		long id = item("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"Tủ\"}".formatted(this.room, an));
		action(id, "remove", "{\"version\": 1}").andExpect(status().isOk());
		this.people.delete(CTX_A, an, 1, null);

		action(id, "put-back", "{\"version\": 2}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Restore person " + an + " first"));
		this.people.restore(CTX_A, an, new com.home_platform.registry.person.PersonRestoreRequest(2, null));
		action(id, "put-back", "{\"version\": 2}").andExpect(status().isOk());
	}

	@Test
	void restoreIsRefusedWhileTheOwnerIsDeleted() throws Exception {
		long an = person("An");
		long id = item("{\"placeId\": \"%d\", \"ownerPersonId\": \"%d\", \"name\": \"Tủ\"}".formatted(this.room, an));
		this.mvc.perform(asA(delete("/items/" + id)).param("version", "1")).andExpect(status().isNoContent());
		this.people.delete(CTX_A, an, 1, null);

		action(id, "restore", "{\"version\": 2}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Restore person " + an + " first"));
	}

	@Test
	void firstEditAfterTheColumnWasAddedDoesNotReportTheOwner() throws Exception {
		this.jdbc.sql("""
				INSERT INTO reg_item (workspace_id, id, place_id, code, name, created_by, updated_by)
				VALUES (:ws, 910001, :place, 'OLD-1', 'Cũ', :user, :user)""")
			.param("ws", WS_A).param("place", this.room).param("user", USER).update();
		this.jdbc.sql("""
				INSERT INTO reg_entity_version (workspace_id, entity_type, entity_id, version_no, action, snapshot, changed_by, changed_at)
				VALUES (:ws, 'ITEM', 910001, 1, 'CREATE', :snapshot, :user, UTC_TIMESTAMP(3))""")
			.param("ws", WS_A).param("user", USER)
			.param("snapshot", "{\"id\": 910001, \"placeId\": %d, \"categoryId\": null, \"code\": \"OLD-1\", \"name\": \"Cũ\", \"quantity\": 1, \"unit\": null, \"status\": \"IN_USE\", \"placedAt\": null, \"removedAt\": null, \"replacedByItemId\": null, \"description\": null, \"attributes\": null, \"coverFileId\": null, \"deletedAt\": null}"
				.formatted(this.room))
			.update();

		putItem(910001, "{\"version\": 1, \"name\": \"Mới\", \"status\": \"IN_USE\"}").andExpect(status().isOk());

		assertThat(version(910001, 2).changedFields()).containsExactly("name");
	}

}
