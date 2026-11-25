package com.home_platform.media.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.time.Duration;

import com.home_platform.media.MediaIT;
import com.home_platform.media.storage.FileStorage;
import com.home_platform.media.storage.StoredObjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Dọn file tải lên dở (spec Đợt 7, M9): PENDING quá 1 giờ bị xoá cả dòng lẫn nội dung trong kho. */
class PendingCleanupIT extends MediaIT {

	@Autowired
	private PendingCleanup cleanup;

	@Autowired
	private FileStorage storage;

	@Test
	void oldPendingRowsAndContentAreRemoved() {
		file(-81, "PENDING", 120);
		file(-82, "PENDING", 10);
		String oldKey = "workspaces/" + WS_A + "/files/-81";
		this.storage.put(oldKey, new ByteArrayInputStream(new byte[] { 1 }), 1, "image/png");
		this.storage.put(oldKey + "/thumb", new ByteArrayInputStream(new byte[] { 2 }), 1, "image/jpeg");

		int removed = this.cleanup.removeOlderThan(Duration.ofHours(1));

		assertThat(removed).isGreaterThanOrEqualTo(1);
		assertThat(statusOf(-81)).isNull();
		assertThat(statusOf(-82)).isEqualTo("PENDING");
		assertThatThrownBy(() -> this.storage.open(oldKey)).isInstanceOf(StoredObjectNotFoundException.class);
		assertThatThrownBy(() -> this.storage.open(oldKey + "/thumb")).isInstanceOf(StoredObjectNotFoundException.class);
	}

	@Test
	void readyFilesAreNeverTouched() {
		file(-83, "READY", 24 * 60);

		this.cleanup.removeOlderThan(Duration.ofHours(1));

		assertThat(statusOf(-83)).isEqualTo("READY");
	}

	/** Một dòng {@code med_file} tạo cách đây {@code minutesAgo} phút. */
	private void file(long id, String status, int minutesAgo) {
		this.jdbc.sql("INSERT INTO med_file (workspace_id, id, storage_key, original_name, content_type, size_bytes, "
				+ "checksum_sha256, status, uploaded_by, created_at) VALUES (:ws, :id, :key, 'a.png', 'image/png', 1, "
				+ "REPEAT('b', 64), :status, :user, UTC_TIMESTAMP(3) - INTERVAL :minutes MINUTE)")
			.param("ws", WS_A)
			.param("id", id)
			.param("key", "workspaces/" + WS_A + "/files/" + id)
			.param("status", status)
			.param("user", USER)
			.param("minutes", minutesAgo)
			.update();
	}

	private String statusOf(long id) {
		return this.jdbc.sql("SELECT status FROM med_file WHERE workspace_id = :ws AND id = :id")
			.param("ws", WS_A)
			.param("id", id)
			.query(String.class)
			.optional()
			.orElse(null);
	}

}
