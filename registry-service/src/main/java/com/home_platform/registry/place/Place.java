package com.home_platform.registry.place;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.home_platform.common.jdbc.JsonColumn;
import lombok.With;

/**
 * Một dòng của {@code reg_place}. Địa điểm lồng trong địa điểm: {@code path} là chuỗi
 * {@code id} từ gốc tới chính nó, ví dụ {@code /12/345/6789/}; {@code depth} 0 là cấp cao nhất.
 */
@With
public record Place(long workspaceId, long id, Long parentId, long placeTypeId, String path, int depth, String code,
		String name, String description, PlaceStatus status, @JsonColumn Map<String, Object> attributes,
		Long coverFileId, Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version,
		Instant deletedAt) {

	/** Cấp sâu nhất: 20 cấp, {@code depth} từ 0 đến 19. */
	public static final int MAX_DEPTH = 19;

	/** Các {@code id} trong {@code path}, từ gốc tới chính nó. */
	public List<Long> pathIds() {
		return Arrays.stream(this.path.split("/")).filter(part -> !part.isEmpty()).map(Long::valueOf).toList();
	}

}
