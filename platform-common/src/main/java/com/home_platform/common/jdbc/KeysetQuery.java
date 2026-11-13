package com.home_platform.common.jdbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorCodec;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Bộ dựng một truy vấn danh sách phân trang theo vị trí (keyset) trên một bảng có khoá
 * {@code (workspace_id, id)}. Lấy qua {@link WorkspaceRepository#keyset()}.
 *
 * <p>Truy vấn luôn có điều kiện {@code workspace_id}, và {@code deleted_at IS NULL} nếu bảng
 * có cột đó. {@code id} luôn là cột sắp xếp phụ để thứ tự ổn định khi nhiều dòng có cùng
 * giá trị sắp xếp. Trang sau được lấy bằng điều kiện "đứng sau dòng cuối của trang trước",
 * không dùng {@code OFFSET}, nên trang sau cũng nhanh như trang đầu nếu có index phục vụ.
 *
 * <p>Cột sắp xếp là hằng số trong code, phải là cột {@code NOT NULL}. Không bao giờ truyền
 * tên cột lấy từ client vào {@link #orderBy}.
 *
 * @param <T> record ứng với một dòng của bảng
 */
public final class KeysetQuery<T extends Record> {

	private static final String CURSOR_SORT = "cursorSort";

	private static final String CURSOR_ID = "cursorId";

	private static final String LIMIT = "limit";

	private static final Set<String> RESERVED = Set.of(CrudSql.WORKSPACE_ID, CURSOR_SORT, CURSOR_ID, LIMIT);

	private final JdbcClient jdbc;

	private final String table;

	private final EntityMetadata<T> metadata;

	private final RowMapper<T> rowMapper;

	private final List<String> conditions = new ArrayList<>();

	private final Map<String, Object> params = new HashMap<>();

	/** {@code null} khi chỉ sắp theo {@code id}. */
	private String sortColumn;

	private ColumnType sortType;

	private boolean descending;

	KeysetQuery(JdbcClient jdbc, String table, EntityMetadata<T> metadata, RowMapper<T> rowMapper) {
		this.jdbc = jdbc;
		this.table = table;
		this.metadata = metadata;
		this.rowMapper = rowMapper;
	}

	/**
	 * Thêm một điều kiện, nối với các điều kiện khác bằng {@code AND}.
	 * @param condition đoạn SQL dùng tham số có tên, ví dụ {@code "parent_id = :parentId"}
	 * @param params giá trị của các tham số trong đoạn SQL đó
	 * @throws IllegalArgumentException nếu dùng tên tham số dành riêng ({@code workspaceId},
	 * {@code cursorSort}, {@code cursorId}, {@code limit})
	 */
	public KeysetQuery<T> where(String condition, Map<String, ?> params) {
		for (String name : params.keySet()) {
			if (RESERVED.contains(name)) {
				throw new IllegalArgumentException("Parameter name " + name + " is reserved by KeysetQuery");
			}
		}
		this.conditions.add("(" + condition + ")");
		this.params.putAll(params);
		return this;
	}

	/** Sắp tăng dần theo một cột của bảng, rồi theo {@code id}. */
	public KeysetQuery<T> orderBy(String column) {
		return sort(column, false);
	}

	/** Sắp giảm dần theo một cột của bảng, rồi theo {@code id} giảm dần. */
	public KeysetQuery<T> orderByDesc(String column) {
		return sort(column, true);
	}

	/**
	 * Lấy một trang.
	 * @throws BadRequestException nếu {@code cursor} hỏng hoặc thuộc về một danh sách khác
	 */
	public CursorPage<T> fetch(long workspaceId, CursorRequest page) {
		if (page.limit() < 1) {
			throw new IllegalArgumentException("limit must be positive: " + page.limit());
		}
		boolean afterCursor = page.cursor() != null;
		JdbcClient.StatementSpec statement = this.jdbc.sql(sql(afterCursor))
			.params(this.params)
			.param(CrudSql.WORKSPACE_ID, workspaceId)
			.param(LIMIT, page.limit() + 1);
		if (afterCursor) {
			statement = statement.params(decode(page.cursor()));
		}
		List<T> rows = statement.query(this.rowMapper).list();
		if (rows.size() <= page.limit()) {
			return new CursorPage<>(rows, null);
		}
		List<T> items = rows.subList(0, page.limit());
		return new CursorPage<>(List.copyOf(items), encode(items.get(items.size() - 1)));
	}

	/** Câu SQL sẽ chạy; {@code afterCursor} là có lấy từ sau một vị trí hay không. */
	String sql(boolean afterCursor) {
		StringBuilder sql = new StringBuilder("SELECT ").append(this.metadata.selectList())
			.append(" FROM ")
			.append(this.table)
			.append(" WHERE workspace_id = :workspaceId");
		if (this.metadata.has(CrudSql.DELETED_AT)) {
			sql.append(" AND deleted_at IS NULL");
		}
		for (String condition : this.conditions) {
			sql.append(" AND ").append(condition);
		}
		String compare = this.descending ? "<" : ">";
		String direction = this.descending ? "DESC" : "ASC";
		if (afterCursor) {
			if (this.sortColumn == null) {
				sql.append(" AND id ").append(compare).append(" :cursorId");
			}
			else {
				sql.append(" AND (")
					.append(this.sortColumn).append(' ').append(compare).append(" :cursorSort OR (")
					.append(this.sortColumn).append(" = :cursorSort AND id ").append(compare).append(" :cursorId))");
			}
		}
		sql.append(" ORDER BY ");
		if (this.sortColumn != null) {
			sql.append(this.sortColumn).append(' ').append(direction).append(", ");
		}
		return sql.append("id ").append(direction).append(" LIMIT :limit").toString();
	}

	private KeysetQuery<T> sort(String column, boolean descending) {
		ColumnType type = this.metadata.typeOfColumn(column);
		if (type instanceof ColumnTypes.JsonType) {
			throw new IllegalArgumentException("A JSON column cannot be used for ordering: " + column);
		}
		this.sortColumn = column;
		this.sortType = type;
		this.descending = descending;
		return this;
	}

	/** Cursor là {@code [giá trị sắp xếp, id]}, hoặc {@code [id]} khi chỉ sắp theo {@code id}. */
	private String encode(T last) {
		String id = this.metadata.get(last, CrudSql.ID).toString();
		if (this.sortColumn == null) {
			return CursorCodec.encode(List.of(id));
		}
		Object value = this.metadata.get(last, componentOf(this.sortColumn));
		if (value == null) {
			throw new IllegalStateException(
					"Sort column " + this.table + "." + this.sortColumn + " is NULL; only NOT NULL columns can be sorted on");
		}
		return CursorCodec.encode(List.of(this.sortType.toText(value), id));
	}

	private Map<String, Object> decode(String cursor) {
		List<String> parts = CursorCodec.decode(cursor);
		int expected = (this.sortColumn == null) ? 1 : 2;
		if (parts.size() != expected) {
			throw invalidCursor();
		}
		try {
			Map<String, Object> values = new HashMap<>();
			values.put(CURSOR_ID, Long.valueOf(parts.get(expected - 1)));
			if (this.sortColumn != null) {
				values.put(CURSOR_SORT, this.sortType.toJdbc(this.sortType.fromText(parts.get(0))));
			}
			return values;
		}
		catch (IllegalArgumentException ex) {
			throw invalidCursor();
		}
	}

	private String componentOf(String column) {
		for (String component : this.metadata.components()) {
			if (this.metadata.column(component).equals(column)) {
				return component;
			}
		}
		throw new IllegalArgumentException(this.metadata.type().getSimpleName() + " has no column " + column);
	}

	private static BadRequestException invalidCursor() {
		return new BadRequestException("Invalid cursor");
	}

}
