package com.home_platform.media.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import com.home_platform.media.MediaIT;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** {@code GET /workspaces/{ws}/files/{id}} và {@code /thumbnail}: trình duyệt mở bằng {@code <img src>}. */
class FileContentIT extends MediaIT {

	private static final String CACHE = "private, max-age=31536000, immutable";

	@Test
	void servesOriginalBytes() throws Exception {
		byte[] image = image(900, 300, "png", Color.RED);
		String fileId = upload(image)[1];

		byte[] served = this.mvc.perform(get("/workspaces/" + WS_A + "/files/" + fileId))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "image/png"))
			.andExpect(header().string("Cache-Control", CACHE))
			.andExpect(header().string("X-Content-Type-Options", "nosniff"))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();
		assertThat(served).isEqualTo(image);
	}

	@Test
	void servesThumbnail() throws Exception {
		String fileId = upload(image(900, 300, "png", Color.BLUE))[1];

		byte[] served = this.mvc.perform(get("/workspaces/" + WS_A + "/files/" + fileId + "/thumbnail"))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "image/jpeg"))
			.andExpect(header().string("Cache-Control", CACHE))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();
		BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(served));
		assertThat(thumbnail.getWidth()).isEqualTo(480);
		assertThat(thumbnail.getHeight()).isEqualTo(160);
	}

	@Test
	void otherWorkspaceIs404() throws Exception {
		String fileId = upload(image(10, 10, "png", Color.GREEN))[1];
		this.mvc.perform(get("/workspaces/" + WS_B + "/files/" + fileId)).andExpect(status().isNotFound());
		this.mvc.perform(get("/workspaces/" + WS_B + "/files/" + fileId + "/thumbnail")).andExpect(status().isNotFound());
	}

	@Test
	void pendingFileIs404() throws Exception {
		this.jdbc.sql("INSERT INTO med_file (workspace_id, id, storage_key, original_name, content_type, size_bytes, "
				+ "checksum_sha256, status, uploaded_by) VALUES (:ws, -77, 'workspaces/-1/files/-77', 'a.png', "
				+ "'image/png', 10, REPEAT('a', 64), 'PENDING', :user)")
			.param("ws", WS_A)
			.param("user", USER)
			.update();
		this.mvc.perform(get("/workspaces/" + WS_A + "/files/-77")).andExpect(status().isNotFound());
	}

	@Test
	void unknownFileIs404() throws Exception {
		this.mvc.perform(get("/workspaces/" + WS_A + "/files/123456789")).andExpect(status().isNotFound());
		this.mvc.perform(get("/workspaces/" + WS_A + "/files/abc")).andExpect(status().isBadRequest());
	}

	@Test
	void fileOfARemovedPhotoStillServes() throws Exception {
		String[] ids = upload(image(10, 10, "png", Color.ORANGE));
		this.mvc.perform(asA(delete("/attachments/" + ids[0]))).andExpect(status().isNoContent());
		this.mvc.perform(get("/workspaces/" + WS_A + "/files/" + ids[1])).andExpect(status().isOk());
	}

	/** Tải ảnh lên một địa điểm; trả {@code [id dòng gắn, id file]}. */
	private String[] upload(byte[] image) throws Exception {
		String body = this.mvc.perform(asA(multipart("/attachments")
			.file(new MockMultipartFile("file", "a.png", "image/png", image)))
			.param("ownerType", "PLACE")
			.param("ownerId", "10")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return new String[] { JsonPath.read(body, "$.id"), JsonPath.read(body, "$.fileId") };
	}

}
