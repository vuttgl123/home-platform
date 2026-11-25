package com.home_platform.registry.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.junit.jupiter.api.Test;

class TodoOrderTests {

	private static final LocalDate DAY = LocalDate.of(2026, 12, 1);

	@Test
	void sortsByDueDateThenKindThenKey() {
		List<Todo> sorted = TodoOrder.sort(List.of(todo("CUSTOM_DATE:PERSON:1:a", TodoKind.CUSTOM_DATE, DAY),
				todo("WARRANTY:9", TodoKind.WARRANTY, DAY), todo("BROKEN:5", TodoKind.BROKEN, DAY.plusDays(1)),
				todo("BROKEN:3", TodoKind.BROKEN, DAY.minusDays(40)), todo("WARRANTY:2", TodoKind.WARRANTY, DAY)));

		assertThat(sorted).extracting(Todo::key)
			.containsExactly("BROKEN:3", "WARRANTY:2", "WARRANTY:9", "CUSTOM_DATE:PERSON:1:a", "BROKEN:5");
	}

	@Test
	void pagesContinueRightAfterTheCursor() {
		List<Todo> sorted = TodoOrder.sort(List.of(todo("BROKEN:1", TodoKind.BROKEN, DAY),
				todo("BROKEN:2", TodoKind.BROKEN, DAY), todo("SERVICE:3", TodoKind.SERVICE, DAY),
				todo("MOVE_OUT:4", TodoKind.MOVE_OUT, DAY.plusDays(2))));

		CursorPage<Todo> first = TodoOrder.page(sorted, new CursorRequest(null, 2));
		CursorPage<Todo> second = TodoOrder.page(sorted, new CursorRequest(first.nextCursor(), 2));

		assertThat(first.items()).extracting(Todo::key).containsExactly("BROKEN:1", "BROKEN:2");
		assertThat(first.nextCursor()).isNotNull();
		assertThat(second.items()).extracting(Todo::key).containsExactly("SERVICE:3", "MOVE_OUT:4");
		assertThat(second.nextCursor()).isNull();
	}

	@Test
	void foreignCursorIs400() {
		assertThatThrownBy(() -> TodoOrder.page(List.of(), new CursorRequest("WyJ4Il0", 2)))
			.isInstanceOf(BadRequestException.class);
	}

	private static Todo todo(String key, TodoKind kind, LocalDate dueOn) {
		return new Todo(key, kind, dueOn, Instant.EPOCH, "ITEM", 1, "x", null, null);
	}

}
