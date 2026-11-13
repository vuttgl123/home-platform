package com.home_platform.common.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.util.Map;

import com.home_platform.common.json.StorageJson;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class KeysetQueryTests {

	private final RepositorySupport support = new RepositorySupport(mock(JdbcClient.class), () -> 1L,
			Clock.systemUTC(), new StorageJson());

	private final SampleDocRepository docs = new SampleDocRepository(this.support);

	private final String cols = this.docs.columns();

	@Test
	void sqlForNameOrder() {
		KeysetQuery<SampleDoc> query = this.docs.keyset()
			.where("parent_id = :parentId", Map.of("parentId", 1L))
			.orderBy("name");

		assertThat(query.sql(false)).isEqualTo("SELECT " + this.cols
				+ " FROM tmp_sample_doc WHERE workspace_id = :workspaceId AND deleted_at IS NULL "
				+ "AND (parent_id = :parentId) ORDER BY name ASC, id ASC LIMIT :limit");
		assertThat(query.sql(true)).isEqualTo("SELECT " + this.cols
				+ " FROM tmp_sample_doc WHERE workspace_id = :workspaceId AND deleted_at IS NULL "
				+ "AND (parent_id = :parentId) "
				+ "AND (name > :cursorSort OR (name = :cursorSort AND id > :cursorId)) ORDER BY name ASC, id ASC LIMIT :limit");
	}

	@Test
	void sqlForDescendingOrder() {
		assertThat(this.docs.keyset().orderByDesc("name").sql(true))
			.contains("(name < :cursorSort OR (name = :cursorSort AND id < :cursorId)) "
					+ "ORDER BY name DESC, id DESC LIMIT :limit");
	}

	@Test
	void sqlWithoutOrderByUsesId() {
		assertThat(this.docs.keyset().sql(true)).endsWith("AND id > :cursorId ORDER BY id ASC LIMIT :limit");
	}

	@Test
	void conditionsAreJoinedWithAnd() {
		assertThat(this.docs.keyset()
			.where("parent_id = :parentId", Map.of("parentId", 1L))
			.where("pinned = :pinned", Map.of("pinned", true))
			.sql(false)).contains("AND (parent_id = :parentId) AND (pinned = :pinned) ORDER BY");
	}

	@Test
	void tableWithoutDeletedAtHasNoDeletedFilter() {
		SampleTagRepository tags = new SampleTagRepository(this.support);

		assertThat(tags.keyset().sql(false)).doesNotContain("deleted_at");
	}

	@Test
	void unknownSortColumnIsRejected() {
		assertThatThrownBy(() -> this.docs.keyset().orderBy("nope")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void jsonSortColumnIsRejected() {
		assertThatThrownBy(() -> this.docs.keyset().orderBy("attributes"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void parameterNameUsedTwiceIsRejected() {
		KeysetQuery<SampleDoc> query = this.docs.keyset().where("parent_id = :p", Map.of("p", 1L));

		assertThatThrownBy(() -> query.where("rating = :p", Map.of("p", 2))).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("p");
	}

	@Test
	void reservedParameterNamesAreRejected() {
		assertThatThrownBy(() -> this.docs.keyset().where("x = :limit", Map.of("limit", 1)))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> this.docs.keyset().where("x = :workspaceId", Map.of("workspaceId", 1)))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
