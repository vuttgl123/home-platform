package com.home_platform.registry.code;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.registry.catalog.CatalogCache;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Mẫu mã. Bảng có khoá {@code (workspace_id, target, type_id)}, không có {@code id}, nên SQL viết
 * tay thay vì kế thừa {@code WorkspaceRepository}.
 */
@Repository
public class CodeRuleRepository {

	private final JdbcClient jdbc;

	CodeRuleRepository(RepositorySupport support) {
		this.jdbc = support.jdbc();
	}

	/** Mọi mẫu đã lưu của không gian. Là nguồn nạp cache nên đọc bản đã commit mới nhất. */
	public List<CodeRule> listAll(long workspaceId) {
		return this.jdbc.sql("SELECT workspace_id, target, type_id, prefix, digits, yearly, created_at, updated_at "
				+ "FROM reg_code_rule WHERE workspace_id = :workspaceId ORDER BY target, type_id"
				+ CatalogCache.LATEST_COMMITTED)
			.param("workspaceId", workspaceId)
			.query((rs, rowNum) -> new CodeRule(rs.getLong("workspace_id"), CodeTarget.valueOf(rs.getString("target")),
					rs.getLong("type_id"), rs.getString("prefix"), rs.getInt("digits"), rs.getBoolean("yearly"),
					rs.getObject("created_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
					rs.getObject("updated_at", LocalDateTime.class).toInstant(ZoneOffset.UTC)))
			.list();
	}

	/** Đặt hoặc thay mẫu. */
	public void upsert(long workspaceId, CodeTarget target, long typeId, CodeFormat format) {
		this.jdbc.sql("INSERT INTO reg_code_rule (workspace_id, target, type_id, prefix, digits, yearly) "
				+ "VALUES (:workspaceId, :target, :typeId, :prefix, :digits, :yearly) AS new "
				+ "ON DUPLICATE KEY UPDATE prefix = new.prefix, digits = new.digits, yearly = new.yearly, "
				+ "updated_at = UTC_TIMESTAMP(3)")
			.param("workspaceId", workspaceId)
			.param("target", target.name())
			.param("typeId", typeId)
			.param("prefix", format.prefix())
			.param("digits", format.digits())
			.param("yearly", format.yearly())
			.update();
	}

	/** @return {@code true} nếu có mẫu bị xoá */
	public boolean delete(long workspaceId, CodeTarget target, long typeId) {
		return this.jdbc.sql("DELETE FROM reg_code_rule WHERE workspace_id = :workspaceId AND target = :target "
				+ "AND type_id = :typeId")
			.param("workspaceId", workspaceId)
			.param("target", target.name())
			.param("typeId", typeId)
			.update() > 0;
	}

}
