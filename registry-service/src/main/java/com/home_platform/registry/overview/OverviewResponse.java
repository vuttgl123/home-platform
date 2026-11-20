package com.home_platform.registry.overview;

import java.util.List;
import java.util.Map;

import com.home_platform.registry.item.ItemStatus;

/**
 * Các con số của trang tổng quan. Mọi số chỉ tính bản ghi chưa xoá.
 * @param places tổng số địa điểm
 * @param placeTypes mỗi loại có ít nhất một địa điểm, theo thứ tự hiển thị của loại
 * @param people tổng số hồ sơ người
 * @param peopleLiving số người khác nhau đang ở đâu đó hôm nay
 * @param itemsByStatus số món theo trạng thái; đủ mọi trạng thái, không có món nào thì 0
 * @param missingCodes bản ghi chưa có mã, đúng con số của {@code GET /codes/status}
 * @param activityByDay số thay đổi theo ngày của 14 ngày gần nhất (tính cả hôm nay), cũ trước; ngày không có gì
 *   vẫn có dòng với các số 0
 */
public record OverviewResponse(int places, List<PlaceTypeCount> placeTypes, int people, int peopleLiving,
		Map<ItemStatus, Integer> itemsByStatus, MissingCodes missingCodes, List<DayActivity> activityByDay) {

	public record MissingCodes(int places, int items) {
	}

}
