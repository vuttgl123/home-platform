package com.home_platform.common.web;

import java.util.List;

/**
 * Một trang của danh sách phân trang theo vị trí.
 *
 * @param items các phần tử của trang này
 * @param nextCursor vị trí để lấy trang kế tiếp; {@code null} nếu đây là trang cuối
 */
public record CursorPage<T>(List<T> items, String nextCursor) {
}
