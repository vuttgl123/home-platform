package com.home_platform.registry.ownership;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class OwnershipTransferIT extends OwnershipTestData {

	private static final LocalDate START = LocalDate.of(2024, 3, 1);

	@Test
	void transferEndsTheOldRowTheDayBeforeAndStartsTheNewOne() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		long cuong = person("Cường");
		long id = ownership(room, an, "50", START, null);

		transfer(id, "{\"version\": 1, \"toPersonId\": \"%d\", \"transferDate\": \"2026-11-16\", \"documentNo\": \"CS 999\"}"
			.formatted(cuong))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ended.id").value(String.valueOf(id)))
			.andExpect(jsonPath("$.ended.endDate").value("2026-11-15"))
			.andExpect(jsonPath("$.ended.version").value(2))
			.andExpect(jsonPath("$.started.personId").value(String.valueOf(cuong)))
			.andExpect(jsonPath("$.started.placeId").value(String.valueOf(room)))
			.andExpect(jsonPath("$.started.sharePercent").value(50))
			.andExpect(jsonPath("$.started.startDate").value("2026-11-16"))
			.andExpect(jsonPath("$.started.documentNo").value("CS 999"))
			.andExpect(jsonPath("$.started.endDate").value(Matchers.nullValue()));
	}

	@Test
	void transferDefaultsToToday() throws Exception {
		long id = ownership(place(null, "P101"), person("An"), null, START, null);

		transfer(id, "{\"version\": 1, \"toPersonId\": \"%d\"}".formatted(person("Cường")))
			.andExpect(jsonPath("$.started.startDate").value(this.today.toString()))
			.andExpect(jsonPath("$.ended.endDate").value(this.today.minusDays(1).toString()));
	}

	@Test
	void transferKeepsACoOwnersRowUntouched() throws Exception {
		long room = place(null, "P101");
		long id = ownership(room, person("An"), "50", START, null);
		long binh = ownership(room, person("Bình"), "50", START, null);

		transfer(id, "{\"version\": 1, \"toPersonId\": \"%d\"}".formatted(person("Cường"))).andExpect(status().isOk());

		this.mvc.perform(asA(get("/ownerships/" + binh)))
			.andExpect(jsonPath("$.version").value(1))
			.andExpect(jsonPath("$.endDate").value(Matchers.nullValue()));
	}

	@Test
	void transferToTheSamePersonIs400() throws Exception {
		long an = person("An");
		long id = ownership(place(null, "P101"), an, null, START, null);

		transfer(id, "{\"version\": 1, \"toPersonId\": \"%d\"}".formatted(an))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("toPersonId must be a different person"));
	}

	@Test
	void transferDateMustBeAfterTheStart() throws Exception {
		long id = ownership(place(null, "P101"), person("An"), null, START, null);

		transfer(id, "{\"version\": 1, \"toPersonId\": \"%d\", \"transferDate\": \"%s\"}".formatted(person("Cường"), START))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("transferDate must be after startDate"));
	}

	@Test
	void endedOwnershipCannotBeTransferred() throws Exception {
		long id = ownership(place(null, "P101"), person("An"), null, START, LocalDate.of(2025, 1, 1));

		transfer(id, "{\"version\": 1, \"toPersonId\": \"%d\"}".formatted(person("Cường")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Ownership " + id + " has already ended"));
	}

	@Test
	void receiverAlreadyOwningHereIs409AndNothingChanges() throws Exception {
		long room = place(null, "P101");
		long id = ownership(room, person("An"), "50", START, null);
		long binh = person("Bình");
		ownership(room, binh, "50", START, null);

		transfer(id, "{\"version\": 1, \"toPersonId\": \"%d\"}".formatted(binh))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Person " + binh + " already has an overlapping ownership at this place"));
		this.mvc.perform(asA(get("/ownerships/" + id)))
			.andExpect(jsonPath("$.endDate").value(Matchers.nullValue()))
			.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void unknownReceiverIs404AndNothingChanges() throws Exception {
		long id = ownership(place(null, "P101"), person("An"), null, START, null);

		transfer(id, "{\"version\": 1, \"toPersonId\": \"999999\"}").andExpect(status().isNotFound());
		this.mvc.perform(asA(get("/ownerships/" + id))).andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void transferChecksTheVersion() throws Exception {
		long id = ownership(place(null, "P101"), person("An"), null, START, null);

		transfer(id, "{\"version\": 4, \"toPersonId\": \"%d\"}".formatted(person("Cường")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
	}

	@Test
	void transferOfARowInADeletedPlaceIs409() throws Exception {
		long room = place(null, "P101");
		long id = ownership(room, person("An"), null, START, null);
		this.jdbc.sql("UPDATE reg_place SET deleted_at = UTC_TIMESTAMP(3) WHERE workspace_id = :ws AND id = :id")
			.param("ws", WS_A).param("id", room).update();

		transfer(id, "{\"version\": 1, \"toPersonId\": \"%d\"}".formatted(person("Cường")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Restore place " + room + " first"));
	}

	private org.springframework.test.web.servlet.ResultActions transfer(long id, String json) throws Exception {
		return post("/ownerships/" + id + "/transfer", json);
	}

}
