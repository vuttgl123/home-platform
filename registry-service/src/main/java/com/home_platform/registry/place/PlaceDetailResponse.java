package com.home_platform.registry.place;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Một địa điểm đầy đủ.
 *
 * @param ancestors tổ tiên từ gốc tới cha, để hiện "Toà A › Tầng 3 › P301"
 * @param address địa chỉ của chính nó
 * @param effectiveAddress địa chỉ của nó hoặc của tổ tiên gần nhất có địa chỉ
 */
public record PlaceDetailResponse(long id, Long parentId, long placeTypeId, String path, int depth, String code,
		String name, String description, PlaceStatus status, Map<String, Object> attributes, Long coverFileId,
		Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version, Instant deletedAt,
		List<PlaceRef> ancestors, AddressResponse address, EffectiveAddressResponse effectiveAddress) {
}
