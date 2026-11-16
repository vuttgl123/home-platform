package com.home_platform.registry.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

/** Mã của món đồ do hệ thống cấp khi tạo và không đổi được bằng lệnh sửa. */
class ItemCodeIT extends ItemTestData {

	@Test
	void createIssuesConsecutiveCodes() throws Exception {
		assertThat(codeOf(item(this.room, "a"))).isEqualTo("TS-000001");
		assertThat(codeOf(item(this.room, "b"))).isEqualTo("TS-000002");
	}

	@Test
	void createIgnoresASentCode() throws Exception {
		postItem("{\"placeId\": \"%d\", \"name\": \"a\", \"code\": \"X\"}".formatted(this.room))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value("TS-000001"));
	}

	@Test
	void categoryRuleWinsOverTheSharedRule() throws Exception {
		long tv = category("Tivi");
		codeRule("ITEM", 0, "X", 3, false);
		codeRule("ITEM", tv, "TV", 4, false);

		assertThat(codeOf(item("{\"placeId\": \"%d\", \"categoryId\": \"%d\", \"name\": \"a\"}".formatted(this.room, tv))))
			.isEqualTo("TV-0001");
		assertThat(codeOf(item(this.room, "b"))).isEqualTo("X-001");
	}

	@Test
	void itemWithoutCategoryUsesTheSharedRule() throws Exception {
		codeRule("ITEM", 0, "X", 3, false);
		assertThat(codeOf(item(this.room, "a"))).isEqualTo("X-001");
	}

	@Test
	void replacementGetsItsOwnCode() throws Exception {
		long old = item(this.room, "Tivi cũ");

		String body = action(old, "replace", """
				{"version": 1, "replacement": {"name": "Tivi mới", "code": "X"}}""")
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		assertThat((String) JsonPath.read(body, "$.removed.code")).isEqualTo("TS-000001");
		assertThat((String) JsonPath.read(body, "$.replacement.code")).isEqualTo("TS-000002");
	}

	@Test
	void updateIgnoresASentCode() throws Exception {
		long id = item(this.room, "a");

		putItem(id, """
				{"version": 1, "name": "b", "status": "IN_USE", "code": "HACK"}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("TS-000001"));
		assertThat(version(id, 2).changedFields()).doesNotContain("code");
	}

	@Test
	void placesAndItemsCountSeparately() throws Exception {
		codeRule("PLACE", 0, "X", 6, false);
		codeRule("ITEM", 0, "X", 6, false);
		long place = place(null, "P102");

		assertThat(this.places.get(WS_A, place).code()).isEqualTo("X-000001");
		assertThat(codeOf(item(place, "a"))).isEqualTo("X-000001");
	}

	@Test
	void reissueGivesTheNextCodeAndRecordsIt() throws Exception {
		long id = item(this.room, "a");

		action(id, "reissue-code", "{\"version\": 1}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("TS-000002"))
			.andExpect(jsonPath("$.version").value(2));
		assertThat(version(id, 2).changedFields()).containsExactly("code");
		assertThat(version(id, 2).note()).isEqualTo("Code changed from TS-000001");
	}

	@Test
	void reissueFollowsTheCurrentCategorysRule() throws Exception {
		long tv = category("Tivi");
		long id = item("{\"placeId\": \"%d\", \"categoryId\": \"%d\", \"name\": \"a\"}".formatted(this.room, tv));
		codeRule("ITEM", tv, "TV", 4, false);

		action(id, "reissue-code", "{\"version\": 1}").andExpect(jsonPath("$.code").value("TV-0001"));
	}

	@Test
	void reissueChecksTheVersion() throws Exception {
		long id = item(this.room, "a");

		action(id, "reissue-code", "{\"version\": 9}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
	}

	@Test
	void removedItemCannotGetANewCode() throws Exception {
		long id = item(this.room, "a");
		action(id, "remove", "{\"version\": 1}").andExpect(status().isOk());

		action(id, "reissue-code", "{\"version\": 2}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Item " + id + " is removed"));
	}

	private String codeOf(long id) throws Exception {
		return JsonPath.read(getItem(id).andReturn().getResponse().getContentAsString(), "$.code");
	}

}
