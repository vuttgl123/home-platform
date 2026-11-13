package com.home_platform.common.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
