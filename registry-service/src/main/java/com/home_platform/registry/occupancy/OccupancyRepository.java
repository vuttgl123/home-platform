package com.home_platform.registry.occupancy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.springframework.stereotype.Repository;

@Repository
public class OccupancyRepository extends WorkspaceRepository<Occupancy> {

	/** Thay cho ngày kết thúc trống khi so khoảng thời gian. */
	private static final LocalDate OPEN_END = LocalDate.of(9999, 12, 31);

	OccupancyRepository(RepositorySupport support) {
		super(support, "reg_occupancy", Occupancy.class);
	}

	private static final String CURRENT = "start_date <= :today AND (end_date IS NULL OR end_date >= :today)";

	/**
	 * Cư trú ở một địa điểm theo trạng thái. Đang ở và sắp đến: theo ngày bắt đầu; đã rời: ngày
	 * kết thúc gần nhất trước. Index (workspace_id, place_id, end_date, start_date).
	 */
	public CursorPage<Occupancy> atPlace(long workspaceId, long placeId, OccupancyStatus status, LocalDate today,
			CursorRequest page) {
		Map<String, Object> params = Map.of("placeId", placeId, "today", today);
		return switch (status) {
			case CURRENT -> keyset().where("place_id = :placeId AND " + CURRENT, params)
				.orderBy("start_date")
				.fetch(workspaceId, page);
			case UPCOMING -> keyset().where("place_id = :placeId AND start_date > :today", params)
				.orderBy("start_date")
				.fetch(workspaceId, page);
			case PAST -> keyset().where("place_id = :placeId AND end_date < :today", params)
				.orderByDesc("end_date")
				.fetch(workspaceId, page);
		};
	}

	/** Người đang ở tại địa điểm có {@code path} này hoặc bất kỳ hậu duệ chưa xoá nào của nó; theo ngày bắt đầu. */
	public CursorPage<Occupancy> currentInSubtree(long workspaceId, String rootPath, LocalDate today,
			CursorRequest page) {
		return keyset()
			.where("place_id IN (SELECT p.id FROM reg_place p WHERE p.workspace_id = :workspaceId "
					+ "AND p.path LIKE :subtreePrefix AND p.deleted_at IS NULL) AND " + CURRENT,
					Map.of("subtreePrefix", rootPath + "%", "today", today))
			.orderBy("start_date")
			.fetch(workspaceId, page);
	}

	/** Mọi dòng chưa xoá của một người, mới nhất trước. Index (workspace_id, person_id, start_date). */
	public CursorPage<Occupancy> ofPerson(long workspaceId, long personId, CursorRequest page) {
		return keyset().where("person_id = :personId", Map.of("personId", personId))
			.orderByDesc("start_date")
			.fetch(workspaceId, page);
	}

	/**
	 * Tối đa {@code limit} lần ở chưa xoá của người chưa xoá tại một địa điểm, mọi trạng thái, ngày bắt
	 * đầu mới nhất trước. Index (workspace_id, place_id, end_date, start_date).
	 */
	public List<Occupancy> latestAtPlace(long workspaceId, long placeId, int limit) {
		return latest("place_id = :ownerId AND EXISTS (SELECT 1 FROM reg_person pe WHERE pe.workspace_id = "
				+ "reg_occupancy.workspace_id AND pe.id = reg_occupancy.person_id AND pe.deleted_at IS NULL)",
				workspaceId, placeId, limit);
	}

	/**
	 * Tối đa {@code limit} lần ở chưa xoá của một người tại địa điểm chưa xoá, mọi trạng thái, ngày bắt
	 * đầu mới nhất trước. Index (workspace_id, person_id, start_date).
	 */
	public List<Occupancy> latestOfPerson(long workspaceId, long personId, int limit) {
		return latest("person_id = :ownerId AND EXISTS (SELECT 1 FROM reg_place p WHERE p.workspace_id = "
				+ "reg_occupancy.workspace_id AND p.id = reg_occupancy.place_id AND p.deleted_at IS NULL)",
				workspaceId, personId, limit);
	}

	private List<Occupancy> latest(String condition, long workspaceId, long ownerId, int limit) {
		return jdbc().sql("SELECT " + columns() + " FROM reg_occupancy WHERE workspace_id = :workspaceId "
				+ "AND deleted_at IS NULL AND " + condition + " ORDER BY start_date DESC, id DESC LIMIT :limit")
			.param("workspaceId", workspaceId)
			.param("ownerId", ownerId)
			.param("limit", limit)
			.query(rowMapper())
			.list();
	}

	/** Những nơi một người đang ở, theo ngày bắt đầu. */
	public List<Occupancy> currentOfPerson(long workspaceId, long personId, LocalDate today) {
		return jdbc().sql("SELECT " + columns() + " FROM reg_occupancy WHERE workspace_id = :workspaceId "
				+ "AND person_id = :personId AND deleted_at IS NULL AND " + CURRENT + " ORDER BY start_date, id")
			.param("workspaceId", workspaceId)
			.param("personId", personId)
			.param("today", today)
			.query(rowMapper())
			.list();
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
