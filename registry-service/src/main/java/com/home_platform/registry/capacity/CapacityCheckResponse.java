package com.home_platform.registry.capacity;

import java.time.LocalDate;

/**
 * Kết quả kiểm tra sức chứa cho một khoảng ngày (spec Đợt 6, L3). Vượt khi {@code capacity} có giá
 * trị và {@code peak + 1 > capacity}; frontend chỉ cảnh báo, không chặn.
 *
 * @param capacity sức chứa của địa điểm, {@code null} khi chưa đặt
 * @param peak số người nhiều nhất đã có mặt cùng một ngày trong khoảng, không tính dòng đang sửa
 * @param peakDate ngày đầu tiên đạt {@code peak}; {@code null} khi {@code peak} là 0
 */
public record CapacityCheckResponse(Integer capacity, int peak, LocalDate peakDate) {
}
