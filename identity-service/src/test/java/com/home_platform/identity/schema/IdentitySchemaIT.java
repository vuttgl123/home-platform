package com.home_platform.identity.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import com.home_platform.common.test.AbstractSchemaIT;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

class IdentitySchemaIT extends AbstractSchemaIT {

	@Test
	void identityTablesExist() {
		assertThat(tables("idn_")).containsExactly("idn_user", "idn_workspace", "idn_workspace_member");
	}

	@Test
	void keysAndIndexesMatchDesign() {
		assertThat(primaryKey("idn_user")).containsExactly("id");
		assertThat(primaryKey("idn_workspace")).containsExactly("id");
		assertThat(primaryKey("idn_workspace_member")).containsExactly("workspace_id", "user_id");
		assertThat(index("idn_user", "uk_idn_user_email")).containsExactly("email");
		assertThat(index("idn_workspace", "idx_idn_workspace_owner")).containsExactly("owner_user_id");
		assertThat(index("idn_workspace_member", "idx_idn_member_user")).containsExactly("user_id", "workspace_id");
	}

	@Test
	void emailIsUnique() {
		jdbc.update("INSERT INTO idn_user (id, email, display_name) VALUES (-1, 'a@example.test', 'A')");
		assertThatThrownBy(() -> jdbc
				.update("INSERT INTO idn_user (id, email, display_name) VALUES (-2, 'a@example.test', 'B')"))
				.isInstanceOf(DuplicateKeyException.class);
	}

	@Test
	void workspaceOwnerMustExist() {
		assertThatThrownBy(
				() -> jdbc.update("INSERT INTO idn_workspace (id, owner_user_id, name) VALUES (-1, -999, 'W')"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void defaultsFollowDesign() {
		jdbc.update("INSERT INTO idn_user (id, email, display_name) VALUES (-1, 'a@example.test', 'A')");
		Map<String, Object> row = jdbc.queryForMap(
				"SELECT status, ABS(TIMESTAMPDIFF(SECOND, created_at, UTC_TIMESTAMP(3))) AS age FROM idn_user WHERE id = -1");
		assertThat(row.get("status")).isEqualTo("ACTIVE");
		assertThat(((Number) row.get("age")).longValue()).isLessThanOrEqualTo(5L);
	}

}
