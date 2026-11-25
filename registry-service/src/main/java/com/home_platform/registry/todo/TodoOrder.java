package com.home_platform.registry.todo;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorCodec;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;

/**
 * Thứ tự và phân trang của danh sách việc, làm trong bộ nhớ vì danh sách gộp từ nhiều nguồn. Con trỏ là
 * {@code [dueOn, kind, key]} của việc cuối trang trước.
 */
final class TodoOrder {

	/** Hạn sớm nhất (quá hạn lâu nhất, hỏng lâu nhất) trước; cùng ngày thì theo loại, rồi theo {@code key}. */
	static final Comparator<Todo> ORDER = Comparator.comparing(Todo::dueOn)
		.thenComparing(Todo::kind)
		.thenComparing(Todo::key);

	private TodoOrder() {
	}

	static List<Todo> sort(Collection<Todo> todos) {
		return todos.stream().sorted(ORDER).toList();
	}

	/**
	 * @param sorted đã sắp theo {@link #ORDER}
	 * @throws BadRequestException nếu con trỏ hỏng hoặc thuộc danh sách khác
	 */
	static CursorPage<Todo> page(List<Todo> sorted, CursorRequest page) {
		int from = 0;
		if (page.cursor() != null) {
			Todo after = decode(page.cursor());
			while (from < sorted.size() && ORDER.compare(sorted.get(from), after) <= 0) {
				from++;
			}
		}
		List<Todo> rows = sorted.subList(from, Math.min(sorted.size(), from + page.limit() + 1));
		return CursorPage.of(rows, page.limit(),
				todo -> List.of(todo.dueOn().toString(), todo.kind().name(), todo.key()));
	}

	private static Todo decode(String cursor) {
		List<String> parts = CursorCodec.decode(cursor);
		if (parts.size() != 3) {
			throw new BadRequestException("Invalid cursor");
		}
		try {
			return new Todo(parts.get(2), TodoKind.valueOf(parts.get(1)), LocalDate.parse(parts.get(0)), null, null, 0,
					null, null, null);
		}
		catch (IllegalArgumentException | DateTimeParseException ex) {
			throw new BadRequestException("Invalid cursor");
		}
	}

}
