package com.home_platform.registry.place;

import java.time.Instant;
import java.util.List;

import com.home_platform.common.history.ChangeAction;

/** Một version kèm nội dung; nội dung đã đổi về {@link PlaceSnapshot} để số {@code long} ra chuỗi như mọi API. */
public record PlaceVersionResponse(int versionNo, ChangeAction action, PlaceSnapshot snapshot,
		List<String> changedFields, long changedBy, Instant changedAt, String note) {
}
