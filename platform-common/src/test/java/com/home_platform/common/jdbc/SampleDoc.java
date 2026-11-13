package com.home_platform.common.jdbc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import lombok.With;

/** Bảng mẫu có đủ các cột chuẩn và mọi kiểu dữ liệu được hỗ trợ. */
@With
record SampleDoc(long workspaceId, long id, Long parentId, String name, SampleStatus status, boolean pinned,
		Integer rating, BigDecimal price, LocalDate validFrom, @JsonColumn Map<String, Object> attributes,
		Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version, Instant deletedAt) {

	/** Một bản ghi mới chỉ có phần nghiệp vụ; các cột chuẩn để repository điền. */
	static SampleDoc named(String name) {
		return new SampleDoc(0, 0, null, name, SampleStatus.ACTIVE, false, null, null, null, null, null, 0, null, 0, 0,
				null);
	}

}
