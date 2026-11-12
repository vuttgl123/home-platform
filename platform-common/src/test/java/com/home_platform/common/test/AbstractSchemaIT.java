package com.home_platform.common.test;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khung cho test cấu trúc database của từng service: chạy trên database thật, mọi dữ liệu
 * test được rollback. Lớp con đặt trong package của service để Spring Boot tìm thấy lớp
 * khởi động của service đó.
 */
@SpringBootTest
@Transactional
public abstract class AbstractSchemaIT {

	protected static final long WS_A = -1L;
	protected static final long WS_B = -2L;

	@Autowired
	protected JdbcTemplate jdbc;

	/** Tên các bảng của database hiện tại bắt đầu bằng prefix, xếp theo tên. */
	protected List<String> tables(String prefix) {
		return jdbc.queryForList(
				"SELECT table_name FROM information_schema.tables"
						+ " WHERE table_schema = DATABASE() AND LEFT(table_name, ?) = ? ORDER BY table_name",
				String.class, prefix.length(), prefix);
	}

	/** Các cột khoá chính theo thứ tự trong khoá. */
	protected List<String> primaryKey(String table) {
		return index(table, "PRIMARY");
	}

	/** Các cột của một index theo thứ tự; danh sách rỗng nếu index không tồn tại. */
	protected List<String> index(String table, String indexName) {
		return jdbc.queryForList(
				"SELECT column_name FROM information_schema.statistics"
						+ " WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ? ORDER BY seq_in_index",
				String.class, table, indexName);
	}

}
