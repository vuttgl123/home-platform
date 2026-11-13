package com.home_platform.common.web;

/**
 * Yêu cầu lấy một trang của danh sách phân trang theo vị trí. Controller khai báo một tham số
 * kiểu này; giá trị lấy từ hai tham số {@code cursor} và {@code limit} trên URL.
 *
 * @param cursor {@code nextCursor} của trang trước; {@code null} nghĩa là trang đầu
 * @param limit số phần tử tối đa của trang
 */
public record CursorRequest(String cursor, int limit) {
}
