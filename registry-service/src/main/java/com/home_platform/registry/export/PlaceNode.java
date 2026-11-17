package com.home_platform.registry.export;

/** Một địa điểm trong cây, vừa đủ để dựng đường dẫn tên: {@code parentId} trống là địa điểm gốc. */
public record PlaceNode(long id, Long parentId, String name) {
}
