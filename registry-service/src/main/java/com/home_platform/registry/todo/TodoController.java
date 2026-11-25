package com.home_platform.registry.todo;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/todos")
class TodoController {

	private final TodoService service;

	TodoController(TodoService service) {
		this.service = service;
	}

	/** Việc cần làm, gấp nhất trước; {@code kind} lọc một loại. */
	@GetMapping
	CursorPage<TodoResponse> list(WorkspaceContext ctx, @RequestParam(required = false) TodoKind kind,
			CursorRequest page) {
		return this.service.page(ctx, kind, page);
	}

	/** Tổng số, số theo loại, số việc mới của người gọi, và các việc mới nhất. */
	@GetMapping("/summary")
	TodoSummaryResponse summary(WorkspaceContext ctx) {
		return this.service.summary(ctx);
	}

	/** Người gọi vừa mở chuông. */
	@PostMapping("/seen")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void seen(WorkspaceContext ctx) {
		this.service.markSeen(ctx);
	}

}
