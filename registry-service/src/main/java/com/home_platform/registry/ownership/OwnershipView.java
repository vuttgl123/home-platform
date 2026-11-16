package com.home_platform.registry.ownership;

/** Một dòng sở hữu cùng tên địa điểm, tên người và trạng thái tính theo hôm nay. */
public record OwnershipView(Ownership ownership, String placeName, String personFullName, OwnershipStatus status) {
}
