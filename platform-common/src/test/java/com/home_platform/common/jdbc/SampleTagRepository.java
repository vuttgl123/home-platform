package com.home_platform.common.jdbc;

import org.springframework.stereotype.Repository;

/** Repository mẫu trên bảng tạm {@code tmp_sample_tag}, bảng không có cột chuẩn nào ngoài hai cột thời gian. */
@Repository
public class SampleTagRepository extends WorkspaceRepository<SampleTag> {

	SampleTagRepository(RepositorySupport support) {
		super(support, "tmp_sample_tag", SampleTag.class);
	}

}
