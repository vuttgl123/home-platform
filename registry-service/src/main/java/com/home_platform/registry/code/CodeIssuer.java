package com.home_platform.registry.code;

import java.util.function.Predicate;

import com.home_platform.common.web.ConflictException;
import com.home_platform.registry.support.BusinessDate;
import org.springframework.stereotype.Service;

/** Cấp mã kế tiếp theo mẫu đang áp dụng cho một loại địa điểm hay nhóm đồ. */
@Service
public class CodeIssuer {

	static final int MAX_ATTEMPTS = 1000;

	/** Ghi chú version khi một bản ghi chưa có mã nhận mã đầu tiên. */
	public static final String CODE_ASSIGNED = "Code assigned";

	private final CodeRuleService rules;

	private final CodeCounterRepository counters;

	private final BusinessDate businessDate;

	CodeIssuer(CodeRuleService rules, CodeCounterRepository counters, BusinessDate businessDate) {
		this.rules = rules;
		this.counters = counters;
		this.businessDate = businessDate;
	}

	/**
	 * Mã kế tiếp. Phải gọi trong transaction của lệnh sẽ ghi mã đó: lệnh hỏng thì số được trả lại.
	 * @param typeId loại địa điểm hoặc nhóm đồ; 0 khi không có
	 * @param taken mã đã có trên một dòng nào đó (kể cả dòng đã xoá); bộ đếm nhảy qua những mã này
	 * @throws ConflictException nếu {@value #MAX_ATTEMPTS} số liền nhau đều đã có người dùng
	 */
	public String issue(long workspaceId, CodeTarget target, long typeId, Predicate<String> taken) {
		CodeFormat format = CodeFormat.resolve(this.rules.stored(workspaceId), target, typeId);
		int year = this.businessDate.today().getYear();
		String series = format.series(year);
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			String code = format.code(year, this.counters.next(workspaceId, target, series));
			if (!taken.test(code)) {
				return code;
			}
		}
		throw new ConflictException(
				"No free code in series " + series + " after " + MAX_ATTEMPTS + " attempts; change the code rule");
	}

}
