package com.home_platform.registry.item;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.history.EntityVersion;
import com.home_platform.registry.RegistryIT;
import com.home_platform.registry.itemcategory.ItemCategoryRequest;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.place.PlaceCreateRequest;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Hàm phụ dùng chung cho các test đồ đạc. Mỗi test có sẵn một phòng {@link #room}. */
abstract class ItemTestData extends RegistryIT {

	@Autowired
	protected PlaceService places;

	@Autowired
	protected PlaceTypeService placeTypes;

	@Autowired
	protected ItemCategoryService categories;

	@Autowired
	protected EntityHistory history;

	protected long type;

	protected long room;

	@BeforeEach
	void room() {
		this.type = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("T", "T", null, null)).id();
		this.room = place(null, "P101");
	}

	protected long place(Long parentId, String name) {
		return this.places
			.create(CTX_A, new PlaceCreateRequest(parentId, this.type, null, name, null, null, null, null, null, null))
			.id();
	}

	protected long category(String name) {
		return this.categories.create(CTX_A, new ItemCategoryRequest(name, null)).id();
	}

	protected long item(String json) throws Exception {
		String body = postItem(json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return Long.parseLong(JsonPath.read(body, "$.id"));
	}

	/** Một món tên {@code name} trong {@code placeId}. */
	protected long item(long placeId, String name) throws Exception {
		return item("{\"placeId\": \"" + placeId + "\", \"name\": \"" + name + "\"}");
	}

	protected ResultActions postItem(String json) throws Exception {
		return this.mvc.perform(asA(post("/items")).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	protected ResultActions putItem(long id, String json) throws Exception {
		return this.mvc.perform(asA(put("/items/" + id)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	protected ResultActions action(long id, String action, String json) throws Exception {
		return this.mvc.perform(asA(post("/items/" + id + "/" + action)).contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	protected ResultActions getItem(long id) throws Exception {
		return this.mvc.perform(asA(get("/items/" + id)));
	}

	protected EntityVersion version(long itemId, int versionNo) {
		return this.history.get(WS_A, "ITEM", itemId, versionNo);
	}

}
