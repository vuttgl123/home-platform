package com.home_platform.registry.support;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ngày "hôm nay" của nghiệp vụ, dùng để biết ai đang ở, món nào còn. Thời điểm lưu trong
 * database là giờ UTC, còn ngày thì tính theo múi giờ {@code registry.business-zone}.
 */
@Component
public class BusinessDate {

	private final Clock clock;

	private final ZoneId zone;

	public BusinessDate(Clock clock, @Value("${registry.business-zone:Asia/Ho_Chi_Minh}") ZoneId zone) {
		this.clock = clock;
		this.zone = zone;
	}

	public LocalDate today() {
		return LocalDate.ofInstant(this.clock.instant(), this.zone);
	}

}
