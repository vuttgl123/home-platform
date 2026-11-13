package com.home_platform.registry.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import com.home_platform.common.history.ChangeAction;
import com.home_platform.registry.support.BusinessDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class PlaceDeleteIT extends PlaceTestData {

	@Autowired
	BusinessDate businessDate;

	private final AtomicLong ids = new AtomicLong(900000);

	@Test
	void deletesAndRestoresWithVersions() throws Exception {
		long id = place(null, type("T"), "A");

		this.mvc.perform(asA(delete("/places/" + id)).param("version", "1").param("note", "x"))
			.andExpect(status().isNoContent());
		getPlace(id).andExpect(status().isNotFound());
		assertThat(version(id, 2).action()).isEqualTo(ChangeAction.DELETE);
		assertThat(version(id, 2).note()).isEqualTo("x");

		restore(id, 2).andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(3))
			.andExpect(jsonPath("$.deletedAt").value(nullValue()));
		assertThat(version(id, 3).action()).isEqualTo(ChangeAction.RESTORE);
	}

	@Test
	void placeWithChildrenOccupantsOrItemsCannotBeDeleted() throws Exception {
		long type = type("T");
		long a = place(null, type, "A");
		place(a, type, "child");
		occupancy(a, LocalDate.of(2020, 1, 1), null);
		item(a, null);

		remove(a, 1).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Place " + a + " still has 1 child places, 1 occupancies and 1 items"));
	}

	@Test
	void pastOccupantsAndRemovedItemsDoNotBlock() throws Exception {
		long a = place(null, type("T"), "A");
		LocalDate today = this.businessDate.today();
		occupancy(a, today.minusYears(1), today.minusDays(1));
		item(a, today);

		remove(a, 1).andExpect(status().isNoContent());
	}

	@Test
	void futureOccupantsBlock() throws Exception {
		long a = place(null, type("T"), "A");
		occupancy(a, this.businessDate.today().plusDays(1), null);

		remove(a, 1).andExpect(status().isConflict());
	}

	@Test
	void deletedChildDoesNotBlock() throws Exception {
		long type = type("T");
		long a = place(null, type, "A");
		long child = place(a, type, "child");
		remove(child, 1).andExpect(status().isNoContent());

		remove(a, 1).andExpect(status().isNoContent());
	}

	@Test
	void restoreNeedsTheParentFirst() throws Exception {
		long type = type("T");
		long p = place(null, type, "P");
		long c = place(p, type, "C");
		remove(c, 1).andExpect(status().isNoContent());
		remove(p, 1).andExpect(status().isNoContent());

		restore(c, 2).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Restore the parent place " + p + " first"));
		restore(p, 2).andExpect(status().isOk());
		restore(c, 2).andExpect(status().isOk());
	}

	@Test
	void restoreOfALivePlaceIsAConflict() throws Exception {
		long id = place(null, type("T"), "A");

		restore(id, 1).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Place " + id + " is not deleted"));
	}

	@Test
	void staleVersionIsAConflict() throws Exception {
		long id = place(null, type("T"), "A");

		remove(id, 7).andExpect(status().isConflict());
		getPlace(id).andExpect(status().isOk());
	}

	private ResultActions remove(long id, int version) throws Exception {
		return this.mvc.perform(asA(delete("/places/" + id)).param("version", String.valueOf(version)));
	}

	private ResultActions restore(long id, int version) throws Exception {
		return this.mvc.perform(asA(post("/places/" + id + "/restore")).contentType(MediaType.APPLICATION_JSON)
			.content("{\"version\": " + version + "}"));
	}

	private void occupancy(long placeId, LocalDate start, LocalDate end) {
		long person = this.ids.incrementAndGet();
		this.jdbc.sql("""
				INSERT INTO reg_person (workspace_id, id, full_name, created_by, updated_by)
				VALUES (:ws, :id, 'Người', :user, :user)""").param("ws", WS_A).param("id", person).param("user", USER)
			.update();
		this.jdbc.sql("""
				INSERT INTO reg_occupancy (workspace_id, id, place_id, person_id, relation, start_date, end_date,
				  created_by, updated_by)
				VALUES (:ws, :id, :place, :person, 'RESIDENT', :start, :end, :user, :user)""")
			.param("ws", WS_A)
			.param("id", this.ids.incrementAndGet())
			.param("place", placeId)
			.param("person", person)
			.param("start", start)
			.param("end", end)
			.param("user", USER)
			.update();
	}

	private void item(long placeId, LocalDate removedAt) {
		this.jdbc.sql("""
				INSERT INTO reg_item (workspace_id, id, place_id, name, removed_at, created_by, updated_by)
				VALUES (:ws, :id, :place, 'Ghế', :removedAt, :user, :user)""")
			.param("ws", WS_A)
			.param("id", this.ids.incrementAndGet())
			.param("place", placeId)
			.param("removedAt", removedAt)
			.param("user", USER)
			.update();
	}

}
