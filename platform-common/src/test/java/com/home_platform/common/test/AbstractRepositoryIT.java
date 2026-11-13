package com.home_platform.common.test;

import com.home_platform.common.context.WorkspaceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khung cho test repository của từng service: chạy trên database thật, mỗi test nằm trong
 * một transaction được rollback, và chỉ dùng {@code workspace_id} âm để không bao giờ chạm
 * vào dữ liệu thật. Lớp con đặt trong package của service để Spring Boot tìm thấy lớp khởi
 * động của service đó.
 */
@SpringBootTest
@Transactional
public abstract class AbstractRepositoryIT {

	protected static final long WS_A = -1L;

	protected static final long WS_B = -2L;

	protected static final long USER = -101L;

	protected static final WorkspaceContext CTX_A = new WorkspaceContext(WS_A, USER);

	protected static final WorkspaceContext CTX_B = new WorkspaceContext(WS_B, USER);

	@Autowired
	protected JdbcClient jdbc;

}
