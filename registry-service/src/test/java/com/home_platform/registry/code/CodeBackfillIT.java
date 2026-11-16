package com.home_platform.registry.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.common.history.EntityHistory;
import com.home_platform.registry.RegistryIT;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/** Cấp mã cho bản ghi có từ trước khi có mã tự cấp. Bản ghi không mã được chèn thẳng bằng SQL. */
class CodeBackfillIT extends RegistryIT {

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	EntityHistory history;

	long type;

	@BeforeEach
	void placeType() {
		this.type = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("T", "T", null, null)).id();
	}

	@Test
	void statusCountsLiveRecordsWithoutCode() throws Exception {
		place(WS_A, 900010, this.type, false);
		place(WS_A, 900011, this.type, false);
		place(WS_A, 900012, this.type, true);
		item(WS_A, 900020, 900010, false);
		item(WS_A, 900021, 900010, true);

		this.mvc.perform(asA(get("/codes/status")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.placesWithoutCode").value(2))
			.andExpect(jsonPath("$.itemsWithoutCode").value(1));
	}

	@Test
	void backfillAssignsOldestFirstWithOneVersionEach() throws Exception {
		place(WS_A, 900032, this.type, false);
		place(WS_A, 900030, this.type, false);
		place(WS_A, 900031, this.type, false);

		backfill().andExpect(jsonPath("$.placesAssigned").value(3)).andExpect(jsonPath("$.placesRemaining").value(0));

		for (long id = 900030, n = 1; id <= 900032; id++, n++) {
			assertThat(codeOf("reg_place", id)).isEqualTo("DD-00000" + n);
			assertThat(this.history.get(WS_A, "PLACE", id, 2).note()).isEqualTo("Code assigned");
		}
		assertThat(this.jdbc.sql("SELECT version FROM reg_place WHERE workspace_id = :ws AND id = 900030")
			.param("ws", WS_A).query(Integer.class).single()).isEqualTo(2);
	}

	@Test
	void backfillUsesEachRecordsOwnRule() throws Exception {
		long room = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("ROOM", "Phòng", null, null)).id();
		codeRule("PLACE", room, "P", 4, false);
		place(WS_A, 900040, room, false);
		place(WS_A, 900041, this.type, false);

		backfill().andExpect(status().isOk());

		assertThat(codeOf("reg_place", 900040)).isEqualTo("P-0001");
		assertThat(codeOf("reg_place", 900041)).isEqualTo("DD-000001");
	}

	@Test
	void backfillSkipsDeletedPlacesAndRemovedItems() throws Exception {
		place(WS_A, 900050, this.type, false);
		place(WS_A, 900051, this.type, true);
		item(WS_A, 900060, 900050, true);
		item(WS_A, 900061, 900050, false);

		backfill().andExpect(jsonPath("$.placesAssigned").value(1)).andExpect(jsonPath("$.itemsAssigned").value(1));

		assertThat(codeOf("reg_place", 900051)).isNull();
		assertThat(codeOf("reg_item", 900060)).isNull();
		assertThat(codeOf("reg_item", 900061)).isEqualTo("TS-000001");
	}

	@Test
	void backfillStopsAtOneHundred() throws Exception {
		for (long id = 901000; id < 901101; id++) {
			place(WS_A, id, this.type, false);
		}
		for (long id = 902000; id < 902005; id++) {
			item(WS_A, id, 901000, false);
		}

		backfill().andExpect(jsonPath("$.placesAssigned").value(100))
			.andExpect(jsonPath("$.itemsAssigned").value(0))
			.andExpect(jsonPath("$.placesRemaining").value(1))
			.andExpect(jsonPath("$.itemsRemaining").value(5));
		backfill().andExpect(jsonPath("$.placesAssigned").value(1))
			.andExpect(jsonPath("$.itemsAssigned").value(5))
			.andExpect(jsonPath("$.placesRemaining").value(0))
			.andExpect(jsonPath("$.itemsRemaining").value(0));
	}

	@Test
	void secondRunAssignsNothingTwice() throws Exception {
		place(WS_A, 900070, this.type, false);
		backfill().andExpect(jsonPath("$.placesAssigned").value(1));

		backfill().andExpect(jsonPath("$.placesAssigned").value(0))
			.andExpect(jsonPath("$.itemsAssigned").value(0))
			.andExpect(jsonPath("$.placesRemaining").value(0))
			.andExpect(jsonPath("$.itemsRemaining").value(0));
		assertThat(this.jdbc.sql("SELECT version FROM reg_place WHERE workspace_id = :ws AND id = 900070")
			.param("ws", WS_A).query(Integer.class).single()).isEqualTo(2);
	}

	@Test
	void backfillLeavesOtherWorkspacesAlone() throws Exception {
		long typeB = this.placeTypes.create(CTX_B, new PlaceTypeCreateRequest("T", "T", null, null)).id();
		place(WS_B, 900080, typeB, false);

		backfill().andExpect(jsonPath("$.placesAssigned").value(0));

		assertThat(this.jdbc.sql("SELECT code IS NULL FROM reg_place WHERE workspace_id = :ws AND id = 900080")
			.param("ws", WS_B).query(Boolean.class).single()).isTrue();
	}

	private ResultActions backfill() throws Exception {
		return this.mvc.perform(asA(post("/codes/backfill"))).andExpect(status().isOk());
	}

	private void place(long ws, long id, long typeId, boolean deleted) {
		this.jdbc.sql("INSERT INTO reg_place (workspace_id, id, place_type_id, path, name, created_by, updated_by, deleted_at) "
				+ "VALUES (:ws, :id, :type, :path, 'old', :user, :user, " + (deleted ? "UTC_TIMESTAMP(3)" : "NULL") + ")")
			.param("ws", ws).param("id", id).param("type", typeId).param("path", "/" + id + "/").param("user", USER)
			.update();
	}

	private void item(long ws, long id, long placeId, boolean removed) {
		this.jdbc.sql("INSERT INTO reg_item (workspace_id, id, place_id, name, status, removed_at, created_by, updated_by) "
				+ "VALUES (:ws, :id, :place, 'old', :status, " + (removed ? "'2026-01-01'" : "NULL") + ", :user, :user)")
			.param("ws", ws).param("id", id).param("place", placeId).param("status", removed ? "REMOVED" : "IN_USE")
			.param("user", USER).update();
	}

	private String codeOf(String table, long id) {
		String code = this.jdbc.sql("SELECT COALESCE(code, '') FROM " + table + " WHERE workspace_id = :ws AND id = :id")
			.param("ws", WS_A).param("id", id).query(String.class).single();
		return code.isEmpty() ? null : code;
	}

}
