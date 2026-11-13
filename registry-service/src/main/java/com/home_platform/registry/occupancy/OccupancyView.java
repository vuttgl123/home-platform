package com.home_platform.registry.occupancy;

/** Một dòng cư trú cùng tên địa điểm, tên người và trạng thái tính theo hôm nay. */
public record OccupancyView(Occupancy occupancy, String placeName, String personFullName, OccupancyStatus status) {
}
