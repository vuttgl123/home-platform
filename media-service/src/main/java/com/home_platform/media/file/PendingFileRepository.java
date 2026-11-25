package com.home_platform.media.file;

import java.time.LocalDateTime;
import java.util.List;

import com.home_platform.common.jdbc.RepositorySupport;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * File tải lên dở của mọi không gian, cho việc dọn dẹp định kỳ. Không kế thừa
 * {@code WorkspaceRepository} vì câu này cố ý chạy trên mọi không gian.
 */
@Repository
public class PendingFileRepository {

	/** Một file PENDING cần dọn. */
	public record Pending(long workspaceId, long id, String storageKey) {
	}

	private final JdbcClient jdbc;

	PendingFileRepository(RepositorySupport support) {
		this.jdbc = support.jdbc();
	}

	/** Tối đa {@code limit} file PENDING tạo trước {@code cutoff} (giờ UTC) và không gắn vào đâu. */
	public List<Pending> olderThan(LocalDateTime cutoff, int limit) {
		return this.jdbc.sql("SELECT f.workspace_id, f.id, f.storage_key FROM med_file f WHERE f.status = 'PENDING' "
				+ "AND f.created_at < :cutoff AND NOT EXISTS (SELECT 1 FROM med_attachment a "
				+ "WHERE a.workspace_id = f.workspace_id AND a.file_id = f.id) ORDER BY f.created_at LIMIT :limit")
			.param("cutoff", cutoff)
			.param("limit", limit)
			.query((rs, rowNum) -> new Pending(rs.getLong("workspace_id"), rs.getLong("id"), rs.getString("storage_key")))
			.list();
	}

	/** Xoá dòng, chỉ khi nó vẫn còn PENDING. */
	public boolean delete(Pending pending) {
		return this.jdbc.sql("DELETE FROM med_file WHERE workspace_id = :workspaceId AND id = :id AND status = 'PENDING'")
			.param("workspaceId", pending.workspaceId())
			.param("id", pending.id())
			.update() > 0;
	}

}
