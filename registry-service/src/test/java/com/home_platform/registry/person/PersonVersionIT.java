package com.home_platform.registry.person;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class PersonVersionIT extends PersonTestData {

	@Test
	void listsAndShowsVersions() throws Exception {
		long id = named("A");
		putPerson(id, """
				{"version": 1, "fullName": "B"}""").andExpect(status().isOk());

		this.mvc.perform(asA(get("/people/" + id + "/versions")))
			.andExpect(jsonPath("$.items[*].versionNo", contains(2, 1)));
		this.mvc.perform(asA(get("/people/" + id + "/versions/1")))
			.andExpect(jsonPath("$.snapshot.fullName").value("A"))
			.andExpect(jsonPath("$.snapshot.id").value(String.valueOf(id)));
	}

	@Test
	void restoresAnOldVersion() throws Exception {
		long id = person("""
				{"fullName": "A", "phone": "0901"}""");
		putPerson(id, """
				{"version": 1, "fullName": "B"}""").andExpect(status().isOk());

		restoreVersion(id, 1, 2).andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("A"))
			.andExpect(jsonPath("$.phone").value("0901"))
			.andExpect(jsonPath("$.version").value(3));
		this.mvc.perform(asA(get("/people/" + id + "/versions/3")))
			.andExpect(jsonPath("$.note").value("Restored from version 1"));
	}

	@Test
	void restoreVersionChecksDocumentDuplicates() throws Exception {
		long p = person("""
				{"fullName": "P", "idType": "CCCD", "idNumber": "001"}""");
		putPerson(p, """
				{"version": 1, "fullName": "P", "idType": "CCCD", "idNumber": "002"}""").andExpect(status().isOk());
		person("""
				{"fullName": "Q", "idType": "CCCD", "idNumber": "001"}""");

		restoreVersion(p, 1, 2).andExpect(status().isConflict());
		getPerson(p).andExpect(jsonPath("$.version").value(2)).andExpect(jsonPath("$.idNumber").value("002"));
	}

	@Test
	void restoringADeletedPersonIsAConflict() throws Exception {
		long id = named("A");
		this.mvc.perform(asA(delete("/people/" + id)).param("version", "1")).andExpect(status().isNoContent());

		restoreVersion(id, 1, 2).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Restore person " + id + " before restoring an old version"));
	}

	private ResultActions restoreVersion(long id, int versionNo, int currentVersion) throws Exception {
		return this.mvc.perform(asA(post("/people/" + id + "/versions/" + versionNo + "/restore"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"version\": " + currentVersion + "}"));
	}

}
