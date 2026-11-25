package com.home_platform.media.attachment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.jdbc.RepositorySupport;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Dòng {@code med_attachment}, SQL viết tay vì cột chuẩn của bảng mang tên khác ({@code added_*},
 * {@code removed_*}). "Đang hiện" là {@code removed_at IS NULL}. Danh sách ảnh của một bản ghi đi
 * theo index (workspace_id, owner_type, owner_id, removed_at, sort_order).
 */
@Repository
public class AttachmentRepository {

	private static final String COLUMNS = "workspace_id, id, owner_type, owner_id, file_id, caption, sort_order, "
			+ "added_by, added_at, removed_by, removed_at";

	private static final String OF_OWNER = " FROM med_attachment WHERE workspace_id = :workspaceId "
			+ "AND owner_type = :ownerType AND owner_id = :ownerId AND removed_at IS NULL";

	private final RepositorySupport support;

	private final JdbcClient jdbc;

	AttachmentRepository(RepositorySupport support) {
		this.support = support;
		this.jdbc = support.jdbc();
	}

	/** Gắn một file vào một bản ghi; {@code id} sinh mới, {@code added_*} lấy từ {@code ctx} và giờ hiện tại. */
	public Attachment insert(WorkspaceContext ctx, OwnerType ownerType, long ownerId, long fileId, String caption,
			int sortOrder) {
		Instant now = Instant.now(this.support.clock()).truncatedTo(ChronoUnit.MILLIS);
		Attachment attachment = new Attachment(ctx.workspaceId(), this.support.idGenerator().nextId(), ownerType,
				ownerId, fileId, caption, sortOrder, ctx.userId(), now, null, null);
		this.jdbc.sql("INSERT INTO med_attachment (" + COLUMNS + ") VALUES (:workspaceId, :id, :ownerType, "
				+ ":ownerId, :fileId, :caption, :sortOrder, :addedBy, :addedAt, NULL, NULL)")
			.param("workspaceId", attachment.workspaceId())
			.param("id", attachment.id())
			.param("ownerType", ownerType.name())
			.param("ownerId", ownerId)
			.param("fileId", fileId)
			.param("caption", caption)
			.param("sortOrder", sortOrder)
			.param("addedBy", attachment.addedBy())
			.param("addedAt", LocalDateTime.ofInstant(now, ZoneOffset.UTC))
			.update();
		return attachment;
	}

	/** Ảnh đang hiện của một bản ghi, theo {@code sort_order} rồi {@code id}. */
	public List<Attachment> visible(long workspaceId, OwnerType ownerType, long ownerId) {
		return ofOwner("SELECT " + COLUMNS + OF_OWNER + " ORDER BY sort_order, id", workspaceId, ownerType, ownerId)
			.query(AttachmentRepository::map)
			.list();
	}

	/** Như {@link #visible} nhưng khoá các dòng tới hết transaction, để hai lệnh thêm hay sắp xếp không chen nhau. */
	public List<Attachment> lockVisible(long workspaceId, OwnerType ownerType, long ownerId) {
		return ofOwner("SELECT " + COLUMNS + OF_OWNER + " ORDER BY sort_order, id FOR UPDATE", workspaceId, ownerType,
				ownerId)
			.query(AttachmentRepository::map)
			.list();
	}

	public int countVisible(long workspaceId, OwnerType ownerType, long ownerId) {
		return ofOwner("SELECT COUNT(*)" + OF_OWNER, workspaceId, ownerType, ownerId).query(Integer.class).single();
	}

	/** Ảnh đang hiện có {@code id} này, khoá tới hết transaction. */
	public Optional<Attachment> findVisibleForUpdate(long workspaceId, long id) {
		return this.jdbc.sql("SELECT " + COLUMNS + " FROM med_attachment WHERE workspace_id = :workspaceId "
				+ "AND id = :id AND removed_at IS NULL FOR UPDATE")
			.param("workspaceId", workspaceId)
			.param("id", id)
			.query(AttachmentRepository::map)
			.optional();
	}

	public void updateCaption(long workspaceId, long id, String caption) {
		this.jdbc.sql("UPDATE med_attachment SET caption = :caption WHERE workspace_id = :workspaceId AND id = :id")
			.param("workspaceId", workspaceId)
			.param("id", id)
			.param("caption", caption)
			.update();
	}

	public void updateSortOrder(long workspaceId, long id, int sortOrder) {
		this.jdbc.sql("UPDATE med_attachment SET sort_order = :sortOrder WHERE workspace_id = :workspaceId AND id = :id")
			.param("workspaceId", workspaceId)
			.param("id", id)
			.param("sortOrder", sortOrder)
			.update();
	}

	/** Gỡ: ghi người gỡ và lúc gỡ; dòng không bị xoá. */
	public void markRemoved(WorkspaceContext ctx, long id) {
		this.jdbc.sql("UPDATE med_attachment SET removed_by = :userId, removed_at = :now "
				+ "WHERE workspace_id = :workspaceId AND id = :id AND removed_at IS NULL")
			.param("workspaceId", ctx.workspaceId())
			.param("id", id)
			.param("userId", ctx.userId())
			.param("now", LocalDateTime.ofInstant(Instant.now(this.support.clock()).truncatedTo(ChronoUnit.MILLIS), ZoneOffset.UTC))
			.update();
	}

	private JdbcClient.StatementSpec ofOwner(String sql, long workspaceId, OwnerType ownerType, long ownerId) {
		return this.jdbc.sql(sql)
			.param("workspaceId", workspaceId)
			.param("ownerType", ownerType.name())
			.param("ownerId", ownerId);
	}

	private static Attachment map(ResultSet rs, int rowNum) throws SQLException {
		long removedBy = rs.getLong("removed_by");
		Long removedByOrNull = rs.wasNull() ? null : removedBy;
		return new Attachment(rs.getLong("workspace_id"), rs.getLong("id"), OwnerType.valueOf(rs.getString("owner_type")),
				rs.getLong("owner_id"), rs.getLong("file_id"), rs.getString("caption"), rs.getInt("sort_order"),
				rs.getLong("added_by"), instant(rs, "added_at"), removedByOrNull, instant(rs, "removed_at"));
	}

	private static Instant instant(ResultSet rs, String column) throws SQLException {
		LocalDateTime value = rs.getObject(column, LocalDateTime.class);
		return (value != null) ? value.toInstant(ZoneOffset.UTC) : null;
	}

}
