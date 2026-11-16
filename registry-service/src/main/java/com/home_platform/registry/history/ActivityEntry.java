package com.home_platform.registry.history;

import com.home_platform.common.history.EntityChange;

/**
 * Một dòng hoạt động kèm tên hiện tại của bản ghi.
 *
 * @param entityName tên lúc này, kể cả khi bản ghi đã bị xoá mềm; {@code null} nếu dòng không còn
 */
public record ActivityEntry(EntityChange change, String entityName) {
}
