package com.home_platform.media.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.util.Map;

import com.home_platform.media.MediaIT;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/** Chú thích, sắp xếp, gỡ ảnh (spec Đợt 7, M5 tới M7). */
class AttachmentEditIT extends MediaIT {

	@Test
	void captionIsSavedAndCleared() throws Exception {
		String id = photo("PLACE", 10, Color.RED);
		caption(id, "\"  Góc bếp  \"").andExpect(status().isOk()).andExpect(jsonPath("$.caption").value("Góc bếp"));
		caption(id, "\"   \"").andExpect(status().isOk()).andExpect(jsonPath("$.caption").doesNotExist());
		list("PLACE", 10).andExpect(jsonPath("$[0].caption").doesNotExist());
	}

	@Test
	void captionOver255Is400() throws Exception {
		String id = photo("PLACE", 10, Color.RED);
		caption(id, "\"" + "a".repeat(256) + "\"").andExpect(status().isBadRequest());
	}

	@Test
	void reorderSetsSortOrders() throws Exception {
		String a = photo("ITEM", 5, Color.RED);
		String b = photo("ITEM", 5, Color.GREEN);
		String c = photo("ITEM", 5, Color.BLUE);

		order("ITEM", 5, c, a, b).andExpect(status().isOk()).andExpect(jsonPath("$[*].id", contains(c, a, b)));
		list("ITEM", 5).andExpect(jsonPath("$[*].id", contains(c, a, b)))
			.andExpect(jsonPath("$[*].sortOrder", contains(0, 1, 2)));
	}

	@Test
	void staleOrderIs409() throws Exception {
		String a = photo("ITEM", 5, Color.RED);
		String b = photo("ITEM", 5, Color.GREEN);
		String other = photo("ITEM", 6, Color.BLUE);
		String gone = photo("ITEM", 5, Color.ORANGE);
		remove(gone).andExpect(status().isNoContent());

		order("ITEM", 5, b).andExpect(status().isConflict());
		order("ITEM", 5, b, a, other).andExpect(status().isConflict());
		order("ITEM", 5, b, a, gone).andExpect(status().isConflict());
		order("ITEM", 5, b, a, a).andExpect(status().isConflict());
		list("ITEM", 5).andExpect(jsonPath("$[*].id", contains(a, b)));
	}

	@Test
	void removeHidesButKeepsTheRow() throws Exception {
		String keep = photo("PERSON", 7, Color.RED);
		String drop = photo("PERSON", 7, Color.GREEN);

		remove(drop).andExpect(status().isNoContent());

		list("PERSON", 7).andExpect(jsonPath("$[*].id", contains(keep)));
		Map<String, Object> row = this.jdbc.sql("SELECT removed_by, removed_at FROM med_attachment "
				+ "WHERE workspace_id = :ws AND id = :id")
			.param("ws", WS_A)
			.param("id", Long.parseLong(drop))
			.query()
			.singleRow();
		assertThat(((Number) row.get("removed_by")).longValue()).isEqualTo(USER);
		assertThat(row.get("removed_at")).isNotNull();
	}

	@Test
	void removingTwiceIs404() throws Exception {
		String id = photo("PERSON", 7, Color.RED);
		remove(id).andExpect(status().isNoContent());
		remove(id).andExpect(status().isNotFound());
		caption(id, "\"muộn\"").andExpect(status().isNotFound());
	}

	@Test
	void otherWorkspaceCannotEdit() throws Exception {
		String id = photo("PLACE", 10, Color.RED);
		this.mvc.perform(asB(put("/attachments/" + id)).contentType(MediaType.APPLICATION_JSON)
			.content("{\"caption\": \"lạ\"}")).andExpect(status().isNotFound());
		this.mvc.perform(asB(delete("/attachments/" + id))).andExpect(status().isNotFound());
		this.mvc.perform(asB(put("/attachments/order")).contentType(MediaType.APPLICATION_JSON)
			.content("{\"ownerType\": \"PLACE\", \"ownerId\": \"10\", \"attachmentIds\": [\"" + id + "\"]}"))
			.andExpect(status().isConflict());
		list("PLACE", 10).andExpect(jsonPath("$[0].caption").doesNotExist());
	}

	@Test
	void emptyOrderOfAnEmptyRecordIsFine() throws Exception {
		order("PLACE", 99).andExpect(status().isOk()).andExpect(jsonPath("$", empty()));
	}

	/** Tải một ảnh một màu lên bản ghi; trả {@code id} của dòng gắn. */
	private String photo(String ownerType, long ownerId, Color color) throws Exception {
		byte[] image = AttachmentUploadIT.image(12, 12, "png", color);
		String body = this.mvc.perform(asA(multipart("/attachments")
			.file(new MockMultipartFile("file", "a.png", "image/png", image)))
			.param("ownerType", ownerType)
			.param("ownerId", Long.toString(ownerId))).andExpect(status().isCreated()).andReturn().getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private ResultActions caption(String id, String captionJson) throws Exception {
		return this.mvc.perform(asA(put("/attachments/" + id)).contentType(MediaType.APPLICATION_JSON)
			.content("{\"caption\": " + captionJson + "}"));
	}

	private ResultActions order(String ownerType, long ownerId, String... ids) throws Exception {
		String list = String.join(",", java.util.Arrays.stream(ids).map(id -> "\"" + id + "\"").toList());
		return this.mvc.perform(asA(put("/attachments/order")).contentType(MediaType.APPLICATION_JSON)
			.content("{\"ownerType\": \"%s\", \"ownerId\": \"%d\", \"attachmentIds\": [%s]}"
				.formatted(ownerType, ownerId, list)));
	}

	private ResultActions remove(String id) throws Exception {
		return this.mvc.perform(asA(delete("/attachments/" + id)));
	}

	private ResultActions list(String ownerType, long ownerId) throws Exception {
		return this.mvc.perform(asA(get("/attachments")).param("ownerType", ownerType)
			.param("ownerId", Long.toString(ownerId)));
	}

}
