package com.home_platform.registry.overview;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.home_platform.registry.RegistryIT;
import com.home_platform.registry.person.PersonRequest;
import com.home_platform.registry.person.PersonService;
import com.home_platform.registry.place.PlaceCreateRequest;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import com.home_platform.registry.support.BusinessDate;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class OverviewIT extends RegistryIT {

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	PlaceService places;

	@Autowired
	PersonService people;

	@Autowired
	BusinessDate businessDate;

	LocalDate today;

	@BeforeEach
	void today() {
		this.today = this.businessDate.today();
	}

	@Test
	void emptyWorkspaceIsAllZero() throws Exception {
		this.mvc.perform(asA(get("/overview")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.places").value(0))
			.andExpect(jsonPath("$.placeTypes", hasSize(0)))
			.andExpect(jsonPath("$.people").value(0))
			.andExpect(jsonPath("$.peopleLiving").value(0))
			.andExpect(jsonPath("$.itemsByStatus.IN_USE").value(0))
			.andExpect(jsonPath("$.itemsByStatus.BROKEN").value(0))
			.andExpect(jsonPath("$.itemsByStatus.STORED").value(0))
			.andExpect(jsonPath("$.itemsByStatus.REMOVED").value(0))
			.andExpect(jsonPath("$.missingCodes.places").value(0))
			.andExpect(jsonPath("$.missingCodes.items").value(0))
			.andExpect(jsonPath("$.activityByDay", hasSize(14)))
			.andExpect(jsonPath("$.activityByDay[13].date").value(this.today.toString()))
			.andExpect(jsonPath("$.activityByDay[13].created").value(0));
	}

	@Test
	void activityByDayCountsTheLastFourteenDaysByAction() throws Exception {
		// Giờ Việt Nam; 00:30 hôm nay là 17:30 hôm qua theo UTC nhưng vẫn là việc của hôm nay.
		version(WS_A, 1, "CREATE", this.today.atTime(0, 30));
		version(WS_A, 2, "CREATE", this.today.atTime(10, 0));
		version(WS_A, 3, "UPDATE", this.today.atTime(23, 59));
		version(WS_A, 4, "DELETE", this.today.minusDays(1).atTime(12, 0));
		version(WS_A, 5, "RESTORE", this.today.minusDays(13).atTime(8, 0));
		// Ngoài 14 ngày, và của không gian khác: không tính.
		version(WS_A, 6, "CREATE", this.today.minusDays(14).atTime(23, 59));
		version(WS_B, 7, "CREATE", this.today.atTime(9, 0));

		this.mvc.perform(asA(get("/overview")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.activityByDay", hasSize(14)))
			.andExpect(jsonPath("$.activityByDay[0].date").value(this.today.minusDays(13).toString()))
			.andExpect(jsonPath("$.activityByDay[0].restored").value(1))
			.andExpect(jsonPath("$.activityByDay[0].created").value(0))
			.andExpect(jsonPath("$.activityByDay[12].deleted").value(1))
			.andExpect(jsonPath("$.activityByDay[13].date").value(this.today.toString()))
			.andExpect(jsonPath("$.activityByDay[13].created").value(2))
			.andExpect(jsonPath("$.activityByDay[13].updated").value(1))
			.andExpect(jsonPath("$.activityByDay[13].deleted").value(0))
			.andExpect(jsonPath("$.activityByDay[13].restored").value(0));
	}

	@Test
	void countsPlacesByTypeWithOccupied() throws Exception {
		long floor = type("FLOOR", "Tầng", 1);
		long room = type("ROOM", "Phòng", 2);
		type("UNUSED", "Kho", 3);
		long level = place(floor, null, "Tầng 1");
		long r1 = place(room, level, "P1");
		long r2 = place(room, level, "P2");
		place(room, level, "P3");
		occupancy(r1, person("An"), this.today.minusDays(3), null);
		occupancy(r2, person("Bình"), this.today.minusDays(3), null);

		this.mvc.perform(asA(get("/overview")))
			.andExpect(jsonPath("$.places").value(4))
			.andExpect(jsonPath("$.placeTypes[*].name", contains("Tầng", "Phòng")))
			.andExpect(jsonPath("$.placeTypes[0].typeId").value(String.valueOf(floor)))
			.andExpect(jsonPath("$.placeTypes[0].total").value(1))
			.andExpect(jsonPath("$.placeTypes[0].occupied").value(0))
			.andExpect(jsonPath("$.placeTypes[1].total").value(3))
			.andExpect(jsonPath("$.placeTypes[1].occupied").value(2))
			.andExpect(jsonPath("$.people").value(2))
			.andExpect(jsonPath("$.peopleLiving").value(2));
	}

	/** Hết hạn hôm qua và bắt đầu ngày mai thì không tính; kết thúc đúng hôm nay vẫn tính. */
	@Test
	void onlyOccupanciesValidTodayCount() throws Exception {
		long room = type("ROOM", "Phòng", 1);
		long past = place(room, null, "Past");
		long future = place(room, null, "Future");
		long lastDay = place(room, null, "LastDay");
		occupancy(past, person("Gone"), this.today.minusDays(10), this.today.minusDays(1));
		occupancy(future, person("Soon"), this.today.plusDays(1), null);
		occupancy(lastDay, person("Leaving"), this.today.minusDays(10), this.today);

		this.mvc.perform(asA(get("/overview")))
			.andExpect(jsonPath("$.placeTypes[0].total").value(3))
			.andExpect(jsonPath("$.placeTypes[0].occupied").value(1))
			.andExpect(jsonPath("$.people").value(3))
			.andExpect(jsonPath("$.peopleLiving").value(1));
	}

	@Test
	void personInTwoRoomsCountsOnceButFillsBoth() throws Exception {
		long room = type("ROOM", "Phòng", 1);
		long r1 = place(room, null, "P1");
		long r2 = place(room, null, "P2");
		long an = person("An");
		occupancy(r1, an, this.today.minusDays(3), null);
		occupancy(r2, an, this.today.minusDays(2), null);
		occupancy(r1, person("Bình"), this.today.minusDays(1), null);

		this.mvc.perform(asA(get("/overview")))
			.andExpect(jsonPath("$.placeTypes[0].occupied").value(2))
			.andExpect(jsonPath("$.peopleLiving").value(2));
	}

	@Test
	void deletedRecordsAreNotCounted() throws Exception {
		long room = type("ROOM", "Phòng", 1);
		long kept = place(room, null, "Kept");
		long emptied = place(room, null, "Emptied");
		long goneRoom = place(room, null, "Gone");
		long stays = person("Stays");
		long gonePerson = person("Gone");
		occupancy(kept, stays, this.today.minusDays(3), null);
		long endedByDelete = occupancy(emptied, stays, this.today.minusDays(3), null);
		String keptItem = item(kept, "Tủ", "IN_USE");
		String goneItem = item(kept, "Ghế", "BROKEN");
		this.mvc.perform(asA(delete("/places/" + goneRoom)).param("version", "1")).andExpect(status().isNoContent());
		this.mvc.perform(asA(delete("/people/" + gonePerson)).param("version", "1")).andExpect(status().isNoContent());
		this.mvc.perform(asA(delete("/occupancies/" + endedByDelete)).param("version", "1"))
			.andExpect(status().isNoContent());
		this.mvc.perform(asA(delete("/items/" + goneItem)).param("version", "1")).andExpect(status().isNoContent());

		this.mvc.perform(asA(get("/overview")))
			.andExpect(jsonPath("$.places").value(2))
			.andExpect(jsonPath("$.placeTypes[0].total").value(2))
			.andExpect(jsonPath("$.placeTypes[0].occupied").value(1))
			.andExpect(jsonPath("$.people").value(1))
			.andExpect(jsonPath("$.peopleLiving").value(1))
			.andExpect(jsonPath("$.itemsByStatus.IN_USE").value(1))
			.andExpect(jsonPath("$.itemsByStatus.BROKEN").value(0));
		this.mvc.perform(asA(get("/items/" + keptItem))).andExpect(status().isOk());
	}

	/** Cư trú của một người đã xoá (dòng cư trú còn đó) không làm chỗ đó "có người". */
	@Test
	void occupancyOfADeletedPersonDoesNotCount() throws Exception {
		long room = type("ROOM", "Phòng", 1);
		long place = place(room, null, "P1");
		long ghost = person("Ghost");
		occupancy(place, ghost, this.today.minusDays(3), null);
		this.jdbc.sql("UPDATE reg_person SET deleted_at = UTC_TIMESTAMP(3) WHERE workspace_id = :ws AND id = :id")
			.param("ws", WS_A)
			.param("id", ghost)
			.update();

		this.mvc.perform(asA(get("/overview")))
			.andExpect(jsonPath("$.placeTypes[0].occupied").value(0))
			.andExpect(jsonPath("$.peopleLiving").value(0));
	}

	@Test
	void itemsByStatusCountsEachStatus() throws Exception {
		long room = type("ROOM", "Phòng", 1);
		long place = place(room, null, "P1");
		item(place, "A", "IN_USE");
		item(place, "B", "IN_USE");
		item(place, "C", "BROKEN");
		item(place, "D", "STORED");
		String removed = item(place, "E", "IN_USE");
		this.mvc
			.perform(asA(post("/items/" + removed + "/remove")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"version\": 1}"))
			.andExpect(status().isOk());

		this.mvc.perform(asA(get("/overview")))
			.andExpect(jsonPath("$.itemsByStatus.IN_USE").value(2))
			.andExpect(jsonPath("$.itemsByStatus.BROKEN").value(1))
			.andExpect(jsonPath("$.itemsByStatus.STORED").value(1))
			.andExpect(jsonPath("$.itemsByStatus.REMOVED").value(1));
		this.mvc.perform(asA(get("/items").param("status", "REMOVED"))).andExpect(jsonPath("$.items", hasSize(1)));
		this.mvc.perform(asA(get("/items").param("status", "IN_USE"))).andExpect(jsonPath("$.items", hasSize(2)));
	}

	@Test
	void missingCodesMatchCodeStatus() throws Exception {
		long room = type("ROOM", "Phòng", 1);
		for (long id = 910001; id <= 910003; id++) {
			this.jdbc.sql("INSERT INTO reg_place (workspace_id, id, place_type_id, path, name, created_by, updated_by) "
					+ "VALUES (:ws, :id, :type, :path, 'old', :user, :user)")
				.param("ws", WS_A)
				.param("id", id)
				.param("type", room)
				.param("path", "/" + id + "/")
				.param("user", USER)
				.update();
		}
		this.jdbc.sql("INSERT INTO reg_item (workspace_id, id, place_id, name, status, created_by, updated_by) "
				+ "VALUES (:ws, 910010, 910001, 'old', 'IN_USE', :user, :user)")
			.param("ws", WS_A)
			.param("user", USER)
			.update();

		String codes = this.mvc.perform(asA(get("/codes/status"))).andReturn().getResponse().getContentAsString();
		int placesWithoutCode = JsonPath.read(codes, "$.placesWithoutCode");
		int itemsWithoutCode = JsonPath.read(codes, "$.itemsWithoutCode");

		this.mvc.perform(asA(get("/overview")))
			.andExpect(jsonPath("$.missingCodes.places").value(3))
			.andExpect(jsonPath("$.missingCodes.places").value(placesWithoutCode))
			.andExpect(jsonPath("$.missingCodes.items").value(1))
			.andExpect(jsonPath("$.missingCodes.items").value(itemsWithoutCode));
	}

	@Test
	void otherWorkspaceSeesNothing() throws Exception {
		long room = type("ROOM", "Phòng", 1);
		long place = place(room, null, "P1");
		occupancy(place, person("An"), this.today.minusDays(3), null);
		item(place, "Tủ", "IN_USE");

		this.mvc.perform(asB(get("/overview")))
			.andExpect(jsonPath("$.places").value(0))
			.andExpect(jsonPath("$.placeTypes", hasSize(0)))
			.andExpect(jsonPath("$.people").value(0))
			.andExpect(jsonPath("$.peopleLiving").value(0))
			.andExpect(jsonPath("$.itemsByStatus.IN_USE").value(0));
	}

	/** Một dòng version lúc {@code local} giờ Việt Nam, lưu UTC như {@code EntityHistory}. */
	private void version(long workspaceId, long entityId, String action, LocalDateTime local) {
		LocalDateTime utc = local.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
		this.jdbc.sql("""
				INSERT INTO reg_entity_version (workspace_id, entity_type, entity_id, version_no, action, snapshot,
				  changed_by, changed_at)
				VALUES (:ws, 'ITEM', :entity, 1, :action, '{}', :user, :at)""")
			.param("ws", workspaceId)
			.param("entity", entityId)
			.param("action", action)
			.param("user", USER)
			.param("at", utc)
			.update();
	}

	private long type(String code, String name, int sortOrder) {
		return this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest(code, name, null, sortOrder)).id();
	}

	private long place(long type, Long parentId, String name) {
		return this.places
			.create(CTX_A, new PlaceCreateRequest(parentId, type, name, null, null, null, null, null, null, null))
			.id();
	}

	private long person(String fullName) {
		return this.people
			.create(CTX_A, new PersonRequest(fullName, null, null, null, null, null, null, null, null, null, null))
			.id();
	}

	private long occupancy(long placeId, long personId, LocalDate start, LocalDate end) throws Exception {
		String endJson = (end != null) ? "\"" + end + "\"" : "null";
		String body = this.mvc.perform(asA(post("/occupancies")).contentType(MediaType.APPLICATION_JSON).content("""
				{"placeId": "%d", "personId": "%d", "relation": "RESIDENT", "startDate": "%s", "endDate": %s}"""
			.formatted(placeId, personId, start, endJson)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return Long.parseLong(JsonPath.read(body, "$.id"));
	}

	private String item(long placeId, String name, String status) throws Exception {
		String body = this.mvc.perform(asA(post("/items")).contentType(MediaType.APPLICATION_JSON).content("""
				{"placeId": "%d", "name": "%s", "status": "%s"}""".formatted(placeId, name, status)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.id");
	}

}
