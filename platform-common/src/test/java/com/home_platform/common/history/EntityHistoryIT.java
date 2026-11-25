package com.home_platform.common.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.common.CommonIT;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.common.web.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponse;

@RecordApplicationEvents
class EntityHistoryIT extends CommonIT {

	private static final long BIG = 9007199254740993L;

	@Autowired
	EntityHistory history;

	@Autowired
	ApplicationEvents events;

	@Test
	void recordsVersionsAndComputesChangedFields() {
		PlaceSnapshot second = new PlaceSnapshot(10, "B", null, Map.of("big", BIG));
		this.history.record(CTX_A, "PLACE", 10, 1, ChangeAction.CREATE, new PlaceSnapshot(10, "A", null, Map.of("big", BIG)),
				null);
		this.history.record(CTX_A, "PLACE", 10, 2, ChangeAction.UPDATE, second, "đổi tên");
		this.history.record(CTX_A, "PLACE", 10, 3, ChangeAction.UPDATE,
				new PlaceSnapshot(10, "B", "ghi chú", Map.of("big", BIG)), null);

		EntityVersion v1 = this.history.get(WS_A, "PLACE", 10, 1);
		assertThat(v1.versionNo()).isEqualTo(1);
		assertThat(v1.action()).isEqualTo(ChangeAction.CREATE);
		assertThat(v1.changedFields()).isNull();
		assertThat(v1.changedBy()).isEqualTo(USER);
		assertThat(v1.changedAt()).isEqualTo(NOW);
		assertThat(v1.note()).isNull();
		assertThat(v1.snapshot().get("name").asString()).isEqualTo("A");
		assertThat(v1.snapshot().get("id").isNumber()).isTrue();
		assertThat(v1.snapshot().get("attributes").get("big").isIntegralNumber()).isTrue();
		assertThat(v1.snapshot().get("attributes").get("big").asLong()).isEqualTo(BIG);

		EntityVersion v2 = this.history.get(WS_A, "PLACE", 10, 2);
		assertThat(v2.changedFields()).containsExactly("name");
		assertThat(v2.note()).isEqualTo("đổi tên");
		assertThat(this.history.get(WS_A, "PLACE", 10, 3).changedFields()).containsExactly("note");
		assertThat(this.history.snapshotAs(v2, PlaceSnapshot.class)).isEqualTo(second);
	}

	@Test
	void fieldsAddedOrRemovedCountAsChanged() {
		this.history.record(CTX_A, "PLACE", 10, 1, ChangeAction.CREATE, Map.of("a", 1, "b", 2), null);
		this.history.record(CTX_A, "PLACE", 10, 2, ChangeAction.UPDATE, Map.of("a", 1, "c", 3), null);

		assertThat(this.history.get(WS_A, "PLACE", 10, 2).changedFields()).containsExactlyInAnyOrder("b", "c");
	}

	@Test
	void aFieldAddedAsNullIsNotAChange() {
		this.history.record(CTX_A, "PLACE", 10, 1, ChangeAction.CREATE, map("a", 1), null);
		this.history.record(CTX_A, "PLACE", 10, 2, ChangeAction.UPDATE, map("a", 1, "b", null), null);

		assertThat(this.history.get(WS_A, "PLACE", 10, 2).changedFields()).isEmpty();
	}

	@Test
	void aFieldAddedWithAValueIsAChange() {
		this.history.record(CTX_A, "PLACE", 10, 1, ChangeAction.CREATE, map("a", 1), null);
		this.history.record(CTX_A, "PLACE", 10, 2, ChangeAction.UPDATE, map("a", 1, "b", 2), null);

		assertThat(this.history.get(WS_A, "PLACE", 10, 2).changedFields()).containsExactly("b");
	}

	@Test
	void aFieldDroppedWhileNullIsNotAChange() {
		this.history.record(CTX_A, "PLACE", 10, 1, ChangeAction.CREATE, map("a", 1, "b", null), null);
		this.history.record(CTX_A, "PLACE", 10, 2, ChangeAction.UPDATE, map("a", 1), null);

		assertThat(this.history.get(WS_A, "PLACE", 10, 2).changedFields()).isEmpty();
	}

	@Test
	void aFieldSetBackToNullIsAChange() {
		this.history.record(CTX_A, "PLACE", 10, 1, ChangeAction.CREATE, map("a", 1, "b", 2), null);
		this.history.record(CTX_A, "PLACE", 10, 2, ChangeAction.UPDATE, map("a", 1, "b", null), null);

		assertThat(this.history.get(WS_A, "PLACE", 10, 2).changedFields()).containsExactly("b");
	}

