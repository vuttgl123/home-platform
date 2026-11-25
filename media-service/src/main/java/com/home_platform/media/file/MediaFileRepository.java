package com.home_platform.media.file;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import org.springframework.stereotype.Repository;

/** Dòng {@code med_file}: thông tin về file đã tải lên. */
@Repository
public class MediaFileRepository extends WorkspaceRepository<MediaFile> {

	MediaFileRepository(RepositorySupport support) {
		super(support, "med_file", MediaFile.class);
	}

	/** File READY có checksum này trong không gian, nếu có. Index (workspace_id, checksum_sha256). */
	public Optional<MediaFile> findReadyByChecksum(long workspaceId, String checksum) {
		return jdbc().sql("SELECT " + columns() + " FROM med_file WHERE workspace_id = :workspaceId "
				+ "AND checksum_sha256 = :checksum AND status = 'READY' ORDER BY id LIMIT 1")
			.param("workspaceId", workspaceId)
			.param("checksum", checksum)
			.query(rowMapper())
			.optional();
	}

	/** Các file có {@code id} trong {@code ids}; danh sách rỗng thì không chạy SQL. */
	public List<MediaFile> findAll(long workspaceId, Collection<Long> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		return jdbc().sql("SELECT " + columns() + " FROM med_file WHERE workspace_id = :workspaceId AND id IN (:ids)")
			.param("workspaceId", workspaceId)
			.param("ids", ids)
			.query(rowMapper())
			.list();
	}

}
