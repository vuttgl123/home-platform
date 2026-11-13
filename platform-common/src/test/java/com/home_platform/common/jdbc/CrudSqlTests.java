package com.home_platform.common.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.home_platform.common.json.StorageJson;
import org.junit.jupiter.api.Test;

class CrudSqlTests {

	private final StorageJson json = new StorageJson();

	private final CrudSql tag = new CrudSql("tmp_sample_tag", EntityMetadata.of(SampleTag.class, this.json));

	private final CrudSql doc = new CrudSql("tmp_sample_doc", EntityMetadata.of(SampleDoc.class, this.json));

	@Test
	void statementsForAPlainTable() {
		assertThat(this.tag.insert())
			.isEqualTo("INSERT INTO tmp_sample_tag (workspace_id, id, code, name, created_at, updated_at) "
					+ "VALUES (:workspaceId, :id, :code, :name, :createdAt, :updatedAt)");
		assertThat(this.tag.selectById())
			.isEqualTo("SELECT workspace_id, id, code, name, created_at, updated_at FROM tmp_sample_tag "
					+ "WHERE workspace_id = :workspaceId AND id = :id");
		assertThat(this.tag.update())
			.isEqualTo("UPDATE tmp_sample_tag SET code = :code, name = :name, updated_at = :updatedAt "
					+ "WHERE workspace_id = :workspaceId AND id = :id");
		assertThat(this.tag.deletePermanently())
			.isEqualTo("DELETE FROM tmp_sample_tag WHERE workspace_id = :workspaceId AND id = :id");
		assertThat(this.tag.versioned()).isFalse();
		assertThat(this.tag.softDeletable()).isFalse();
	}

	@Test
	void statementsForATableWithStandardColumns() {
		assertThat(this.doc.selectById())
			.endsWith("FROM tmp_sample_doc WHERE workspace_id = :workspaceId AND id = :id AND deleted_at IS NULL");
		assertThat(this.doc.selectByIdIncludingDeleted())
			.endsWith("FROM tmp_sample_doc WHERE workspace_id = :workspaceId AND id = :id");
		assertThat(this.doc.update())
			.isEqualTo("UPDATE tmp_sample_doc SET parent_id = :parentId, name = :name, status = :status, "
					+ "pinned = :pinned, rating = :rating, price = :price, valid_from = :validFrom, "
					+ "attributes = :attributes, updated_at = :updatedAt, updated_by = :updatedBy, "
					+ "version = version + 1 "
					+ "WHERE workspace_id = :workspaceId AND id = :id AND version = :version AND deleted_at IS NULL");
		assertThat(this.doc.softDelete())
			.isEqualTo("UPDATE tmp_sample_doc SET deleted_at = :deletedAt, updated_at = :updatedAt, "
					+ "updated_by = :updatedBy, version = version + 1 "
					+ "WHERE workspace_id = :workspaceId AND id = :id AND version = :version AND deleted_at IS NULL");
		assertThat(this.doc.restore())
			.isEqualTo("UPDATE tmp_sample_doc SET deleted_at = NULL, updated_at = :updatedAt, "
					+ "updated_by = :updatedBy, version = version + 1 "
					+ "WHERE workspace_id = :workspaceId AND id = :id AND version = :version AND deleted_at IS NOT NULL");
		assertThat(this.doc.versioned()).isTrue();
		assertThat(this.doc.softDeletable()).isTrue();
	}

	@Test
	void tableNameMustBeAPlainIdentifier() {
		assertThatThrownBy(() -> new CrudSql("t; DROP TABLE x", EntityMetadata.of(SampleTag.class, this.json)))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
