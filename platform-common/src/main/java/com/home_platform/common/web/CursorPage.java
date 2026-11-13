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

	/** Cùng trang đó với từng phần tử được đổi sang kiểu khác, ví dụ từ record của bảng sang response. */
	public <R> CursorPage<R> map(Function<? super T, ? extends R> mapper) {
		return new CursorPage<>(this.items.stream().<R>map(mapper).toList(), this.nextCursor);
	}

}
