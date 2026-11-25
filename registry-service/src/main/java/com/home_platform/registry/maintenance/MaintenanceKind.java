package com.home_platform.registry.maintenance;

/** Loại một dòng nhật ký: chỉ bảo trì định kỳ làm lùi hạn bảo trì kế tiếp của món (spec Đợt 8, L3). */
public enum MaintenanceKind {

	/** Sửa chữa: một lần hỏng được sửa. */
	REPAIR,

	/** Bảo trì định kỳ: vệ sinh, thay lọc, kiểm tra theo chu kỳ. */
	SERVICE

}
