package com.home_platform.registry.occupancy;

import java.util.List;

/**
 * Mọi lần ở của một địa điểm hay một người, mọi trạng thái, ngày bắt đầu mới nhất trước.
 *
 * @param truncated còn lần ở cũ hơn chưa trả về (quá {@value OccupancyService#TIMELINE_LIMIT} lần)
 */
public record OccupancyTimelineResponse(List<OccupancyResponse> rows, boolean truncated) {
}
