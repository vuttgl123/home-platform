package com.home_platform.schema;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

class RegistrySchemaIT extends AbstractSchemaIT {

	@Test
	void registryTablesExist() {
		assertThat(tables("reg_")).containsExactly("reg_entity_version", "reg_field_def", "reg_item",
				"reg_item_category", "reg_occupancy", "reg_person", "reg_place", "reg_place_address",
				"reg_place_type");
	}

	@Test
	void primaryKeysStartWithWorkspaceId() {
		assertThat(tables("reg_")).allSatisfy(t -> assertThat(primaryKey(t)).startsWith("workspace_id"));
		assertThat(primaryKey("reg_place")).containsExactly("workspace_id", "id");
		assertThat(primaryKey("reg_place_address")).containsExactly("workspace_id", "place_id");
		assertThat(primaryKey("reg_entity_version")).containsExactly("workspace_id", "entity_type", "entity_id",
				"version_no");
	}

	@Test
	void indexesMatchDesign() {
		Map<String, List<String>> expected = Map.ofEntries(
				entry("reg_place_type.uk_reg_place_type_code", List.of("workspace_id", "code")),
				entry("reg_field_def.uk_reg_field_def_key", List.of("workspace_id", "target", "type_id", "field_key")),
				entry("reg_place.idx_reg_place_parent", List.of("workspace_id", "parent_id", "name")),
				entry("reg_place.idx_reg_place_path", List.of("workspace_id", "path")),
				entry("reg_place.idx_reg_place_type", List.of("workspace_id", "place_type_id", "name")),
				entry("reg_place.idx_reg_place_name", List.of("workspace_id", "name")),
				entry("reg_place.idx_reg_place_code", List.of("workspace_id", "code")),
				entry("reg_place_address.idx_reg_address_area", List.of("workspace_id", "province_code", "ward_code")),
				entry("reg_place_address.idx_reg_address_geohash", List.of("workspace_id", "geohash")),
				entry("reg_person.idx_reg_person_name", List.of("workspace_id", "full_name")),
				entry("reg_person.idx_reg_person_phone", List.of("workspace_id", "phone")),
				entry("reg_person.idx_reg_person_id_number", List.of("workspace_id", "id_number")),
				entry("reg_occupancy.idx_reg_occupancy_place",
						List.of("workspace_id", "place_id", "end_date", "start_date")),
				entry("reg_occupancy.idx_reg_occupancy_person", List.of("workspace_id", "person_id", "start_date")),
				entry("reg_item_category.uk_reg_item_category_name", List.of("workspace_id", "name")),
				entry("reg_item.idx_reg_item_place", List.of("workspace_id", "place_id", "removed_at", "name")),
				entry("reg_item.idx_reg_item_category", List.of("workspace_id", "category_id", "name")),
				entry("reg_item.idx_reg_item_name", List.of("workspace_id", "name")),
				entry("reg_item.idx_reg_item_code", List.of("workspace_id", "code")),
				entry("reg_item.idx_reg_item_replaced_by", List.of("workspace_id", "replaced_by_item_id")),
				entry("reg_entity_version.idx_reg_version_changed_at", List.of("workspace_id", "changed_at")));
		expected.forEach((key, columns) -> {
			String[] parts = key.split("\\.");
			assertThat(index(parts[0], parts[1])).as(key).isEqualTo(columns);
		});
	}

	@Test
	void versionTableHas64Partitions() {
		Integer partitions = jdbc.queryForObject(
				"SELECT COUNT(*) FROM information_schema.partitions"
						+ " WHERE table_schema = DATABASE() AND table_name = 'reg_entity_version'",
				Integer.class);
		assertThat(partitions).isEqualTo(64);
	}

