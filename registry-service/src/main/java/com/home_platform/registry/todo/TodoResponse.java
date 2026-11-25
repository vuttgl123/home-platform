package com.home_platform.registry.todo;

import java.time.Instant;
import java.time.LocalDate;

/** @param isNew {@code since} sau lần cuối người gọi mở chuông, hoặc người đó chưa mở lần nào */
public record TodoResponse(String key, TodoKind kind, LocalDate dueOn, Instant since, boolean isNew,
		String subjectType, long subjectId, String subjectName, String subjectCode, String context) {

	static TodoResponse of(Todo todo, boolean isNew) {
		return new TodoResponse(todo.key(), todo.kind(), todo.dueOn(), todo.since(), isNew, todo.subjectType(),
				todo.subjectId(), todo.subjectName(), todo.subjectCode(), todo.context());
	}

}
