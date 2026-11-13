package com.home_platform.registry.place;

import java.time.Instant;
import java.util.Map;

/**
 * Nội dung một địa điểm ghi vào version. Không có cột kiểm toán và {@code version}: người sửa,
 * lúc sửa, số version đã nằm trên dòng version, và nếu đưa vào đây thì lần sửa nào cũng thấy
 * chúng trong danh sách trường đã đổi. Địa chỉ là một phần của địa điểm nên nằm trong snapshot.
 */
public record PlaceSnapshot(long id, Long parentId, long placeTypeId, String path, int depth, String code,
		String name, String description, PlaceStatus status, Map<String, Object> attributes, Long coverFileId,
		Instant deletedAt, AddressSnapshot address) {

	public static PlaceSnapshot of(Place place, PlaceAddress address) {
		return new PlaceSnapshot(place.id(), place.parentId(), place.placeTypeId(), place.path(), place.depth(),
				place.code(), place.name(), place.description(), place.status(), place.attributes(),
				place.coverFileId(), place.deletedAt(), AddressSnapshot.of(address));
	}

}
