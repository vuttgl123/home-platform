package com.home_platform.registry.ownership;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.home_platform.registry.person.PersonRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class OwnershipIT extends OwnershipTestData {

	private static final LocalDate JAN = LocalDate.of(2026, 1, 1);

	@Test
	void createsAnOwnershipWithShareAndDocument() throws Exception {
		long room = place(null, "P101");
		long an = person("An");

		post("/ownerships", """
				{"placeId": "%d", "personId": "%d", "sharePercent": 50, "startDate": "2026-01-01", "documentNo": " CS 123456 "}"""
			.formatted(room, an))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.placeName").value("P101"))
			.andExpect(jsonPath("$.personFullName").value("An"))
			.andExpect(jsonPath("$.sharePercent").value(50))
			.andExpect(jsonPath("$.documentNo").value("CS 123456"))
			.andExpect(jsonPath("$.endDate").value(nullValue()))
			.andExpect(jsonPath("$.status").value("CURRENT"))
			.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void shareIsOptional() throws Exception {
		postOwnership(place(null, "P101"), person("An"), null, JAN, null)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.sharePercent").value(nullValue()));
	}

	@Test
	void invalidInputIs400() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		for (String share : new String[] { "0", "100.01", "33.333" }) {
			postOwnership(room, an, share, JAN, null).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItem("sharePercent")));
		}
		postOwnership(room, an, "50", JAN, JAN.minusDays(1)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("endDate must not be before startDate"));
		post("/ownerships", "{\"placeId\": \"%d\", \"personId\": \"%d\"}".formatted(room, an))
			.andExpect(status().isBadRequest());
	}

	@Test
	void placeAndPersonMustBeAlive() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		long gone = person("Gone");
		this.people.delete(CTX_A, gone, 1, null);
		long other = this.people.create(CTX_B, new PersonRequest("B", null, null, null, null, null, null, null, null, null, null)).id();

		postOwnership(999_999, an, null, JAN, null).andExpect(status().isNotFound());
		postOwnership(room, gone, null, JAN, null).andExpect(status().isNotFound());
		postOwnership(room, other, null, JAN, null).andExpect(status().isNotFound());
	}

	@Test
	void samePersonCannotOverlapAtOnePlace() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		ownership(room, an, null, JAN, null);

		postOwnership(room, an, null, LocalDate.of(2026, 3, 1), null)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Person " + an + " already has an overlapping ownership at this place"));
	}

	@Test
	void samePersonMayOwnTwoPlaces() throws Exception {
		long an = person("An");
		ownership(place(null, "P101"), an, null, JAN, null);
		postOwnership(place(null, "P102"), an, null, JAN, null).andExpect(status().isCreated());
	}

	@Test
	void backToBackPeriodsDoNotOverlap() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		ownership(room, an, null, JAN, LocalDate.of(2026, 6, 30));
		postOwnership(room, an, null, LocalDate.of(2026, 7, 1), null).andExpect(status().isCreated());
	}

	@Test
	void sharesOverOneHundredAreRefusedWithTheDay() throws Exception {
		long room = place(null, "P101");
		ownership(room, person("An"), "60", JAN, null);

		postOwnership(room, person("Bình"), "50", LocalDate.of(2026, 6, 1), null)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Ownership shares of place " + room + " would total 110% on 2026-06-01"));
	}

	@Test
	void sharesThatNeverMeetAreAccepted() throws Exception {
		long room = place(null, "P101");
		ownership(room, person("An"), "60", JAN, LocalDate.of(2026, 6, 30));
		ownership(room, person("Bình"), "60", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));

		postOwnership(room, person("Cường"), "40", JAN, LocalDate.of(2026, 12, 31)).andExpect(status().isCreated());
	}

	@Test
	void rowsWithoutShareAreNotCounted() throws Exception {
		long room = place(null, "P101");
		ownership(room, person("An"), null, JAN, null);
		postOwnership(room, person("Bình"), "100", JAN, null).andExpect(status().isCreated());
	}

	@Test
	void deletedRowsAreNotCounted() throws Exception {
		long room = place(null, "P101");
		long id = ownership(room, person("An"), "100", JAN, null);
		this.mvc.perform(asA(delete("/ownerships/" + id)).param("version", "1")).andExpect(status().isNoContent());

		postOwnership(room, person("Bình"), "100", JAN, null).andExpect(status().isCreated());
	}

	@Test
	void updateChangesShareDatesDocumentAndNote() throws Exception {
		long id = ownership(place(null, "P101"), person("An"), "50", JAN, null);

		update(id, "{\"version\": 1, \"sharePercent\": 40, \"startDate\": \"2026-02-01\", \"documentNo\": \"X\", \"note\": \"n\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sharePercent").value(40))
			.andExpect(jsonPath("$.startDate").value("2026-02-01"))
			.andExpect(jsonPath("$.documentNo").value("X"))
			.andExpect(jsonPath("$.note").value("n"))
			.andExpect(jsonPath("$.version").value(2));
	}

	@Test
	void updateRechecksOverlapAndShares() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		ownership(room, an, null, JAN, LocalDate.of(2026, 3, 31));
		long later = ownership(room, an, null, LocalDate.of(2026, 4, 1), null);
		ownership(room, person("Bình"), "70", JAN, null);
		long cuong = ownership(room, person("Cường"), "30", JAN, null);

		update(later, "{\"version\": 1, \"startDate\": \"2026-03-01\"}").andExpect(status().isConflict());
		update(cuong, "{\"version\": 1, \"sharePercent\": 31, \"startDate\": \"2026-01-01\"}").andExpect(status().isConflict());
	}

	@Test
	void updateDoesNotCountTheRowAgainstItself() throws Exception {
		long id = ownership(place(null, "P101"), person("An"), "100", JAN, null);
		update(id, "{\"version\": 1, \"sharePercent\": 100, \"startDate\": \"2026-01-01\", \"note\": \"n\"}")
			.andExpect(status().isOk());
	}

	@Test
	void updateOfARowWhosePlaceOrPersonIsDeletedIs409() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		long id = ownership(room, an, null, JAN, JAN);
		this.places.delete(CTX_A, room, 1, null);

		update(id, "{\"version\": 1, \"startDate\": \"2026-01-01\"}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Restore place " + room + " first"));
	}

	@Test
	void updateChecksTheVersion() throws Exception {
		long id = ownership(place(null, "P101"), person("An"), null, JAN, null);
		update(id, "{\"version\": 9, \"startDate\": \"2026-01-01\"}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
	}

	@Test
	void endSetsTheLastDayAndDefaultsToToday() throws Exception {
		long room = place(null, "P101");
		long a = ownership(room, person("An"), null, JAN, null);
		long b = ownership(room, person("Bình"), null, JAN, null);

		post("/ownerships/" + a + "/end", "{\"version\": 1, \"endDate\": \"2026-05-31\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.endDate").value("2026-05-31"));
		post("/ownerships/" + b + "/end", "{\"version\": 1}").andExpect(jsonPath("$.endDate").value(this.today.toString()));
	}

	@Test
	void endingTwiceIs409() throws Exception {
		long id = ownership(place(null, "P101"), person("An"), null, JAN, LocalDate.of(2026, 3, 1));
		post("/ownerships/" + id + "/end", "{\"version\": 1}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Ownership " + id + " has already ended"));
	}

	@Test
	void deleteIsSoftAndChecksTheVersion() throws Exception {
		long id = ownership(place(null, "P101"), person("An"), null, JAN, null);
		this.mvc.perform(asA(delete("/ownerships/" + id)).param("version", "5")).andExpect(status().isConflict());
		this.mvc.perform(asA(delete("/ownerships/" + id)).param("version", "1")).andExpect(status().isNoContent());
		this.mvc.perform(asA(get("/ownerships/" + id))).andExpect(status().isNotFound());
	}

	@Test
	void otherWorkspaceSeesNothing() throws Exception {
		long id = ownership(place(null, "P101"), person("An"), null, JAN, null);
		this.mvc.perform(asB(get("/ownerships/" + id))).andExpect(status().isNotFound());
	}

	private ResultActions update(long id, String json) throws Exception {
		return this.mvc.perform(asA(put("/ownerships/" + id)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

}
