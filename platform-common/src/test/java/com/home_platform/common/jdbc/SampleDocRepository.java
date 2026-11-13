package com.home_platform.common.jdbc;

import java.util.Map;

import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.springframework.stereotype.Repository;

/** Repository mẫu trên bảng tạm {@code tmp_sample_doc}; viết đúng như một service sẽ viết. */
@Repository
public class SampleDocRepository extends WorkspaceRepository<SampleDoc> {

	SampleDocRepository(RepositorySupport support) {
		super(support, "tmp_sample_doc", SampleDoc.class);
	}

	/** Con của một địa điểm theo tên; dùng index {@code (workspace_id, parent_id, name)}. */
	CursorPage<SampleDoc> listByParent(long workspaceId, long parentId, CursorRequest page) {
		return keyset().where("parent_id = :parentId", Map.of("parentId", parentId))
			.orderBy("name")
			.fetch(workspaceId, page);
	}

	CursorPage<SampleDoc> listByParentDesc(long workspaceId, long parentId, CursorRequest page) {
		return keyset().where("parent_id = :parentId", Map.of("parentId", parentId))
			.orderByDesc("name")
			.fetch(workspaceId, page);
	}

	CursorPage<SampleDoc> listAll(long workspaceId, CursorRequest page) {
		return keyset().fetch(workspaceId, page);
	}

	/** Sắp theo một cột thời gian, để thử cursor mang giá trị {@code Instant}. */
	CursorPage<SampleDoc> listByCreatedAt(long workspaceId, CursorRequest page) {
		return keyset().orderBy("created_at").fetch(workspaceId, page);
	}

}
