package com.home_platform.registry.place;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.history.EntityVersion;
import com.home_platform.registry.RegistryIT;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Hàm phụ dùng chung cho các test địa điểm. */
abstract class PlaceTestData extends RegistryIT {

	@Autowired
	protected PlaceTypeService placeTypes;

	@Autowired
	protected EntityHistory history;

	/** Một loại địa điểm mới của WS_A. */
	protected long type(String code) {
		return this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest(code, code, null, null)).id();
	}

	/** Tạo địa điểm qua API, trả {@code id}. */
	protected long place(String json) throws Exception {
		String body = postPlace(json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return Long.parseLong(JsonPath.read(body, "$.id"));
	}

	protected long place(Long parentId, long typeId, String name) throws Exception {
		String parent = (parentId != null) ? "\"" + parentId + "\"" : "null";
		return place("""
				{"parentId": %s, "placeTypeId": "%d", "name": "%s"}""".formatted(parent, typeId, name));
	}

	protected ResultActions postPlace(String json) throws Exception {
		return this.mvc.perform(asA(post("/places")).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	protected ResultActions putPlace(long id, String json) throws Exception {
		return this.mvc.perform(asA(put("/places/" + id)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	protected ResultActions getPlace(long id) throws Exception {
		return this.mvc.perform(asA(get("/places/" + id)));
	}

	protected EntityVersion version(long placeId, int versionNo) {
		return this.history.get(WS_A, "PLACE", placeId, versionNo);
	}

}
