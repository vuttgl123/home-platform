package com.home_platform.common.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Clock;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.json.StorageJson;
import com.home_platform.common.web.CursorRequest;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;

class EntityHistoryTests {

	private final EntityHistory history = new EntityHistory(
			new RepositorySupport(mock(JdbcClient.class), () -> 1L, Clock.systemUTC(), new StorageJson()),
			mock(ApplicationEventPublisher.class), "tmp_entity_version");

	@Test
	void restoredNoteIsCutToTheColumnLength() {
		assertThat(EntityHistory.restoredNote(3, null)).isEqualTo("Restored from version 3");
		assertThat(EntityHistory.restoredNote(3, "x")).isEqualTo("Restored from version 3: x");
		assertThat(EntityHistory.restoredNote(3, "x".repeat(300))).hasSize(255).startsWith("Restored from version 3: xxx");
	}

	@Test
	void nonPositiveLimitIsRejected() {
		assertThatThrownBy(() -> this.history.list(-1, "PLACE", 10, new CursorRequest(null, 0)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("limit must be positive: 0");
	}

}
