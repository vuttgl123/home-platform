package com.home_platform.registry.todo;

import java.util.List;
import java.util.Map;

/**
 * Số việc cho thanh bên và chuông, cùng một lần tính với {@code GET /todos} nên các số không lệch nhau.
 * @param byKind đủ mọi loại; loại không có việc thì 0
 * @param newCount số việc có {@code since} sau lần cuối người gọi mở chuông
 * @param latest tối đa {@value TodoService#LATEST} việc có {@code since} mới nhất
 */
public record TodoSummaryResponse(int total, Map<TodoKind, Integer> byKind, int newCount, List<TodoResponse> latest) {
}
