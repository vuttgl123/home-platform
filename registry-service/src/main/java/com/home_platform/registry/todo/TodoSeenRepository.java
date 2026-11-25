package com.home_platform.registry.todo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.home_platform.common.jdbc.RepositorySupport;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Lần cuối mỗi người mở chuông trong một không gian ({@code reg_todo_seen}). */
@Repository
class TodoSeenRepository {

	private final JdbcClient jdbc;

	TodoSeenRepository(RepositorySupport support) {
		this.jdbc = support.jdbc();
	}

	Optional<Instant> seenAt(long workspaceId, long userId) {
		return this.jdbc.sql("SELECT seen_at FROM reg_todo_seen WHERE workspace_id = :workspaceId AND user_id = :userId")
			.param("workspaceId", workspaceId)
			.param("userId", userId)
			.query((rs, rowNum) -> rs.getObject("seen_at", LocalDateTime.class).toInstant(ZoneOffset.UTC))
			.optional();
	}

	void markSeen(long workspaceId, long userId, Instant at) {
		this.jdbc.sql("INSERT INTO reg_todo_seen (workspace_id, user_id, seen_at) VALUES (:workspaceId, :userId, :at) "
				+ "ON DUPLICATE KEY UPDATE seen_at = :at")
			.param("workspaceId", workspaceId)
			.param("userId", userId)
			.param("at", LocalDateTime.ofInstant(at.truncatedTo(ChronoUnit.MILLIS), ZoneOffset.UTC))
			.update();
	}

}
