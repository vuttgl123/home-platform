package com.home_platform.registry.imports;

import java.util.List;

import com.home_platform.common.jdbc.RepositorySupport;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Các câu đọc của việc nhập: hồ sơ đã có mà một dòng của file có thể nhắc tới, hoặc trùng với. Chỉ
 * bản ghi chưa xoá, chỉ vài cột, đọc một lần cho cả file.
 */
@Repository
class ImportLookupRepository {

	private final JdbcClient jdbc;

	ImportLookupRepository(RepositorySupport support) {
		this.jdbc = support.jdbc();
	}

	List<KnownPlace> places(long workspaceId) {
		return this.jdbc
			.sql("SELECT id, parent_id, code, name FROM reg_place WHERE workspace_id = :workspaceId AND deleted_at IS NULL")
			.param("workspaceId", workspaceId)
			.query((rs, rowNum) -> new KnownPlace(rs.getLong("id"), (Long) rs.getObject("parent_id", Long.class),
					rs.getString("code"), rs.getString("name")))
			.list();
	}

	List<KnownPerson> people(long workspaceId) {
		return this.jdbc
			.sql("SELECT id, full_name, phone, id_number FROM reg_person WHERE workspace_id = :workspaceId "
					+ "AND deleted_at IS NULL")
			.param("workspaceId", workspaceId)
			.query((rs, rowNum) -> new KnownPerson(rs.getLong("id"), rs.getString("full_name"), rs.getString("phone"),
					rs.getString("id_number")))
			.list();
	}

	/** Món chưa xoá, chưa gỡ. */
	List<KnownItem> items(long workspaceId) {
		return this.jdbc
			.sql("SELECT place_id, name FROM reg_item WHERE workspace_id = :workspaceId AND deleted_at IS NULL "
					+ "AND status <> 'REMOVED'")
			.param("workspaceId", workspaceId)
			.query((rs, rowNum) -> new KnownItem(rs.getLong("place_id"), rs.getString("name")))
			.list();
	}

	record KnownPlace(long id, Long parentId, String code, String name) {
	}

	record KnownPerson(long id, String fullName, String phone, String idNumber) {
	}

	record KnownItem(long placeId, String name) {
	}

}
