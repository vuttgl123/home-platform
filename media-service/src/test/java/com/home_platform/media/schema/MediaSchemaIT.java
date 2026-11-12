package com.home_platform.media.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import com.home_platform.common.test.AbstractSchemaIT;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class MediaSchemaIT extends AbstractSchemaIT {

	@Test
	void mediaTablesExist() {
		assertThat(tables("med_")).containsExactly("med_attachment", "med_file");
	}

	@Test
	void keysAndIndexesMatchDesign() {
		assertThat(primaryKey("med_file")).containsExactly("workspace_id", "id");
		assertThat(primaryKey("med_attachment")).containsExactly("workspace_id", "id");
		assertThat(index("med_file", "idx_med_file_checksum")).containsExactly("workspace_id", "checksum_sha256");
		assertThat(index("med_attachment", "idx_med_attachment_owner")).containsExactly("workspace_id", "owner_type",
				"owner_id", "removed_at", "sort_order");
		assertThat(index("med_attachment", "idx_med_attachment_file")).containsExactly("workspace_id", "file_id");
	}

	@Test
	void attachmentMustPointToFileInSameWorkspace() {
		file(WS_A, 900);
		assertThatThrownBy(() -> attachment(WS_B, 950, 900, 100))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void attachmentOwnerIsNotTiedToRegistryTables() {
		file(WS_A, 900);
		attachment(WS_A, 950, 900, 123456789L);
		Map<String, Object> row = jdbc.queryForMap(
				"SELECT sort_order, removed_at FROM med_attachment WHERE workspace_id = ? AND id = 950", WS_A);
		assertThat(((Number) row.get("sort_order")).intValue()).isZero();
		assertThat(row.get("removed_at")).isNull();
	}

	@Test
	void newFileStartsAsPending() {
		file(WS_A, 900);
		assertThat(jdbc.queryForObject("SELECT status FROM med_file WHERE workspace_id = ? AND id = 900", String.class,
				WS_A)).isEqualTo("PENDING");
	}

	private void file(long ws, long id) {
		jdbc.update(
				"INSERT INTO med_file (workspace_id, id, storage_key, original_name, content_type, size_bytes, checksum_sha256, uploaded_by)"
						+ " VALUES (?, ?, ?, 'a.jpg', 'image/jpeg', 1, ?, -1)",
				ws, id, "k/" + id, "0".repeat(64));
	}

	private void attachment(long ws, long id, long fileId, long ownerId) {
		jdbc.update(
				"INSERT INTO med_attachment (workspace_id, id, owner_type, owner_id, file_id, added_by)"
						+ " VALUES (?, ?, 'PLACE', ?, ?, -1)",
				ws, id, ownerId, fileId);
	}

}
