package com.home_platform.registry.overview;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.home_platform.common.history.ChangeAction;
import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.registry.item.ItemStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Các câu đếm của trang tổng quan. Mỗi câu chỉ tính bản ghi chưa xoá và đi theo một index bắt đầu
 * bằng {@code workspace_id}.
 */
@Repository
public class OverviewRepository {

	/**
	 * Dòng cư trú {@code o} còn hiệu lực hôm nay: chưa xoá, đã bắt đầu, chưa kết thúc (ngày kết thúc
	 * vẫn là ngày còn ở).
	 */
	private static final String CURRENT = "o.deleted_at IS NULL AND o.start_date <= :today "
			+ "AND (o.end_date IS NULL OR o.end_date >= :today)";

	private final JdbcClient jdbc;

	OverviewRepository(RepositorySupport support) {
		this.jdbc = support.jdbc();
	}

	public int countPlaces(long workspaceId) {
		return count("SELECT COUNT(*) FROM reg_place WHERE workspace_id = :workspaceId AND deleted_at IS NULL",
				workspaceId);
	}

	/**
	 * Mỗi loại có ít nhất một địa điểm: tổng số, và số địa điểm có người đang ở. Theo
	 * {@code sort_order} rồi tên của loại.
	 */
	public List<PlaceTypeCount> placeTypeCounts(long workspaceId, LocalDate today) {
		return this.jdbc.sql("SELECT t.id, t.name, COUNT(*) AS total, SUM(EXISTS (SELECT 1 FROM reg_occupancy o "
				+ "JOIN reg_person pe ON pe.workspace_id = o.workspace_id AND pe.id = o.person_id "
				+ "WHERE o.workspace_id = p.workspace_id AND o.place_id = p.id AND pe.deleted_at IS NULL AND "
				+ CURRENT + ")) AS occupied FROM reg_place p "
				+ "JOIN reg_place_type t ON t.workspace_id = p.workspace_id AND t.id = p.place_type_id "
				+ "WHERE p.workspace_id = :workspaceId AND p.deleted_at IS NULL "
				+ "GROUP BY t.id, t.name, t.sort_order ORDER BY t.sort_order, t.name, t.id")
			.param("workspaceId", workspaceId)
			.param("today", today)
			.query((rs, rowNum) -> new PlaceTypeCount(rs.getLong("id"), rs.getString("name"), rs.getInt("total"),
					rs.getInt("occupied")))
			.list();
	}

	public int countPeople(long workspaceId) {
		return count("SELECT COUNT(*) FROM reg_person WHERE workspace_id = :workspaceId AND deleted_at IS NULL",
				workspaceId);
	}

	/** Số người khác nhau đang ở một địa điểm chưa xoá; ở hai chỗ vẫn là một người. */
	public int countPeopleLiving(long workspaceId, LocalDate today) {
		return this.jdbc.sql("SELECT COUNT(DISTINCT o.person_id) FROM reg_occupancy o "
				+ "JOIN reg_person pe ON pe.workspace_id = o.workspace_id AND pe.id = o.person_id "
				+ "JOIN reg_place p ON p.workspace_id = o.workspace_id AND p.id = o.place_id "
				+ "WHERE o.workspace_id = :workspaceId AND pe.deleted_at IS NULL AND p.deleted_at IS NULL AND "
				+ CURRENT)
			.param("workspaceId", workspaceId)
			.param("today", today)
			.query(Integer.class)
			.single();
	}

	/** Số món chưa xoá theo trạng thái; trạng thái không có món nào vẫn có khoá, giá trị 0. */
	public Map<ItemStatus, Integer> countItemsByStatus(long workspaceId) {
		Map<ItemStatus, Integer> counts = new EnumMap<>(ItemStatus.class);
		for (ItemStatus status : ItemStatus.values()) {
			counts.put(status, 0);
		}
		this.jdbc.sql("SELECT status, COUNT(*) AS total FROM reg_item "
				+ "WHERE workspace_id = :workspaceId AND deleted_at IS NULL GROUP BY status")
			.param("workspaceId", workspaceId)
			.query((rs, rowNum) -> counts.put(ItemStatus.valueOf(rs.getString("status")), rs.getInt("total")))
			.list();
		return counts;
	}

	private int count(String sql, long workspaceId) {
		return this.jdbc.sql(sql).param("workspaceId", workspaceId).query(Integer.class).single();
	}


	/**
	 * Số version theo ngày và hành động, từ đầu ngày {@code first} tới hết ngày {@code last} theo
	 * {@code zone}; mỗi ngày một dòng (cả ngày không có gì), cũ trước. Chia ngày ở Java chứ không ở SQL
	 * để đúng với mọi múi giờ; đi theo index {@code (workspace_id, changed_at)}.
	 */
	public List<DayActivity> activityByDay(long workspaceId, LocalDate first, LocalDate last, ZoneId zone) {
		Map<LocalDate, int[]> counts = new TreeMap<>();
		for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
			counts.put(day, new int[4]);
		}
		this.jdbc.sql("SELECT action, changed_at FROM reg_entity_version WHERE workspace_id = :workspaceId "
				+ "AND changed_at >= :from AND changed_at < :to")
			.param("workspaceId", workspaceId)
			.param("from", LocalDateTime.ofInstant(first.atStartOfDay(zone).toInstant(), ZoneOffset.UTC))
			.param("to", LocalDateTime.ofInstant(last.plusDays(1).atStartOfDay(zone).toInstant(), ZoneOffset.UTC))
			.query((ResultSet rs) -> {
				LocalDate day = LocalDate.ofInstant(rs.getObject("changed_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
						zone);
				int[] perAction = counts.get(day);
				if (perAction != null) {
					perAction[switch (ChangeAction.valueOf(rs.getString("action"))) {
						case CREATE -> 0;
						case UPDATE -> 1;
						case DELETE -> 2;
						case RESTORE -> 3;
					}]++;
				}
			});
		return counts.entrySet()
			.stream()
			.map((entry) -> new DayActivity(entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2],
					entry.getValue()[3]))
			.toList();
	}

}
