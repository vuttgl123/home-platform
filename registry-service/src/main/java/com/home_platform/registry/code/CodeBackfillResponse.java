package com.home_platform.registry.code;

/** Kết quả một lượt cấp mã cho bản ghi cũ; hai số còn lại đếm sau lượt này. */
public record CodeBackfillResponse(int placesAssigned, int itemsAssigned, int placesRemaining, int itemsRemaining) {
}
