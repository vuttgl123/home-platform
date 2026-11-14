package com.home_platform.common.web;

import java.util.List;
import java.util.function.Function;

/**
 * Một trang của danh sách phân trang theo vị trí.
 *
 * @param items các phần tử của trang này
 * @param nextCursor vị trí để lấy trang kế tiếp; {@code null} nếu đây là trang cuối
 */
public record CursorPage<T>(List<T> items, String nextCursor) {

	/**
	 * Dựng một trang từ kết quả của truy vấn đã lấy thừa một dòng ({@code LIMIT limit + 1}): có dòng
	 * thừa nghĩa là còn trang sau, và cursor của trang sau lấy từ dòng cuối cùng được giữ lại.
	 * @param rows tối đa {@code limit + 1} dòng, theo đúng thứ tự của danh sách
	 * @param cursorOf các phần của cursor ứng với một dòng, để {@link CursorCodec} mã hoá
	 * @throws IllegalArgumentException nếu {@code limit} nhỏ hơn 1
	 */
	public static <T> CursorPage<T> of(List<T> rows, int limit, Function<? super T, List<String>> cursorOf) {
		if (limit < 1) {
			throw new IllegalArgumentException("limit must be positive: " + limit);
		}
		if (rows.size() <= limit) {
			return new CursorPage<>(rows, null);
		}
		List<T> items = List.copyOf(rows.subList(0, limit));
		return new CursorPage<>(items, CursorCodec.encode(cursorOf.apply(items.get(limit - 1))));
	}

	/** Cùng trang đó với từng phần tử được đổi sang kiểu khác, ví dụ từ record của bảng sang response. */
	public <R> CursorPage<R> map(Function<? super T, ? extends R> mapper) {
		return new CursorPage<>(this.items.stream().<R>map(mapper).toList(), this.nextCursor);
	}

}
