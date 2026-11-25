package com.home_platform.registry.maintenance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.springframework.stereotype.Repository;

@Repository
public class MaintenanceRepository extends WorkspaceRepository<Maintenance> {

	MaintenanceRepository(RepositorySupport support) {
		super(support, "reg_maintenance", Maintenance.class);
	}

	/** Các dòng chưa xoá của một món, mới làm trước. Index (workspace_id, item_id, done_on). */
	public CursorPage<Maintenance> ofItem(long workspaceId, long itemId, CursorRequest page) {
		return keyset().where("item_id = :itemId", Map.of("itemId", itemId)).orderByDesc("done_on").fetch(workspaceId,
				page);
	}

	/** "Tên món: việc" và id món của các dòng (kể cả đã xoá, của món đã xoá) theo {@code id}. */
	public Map<Long, Line> lines(long workspaceId, List<Long> ids) {
		Map<Long, Line> lines = new HashMap<>();
		if (ids.isEmpty()) {
			return lines;
		}
		jdbc().sql("SELECT m.id, m.item_id, m.work, i.name FROM reg_maintenance m "
				+ "JOIN reg_item i ON i.workspace_id = m.workspace_id AND i.id = m.item_id "
				+ "WHERE m.workspace_id = :workspaceId AND m.id IN (:ids)")
			.param("workspaceId", workspaceId)
			.param("ids", ids)
			.query((rs, rowNum) -> lines.put(rs.getLong("id"),
					new Line(rs.getLong("item_id"), rs.getString("name") + ": " + rs.getString("work"))))
			.list();
		return lines;
	}

	/** Một dòng bảo trì như Hoạt động cần: món của nó và tên để hiện. */
	public record Line(long itemId, String name) {
	}

}
