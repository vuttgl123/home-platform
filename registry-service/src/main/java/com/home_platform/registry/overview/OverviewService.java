package com.home_platform.registry.overview;

import java.time.LocalDate;

import com.home_platform.registry.code.CodeBackfillService;
import com.home_platform.registry.code.CodeStatusResponse;
import com.home_platform.registry.support.BusinessDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Trang tổng quan của một không gian: mọi con số trong một lần gọi, không cache. */
@Service
public class OverviewService {

	private final OverviewRepository repository;

	private final CodeBackfillService codes;

	private final BusinessDate businessDate;

	OverviewService(OverviewRepository repository, CodeBackfillService codes, BusinessDate businessDate) {
		this.repository = repository;
		this.codes = codes;
		this.businessDate = businessDate;
	}

	/** Số ngày của biểu đồ hoạt động, tính cả hôm nay. */
	static final int ACTIVITY_DAYS = 14;

	/** Mọi câu đếm đọc trong một transaction nên các con số khớp nhau. */
	@Transactional(readOnly = true)
	public OverviewResponse get(long workspaceId) {
		LocalDate today = this.businessDate.today();
		CodeStatusResponse missing = this.codes.status(workspaceId);
		return new OverviewResponse(this.repository.countPlaces(workspaceId),
				this.repository.placeTypeCounts(workspaceId, today), this.repository.countPeople(workspaceId),
				this.repository.countPeopleLiving(workspaceId, today),
				this.repository.countItemsByStatus(workspaceId),
				new OverviewResponse.MissingCodes(missing.placesWithoutCode(), missing.itemsWithoutCode()),
				this.repository.activityByDay(workspaceId, today.minusDays(ACTIVITY_DAYS - 1), today, this.businessDate.zone()));
	}

}
