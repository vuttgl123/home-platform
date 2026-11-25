package com.home_platform.registry.todo;

/** Loại việc cần làm (spec Đợt 8, L5). Thứ tự khai báo là thứ tự sắp khi hai việc cùng ngày. */
public enum TodoKind {

	/** Món đang hỏng, chưa gỡ. */
	BROKEN,

	/** Bảo hành hết trong 30 ngày tới. */
	WARRANTY,

	/** Bảo trì định kỳ tới hạn trong 7 ngày tới, hoặc đã quá hạn. */
	SERVICE,

	/** Cư trú kết thúc trong 30 ngày tới (như "sắp chuyển đi" của trang Tổng quan). */
	MOVE_OUT,

	/** Mục tuỳ biến kiểu ngày có nhắc. */
	CUSTOM_DATE

}
