package com.home_platform.registry.todo;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Một việc cần làm, tính khi đọc.
 *
 * @param key chuỗi duy nhất của việc, ví dụ {@code BROKEN:123}, {@code CUSTOM_DATE:PERSON:7:id_expiry}
 * @param dueOn ngày dùng để sắp: hạn, hoặc ngày món chuyển sang hỏng
 * @param since lúc việc xuất hiện, để chuông biết việc nào mới (L6)
 * @param subjectType hồ sơ cần mở: {@code ITEM}, {@code PERSON} hoặc {@code PLACE}
 * @param subjectCode mã của hồ sơ; {@code null} với người
 * @param context chữ phụ: tên địa điểm của món hay của dòng cư trú, hoặc nhãn của mục tuỳ biến
 */
public record Todo(String key, TodoKind kind, LocalDate dueOn, Instant since, String subjectType, long subjectId,
		String subjectName, String subjectCode, String context) {
}