	/** Như {@code Map.of} nhưng nhận giá trị {@code null}. */
	private static Map<String, Object> map(Object... keysAndValues) {
		Map<String, Object> map = new HashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			map.put((String) keysAndValues[i], keysAndValues[i + 1]);
		}
		return map;
	}

	@Test
	void snapshotThatIsNotAnObjectHasNoChangedFields() {
		this.history.record(CTX_A, "PLACE", 10, 1, ChangeAction.CREATE, java.util.List.of(1), null);
		this.history.record(CTX_A, "PLACE", 10, 2, ChangeAction.UPDATE, java.util.List.of(2), null);

		assertThat(this.history.get(WS_A, "PLACE", 10, 2).changedFields()).isNull();
	}

	@Test
	void updateWithoutAPreviousVersionHasNoChangedFields() {
		this.history.record(CTX_A, "PLACE", 10, 5, ChangeAction.UPDATE, new PlaceSnapshot(10, "A", null, null), null);

		assertThat(this.history.get(WS_A, "PLACE", 10, 5).changedFields()).isNull();
	}

	@Test
	void listReturnsNewestFirstInPages() {
		for (int v = 1; v <= 3; v++) {
			this.history.record(CTX_A, "PLACE", 10, v, (v == 1) ? ChangeAction.CREATE : ChangeAction.UPDATE,
					new PlaceSnapshot(10, "N" + v, null, null), null);
		}

		CursorPage<EntityVersionSummary> first = this.history.list(WS_A, "PLACE", 10, new CursorRequest(null, 2));
		assertThat(first.items()).extracting(EntityVersionSummary::versionNo).containsExactly(3, 2);
		assertThat(first.items().get(0).changedFields()).containsExactly("name");
		assertThat(first.nextCursor()).isNotNull();

		CursorPage<EntityVersionSummary> second = this.history.list(WS_A, "PLACE", 10,
				new CursorRequest(first.nextCursor(), 2));
		assertThat(second.items()).extracting(EntityVersionSummary::versionNo).containsExactly(1);
		assertThat(second.nextCursor()).isNull();
	}

	/** Đồng hồ của test đứng yên nên mọi version cùng thời điểm: thứ tự do loại, id, số version quyết định. */
	@Test
	void listRecentIsNewestFirstAcrossEntities() {
		this.history.record(CTX_A, "ITEM", 5, 1, ChangeAction.CREATE, Map.of("a", 1), null);
		this.history.record(CTX_A, "PLACE", 10, 1, ChangeAction.CREATE, Map.of("a", 1), null);
		this.history.record(CTX_A, "PLACE", 10, 2, ChangeAction.UPDATE, Map.of("a", 2), "đổi");
		this.history.record(CTX_B, "PLACE", 10, 1, ChangeAction.CREATE, Map.of("a", 1), null);

		CursorPage<EntityChange> first = this.history.listRecent(WS_A, ChangeFilter.none(), new CursorRequest(null, 2));
		assertThat(first.items()).extracting(EntityChange::entityType, EntityChange::entityId, EntityChange::versionNo)
			.containsExactly(org.assertj.core.groups.Tuple.tuple("PLACE", 10L, 2),
					org.assertj.core.groups.Tuple.tuple("PLACE", 10L, 1));
		assertThat(first.items().get(0).action()).isEqualTo(ChangeAction.UPDATE);
		assertThat(first.items().get(0).changedFields()).containsExactly("a");
		assertThat(first.items().get(0).changedBy()).isEqualTo(USER);
		assertThat(first.items().get(0).changedAt()).isEqualTo(NOW);
		assertThat(first.items().get(0).note()).isEqualTo("đổi");
		assertThat(first.nextCursor()).isNotNull();

		CursorPage<EntityChange> second = this.history.listRecent(WS_A, ChangeFilter.none(), new CursorRequest(first.nextCursor(), 2));
		assertThat(second.items()).extracting(EntityChange::entityType).containsExactly("ITEM");
		assertThat(second.nextCursor()).isNull();

		assertThat(this.history.listRecent(WS_A, new ChangeFilter("ITEM", null, null, null, null), new CursorRequest(null, 20)).items()).hasSize(1);
		assertThat(this.history.listRecent(WS_B, ChangeFilter.none(), new CursorRequest(null, 20)).items()).hasSize(1);
		assertThatThrownBy(() -> this.history.listRecent(WS_A, ChangeFilter.none(), new CursorRequest("!!!", 20)))
			.isInstanceOf(BadRequestException.class);
	}

	/**
	 * Ba version ở ba giờ khác nhau, hai người. {@code from} giữ dòng đúng mốc, {@code to} loại dòng
	 * đúng mốc.
	 */
	@Test
	void listRecentFiltersByActionUserAndTime() {
		insertVersion(1, ChangeAction.CREATE, 7L, "2026-11-10 08:00:00.000");
		insertVersion(2, ChangeAction.UPDATE, 8L, "2026-11-11 08:00:00.000");
		insertVersion(3, ChangeAction.DELETE, 7L, "2026-11-12 08:00:00.000");
		Instant day11 = Instant.parse("2026-11-11T08:00:00Z");
		Instant day12 = Instant.parse("2026-11-12T08:00:00Z");

		assertThat(versionsOf(new ChangeFilter(null, ChangeAction.UPDATE, null, null, null))).containsExactly(2);
		assertThat(versionsOf(new ChangeFilter(null, null, 7L, null, null))).containsExactly(3, 1);
		assertThat(versionsOf(new ChangeFilter(null, null, null, day11, null))).containsExactly(3, 2);
		assertThat(versionsOf(new ChangeFilter(null, null, null, null, day12))).containsExactly(2, 1);
		assertThat(versionsOf(new ChangeFilter(null, null, null, day11, day12))).containsExactly(2);
		assertThat(versionsOf(new ChangeFilter("PLACE", ChangeAction.DELETE, 7L, day11, null))).containsExactly(3);
		assertThat(versionsOf(new ChangeFilter("ITEM", null, null, null, null))).isEmpty();
	}

	private List<Integer> versionsOf(ChangeFilter filter) {
		return this.history.listRecent(WS_A, filter, new CursorRequest(null, 20))
			.items()
			.stream()
			.map(EntityChange::versionNo)
			.toList();
	}

	private void insertVersion(int versionNo, ChangeAction action, long changedBy, String changedAt) {
		this.jdbc.sql("""
				INSERT INTO tmp_entity_version (workspace_id, entity_type, entity_id, version_no, action, snapshot,
				  changed_by, changed_at)
				VALUES (:ws, 'PLACE', 10, :v, :action, '{}', :by, :at)""")
			.param("ws", WS_A)
			.param("v", versionNo)
			.param("action", action.name())
			.param("by", changedBy)
			.param("at", changedAt)
			.update();
	}

	@Test
	void valueSetAtFindsTheLastChangeOfThatValue() {
		insertSnapshot(10, 1, "{\"status\": \"A\", \"name\": \"x\"}", "2026-01-01 00:00:00.000");
		insertSnapshot(10, 2, "{\"status\": \"A\", \"name\": \"y\"}", "2026-01-02 00:00:00.000");
		insertSnapshot(10, 3, "{\"status\": \"B\", \"name\": \"y\"}", "2026-01-03 00:00:00.000");
		insertSnapshot(10, 4, "{\"status\": \"B\", \"name\": \"z\"}", "2026-01-04 00:00:00.000");
		insertSnapshot(10, 5, "{\"status\": \"B\", \"name\": \"w\"}", "2026-01-05 00:00:00.000");
		insertSnapshot(11, 1, "{\"status\": \"A\"}", "2026-02-01 00:00:00.000");
		insertSnapshot(12, 1, "{\"name\": \"no status\"}", "2026-03-01 00:00:00.000");
		insertSnapshot(12, 2, "{\"name\": \"still none\"}", "2026-03-02 00:00:00.000");

		Map<Long, Instant> found = this.history.valueSetAt(WS_A, "PLACE", List.of(10L, 11L, 12L, 13L), "$.status");

		assertThat(found).containsOnly(Map.entry(10L, Instant.parse("2026-01-03T00:00:00Z")),
				Map.entry(11L, Instant.parse("2026-02-01T00:00:00Z")),
				Map.entry(12L, Instant.parse("2026-03-01T00:00:00Z")));
		assertThat(this.history.valueSetAt(WS_A, "PLACE", List.of(), "$.status")).isEmpty();
	}

	@Test
	void valueSetAtReadsNestedPaths() {
		insertSnapshot(10, 1, "{\"attributes\": {\"expiry\": \"2027-01-01\"}}", "2026-01-01 00:00:00.000");
		insertSnapshot(10, 2, "{\"attributes\": {\"expiry\": \"2027-01-01\", \"job\": \"x\"}}",
				"2026-01-02 00:00:00.000");
		insertSnapshot(10, 3, "{\"attributes\": {\"expiry\": \"2028-01-01\", \"job\": \"x\"}}",
				"2026-01-03 00:00:00.000");
		insertSnapshot(10, 4, "{\"attributes\": {\"expiry\": \"2028-01-01\", \"job\": \"y\"}}",
				"2026-01-04 00:00:00.000");

		assertThat(this.history.valueSetAt(WS_A, "PLACE", List.of(10L), "$.attributes.expiry"))
			.containsOnly(Map.entry(10L, Instant.parse("2026-01-03T00:00:00Z")));
	}

	private void insertSnapshot(long entityId, int versionNo, String snapshot, String changedAt) {
		this.jdbc.sql("""
				INSERT INTO tmp_entity_version (workspace_id, entity_type, entity_id, version_no, action, snapshot,
				  changed_by, changed_at)
				VALUES (:ws, 'PLACE', :id, :v, 'UPDATE', :snapshot, 1, :at)""")
			.param("ws", WS_A)
			.param("id", entityId)
			.param("v", versionNo)
			.param("snapshot", snapshot)
			.param("at", changedAt)
			.update();
	}

	@Test
	void badCursorIs400() {
		assertThatThrownBy(() -> this.history.list(WS_A, "PLACE", 10, new CursorRequest("garbage", 2)))
			.isInstanceOf(BadRequestException.class);
	}

	@Test
	void versionsOfOtherEntitiesAndWorkspacesStayOut() {
		this.history.record(CTX_A, "PLACE", 10, 1, ChangeAction.CREATE, new PlaceSnapshot(10, "A place", null, null),
				null);
		this.history.record(CTX_A, "ITEM", 10, 1, ChangeAction.CREATE, new PlaceSnapshot(10, "A item", null, null),
				null);
		this.history.record(CTX_B, "PLACE", 10, 1, ChangeAction.CREATE, new PlaceSnapshot(10, "B place", null, null),
				null);

		assertThat(this.history.list(WS_A, "PLACE", 10, new CursorRequest(null, 20)).items()).hasSize(1);
		assertThat(this.history.get(WS_A, "PLACE", 10, 1).snapshot().get("name").asString()).isEqualTo("A place");
		assertThat(this.history.get(WS_B, "PLACE", 10, 1).snapshot().get("name").asString()).isEqualTo("B place");
		assertThatThrownBy(() -> this.history.get(WS_A, "PERSON", 10, 1)).isInstanceOf(NotFoundException.class)
			.satisfies(ex -> assertThat(((ErrorResponse) ex).getBody().getDetail())
				.isEqualTo("PERSON 10 version 1 not found"));
	}

	@Test
	void publishesAnEventAfterRecording() {
		this.history.record(CTX_A, "PLACE", 10, 1, ChangeAction.CREATE, new PlaceSnapshot(10, "A", null, null), null);

		assertThat(this.events.stream(EntityChangedEvent.class).toList())
			.containsExactly(new EntityChangedEvent(WS_A, "PLACE", 10, 1, ChangeAction.CREATE, USER, NOW));
	}

	@Test
	void recordingTheSameVersionTwiceFails() {
		this.history.record(CTX_A, "PLACE", 10, 1, ChangeAction.CREATE, new PlaceSnapshot(10, "A", null, null), null);

		assertThatThrownBy(() -> this.history.record(CTX_A, "PLACE", 10, 1, ChangeAction.CREATE,
				new PlaceSnapshot(10, "A", null, null), null))
			.isInstanceOf(DuplicateKeyException.class);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void recordOutsideATransactionIsRefused() {
		assertThatThrownBy(() -> this.history.record(CTX_A, "PLACE", 10, 1, ChangeAction.CREATE,
				new PlaceSnapshot(10, "A", null, null), null))
			.isInstanceOf(IllegalTransactionStateException.class);
		assertThat(this.events.stream(EntityChangedEvent.class)).isEmpty();
	}

	record PlaceSnapshot(long id, String name, String note, Map<String, Object> attributes) {
	}


}
