package com.home_platform.registry.occupancy;

import java.time.LocalDate;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import org.springframework.stereotype.Repository;

@Repository
public class OccupancyRepository extends WorkspaceRepository<Occupancy> {

	/** Thay cho ngày kết thúc trống khi so khoảng thời gian. */
	private static final LocalDate OPEN_END = LocalDate.of(9999, 12, 31);

	OccupancyRepository(RepositorySupport support) {
		super(support, "reg_occupancy", Occupancy.class);
	}

	/**
	 * Người này đã có dòng chưa xoá nào ở địa điểm này mà khoảng thời gian giao với
	 * {@code [start, end]} chưa (ngày cuối trống là vô hạn), không kể dòng {@code excludeId}.
	 * Index (workspace_id, person_id, start_date).
	 */
	public boolean overlaps(long workspaceId, long personId, long placeId, LocalDate start, LocalDate end,
			long excludeId) {
		return jdbc().sql("SELECT EXISTS (SELECT 1 FROM reg_occupancy WHERE workspace_id = :workspaceId "
				+ "AND person_id = :personId AND place_id = :placeId AND deleted_at IS NULL AND id <> :excludeId "
				+ "AND start_date <= :end AND (end_date IS NULL OR end_date >= :start))")
			.param("workspaceId", workspaceId)
			.param("personId", personId)
			.param("placeId", placeId)
			.param("excludeId", excludeId)
			.param("start", start)
			.param("end", (end != null) ? end : OPEN_END)
			.query(Integer.class)
			.single() == 1;
	}

}
