package com.home_platform.media.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.nio.charset.StandardCharsets;

import com.home_platform.media.MediaIT;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/** {@code POST /attachments} và {@code GET /attachments} (spec Đợt 7, M1 tới M5). */
class AttachmentUploadIT extends MediaIT {

	@Test
	void uploadsAndAttaches() throws Exception {
		String first = upload(image(800, 600, "jpg", Color.RED), "phong.jpg", "image/jpeg", "PLACE", 10)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.ownerType").value("PLACE"))
			.andExpect(jsonPath("$.ownerId").value("10"))
			.andExpect(jsonPath("$.sortOrder").value(0))
			.andExpect(jsonPath("$.file.width").value(800))
			.andExpect(jsonPath("$.file.height").value(600))
			.andExpect(jsonPath("$.file.contentType").value("image/jpeg"))
			.andExpect(jsonPath("$.file.originalName").value("phong.jpg"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		upload(image(40, 30, "png", Color.BLUE), "cua.png", "image/png", "PLACE", 10).andExpect(status().isCreated())
			.andExpect(jsonPath("$.sortOrder").value(1));

		list("PLACE", 10).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].file.originalName", contains("phong.jpg", "cua.png")));
		String fileId = JsonPath.read(first, "$.fileId");
		assertThat(STORAGE.resolve("workspaces/" + WS_A + "/files/" + fileId + ".bin")).isRegularFile();
		assertThat(STORAGE.resolve("workspaces/" + WS_A + "/files/" + fileId + "/thumb.bin")).isRegularFile();
	}

	@Test
	void portraitPhoneShotIsStoredUpright() throws Exception {
		upload(fixture("portrait-exif6.jpg"), "dien-thoai.jpg", "image/jpeg", "ITEM", 20)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.file.width").value(32))
			.andExpect(jsonPath("$.file.height").value(64));
	}

	@Test
	void webpIsAccepted() throws Exception {
		upload(fixture("small.webp"), "nho.webp", "image/webp", "PERSON", 30).andExpect(status().isCreated())
			.andExpect(jsonPath("$.file.width").value(20));
	}

	@Test
	void fakeImageIsRejected() throws Exception {
		long before = storedFiles();
		upload("không phải ảnh".getBytes(StandardCharsets.UTF_8), "gia.jpg", "image/jpeg", "PLACE", 10)
			.andExpect(status().isBadRequest());
		assertThat(fileRows()).isZero();
		assertThat(storedFiles()).isEqualTo(before);
	}

	@Test
	void mismatchedTypeIsRejected() throws Exception {
		upload(image(10, 10, "png", Color.GREEN), "doi-ten.jpg", "image/jpeg", "PLACE", 10)
			.andExpect(status().isBadRequest());
		assertThat(fileRows()).isZero();
	}

	@Test
	void unsupportedTypeIsRejected() throws Exception {
		upload(image(10, 10, "png", Color.GREEN), "anh.gif", "image/gif", "PLACE", 10)
			.andExpect(status().isBadRequest());
	}

	@Test
	void tooLargeIs413() throws Exception {
		upload(new byte[10 * 1024 * 1024 + 1], "lon.jpg", "image/jpeg", "PLACE", 10)
			.andExpect(status().isPayloadTooLarge());
	}

	@Test
	void unknownOwnerTypeIs400() throws Exception {
		upload(image(10, 10, "png", Color.GREEN), "a.png", "image/png", "ROOM", 10).andExpect(status().isBadRequest());
	}

	@Test
	void sameImageTwiceIsStoredOnce() throws Exception {
		byte[] photo = image(50, 50, "png", Color.ORANGE);
		String first = body(upload(photo, "a.png", "image/png", "PLACE", 10).andExpect(status().isCreated()));
		long stored = storedFiles();
		String second = body(upload(photo, "b.png", "image/png", "ITEM", 11).andExpect(status().isCreated()));

		assertThat((String) JsonPath.read(second, "$.fileId")).isEqualTo(JsonPath.read(first, "$.fileId"));
		assertThat((String) JsonPath.read(second, "$.id")).isNotEqualTo(JsonPath.read(first, "$.id"));
		assertThat(fileRows()).isEqualTo(1);
		assertThat(storedFiles()).isEqualTo(stored);
	}

	@Test
	void sameImageInAnotherWorkspaceIsSeparate() throws Exception {
		byte[] photo = image(50, 50, "png", Color.PINK);
		String a = body(upload(photo, "a.png", "image/png", "PLACE", 10).andExpect(status().isCreated()));
		String b = body(this.mvc.perform(asB(multipart("/attachments").file(part(photo, "a.png", "image/png")))
			.param("ownerType", "PLACE")
			.param("ownerId", "10")).andExpect(status().isCreated()));

		assertThat((String) JsonPath.read(b, "$.fileId")).isNotEqualTo(JsonPath.read(a, "$.fileId"));
	}

	@Test
	void hundredAndFirstIs409() throws Exception {
		String first = body(upload(image(20, 20, "png", Color.CYAN), "1.png", "image/png", "PLACE", 10)
			.andExpect(status().isCreated()));
		long fileId = Long.parseLong(JsonPath.read(first, "$.fileId"));
		for (int i = 1; i < 100; i++) {
			this.jdbc.sql("INSERT INTO med_attachment (workspace_id, id, owner_type, owner_id, file_id, sort_order, "
					+ "added_by) VALUES (:ws, :id, 'PLACE', 10, :fileId, :sort, :user)")
				.param("ws", WS_A)
				.param("id", -1000L - i)
				.param("fileId", fileId)
				.param("sort", i)
				.param("user", USER)
				.update();
		}

		upload(image(21, 21, "png", Color.MAGENTA), "101.png", "image/png", "PLACE", 10)
			.andExpect(status().isConflict());
		upload(image(22, 22, "png", Color.MAGENTA), "khac.png", "image/png", "PLACE", 11)
			.andExpect(status().isCreated());
	}

	@Test
	void captionIsKeptAndTrimmed() throws Exception {
		this.mvc.perform(asA(multipart("/attachments").file(part(image(10, 10, "png", Color.GRAY), "a.png", "image/png")))
			.param("ownerType", "PLACE")
			.param("ownerId", "10")
			.param("caption", "  Cửa sổ hướng nam  ")).andExpect(jsonPath("$.caption").value("Cửa sổ hướng nam"));
	}

	@Test
	void listIsPerOwnerAndWorkspace() throws Exception {
		upload(image(10, 10, "png", Color.RED), "cua-toi.png", "image/png", "PLACE", 10).andExpect(status().isCreated());
		upload(image(11, 11, "png", Color.RED), "cho-khac.png", "image/png", "PLACE", 12).andExpect(status().isCreated());
		upload(image(12, 12, "png", Color.RED), "do.png", "image/png", "ITEM", 10).andExpect(status().isCreated());

		list("PLACE", 10).andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].file.originalName").value("cua-toi.png"));
		this.mvc.perform(asB(get("/attachments")).param("ownerType", "PLACE").param("ownerId", "10"))
			.andExpect(jsonPath("$", hasSize(0)));
	}

	private ResultActions upload(byte[] content, String name, String type, String ownerType, long ownerId)
			throws Exception {
		return this.mvc.perform(asA(multipart("/attachments").file(part(content, name, type)))
			.param("ownerType", ownerType)
			.param("ownerId", Long.toString(ownerId)));
	}

	private ResultActions list(String ownerType, long ownerId) throws Exception {
		return this.mvc.perform(asA(get("/attachments")).param("ownerType", ownerType)
			.param("ownerId", Long.toString(ownerId)));
	}

	private int fileRows() {
		return this.jdbc.sql("SELECT COUNT(*) FROM med_file WHERE workspace_id = :ws")
			.param("ws", WS_A)
			.query(Integer.class)
			.single();
	}

	private static MockMultipartFile part(byte[] content, String name, String type) {
		return new MockMultipartFile("file", name, type, content);
	}

	private static String body(ResultActions result) throws Exception {
		return result.andReturn().getResponse().getContentAsString();
	}

}
