package com.home_platform.registry.history;

import com.home_platform.common.history.EntityChange;

/**
 * Một dòng hoạt động kèm tên hiện tại của bản ghi.
 *
 * @param entityName tên lúc này, kể cả khi bản ghi đã bị xoá mềm; {@code null} nếu dòng không còn
 * @param parentId bản ghi chứa nó, để dẫn link: món đồ của một dòng bảo trì; {@code null} với loại khác
 */
public record ActivityEntry(EntityChange change, String entityName, Long parentId) {
}
