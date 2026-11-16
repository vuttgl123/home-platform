package com.home_platform.registry.place;

import java.util.List;

/**
 * Một kết quả tìm địa điểm: như {@link PlaceResponse}, kèm tổ tiên.
 *
 * @param ancestors từ gốc tới cha; rỗng với địa điểm cấp cao nhất
 */
public record PlaceSearchResponse(long id, Long parentId, long placeTypeId, String code, String name,
		PlaceStatus status, int depth, Long coverFileId, int version, boolean hasChildren, List<PlaceRef> ancestors) {
}
