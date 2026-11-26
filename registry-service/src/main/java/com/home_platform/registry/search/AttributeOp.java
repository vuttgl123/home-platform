package com.home_platform.registry.search;

import java.util.Locale;

/** Phép so của một điều kiện lọc theo mục tuỳ biến (spec Đợt 11, S2). */
public enum AttributeOp {

	/** Chữ chứa. */
	HAS,
	/** Bằng: chữ theo collation, số theo giá trị, đúng lựa chọn, đúng Có/Không. */
	EQ,
	/** Từ (lớn hơn hoặc bằng): số, ngày. */
	GTE,
	/** Đến (nhỏ hơn hoặc bằng): số, ngày. */
	LTE,
	/** Có giá trị. */
	SET,
	/** Trống, kể cả bản ghi mà mục không áp dụng. */
	UNSET;

	/** Mã trên API: tên viết thường. */
	public String code() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** Phép có cần giá trị so không. */
	public boolean needsValue() {
		return this != SET && this != UNSET;
	}

	/** @return {@code null} nếu không phải mã của phép nào */
	public static AttributeOp fromCode(String code) {
		for (AttributeOp op : values()) {
			if (op.code().equals(code)) {
				return op;
			}
		}
		return null;
	}

}
