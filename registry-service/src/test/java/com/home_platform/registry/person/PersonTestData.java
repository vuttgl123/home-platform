package com.home_platform.registry.person;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicLong;

import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.history.EntityVersion;
import com.home_platform.registry.RegistryIT;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Hàm phụ dùng chung cho các test người. */
abstract class PersonTestData extends RegistryIT {

	@Autowired
	protected EntityHistory history;

	private final AtomicLong ids = new AtomicLong(800000);

	protected long person(String json) throws Exception {
		String body = postPerson(json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return Long.parseLong(JsonPath.read(body, "$.id"));
	}

	protected long named(String fullName) throws Exception {
		return person("{\"fullName\": \"" + fullName + "\"}");
	}

	protected ResultActions postPerson(String json) throws Exception {
		return this.mvc.perform(asA(post("/people")).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	protected ResultActions putPerson(long id, String json) throws Exception {
		return this.mvc.perform(asA(put("/people/" + id)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	protected ResultActions getPerson(long id) throws Exception {
		return this.mvc.perform(asA(get("/people/" + id)));
	}

	protected EntityVersion version(long personId, int versionNo) {
		return this.history.get(WS_A, "PERSON", personId, versionNo);
	}

	/** Chèn bằng SQL một địa điểm và một dòng cư trú của người, ngày kết thúc {@code endDate} (có thể null). */
	protected void occupancy(long personId, java.time.LocalDate start, java.time.LocalDate end) {
		long type = this.ids.incrementAndGet();
		long place = this.ids.incrementAndGet();
		this.jdbc.sql("INSERT INTO reg_place_type (workspace_id, id, code, name) VALUES (:ws, :id, :code, 'T')")
			.param("ws", WS_A).param("id", type).param("code", "T" + type).update();
		this.jdbc.sql("""
				INSERT INTO reg_place (workspace_id, id, place_type_id, path, name, created_by, updated_by)
				VALUES (:ws, :id, :type, :path, 'P', :user, :user)""")
			.param("ws", WS_A).param("id", place).param("type", type).param("path", "/" + place + "/")
			.param("user", USER).update();
		this.jdbc.sql("""
				INSERT INTO reg_occupancy (workspace_id, id, place_id, person_id, relation, start_date, end_date,
				  created_by, updated_by)
				VALUES (:ws, :id, :place, :person, 'RESIDENT', :start, :end, :user, :user)""")
			.param("ws", WS_A).param("id", this.ids.incrementAndGet()).param("place", place)
			.param("person", personId).param("start", start).param("end", end).param("user", USER).update();
	}

}
