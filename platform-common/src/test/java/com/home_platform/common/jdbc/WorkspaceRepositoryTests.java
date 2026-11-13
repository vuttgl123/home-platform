package com.home_platform.common.jdbc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Clock;

import com.home_platform.common.json.StorageJson;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class WorkspaceRepositoryTests {

	private final RepositorySupport support = new RepositorySupport(mock(JdbcClient.class), () -> 1L,
			Clock.systemUTC(), new StorageJson());

	@Test
	void recordWithoutWorkspaceIdAndIdIsRejected() {
		assertThatThrownBy(() -> new WorkspaceRepository<>(this.support, "t", NoKey.class) {
		}).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("NoKey must declare long workspaceId and long id");
	}

	@Test
	void boxedKeyIsRejectedToo() {
		assertThatThrownBy(() -> new WorkspaceRepository<>(this.support, "t", BoxedKey.class) {
		}).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("BoxedKey must declare long workspaceId and long id");
	}

	record NoKey(long id, String name) {
	}

	record BoxedKey(Long workspaceId, long id) {
	}

}
