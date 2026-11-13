package com.home_platform.registry.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.common.history.EntityVersion;
import com.home_platform.common.web.CursorRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class PlaceMoveIT extends PlaceTestData {

	long type;

	long a;

	long b;

	long f;

	long r;

	/** A > F > R, và B riêng. */
	@BeforeEach
	void tree() throws Exception {
		this.type = type("T");
		this.a = place(null, this.type, "A");
		this.b = place(null, this.type, "B");
		this.f = place(this.a, this.type, "F");
		this.r = place(this.f, this.type, "R");
	}

	@Test
	void movesASubtree() throws Exception {
		move(this.f, 1, this.b).andExpect(status().isOk())
			.andExpect(jsonPath("$.parentId").value(String.valueOf(this.b)))
			.andExpect(jsonPath("$.path").value("/" + this.b + "/" + this.f + "/"))
			.andExpect(jsonPath("$.depth").value(1))
			.andExpect(jsonPath("$.version").value(2));

		getPlace(this.r).andExpect(jsonPath("$.path").value("/" + this.b + "/" + this.f + "/" + this.r + "/"))
			.andExpect(jsonPath("$.depth").value(2))
			.andExpect(jsonPath("$.version").value(2))
			.andExpect(jsonPath("$.ancestors[*].name", contains("B", "F")));
		EntityVersion v2 = version(this.f, 2);
		assertThat(v2.changedFields()).contains("parentId", "path");
		assertThat(this.history.list(WS_A, "PLACE", this.r, new CursorRequest(null, 20)).items()).hasSize(1);
	}

	@Test
	void movesToTheTop() throws Exception {
		move(this.f, 1, null).andExpect(jsonPath("$.parentId").value(nullValue()))
			.andExpect(jsonPath("$.path").value("/" + this.f + "/"))
			.andExpect(jsonPath("$.depth").value(0));
		getPlace(this.r).andExpect(jsonPath("$.depth").value(1));
	}

	@Test
	void cannotMoveIntoItselfOrADescendant() throws Exception {
		move(this.f, 1, this.f).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("A place cannot be moved into itself or its descendants"));
		move(this.f, 1, this.r).andExpect(status().isConflict());
	}

	@Test
	void movingTooDeepIsRefused() throws Exception {
		Long x = null;
		for (int i = 0; i < 15; i++) {
			x = place(x, this.type, "X" + i);
		}
		long y0 = place(null, this.type, "Y0");
		Long y = y0;
		for (int i = 1; i < 6; i++) {
			y = place(y, this.type, "Y" + i);
		}

		move(y0, 1, x).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Places can be nested at most 20 levels deep"));
		getPlace(y0).andExpect(jsonPath("$.path").value("/" + y0 + "/"));
		getPlace(y).andExpect(jsonPath("$.depth").value(5)).andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void sameParentIsANoOp() throws Exception {
		move(this.f, 1, this.a).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
		assertThat(this.history.list(WS_A, "PLACE", this.f, new CursorRequest(null, 20)).items()).hasSize(1);
		move(this.f, 5, this.a).andExpect(status().isConflict());
	}

	@Test
	void deletedDescendantsMoveToo() throws Exception {
		this.jdbc.sql("UPDATE reg_place SET deleted_at = UTC_TIMESTAMP(3) WHERE workspace_id = :ws AND id = :id")
			.param("ws", WS_A)
			.param("id", this.r)
			.update();

		move(this.f, 1, this.b).andExpect(status().isOk());

		assertThat(pathOf(this.r)).isEqualTo("/" + this.b + "/" + this.f + "/" + this.r + "/");
	}

	@Test
	void staleChildEditAfterMoveIsAConflict() throws Exception {
		move(this.f, 1, this.b).andExpect(status().isOk());

		putPlace(this.r, """
				{"version": 1, "placeTypeId": "%d", "name": "R2", "status": "ACTIVE"}""".formatted(this.type))
			.andExpect(status().isConflict());
		assertThat(pathOf(this.r)).isEqualTo("/" + this.b + "/" + this.f + "/" + this.r + "/");
	}

	@Test
	void movingIntoAPlaceOfAnotherWorkspaceIsNotFound() throws Exception {
		long otherType = this.placeTypes
			.create(CTX_B, new com.home_platform.registry.placetype.PlaceTypeCreateRequest("T", "T", null, null))
			.id();
		String body = this.mvc.perform(asB(post("/places")).contentType(MediaType.APPLICATION_JSON).content("""
				{"placeTypeId": "%d", "name": "other"}""".formatted(otherType)))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long other = Long.parseLong(com.jayway.jsonpath.JsonPath.read(body, "$.id"));

		move(this.f, 1, other).andExpect(status().isNotFound());
	}

	private ResultActions move(long id, int version, Long parentId) throws Exception {
		String parent = (parentId != null) ? "\"" + parentId + "\"" : "null";
		return this.mvc.perform(asA(post("/places/" + id + "/move")).contentType(MediaType.APPLICATION_JSON).content("""
				{"version": %d, "parentId": %s}""".formatted(version, parent)));
	}

	private String pathOf(long id) {
		return this.jdbc.sql("SELECT path FROM reg_place WHERE workspace_id = :ws AND id = :id")
			.param("ws", WS_A)
			.param("id", id)
			.query(String.class)
			.single();
	}

}
