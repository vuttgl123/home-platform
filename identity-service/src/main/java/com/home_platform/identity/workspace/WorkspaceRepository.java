package com.home_platform.identity.workspace;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.home_platform.common.jdbc.EntityMetadata;
import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorCodec;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Không gian và thành viên. Bảng có khoá {@code id} nên SQL viết tay với {@link EntityMetadata}.
 * Mọi cách đọc đều đi qua dòng thành viên của người gọi: không phải thành viên thì không thấy.
 */
@Repository
public class WorkspaceRepository {

	private final JdbcClient jdbc;

	private final EntityMetadata<Workspace> metadata;

	private final String memberColumns;

	private final RowMapper<MemberWorkspace> memberRowMapper;

	WorkspaceRepository(RepositorySupport support) {
		this.jdbc = support.jdbc();
		this.metadata = EntityMetadata.of(Workspace.class, support.json());
		this.memberColumns = Arrays.stream(this.metadata.selectList().split(", "))
			.map(column -> "w." + column)
			.collect(Collectors.joining(", ")) + ", m.role";
		RowMapper<Workspace> workspaces = this.metadata.rowMapper();
		this.memberRowMapper = (rs, rowNum) -> new MemberWorkspace(workspaces.mapRow(rs, rowNum),
				MemberRole.valueOf(rs.getString("role")));
	}

	public void insert(Workspace workspace) {
		String values = this.metadata.components().stream().map(c -> ":" + c).collect(Collectors.joining(", "));
		this.jdbc.sql("INSERT INTO idn_workspace (" + this.metadata.selectList() + ") VALUES (" + values + ")")
			.params(this.metadata.parameters(workspace))
			.update();
	}

	public void insertMember(long workspaceId, long userId, MemberRole role, Instant joinedAt) {
		this.jdbc.sql("INSERT INTO idn_workspace_member (workspace_id, user_id, role, joined_at) "
				+ "VALUES (:workspaceId, :userId, :role, :joinedAt)")
			.param("workspaceId", workspaceId)
			.param("userId", userId)
			.param("role", role.name())
			.param("joinedAt", joinedAt)
			.update();
	}

	/** Không gian {@code workspaceId} nếu {@code userId} là thành viên của nó. */
	public Optional<MemberWorkspace> findForMember(long userId, long workspaceId) {
		return this.jdbc.sql("SELECT " + this.memberColumns + " FROM idn_workspace_member m "
				+ "JOIN idn_workspace w ON w.id = m.workspace_id WHERE m.user_id = :userId AND m.workspace_id = :workspaceId")
			.param("userId", userId)
			.param("workspaceId", workspaceId)
			.query(this.memberRowMapper)
			.optional();
	}

	/**
	 * Các không gian mà {@code userId} là thành viên, theo tên rồi {@code id}; cursor là
	 * {@code [name, id]}. Đi từ index (user_id, workspace_id) của bảng thành viên; một người chỉ có
	 * ít không gian nên việc sắp theo tên là rẻ.
	 */
	public CursorPage<MemberWorkspace> listForMember(long userId, CursorRequest page) {
		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);
		params.put("limit", page.limit() + 1);
		String after = "";
		if (page.cursor() != null) {
			List<String> parts = CursorCodec.decode(page.cursor());
			try {
				if (parts.size() != 2) {
					throw new IllegalArgumentException();
				}
				params.put("afterName", parts.get(0));
				params.put("afterId", Long.parseLong(parts.get(1)));
			}
			catch (IllegalArgumentException ex) {
				throw new BadRequestException("Invalid cursor");
			}
			after = " AND (w.name > :afterName OR (w.name = :afterName AND w.id > :afterId))";
		}
		List<MemberWorkspace> rows = this.jdbc.sql("SELECT " + this.memberColumns + " FROM idn_workspace_member m "
				+ "JOIN idn_workspace w ON w.id = m.workspace_id WHERE m.user_id = :userId" + after
				+ " ORDER BY w.name, w.id LIMIT :limit")
			.params(params)
			.query(this.memberRowMapper)
			.list();
		if (rows.size() <= page.limit()) {
			return new CursorPage<>(rows, null);
		}
		List<MemberWorkspace> items = List.copyOf(rows.subList(0, page.limit()));
		Workspace last = items.getLast().workspace();
		return new CursorPage<>(items, CursorCodec.encode(List.of(last.name(), String.valueOf(last.id()))));
	}

	/** Ghi lại tên, trạng thái và lúc sửa. */
	public void update(Workspace workspace) {
		this.jdbc.sql("UPDATE idn_workspace SET name = :name, status = :status, updated_at = :updatedAt WHERE id = :id")
			.params(this.metadata.parameters(workspace))
			.update();
	}

}
