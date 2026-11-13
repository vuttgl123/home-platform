package com.home_platform.registry.item;

/** Món cũ (đã gỡ, trỏ tới món mới) và món mới. */
public record ItemReplaceResponse(ItemDetailResponse removed, ItemDetailResponse replacement) {
}
