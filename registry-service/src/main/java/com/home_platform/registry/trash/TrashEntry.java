package com.home_platform.registry.trash;

import java.time.Instant;

/**
 * Một bản ghi đang bị xoá.
 *
 * @param context địa điểm: tên chỗ chứa nó; đồ đạc: tên chỗ đặt; người: {@code null}
 * @param deletedBy người xoá: người ghi lần cuối, vì lần ghi cuối của một dòng đang bị xoá là lần xoá
 * @param version {@code version} hiện tại; gửi lại số này để khôi phục
 */
public record TrashEntry(TrashType entityType, long id, String name, String code, String context, Instant deletedAt,
		long deletedBy, int version) {
}
