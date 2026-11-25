package com.home_platform.media.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Hợp đồng mọi bản cài đặt {@link FileStorage} phải giữ (thư mục trên đĩa hôm nay, S3 hay MinIO về
 * sau). Mỗi bản cài đặt có một lớp test kế thừa lớp này.
 */
abstract class FileStorageContract {

	/** Một kho rỗng, riêng cho từng test. */
	abstract FileStorage storage();

	@Test
	void putThenOpenReturnsTheSameBytes() throws IOException {
		storage().put("workspaces/-1/files/1", bytes("ảnh"), 5, "image/jpeg");
		assertThat(read("workspaces/-1/files/1")).isEqualTo("ảnh");
	}

	@Test
	void putOverwrites() throws IOException {
		storage().put("workspaces/-1/files/1", bytes("cũ"), 4, "image/jpeg");
		storage().put("workspaces/-1/files/1", bytes("mới"), 4, "image/jpeg");
		assertThat(read("workspaces/-1/files/1")).isEqualTo("mới");
	}

	@Test
	void deleteRemoves() {
		storage().put("workspaces/-1/files/1", bytes("x"), 1, "image/png");
		storage().delete("workspaces/-1/files/1");
		assertThatThrownBy(() -> storage().open("workspaces/-1/files/1")).isInstanceOf(StoredObjectNotFoundException.class);
	}

	@Test
	void deleteOfMissingKeyDoesNothing() {
		storage().delete("workspaces/-1/files/404");
	}

	@Test
	void openOfMissingKeyIsNotFound() {
		assertThatThrownBy(() -> storage().open("workspaces/-1/files/404")).isInstanceOf(StoredObjectNotFoundException.class);
	}

	@Test
	void nestedKeysWork() throws IOException {
		storage().put("workspaces/-1/files/9", bytes("gốc"), 4, "image/jpeg");
		storage().put("workspaces/-1/files/9/thumb", bytes("nhỏ"), 4, "image/jpeg");
		assertThat(read("workspaces/-1/files/9")).isEqualTo("gốc");
		assertThat(read("workspaces/-1/files/9/thumb")).isEqualTo("nhỏ");
	}

	@Test
	void putReportsSize() {
		StoredObject stored = storage().put("workspaces/-1/files/2", bytes("12345"), 5, "image/webp");
		assertThat(stored).isEqualTo(new StoredObject("workspaces/-1/files/2", 5, "image/webp"));
	}

	@Test
	void keysCannotEscapeTheStore() {
		for (String key : new String[] { "../outside", "/absolute", "a\\b", "workspaces/../../x" }) {
			assertThatThrownBy(() -> storage().put(key, bytes("x"), 1, "image/png"))
				.as(key)
				.isInstanceOf(IllegalArgumentException.class);
		}
	}

	private String read(String key) throws IOException {
		try (InputStream in = storage().open(key)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static InputStream bytes(String text) {
		return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
	}

}
