package com.home_platform.registry.code;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.registry.RegistryIT;
import com.home_platform.registry.itemcategory.ItemCategoryRequest;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class CodeRuleIT extends RegistryIT {

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	ItemCategoryService categories;

	@Test
	void freshWorkspaceListsTheTwoBuiltInRules() throws Exception {
		this.mvc.perform(asA(get("/code-rules")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].target").value("PLACE"))
			.andExpect(jsonPath("$[0].typeId").value("0"))
			.andExpect(jsonPath("$[0].prefix").value("DD"))
			.andExpect(jsonPath("$[0].digits").value(6))
			.andExpect(jsonPath("$[0].yearly").value(false))
			.andExpect(jsonPath("$[0].builtIn").value(true))
			.andExpect(jsonPath("$[1].target").value("ITEM"))
			.andExpect(jsonPath("$[1].prefix").value("TS"))
			.andExpect(jsonPath("$[1].builtIn").value(true));
	}

	@Test
	void putStoresAndUppercasesThePrefix() throws Exception {
		putRule("PLACE", 0, """
				{"prefix": " p ", "digits": 4, "yearly": false}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.prefix").value("P"))
			.andExpect(jsonPath("$.digits").value(4))
			.andExpect(jsonPath("$.builtIn").value(false));

		this.mvc.perform(asA(get("/code-rules")))
			.andExpect(jsonPath("$[0].prefix").value("P"))
			.andExpect(jsonPath("$[0].builtIn").value(false));
	}

	@Test
	void ownRuleIsListedAfterTheSharedRuleOfItsTarget() throws Exception {
		long room = type("ROOM");
		putRule("PLACE", room, """
				{"prefix": "P", "digits": 4, "yearly": true}""").andExpect(status().isOk());

		this.mvc.perform(asA(get("/code-rules")))
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[0].typeId").value("0"))
			.andExpect(jsonPath("$[1].typeId").value(String.valueOf(room)))
			.andExpect(jsonPath("$[1].yearly").value(true))
			.andExpect(jsonPath("$[2].target").value("ITEM"));
	}

	@Test
	void invalidRulesAreRejected() throws Exception {
		for (String prefix : new String[] { "1A", "A-B", "ABCDEFGHIJK", "" }) {
			putRule("PLACE", 0, "{\"prefix\": \"" + prefix + "\", \"digits\": 4}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItem("prefix")));
		}
		for (int digits : new int[] { 2, 10 }) {
			putRule("PLACE", 0, "{\"prefix\": \"P\", \"digits\": " + digits + "}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItem("digits")));
		}
	}

	@Test
	void ruleForAnUnknownTypeIs404() throws Exception {
		long room = type("ROOM");
		putRule("PLACE", 999, "{\"prefix\": \"P\", \"digits\": 4}").andExpect(status().isNotFound());
		putRule("ITEM", room, "{\"prefix\": \"P\", \"digits\": 4}").andExpect(status().isNotFound());
	}

	@Test
	void unknownTargetIs400() throws Exception {
		putRule("PERSON", 0, "{\"prefix\": \"P\", \"digits\": 4}").andExpect(status().isBadRequest());
	}

	@Test
	void deleteReturnsToTheBuiltInRule() throws Exception {
		putRule("ITEM", 0, "{\"prefix\": \"X\", \"digits\": 4}").andExpect(status().isOk());

		this.mvc.perform(asA(delete("/code-rules/ITEM/0"))).andExpect(status().isNoContent());
		this.mvc.perform(asA(get("/code-rules")))
			.andExpect(jsonPath("$[1].prefix").value("TS"))
			.andExpect(jsonPath("$[1].builtIn").value(true));
		this.mvc.perform(asA(delete("/code-rules/ITEM/0"))).andExpect(status().isNotFound());
	}

	@Test
	void deletingAPlaceTypeDeletesItsRule() throws Exception {
		long room = type("ROOM");
		putRule("PLACE", room, "{\"prefix\": \"P\", \"digits\": 4}").andExpect(status().isOk());

		this.placeTypes.delete(CTX_A, room);

		this.mvc.perform(asA(get("/code-rules"))).andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void deletingACategoryDeletesItsRule() throws Exception {
		long tv = this.categories.create(CTX_A, new ItemCategoryRequest("Tivi", null)).id();
		putRule("ITEM", tv, "{\"prefix\": \"TV\", \"digits\": 4}").andExpect(status().isOk());

		this.categories.delete(CTX_A, tv);

		this.mvc.perform(asA(get("/code-rules"))).andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void otherWorkspaceSeesOnlyBuiltInRules() throws Exception {
		putRule("PLACE", 0, "{\"prefix\": \"P\", \"digits\": 4}").andExpect(status().isOk());

		this.mvc.perform(asB(get("/code-rules")))
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].prefix").value("DD"));
	}

	private long type(String code) {
		return this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest(code, code, null, null)).id();
	}

	private ResultActions putRule(String target, long typeId, String json) throws Exception {
		return this.mvc.perform(asA(put("/code-rules/" + target + "/" + typeId)).contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

}
