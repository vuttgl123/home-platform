package com.home_platform.common.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Các câu SQL cố định của một bảng có khoá chính {@code (workspace_id, id)}, dựng một lần từ
 * cấu trúc của record. Tên tham số là tên thành phần của record.
 */
final class CrudSql {

	static final String WORKSPACE_ID = "workspaceId";

	static final String ID = "id";

	static final String CREATED_AT = "createdAt";

	static final String CREATED_BY = "createdBy";

	static final String UPDATED_AT = "updatedAt";

	static final String UPDATED_BY = "updatedBy";

	static final String VERSION = "version";

	static final String DELETED_AT = "deletedAt";

	private static final Set<String> NOT_BUSINESS = Set.of(WORKSPACE_ID, ID, CREATED_AT, CREATED_BY, UPDATED_AT,
			UPDATED_BY, VERSION, DELETED_AT);

	private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+");

	private static final String BY_KEY = " WHERE workspace_id = :workspaceId AND id = :id";

	private final String table;

	private final EntityMetadata<?> metadata;

	private final boolean versioned;

	private final boolean softDeletable;

	CrudSql(String table, EntityMetadata<?> metadata) {
		if (!IDENTIFIER.matcher(table).matches()) {
			throw new IllegalArgumentException("Not a table name: " + table);
		}
		this.table = table;
		this.metadata = metadata;
		this.versioned = metadata.has(VERSION);
		this.softDeletable = metadata.has(DELETED_AT);
	}

	boolean versioned() {
		return this.versioned;
	}

	boolean softDeletable() {
		return this.softDeletable;
	}

	String insert() {
		String parameters = this.metadata.components().stream().map(c -> ":" + c).collect(Collectors.joining(", "));
		return "INSERT INTO " + this.table + " (" + this.metadata.selectList() + ") VALUES (" + parameters + ")";
	}

	String selectById() {
		return selectByIdIncludingDeleted() + (this.softDeletable ? " AND deleted_at IS NULL" : "");
	}

	String selectByIdIncludingDeleted() {
		return "SELECT " + this.metadata.selectList() + " FROM " + this.table + BY_KEY;
	}

	String update() {
		List<String> assignments = new ArrayList<>();
		for (String component : this.metadata.components()) {
			if (!NOT_BUSINESS.contains(component)) {
				assignments.add(this.metadata.column(component) + " = :" + component);
			}
		}
		assignments.addAll(auditAssignments());
		return "UPDATE " + this.table + " SET " + String.join(", ", assignments) + BY_KEY + versionCheck()
				+ (this.softDeletable ? " AND deleted_at IS NULL" : "");
	}

	String softDelete() {
		requireSoftDelete();
		List<String> assignments = new ArrayList<>(List.of("deleted_at = :deletedAt"));
		assignments.addAll(auditAssignments());
		return "UPDATE " + this.table + " SET " + String.join(", ", assignments) + BY_KEY + versionCheck()
				+ " AND deleted_at IS NULL";
	}

	String restore() {
		requireSoftDelete();
		List<String> assignments = new ArrayList<>(List.of("deleted_at = NULL"));
		assignments.addAll(auditAssignments());
		return "UPDATE " + this.table + " SET " + String.join(", ", assignments) + BY_KEY + versionCheck()
				+ " AND deleted_at IS NOT NULL";
	}

	String deletePermanently() {
		if (this.softDeletable) {
			throw new UnsupportedOperationException(
					this.table + " has a deleted_at column: use softDelete instead of deletePermanently");
		}
		return "DELETE FROM " + this.table + BY_KEY;
	}

	/** Phần của mệnh đề SET mà mọi lệnh ghi đều có: người sửa, lúc sửa, tăng version. */
	private List<String> auditAssignments() {
		List<String> assignments = new ArrayList<>();
		if (this.metadata.has(UPDATED_AT)) {
			assignments.add("updated_at = :updatedAt");
		}
		if (this.metadata.has(UPDATED_BY)) {
			assignments.add("updated_by = :updatedBy");
		}
		if (this.versioned) {
			assignments.add("version = version + 1");
		}
		return assignments;
	}

	private String versionCheck() {
		return this.versioned ? " AND version = :version" : "";
	}

	private void requireSoftDelete() {
		if (!this.softDeletable) {
			throw new UnsupportedOperationException(this.table + " has no deleted_at column");
		}
	}

}
