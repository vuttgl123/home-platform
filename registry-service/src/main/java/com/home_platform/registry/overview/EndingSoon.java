package com.home_platform.registry.overview;

import java.util.List;

/**
 * Người sắp chuyển đi (spec Đợt 6, L5).
 *
 * @param total tổng số dòng
 * @param rows tối đa {@value OverviewService#ENDING_SOON_ROWS} dòng, ngày kết thúc gần nhất trước
 */
public record EndingSoon(int total, List<EndingSoonRow> rows) {
}