	@Test
	void rootPlaceWithNullParentIsAccepted() {
		type(WS_A, 10, "HOTEL");
		place(WS_A, 100, null, 10, "/100/", "A");
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reg_place WHERE workspace_id = ? AND id = 100",
				Integer.class, WS_A)).isEqualTo(1);
	}

	@Test
	void parentFromAnotherWorkspaceIsRejected() {
		type(WS_A, 10, "HOTEL");
		type(WS_B, 10, "HOTEL");
		place(WS_A, 100, null, 10, "/100/", "A");
		assertThatThrownBy(() -> place(WS_B, 200, 100L, 10, "/100/200/", "B"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void sameCodeInTwoWorkspacesIsAcceptedButNotTwiceInOne() {
		type(WS_A, 10, "HOTEL");
		type(WS_B, 10, "HOTEL");
		assertThatThrownBy(() -> type(WS_A, 11, "HOTEL")).isInstanceOf(DuplicateKeyException.class);
	}

	@Test
	void hardDeleteOfPlaceInUseIsRejected() {
		type(WS_A, 10, "HOTEL");
		place(WS_A, 100, null, 10, "/100/", "A");
		place(WS_A, 101, 100L, 10, "/100/101/", "P101");
		place(WS_A, 102, 100L, 10, "/100/102/", "P102");
		person(WS_A, 500, "An");
		occupancy(WS_A, 600, 101, 500);
		item(WS_A, 700, 102, "Tủ lạnh");
		for (long placeId : List.of(100L, 101L, 102L)) {
			assertThatThrownBy(
					() -> jdbc.update("DELETE FROM reg_place WHERE workspace_id = ? AND id = ?", WS_A, placeId))
					.as("place " + placeId)
					.isInstanceOf(DataIntegrityViolationException.class);
		}
	}

	@Test
	void vietnameseAndEmojiSurviveRoundTrip() {
		type(WS_A, 10, "HOTEL");
		place(WS_A, 100, null, 10, "/100/", "Nhà nghỉ Đường Số 5 🏠");
		assertThat(jdbc.queryForObject("SELECT name FROM reg_place WHERE workspace_id = ? AND id = 100", String.class,
				WS_A)).isEqualTo("Nhà nghỉ Đường Số 5 🏠");
	}

	@Test
	void defaultsFollowDesign() {
		type(WS_A, 10, "HOTEL");
		place(WS_A, 100, null, 10, "/100/", "A");
		item(WS_A, 700, 100, "Ghế");
		Map<String, Object> place = jdbc.queryForMap(
				"SELECT version, status, depth, deleted_at FROM reg_place WHERE workspace_id = ? AND id = 100", WS_A);
		assertThat(place.get("version")).isEqualTo(1);
		assertThat(place.get("status")).isEqualTo("ACTIVE");
		assertThat(((Number) place.get("depth")).intValue()).isZero();
		assertThat(place.get("deleted_at")).isNull();
		Map<String, Object> item = jdbc.queryForMap(
				"SELECT quantity, status, removed_at FROM reg_item WHERE workspace_id = ? AND id = 700", WS_A);
		assertThat(item.get("quantity")).isEqualTo(1);
		assertThat(item.get("status")).isEqualTo("IN_USE");
		assertThat(item.get("removed_at")).isNull();
	}

	@Test
	void versionNumbersAreUniquePerEntity() {
		version(WS_A, "PLACE", 100, 1);
		version(WS_A, "PLACE", 100, 2);
		version(WS_A, "PERSON", 100, 2);
		assertThatThrownBy(() -> version(WS_A, "PLACE", 100, 2)).isInstanceOf(DuplicateKeyException.class);
	}

	private void type(long ws, long id, String code) {
		jdbc.update("INSERT INTO reg_place_type (workspace_id, id, code, name) VALUES (?, ?, ?, ?)", ws, id, code,
				code);
	}

	private void place(long ws, long id, Long parentId, long typeId, String path, String name) {
		jdbc.update(
				"INSERT INTO reg_place (workspace_id, id, parent_id, place_type_id, path, name, created_by, updated_by)"
						+ " VALUES (?, ?, ?, ?, ?, ?, -1, -1)",
				ws, id, parentId, typeId, path, name);
	}

	private void person(long ws, long id, String fullName) {
		jdbc.update(
				"INSERT INTO reg_person (workspace_id, id, full_name, created_by, updated_by) VALUES (?, ?, ?, -1, -1)",
				ws, id, fullName);
	}

	private void occupancy(long ws, long id, long placeId, long personId) {
		jdbc.update(
				"INSERT INTO reg_occupancy (workspace_id, id, place_id, person_id, relation, start_date, created_by, updated_by)"
						+ " VALUES (?, ?, ?, ?, 'RESIDENT', '2026-01-01', -1, -1)",
				ws, id, placeId, personId);
	}

	private void item(long ws, long id, long placeId, String name) {
		jdbc.update(
				"INSERT INTO reg_item (workspace_id, id, place_id, name, created_by, updated_by) VALUES (?, ?, ?, ?, -1, -1)",
				ws, id, placeId, name);
	}

	private void version(long ws, String entityType, long entityId, int versionNo) {
		jdbc.update(
				"INSERT INTO reg_entity_version (workspace_id, entity_type, entity_id, version_no, action, snapshot, changed_by, changed_at)"
						+ " VALUES (?, ?, ?, ?, 'UPDATE', '{}', -1, UTC_TIMESTAMP(3))",
				ws, entityType, entityId, versionNo);
	}

}
