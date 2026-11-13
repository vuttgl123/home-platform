package com.home_platform.identity.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;

import com.home_platform.identity.IdentityIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class UserIT extends IdentityIT {

	@Test
	void createsAUserWithLowercaseEmail() throws Exception {
		String email = email();

		create(" " + email.toUpperCase(Locale.ROOT) + " ", " Nguyễn An ").andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isString())
			.andExpect(jsonPath("$.email").value(email))
			.andExpect(jsonPath("$.displayName").value("Nguyễn An"))
			.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void duplicateEmailIsAConflict() throws Exception {
		String email = email();
		create(email, "A").andExpect(status().isCreated());

		create(email.toUpperCase(Locale.ROOT), "B").andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("User with email " + email + " already exists"));
	}

	@Test
	void invalidEmailOrBlankNameIs400() throws Exception {
		create("not-an-email", "A").andExpect(status().isBadRequest());
		create(email(), "   ").andExpect(status().isBadRequest());
	}

	@Test
	void meReturnsTheCaller() throws Exception {
		long id = user("An");

		this.mvc.perform(as(id, get("/users/me")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(String.valueOf(id)))
			.andExpect(jsonPath("$.displayName").value("An"));
	}

	@Test
	void unknownCallerIs404() throws Exception {
		this.mvc.perform(as(-12345, get("/users/me"))).andExpect(status().isNotFound());
		this.mvc.perform(get("/users/me")).andExpect(status().isBadRequest());
	}

	@Test
	void renameChangesTheDisplayName() throws Exception {
		long id = user("An");

		this.mvc.perform(as(id, put("/users/me")).contentType(MediaType.APPLICATION_JSON).content("""
				{"displayName": " An Nguyễn "}"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("An Nguyễn"));
	}

	private ResultActions create(String email, String displayName) throws Exception {
		return this.mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "%s", "displayName": "%s"}""".formatted(email, displayName)));
	}

}
