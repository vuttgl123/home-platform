package com.home_platform.registry.place;

/** Một địa điểm trong danh sách. */
public record PlaceResponse(long id, Long parentId, long placeTypeId, String code, String name, PlaceStatus status,
		int depth, Long coverFileId, int version) {
}
