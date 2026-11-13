package com.home_platform.registry.history;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.common.history.ChangeAction;
import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.json.StorageJson;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorCodec;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Hoạt động gần đây của một không gian, đọc thẳng từ {@code reg_entity_version}.
 *
 * <p>Sắp theo {@code changed_at} rồi khoá chính, giảm dần. Index (workspace_id, changed_at)
 * của InnoDB mang sẵn các cột khoá chính ở cuối, nên cả bốn cột sắp xếp đều đi theo index và
 * nhiều version cùng mili giây vẫn có thứ tự chắc chắn.
 */
@Repository
public class ActivityRepository {

	private final JdbcClient jdbc;

	private final StorageJson json;

	ActivityRepository(RepositorySupport support) {
		this.jdbc = support.jdbc();
		this.json = support.json();
	}

	/**
	 * @param entityType {@code null} là mọi loại bản ghi
	 * @throws BadRequestException nếu cursor hỏng
	 */
	public CursorPage<ActivityEntry> list(long workspaceId, String entityType, CursorRequest page) {
		StringBuilder sql = new StringBuilder("SELECT entity_type, entity_id, version_no, action, changed_fields, "
				+ "changed_by, changed_at, note FROM reg_entity_version WHERE workspace_id = :workspaceId");
		Map<String, Object> params = new HashMap<>();
		params.put("workspaceId", workspaceId);
		if (entityType != null) {
			sql.append(" AND entity_type = :entityType");
			params.put("entityType", entityType);
		}
		if (page.cursor() != null) {
			params.putAll(decode(page.cursor()));
			sql.append(" AND (changed_at < :t OR (changed_at = :t AND (entity_type < :e OR (entity_type = :e AND "
					+ "(entity_id < :i OR (entity_id = :i AND version_no < :v))))))");
		}
		sql.append(" ORDER BY changed_at DESC, entity_type DESC, entity_id DESC, version_no DESC LIMIT :limit");
		params.put("limit", page.limit() + 1);
		List<ActivityEntry> rows = this.jdbc.sql(sql.toString())
			.params(params)
			.query((rs, rowNum) -> new ActivityEntry(rs.getString("entity_type"), rs.getLong("entity_id"),
					rs.getInt("version_no"), ChangeAction.valueOf(rs.getString("action")),
					changedFields(rs.getString("changed_fields")), rs.getLong("changed_by"),
					rs.getObject("changed_at", LocalDateTime.class).toInstant(ZoneOffset.UTC), rs.getString("note")))
			.list();
		if (rows.size() <= page.limit()) {
			return new CursorPage<>(rows, null);
		}
		List<ActivityEntry> items = List.copyOf(rows.subList(0, page.limit()));
		ActivityEntry last = items.getLast();
		return new CursorPage<>(items, CursorCodec.encode(List.of(last.changedAt().toString(), last.entityType(),
				String.valueOf(last.entityId()), String.valueOf(last.versionNo()))));
	}

	private List<String> changedFields(String value) {
		if (value == null) {
			return null;
		}
		List<String> fields = new ArrayList<>();
		this.json.readTree(value).forEach(node -> fields.add(node.asString()));
		return List.copyOf(fields);
	}

	private static Map<String, Object> decode(String cursor) {
		List<String> parts = CursorCodec.decode(cursor);
		try {
			if (parts.size() != 4) {
				throw new IllegalArgumentException();
			}
			return Map.of("t", LocalDateTime.ofInstant(Instant.parse(parts.get(0)), ZoneOffset.UTC), "e",
					parts.get(1), "i", Long.parseLong(parts.get(2)), "v", Integer.parseInt(parts.get(3)));
		}
		catch (RuntimeException ex) {
			throw new BadRequestException("Invalid cursor");
		}
	}

}
