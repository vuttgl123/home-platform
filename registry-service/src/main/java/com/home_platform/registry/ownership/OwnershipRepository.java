package com.home_platform.registry.ownership;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.springframework.stereotype.Repository;

@Repository
public class OwnershipRepository extends WorkspaceRepository<Ownership> {

	/** Thay cho ngày kết thúc trống khi so khoảng thời gian. */
	private static final LocalDate OPEN_END = LocalDate.of(9999, 12, 31);

	private static final String CURRENT = "start_date <= :today AND (end_date IS NULL OR end_date >= :today)";

	OwnershipRepository(RepositorySupport support) {
		super(support, "reg_ownership", Ownership.class);
	}

	/**
	 * Sở hữu ở một địa điểm theo trạng thái. Đang và sắp: theo ngày bắt đầu; đã thôi: ngày kết thúc
	 * gần nhất trước. Index (workspace_id, place_id, end_date, start_date).
	 */
	public CursorPage<Ownership> atPlace(long workspaceId, long placeId, OwnershipStatus status, LocalDate today,
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

	/** Sở hữu hiện tại ở địa điểm có {@code path} này hoặc bất kỳ hậu duệ chưa xoá nào; theo ngày bắt đầu. */
	public CursorPage<Ownership> currentInSubtree(long workspaceId, String rootPath, LocalDate today,
			CursorRequest page) {
		return keyset()
			.where("place_id IN (SELECT p.id FROM reg_place p WHERE p.workspace_id = :workspaceId "
					+ "AND p.path LIKE :subtreePrefix AND p.deleted_at IS NULL) AND " + CURRENT,
					Map.of("subtreePrefix", rootPath + "%", "today", today))
			.orderBy("start_date")
			.fetch(workspaceId, page);
	}

	/** Mọi dòng chưa xoá của một người, mới nhất trước. Index (workspace_id, person_id, start_date). */
	public CursorPage<Ownership> ofPerson(long workspaceId, long personId, CursorRequest page) {
		return keyset().where("person_id = :personId", Map.of("personId", personId))
			.orderByDesc("start_date")
			.fetch(workspaceId, page);
	}

	/**
	 * Người này đã có dòng chưa xoá nào ở địa điểm này giao với {@code [start, end]} chưa (ngày cuối
	 * trống là vô hạn), không kể dòng {@code excludeId}.
	 */
	public boolean overlaps(long workspaceId, long personId, long placeId, LocalDate start, LocalDate end,
			long excludeId) {
		return jdbc().sql("SELECT EXISTS (SELECT 1 FROM reg_ownership WHERE workspace_id = :workspaceId "
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

	/** Dòng chưa xoá có ghi tỷ lệ của địa điểm, giao với {@code [start, end]}, không kể {@code excludeId}. */
	public List<Ownership> sharedAt(long workspaceId, long placeId, LocalDate start, LocalDate end, long excludeId) {
		return jdbc().sql("SELECT " + columns() + " FROM reg_ownership WHERE workspace_id = :workspaceId "
				+ "AND place_id = :placeId AND deleted_at IS NULL AND share_percent IS NOT NULL AND id <> :excludeId "
				+ "AND start_date <= :end AND (end_date IS NULL OR end_date >= :start)")
			.param("workspaceId", workspaceId)
			.param("placeId", placeId)
			.param("excludeId", excludeId)
			.param("start", start)
			.param("end", (end != null) ? end : OPEN_END)
			.query(rowMapper())
			.list();
	}

}
