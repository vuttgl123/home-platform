package com.home_platform.registry.item;

/** Một món đồ cùng tên địa điểm, tên nhóm và tên chủ của nó. */
public record ItemView(Item item, String placeName, String categoryName, String ownerName) {
}
