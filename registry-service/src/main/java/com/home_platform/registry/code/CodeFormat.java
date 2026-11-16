package com.home_platform.registry.code;

import java.util.List;
import java.util.Optional;

/**
 * Dạng mã: tiền tố, số chữ số, có theo năm không. Mã là tiền tố, gạch nối, (năm và gạch nối nếu
 * theo năm), số thứ tự thêm số 0 cho đủ {@code digits}; số dài hơn {@code digits} thì in đủ.
 * {@link #series} là phần đầu của mã tới hết gạch nối cuối: mỗi dãy có một bộ đếm.
 */
public record CodeFormat(String prefix, int digits, boolean yearly) {

	private static final CodeFormat PLACE_DEFAULT = new CodeFormat("DD", 6, false);

	private static final CodeFormat ITEM_DEFAULT = new CodeFormat("TS", 6, false);

	/** Dạng có sẵn khi không gian chưa đặt gì: địa điểm {@code DD}, đồ đạc {@code TS}, 6 chữ số. */
	public static CodeFormat builtIn(CodeTarget target) {
		return (target == CodeTarget.PLACE) ? PLACE_DEFAULT : ITEM_DEFAULT;
	}

	public String series(int year) {
		return this.prefix + "-" + (this.yearly ? year + "-" : "");
	}

	public String code(int year, long number) {
		String digitsText = String.valueOf(number);
		String padded = "0".repeat(Math.max(0, this.digits - digitsText.length())) + digitsText;
		return series(year) + padded;
	}

	/** Mẫu riêng của {@code typeId}; không có thì mẫu chung (type 0); không có nữa thì dạng có sẵn. */
	public static CodeFormat resolve(List<CodeRule> stored, CodeTarget target, long typeId) {
		return find(stored, target, typeId).or(() -> find(stored, target, 0))
			.map(CodeRule::format)
			.orElse(builtIn(target));
	}

	private static Optional<CodeRule> find(List<CodeRule> stored, CodeTarget target, long typeId) {
		return stored.stream().filter(rule -> rule.target() == target && rule.typeId() == typeId).findFirst();
	}

}
