package com.home_platform.media.attachment;

import static org.assertj.core.api.Assertions.assertThat;

import com.home_platform.common.jdbc.EntityMetadata;
import com.home_platform.common.json.StorageJson;
import org.junit.jupiter.api.Test;

class AttachmentTests {

	@Test
	void recordMatchesTheTableColumns() {
		assertThat(EntityMetadata.of(Attachment.class, new StorageJson()).selectList())
			.isEqualTo("workspace_id, id, owner_type, owner_id, file_id, caption, sort_order, added_by, added_at, "
					+ "removed_by, removed_at");
	}

}
