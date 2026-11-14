package com.home_platform.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.web.ConflictException;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.common.web.NotFoundException;
import com.home_platform.registry.item.Item;
import com.home_platform.registry.item.ItemRequest;
import com.home_platform.registry.item.ItemService;
import com.home_platform.registry.place.Place;
import com.home_platform.registry.place.PlaceCreateRequest;
import com.home_platform.registry.place.PlaceMoveRequest;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;

/**
 * Hai lệnh ghi chạy cùng lúc không được để lại dữ liệu sai. Mỗi test giữ một lệnh ở trạng thái
 * "đã ghi nhưng chưa commit", cho lệnh thứ hai chạy, rồi mới commit lệnh đầu.
 */
class ConcurrentWritesIT extends CommittedDataIT {

	@Autowired
	PlaceService places;

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	ItemService items;

	@Autowired
	EntityHistory history;

	long type;

	@BeforeEach
	void placeType() {
		this.type = this.placeTypes.create(CTX, new PlaceTypeCreateRequest("T", "T", null, null)).id();
	}

	@Test
	void deleteSeesAnItemAddedConcurrently() throws Exception {
		long room = place(null, "P101");
		OpenTransaction<Item> adding = holdOpen(() -> this.items.create(CTX, item(room, "Tủ")));
		Future<Void> deleting = inBackground(() -> {
			this.places.delete(CTX, room, 1, null);
			return null;
		});
		letItBlock();
		adding.commit();

		assertThatThrownBy(deleting::get).cause()
			.isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.getBody().getDetail())
				.contains("0 child places, 0 occupancies and 1 items"));
		assertThat(this.places.get(WS, room).version()).isEqualTo(1);
		assertThat(this.history.list(WS, "PLACE", room, new CursorRequest(null, 20)).items()).hasSize(1);
	}

	@Test
	void childCreatedDuringADeleteIsRefused() throws Exception {
		long parent = place(null, "A");
		OpenTransaction<Void> deleting = holdOpen(() -> {
			this.places.delete(CTX, parent, 1, null);
			return null;
		});
		Future<Place> creating = inBackground(() -> this.places.create(CTX, newPlace(parent, "child")));
		letItBlock();
		deleting.commit();

		assertThatThrownBy(creating::get).cause().isInstanceOf(NotFoundException.class);
		assertThat(rows("reg_place")).isEqualTo(1);
	}

	@Test
	void childCreatedDuringAMoveLandsUnderTheNewPath() throws Exception {
		long x = place(null, "X");
		long p = place(null, "P");
		OpenTransaction<Place> moving = holdOpen(() -> this.places.move(CTX, p, new PlaceMoveRequest(1, x, null)));
		Future<Place> creating = inBackground(() -> this.places.create(CTX, newPlace(p, "C")));
		letItBlock();
		moving.commit();

		Place child = creating.get();
		assertThat(child.path()).isEqualTo("/" + x + "/" + p + "/" + child.id() + "/");
		assertThat(child.depth()).isEqualTo(2);
	}

	@Test
	void crossingMovesDoNotMakeACycle() throws Exception {
		long a = place(null, "A");
		long b = place(null, "B");
		OpenTransaction<Place> first = holdOpen(() -> this.places.move(CTX, a, new PlaceMoveRequest(1, b, null)));
		Future<Place> second = inBackground(() -> this.places.move(CTX, b, new PlaceMoveRequest(1, a, null)));
		letItBlock();
		first.commit();

		assertThatThrownBy(second::get).cause()
			.isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.getBody().getDetail())
				.isEqualTo("A place cannot be moved into itself or its descendants"));
		assertThat(this.places.get(WS, b).parentId()).isNull();
		assertThat(this.places.get(WS, a).path()).isEqualTo("/" + b + "/" + a + "/");
	}

	@Test
	void waitingTooLongForALockFails() throws Exception {
		long room = place(null, "P101");
		OpenTransaction<Void> first = holdOpen(() -> {
			this.places.delete(CTX, room, 1, null);
			return null;
		});
		Future<Void> second = inBackground(() -> {
			this.places.delete(CTX, room, 1, null);
			return null;
		});

		assertThatThrownBy(() -> second.get(10, TimeUnit.SECONDS)).cause()
			.isInstanceOf(PessimisticLockingFailureException.class);
		first.commit();
	}

	private long place(Long parentId, String name) {
		return this.places.create(CTX, newPlace(parentId, name)).id();
	}

	private PlaceCreateRequest newPlace(Long parentId, String name) {
		return new PlaceCreateRequest(parentId, this.type, null, name, null, null, null, null, null, null);
	}

	private static ItemRequest item(long placeId, String name) {
		return new ItemRequest(placeId, null, null, name, null, null, null, null, null, null, null, null);
	}

}
