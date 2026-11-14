package com.home_platform.common.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Các câu SQL cố định của một bảng có khoá chính {@code (workspace_id, id)}, dựng một lần trong
 * hàm khởi tạo từ cấu trúc của record. Tên tham số là tên thành phần của record.
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

	private final boolean versioned;

	private final boolean softDeletable;

	private final String insert;

	private final String selectById;

	private final String selectByIdIncludingDeleted;

	private final String selectByIdForShare;

	private final String selectByIdForUpdate;

	private final String update;

	/** {@code null} khi bảng không có cột {@code deleted_at}. */
	private final String softDelete;

	/** {@code null} khi bảng không có cột {@code deleted_at}. */
	private final String restore;

	private final String deletePermanently;

	CrudSql(String table, EntityMetadata<?> metadata) {
		if (!IDENTIFIER.matcher(table).matches()) {
			throw new IllegalArgumentException("Not a table name: " + table);
		}
		this.table = table;
		this.versioned = metadata.has(VERSION);
		this.softDeletable = metadata.has(DELETED_AT);
		String versionCheck = this.versioned ? " AND version = :version" : "";
		List<String> audit = auditAssignments(metadata, this.versioned);
		this.insert = metadata.insertInto(table);
		this.selectByIdIncludingDeleted = "SELECT " + metadata.selectList() + " FROM " + table + BY_KEY;
		this.selectById = this.selectByIdIncludingDeleted + (this.softDeletable ? " AND deleted_at IS NULL" : "");
		this.selectByIdForShare = this.selectById + " FOR SHARE";
		this.selectByIdForUpdate = this.selectById + " FOR UPDATE";
		List<String> assignments = new ArrayList<>();
		for (String component : metadata.components()) {
			if (!NOT_BUSINESS.contains(component)) {
				assignments.add(metadata.column(component) + " = :" + component);
			}
		}
		assignments.addAll(audit);
		this.update = "UPDATE " + table + " SET " + String.join(", ", assignments) + BY_KEY + versionCheck
				+ (this.softDeletable ? " AND deleted_at IS NULL" : "");
		this.softDelete = this.softDeletable ? "UPDATE " + table + " SET "
				+ String.join(", ", prepend("deleted_at = :deletedAt", audit)) + BY_KEY + versionCheck
				+ " AND deleted_at IS NULL" : null;
		this.restore = this.softDeletable ? "UPDATE " + table + " SET "
				+ String.join(", ", prepend("deleted_at = NULL", audit)) + BY_KEY + versionCheck
				+ " AND deleted_at IS NOT NULL" : null;
		this.deletePermanently = "DELETE FROM " + table + BY_KEY;
	}

	boolean versioned() {
		return this.versioned;
	}

	boolean softDeletable() {
		return this.softDeletable;
	}

	String insert() {
		return this.insert;
	}

	String selectById() {
		return this.selectById;
	}

	/** Như {@link #selectById()}, giữ dòng tới hết transaction: lệnh khác không sửa, xoá được nó. */
	String selectByIdForShare() {
		return this.selectByIdForShare;
	}

	/** Như {@link #selectById()}, giữ dòng tới hết transaction: lệnh khác không sửa, xoá, giữ được nó. */
	String selectByIdForUpdate() {
		return this.selectByIdForUpdate;
	}

	String selectByIdIncludingDeleted() {
		return this.selectByIdIncludingDeleted;
	}

	String update() {
		return this.update;
	}

	String softDelete() {
		requireSoftDelete();
		return this.softDelete;
	}

	String restore() {
		requireSoftDelete();
		return this.restore;
	}

	String deletePermanently() {
		if (this.softDeletable) {
			throw new UnsupportedOperationException(
					this.table + " has a deleted_at column: use softDelete instead of deletePermanently");
		}
		return this.deletePermanently;
	}

	/** Phần của mệnh đề SET mà mọi lệnh ghi đều có: người sửa, lúc sửa, tăng version. */
	private static List<String> auditAssignments(EntityMetadata<?> metadata, boolean versioned) {
		List<String> assignments = new ArrayList<>();
		if (metadata.has(UPDATED_AT)) {
			assignments.add("updated_at = :updatedAt");
		}
		if (metadata.has(UPDATED_BY)) {
			assignments.add("updated_by = :updatedBy");
		}
		if (versioned) {
			assignments.add("version = version + 1");
		}
		return assignments;
	}

	private static List<String> prepend(String first, List<String> rest) {
		List<String> all = new ArrayList<>(rest.size() + 1);
		all.add(first);
		all.addAll(rest);
		return all;
	}

	private void requireSoftDelete() {
		if (!this.softDeletable) {
			throw new UnsupportedOperationException(this.table + " has no deleted_at column");
		}
	}

}
