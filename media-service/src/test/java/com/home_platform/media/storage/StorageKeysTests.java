package com.home_platform.media.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StorageKeysTests {

	@Test
	void keyGroupsFilesByWorkspace() {
		assertThat(StorageKeys.forFile(-1, 114540094935228416L)).isEqualTo("workspaces/-1/files/114540094935228416");
	}

}
