package com.home_platform.common.jdbc;

import java.time.Clock;

import com.home_platform.common.id.IdGenerator;
import com.home_platform.common.json.StorageJson;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Những thứ mọi repository cần, gom lại để hàm khởi tạo của repository chỉ nhận một tham số.
 *
 * @param jdbc nơi chạy câu SQL
 * @param idGenerator sinh {@code id} cho dòng mới
 * @param clock nguồn của {@code created_at}, {@code updated_at}, {@code deleted_at}
 * @param json đọc ghi các cột kiểu {@code JSON}
 */
public record RepositorySupport(JdbcClient jdbc, IdGenerator idGenerator, Clock clock, StorageJson json) {
}
