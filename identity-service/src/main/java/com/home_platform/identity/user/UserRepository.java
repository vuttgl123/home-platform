package com.home_platform.identity.user;

import java.util.Optional;
import java.util.stream.Collectors;

import com.home_platform.common.jdbc.EntityMetadata;
import com.home_platform.common.jdbc.RepositorySupport;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Người dùng. Bảng có khoá {@code id}, không thuộc không gian nào, nên SQL viết tay với danh
 * sách cột và bộ đọc dòng của {@link EntityMetadata}.
 */
@Repository
public class UserRepository {

	private final JdbcClient jdbc;

	private final EntityMetadata<User> metadata;

	UserRepository(RepositorySupport support) {
		this.jdbc = support.jdbc();
		this.metadata = EntityMetadata.of(User.class, support.json());
	}

	public void insert(User user) {
		String values = this.metadata.components().stream().map(c -> ":" + c).collect(Collectors.joining(", "));
		this.jdbc.sql("INSERT INTO idn_user (" + this.metadata.selectList() + ") VALUES (" + values + ")")
			.params(this.metadata.parameters(user))
			.update();
	}

	public Optional<User> findById(long id) {
		return this.jdbc.sql("SELECT " + this.metadata.selectList() + " FROM idn_user WHERE id = :id")
			.param("id", id)
			.query(this.metadata.rowMapper())
			.optional();
	}

	/** Ghi lại tên hiển thị, trạng thái và lúc sửa. */
	public void update(User user) {
		this.jdbc.sql("UPDATE idn_user SET display_name = :displayName, status = :status, updated_at = :updatedAt "
				+ "WHERE id = :id")
			.params(this.metadata.parameters(user))
			.update();
	}

}
