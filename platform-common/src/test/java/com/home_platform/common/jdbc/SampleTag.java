package com.home_platform.common.jdbc;

import java.time.Instant;

import lombok.With;

/** Bảng mẫu chỉ có {@code created_at} và {@code updated_at}. */
@With
record SampleTag(long workspaceId, long id, String code, String name, Instant createdAt, Instant updatedAt) {

	static SampleTag of(String code, String name) {
		return new SampleTag(0, 0, code, name, null, null);
	}

}
