package com.home_platform.registry.capacity;

import java.time.LocalDate;
import java.util.List;

import com.home_platform.common.jdbc.RepositorySupport;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Các câu đọc về sức chứa và chỗ trống. Chỉ đọc; không ghi gì. */
@Repository
public class CapacityRepository {

	private final JdbcClient jdbc;

	CapacityRepository(RepositorySupport support) {
		this.jdbc = support.jdbc();
	}

	/**
	 * Các lần ở chưa xoá, của người chưa xoá, tại một địa điểm, giao với khoảng {@code from} tới
	 * {@code to}. Index {@code (workspace_id, place_id, end_date, start_date)}.
	 * @param to {@code null} là không có ngày cuối
	 * @param excludeId dòng cư trú bỏ qua (dòng đang sửa); {@code null} là không bỏ dòng nào
	 */
	List<Stay> stays(long workspaceId, long placeId, LocalDate from, LocalDate to, Long excludeId) {
		return this.jdbc.sql("SELECT o.person_id, o.start_date, o.end_date FROM reg_occupancy o "
				+ "JOIN reg_person pe ON pe.workspace_id = o.workspace_id AND pe.id = o.person_id "
				+ "WHERE o.workspace_id = :workspaceId AND o.place_id = :placeId AND o.deleted_at IS NULL "
				+ "AND pe.deleted_at IS NULL AND (o.end_date IS NULL OR o.end_date >= :from) "
				+ "AND (:to IS NULL OR o.start_date <= :to) AND (:excludeId IS NULL OR o.id <> :excludeId)")
			.param("workspaceId", workspaceId)
			.param("placeId", placeId)
			.param("from", from)
			.param("to", to)
			.param("excludeId", excludeId)
			.query((rs, rowNum) -> new Stay(rs.getLong("person_id"), rs.getObject("start_date", LocalDate.class),
					rs.getObject("end_date", LocalDate.class)))
			.list();
	}

}
