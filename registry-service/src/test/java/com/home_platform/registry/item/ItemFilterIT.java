package com.home_platform.registry.item;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.jayway.jsonpath.JsonPath;

/** {@code GET /items} có tìm chữ, lọc theo toà và mục tuỳ biến (spec Đợt 11, S5). */
class ItemFilterIT extends ItemTestData {

	@Autowired
	private FieldDefService fieldDefs;

	@Test
	void textMatchesNameCodeAndDescription() throws Exception {
		codeRule("ITEM", 0, "QX", 3, false);
		item(this.room, "Tủ lạnh");
		String body = postItem("{\"placeId\": \"" + this.room + "\", \"name\": \"Quạt\"}").andReturn()
			.getResponse()
			.getContentAsString();
		String code = JsonPath.read(body, "$.code");
		item("{\"placeId\": \"" + this.room + "\", \"name\": \"Máy giặt\", \"description\": \"Còn bảo hành tới 2027\"}");

		items("q", "TU LANH").andExpect(jsonPath("$.items[*].name", contains("Tủ lạnh")));
		items("q", code.toLowerCase()).andExpect(jsonPath("$.items[*].name", contains("Quạt")));
		items("q", "bao hanh").andExpect(jsonPath("$.items[*].name", contains("Máy giặt")));
	}

	@Test
	void buildingIncludesItsOwnPlace() throws Exception {
		long building = place(null, "Toà A");
		long floor = place(building, "Tầng 1");
		item(building, "Máy bơm");
		item(floor, "Bình cứu hoả");
		item(place(null, "Toà B"), "Tủ B");

		items("rootId", Long.toString(building)).andExpect(jsonPath("$.items[*].name", contains("Bình cứu hoả", "Máy bơm")));
		items("rootId", "999999").andExpect(status().isNotFound());
	}

	@Test
	void combinesWithStatusAndCategory() throws Exception {
		long kitchen = category("Bếp");
		item("{\"placeId\": \"" + this.room + "\", \"name\": \"Bếp từ hỏng\", \"categoryId\": \"" + kitchen
				+ "\", \"status\": \"BROKEN\"}");
		item("{\"placeId\": \"" + this.room + "\", \"name\": \"Bếp từ tốt\", \"categoryId\": \"" + kitchen + "\"}");
		item(this.room, "Bếp ga lẻ");
		item("{\"placeId\": \"" + this.room + "\", \"name\": \"Quạt hỏng\", \"status\": \"BROKEN\"}");
		item("{\"placeId\": \"" + this.room + "\", \"name\": \"Nồi\", \"categoryId\": \"" + kitchen + "\"}");

		items("q", "bep", "status", "BROKEN").andExpect(jsonPath("$.items[*].name", contains("Bếp từ hỏng")));
		items("q", "bep", "categoryId", Long.toString(kitchen))
			.andExpect(jsonPath("$.items[*].name", contains("Bếp từ hỏng", "Bếp từ tốt")));
	}

	@Test
	void categoryFieldOnlyMatchesItsCategory() throws Exception {
		long tv = category("TV");
		long fridge = category("Tủ lạnh");
		field("inch", FieldDataType.NUMBER, tv);
		item("{\"placeId\": \"" + this.room + "\", \"name\": \"TV phòng khách\", \"categoryId\": \"" + tv
				+ "\", \"attributes\": {\"inch\": 55}}");
		item("{\"placeId\": \"" + this.room + "\", \"name\": \"TV nhỏ\", \"categoryId\": \"" + tv
				+ "\", \"attributes\": {\"inch\": 32}}");
		item("{\"placeId\": \"" + this.room + "\", \"name\": \"Tủ\", \"categoryId\": \"" + fridge + "\"}");

		items("attr", "inch:gte:50").andExpect(jsonPath("$.items[*].name", contains("TV phòng khách")));
		items("attr", "inch:unset").andExpect(jsonPath("$.items[*].name", contains("Tủ")));
	}

	@Test
	void uncategorizedItemsAreBlankForCategoryFields() throws Exception {
		long tv = category("TV");
		field("hang", FieldDataType.SELECT, tv, "Sony", "LG");
		item("{\"placeId\": \"" + this.room + "\", \"name\": \"TV\", \"categoryId\": \"" + tv
				+ "\", \"attributes\": {\"hang\": \"Sony\"}}");
		item(this.room, "Chưa có nhóm");

		items("attr", "hang:eq:Sony").andExpect(jsonPath("$.items[*].name", contains("TV")));
		items("attr", "hang:unset").andExpect(jsonPath("$.items[*].name", contains("Chưa có nhóm")));
	}

	@Test
	void textWithAPlaceScopeIs400() throws Exception {
		items("placeId", Long.toString(this.room), "q", "tủ").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail")
				.value("status, q, rootId and attr can only be combined with categoryId or no scope"));
		items("ownerId", Long.toString(person("An")), "attr", "x:set").andExpect(status().isBadRequest());
	}

	@Test
	void pages() throws Exception {
		item(this.room, "Ghế 3");
		item(this.room, "Ghế 1");
		item(this.room, "Ghế 2");
		item(this.room, "Bàn");

		String first = items("q", "ghế", "limit", "2").andExpect(jsonPath("$.items[*].name", contains("Ghế 1", "Ghế 2")))
			.andReturn()
			.getResponse()
			.getContentAsString();
		items("q", "ghế", "limit", "2", "cursor", JsonPath.read(first, "$.nextCursor"))
			.andExpect(jsonPath("$.items[*].name", contains("Ghế 3")));
	}

	private void field(String key, FieldDataType type, long categoryId, String... options) {
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.ITEM, categoryId, key, key, type,
				(options.length > 0) ? java.util.List.of(options) : null, false, null, null));
	}

	private ResultActions items(String... params) throws Exception {
		var request = asA(get("/items"));
		for (int i = 0; i < params.length; i += 2) {
			request = request.param(params[i], params[i + 1]);
		}
		return this.mvc.perform(request);
	}

}
