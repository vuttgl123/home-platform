package com.home_platform.media.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.home_platform.common.jdbc.EntityMetadata;
import com.home_platform.common.json.StorageJson;
import org.junit.jupiter.api.Test;

class MediaFileTests {

	@Test
	void recordMatchesTheTableColumns() {
		assertThat(EntityMetadata.of(MediaFile.class, new StorageJson()).selectList())
			.isEqualTo("workspace_id, id, storage_key, original_name, content_type, size_bytes, width, height, "
					+ "checksum_sha256, status, uploaded_by, created_at");
	}

}
