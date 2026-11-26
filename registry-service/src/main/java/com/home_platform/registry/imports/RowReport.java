package com.home_platform.registry.imports;

import java.util.List;

/**
 * Kết quả của một dòng.
 *
 * @param row số dòng trong Excel
 * @param label hồ sơ của dòng: "mã · tên" với địa điểm và đồ đạc, họ tên với người, "người · địa điểm"
 *     với cư trú và sở hữu
 * @param code mã của địa điểm, món đồ: mã giữ từ file, hoặc mã được cấp; {@code null} với sheet khác
 */
public record RowReport(int row, String label, String code, RowStatus status, List<RowMessage> messages) {
}
