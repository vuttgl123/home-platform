package com.home_platform.registry.person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.home_platform.common.history.ChangeAction;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.support.BusinessDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class PersonIT extends PersonTestData {

	@Autowired
	FieldDefService fieldDefs;

	@Autowired
	BusinessDate businessDate;

	@Test
	void createsAPersonWithNormalizedFields() throws Exception {
		postPerson("""
				{"fullName": " Nguyễn Văn A ", "phone": "090 123.4567", "idType": "cccd", "idNumber": " 001 ",
				 "gender": "MALE", "email": " a@example.com "}""")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isString())
			.andExpect(jsonPath("$.fullName").value("Nguyễn Văn A"))
			.andExpect(jsonPath("$.phone").value("0901234567"))
			.andExpect(jsonPath("$.idType").value("CCCD"))
			.andExpect(jsonPath("$.idNumber").value("001"))
			.andExpect(jsonPath("$.email").value("a@example.com"))
			.andExpect(jsonPath("$.gender").value("MALE"))
			.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void createRecordsVersionOne() throws Exception {
		long id = named("A");

		assertThat(version(id, 1).action()).isEqualTo(ChangeAction.CREATE);
		assertThat(version(id, 1).snapshot().get("fullName").asString()).isEqualTo("A");
		assertThat(version(id, 1).snapshot().has("version")).isFalse();
	}

	@Test
	void documentFieldsComeInPairs() throws Exception {
		postPerson("""
				{"fullName": "A", "idNumber": "001"}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("idType and idNumber must be given together"));
	}

	@Test
	void sameDocumentIsAConflict() throws Exception {
		long first = person("""
				{"fullName": "A", "idType": "CCCD", "idNumber": "001"}""");

		postPerson("""
				{"fullName": "B", "idType": "cccd", "idNumber": "001"}""")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Person " + first + " already has this document"));
		person("""
				{"fullName": "C", "idType": "PASSPORT", "idNumber": "001"}""");
	}

	@Test
	void deletedPersonDoesNotBlockTheSameDocument() throws Exception {
		long first = person("""
				{"fullName": "A", "idType": "CCCD", "idNumber": "001"}""");
		this.mvc.perform(asA(delete("/people/" + first)).param("version", "1")).andExpect(status().isNoContent());

		person("""
				{"fullName": "B", "idType": "CCCD", "idNumber": "001"}""");
	}

	@Test
	void futureBirthDateAndBadEmailAre400() throws Exception {
		postPerson("{\"fullName\": \"A\", \"dateOfBirth\": \"" + LocalDate.now().plusDays(2) + "\"}")
			.andExpect(status().isBadRequest());
		postPerson("""
				{"fullName": "A", "email": "not-an-email"}""").andExpect(status().isBadRequest());
		postPerson("""
				{"fullName": "  "}""").andExpect(status().isBadRequest());
	}

	@Test
	void attributesAreValidatedAgainstPersonFields() throws Exception {
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.PERSON, 0L, "nickname", "Biệt danh",
				FieldDataType.TEXT, null, null, null));

		postPerson("""
				{"fullName": "A", "attributes": {"nickname": 1}}""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("attributes.nickname"));
		postPerson("""
				{"fullName": "A", "attributes": {"nickname": "Bo"}}""").andExpect(status().isCreated())
			.andExpect(jsonPath("$.attributes.nickname").value("Bo"));
	}

	@Test
	void updateWritesFieldsAndChecksVersion() throws Exception {
		long id = named("A");
		String json = """
				{"version": 1, "fullName": "B", "gender": "FEMALE", "versionNote": "đổi tên"}""";

		putPerson(id, json).andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(2))
			.andExpect(jsonPath("$.fullName").value("B"));
		assertThat(version(id, 2).action()).isEqualTo(ChangeAction.UPDATE);
		assertThat(version(id, 2).changedFields()).contains("fullName", "gender");
		assertThat(version(id, 2).note()).isEqualTo("đổi tên");
		putPerson(id, json).andExpect(status().isConflict());
	}

	@Test
	void updateKeepsOwnDocument() throws Exception {
		long id = person("""
				{"fullName": "A", "idType": "CCCD", "idNumber": "001"}""");

		putPerson(id, """
				{"version": 1, "fullName": "A2", "idType": "CCCD", "idNumber": "001"}""").andExpect(status().isOk());
	}

	@Test
	void deleteIsBlockedByCurrentOrUpcomingOccupancy() throws Exception {
		long busy = named("Busy");
		occupancy(busy, LocalDate.of(2020, 1, 1), null);
		long moved = named("Moved");
		occupancy(moved, LocalDate.of(2020, 1, 1), this.businessDate.today().minusDays(1));

		this.mvc.perform(asA(delete("/people/" + busy)).param("version", "1"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Person " + busy + " still has 1 current or upcoming occupancies"));
		this.mvc.perform(asA(delete("/people/" + moved)).param("version", "1")).andExpect(status().isNoContent());
	}

	@Test
	void deleteAndRestoreWithVersions() throws Exception {
		long id = named("A");

		this.mvc.perform(asA(delete("/people/" + id)).param("version", "1").param("note", "nhầm"))
			.andExpect(status().isNoContent());
		getPerson(id).andExpect(status().isNotFound());
		assertThat(version(id, 2).action()).isEqualTo(ChangeAction.DELETE);

		this.mvc.perform(asA(post("/people/" + id + "/restore")).contentType(MediaType.APPLICATION_JSON).content("""
				{"version": 2}""")).andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(3))
			.andExpect(jsonPath("$.deletedAt").value(nullValue()));
		assertThat(version(id, 3).action()).isEqualTo(ChangeAction.RESTORE);
	}

	@Test
	void restoreChecksDocumentDuplicates() throws Exception {
		long first = person("""
				{"fullName": "A", "idType": "CCCD", "idNumber": "001"}""");
		this.mvc.perform(asA(delete("/people/" + first)).param("version", "1")).andExpect(status().isNoContent());
		long second = person("""
				{"fullName": "B", "idType": "CCCD", "idNumber": "001"}""");

		this.mvc.perform(asA(post("/people/" + first + "/restore")).contentType(MediaType.APPLICATION_JSON).content("""
				{"version": 2}""")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Person " + second + " already has this document"));
	}

	@Test
	void otherWorkspaceSeesNothing() throws Exception {
		long id = named("A");

		this.mvc.perform(asB(get("/people/" + id))).andExpect(status().isNotFound());
	}

}
