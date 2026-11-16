package com.home_platform.registry.code;

/** Số bản ghi chưa xoá (và với đồ: chưa gỡ) chưa có mã. */
public record CodeStatusResponse(int placesWithoutCode, int itemsWithoutCode) {
}
