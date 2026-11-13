package com.home_platform.common.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.home_platform.common.json.StorageJson;
import org.junit.jupiter.api.Test;

class EntityMetadataTests {

	private final StorageJson json = new StorageJson();

	private final EntityMetadata<SampleDoc> docs = EntityMetadata.of(SampleDoc.class, this.json);

	@Test
	void columnsAreSnakeCaseInDeclarationOrder() {
		assertThat(this.docs.selectList())
			.isEqualTo("workspace_id, id, parent_id, name, status, pinned, rating, price, valid_from, attributes, "
					+ "created_at, created_by, updated_at, updated_by, version, deleted_at");
		assertThat(this.docs.column("validFrom")).isEqualTo("valid_from");
		assertThat(this.docs.components()).startsWith("workspaceId", "id", "parentId").hasSize(16);
		assertThat(this.docs.has("deletedAt")).isTrue();
		assertThat(this.docs.has("nope")).isFalse();
		assertThat(EntityMetadata.of(Sha.class, this.json).column("checksumSha256")).isEqualTo("checksum_sha256");
	}

	@Test
	void parametersUseJdbcFriendlyValues() {
		SampleDoc doc = SampleDoc.named("A")
			.withPinned(true)
			.withCreatedAt(Instant.parse("2026-11-12T06:40:00.123Z"))
			.withAttributes(Map.of("n", 9007199254740993L));

		Map<String, Object> parameters = this.docs.parameters(doc);

		assertThat(parameters).containsEntry("status", "ACTIVE")
			.containsEntry("pinned", true)
			.containsEntry("parentId", null)
			.containsEntry("createdAt", LocalDateTime.of(2026, 11, 12, 6, 40, 0, 123_000_000))
			.containsEntry("attributes", "{\"n\":9007199254740993}");
		assertThat(parameters.keySet()).containsExactlyElementsOf(this.docs.components());
	}

	@Test
	void withReplacesOnlyNamedComponents() {
		SampleDoc doc = SampleDoc.named("A").withId(7);

		SampleDoc changed = this.docs.with(doc, Map.of("version", 2, "name", "B"));

		assertThat(changed).isEqualTo(doc.withVersion(2).withName("B"));
		assertThat(this.docs.get(changed, "name")).isEqualTo("B");
	}

	@Test
	void unsupportedTypeFailsNamingRecordAndComponent() {
		assertThatThrownBy(() -> EntityMetadata.of(Bad.class, this.json)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Bad.token")
			.hasMessageContaining("java.util.UUID");
	}

	@Test
	void unknownComponentIsRejected() {
		assertThatThrownBy(() -> this.docs.column("nope")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> this.docs.with(SampleDoc.named("A"), Map.of("nope", 1)))
			.isInstanceOf(IllegalArgumentException.class);
	}

	record Sha(long workspaceId, long id, String checksumSha256) {
	}

	record Bad(long workspaceId, long id, UUID token) {
	}

}
