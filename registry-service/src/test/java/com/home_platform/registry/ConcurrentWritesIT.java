package com.home_platform.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.web.ConflictException;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.common.web.NotFoundException;
import com.home_platform.registry.fielddef.AttributeValidation;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.item.Item;
import com.home_platform.registry.item.ItemRequest;
import com.home_platform.registry.item.ItemService;
import com.home_platform.registry.occupancy.Occupancy;
import com.home_platform.registry.occupancy.OccupancyCreateRequest;
import com.home_platform.registry.occupancy.OccupancyService;
import com.home_platform.registry.occupancy.Relation;
import com.home_platform.registry.ownership.Ownership;
import com.home_platform.registry.ownership.OwnershipCreateRequest;
import com.home_platform.registry.ownership.OwnershipService;
import com.home_platform.registry.person.Person;
import com.home_platform.registry.person.PersonRequest;
import com.home_platform.registry.person.PersonService;
import com.home_platform.registry.place.Place;
import com.home_platform.registry.place.PlaceCreateRequest;
import com.home_platform.registry.place.PlaceMoveRequest;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import com.home_platform.registry.support.BusinessDate;
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

	@Autowired
	PersonService people;

	@Autowired
	OccupancyService occupancies;

	@Autowired
	BusinessDate businessDate;

	@Autowired
	FieldDefService fieldDefs;

	@Autowired
	OwnershipService ownerships;

	@Autowired
	AttributeValidation attributeValidation;

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
				.contains("0 child places, 0 occupancies, 0 ownerships and 1 items"));
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

	@Test
	void itemAddedDuringAPlaceDeleteIsRefused() throws Exception {
		long room = place(null, "P101");
		OpenTransaction<Void> deleting = holdOpen(() -> {
			this.places.delete(CTX, room, 1, null);
			return null;
		});
		Future<Item> adding = inBackground(() -> this.items.create(CTX, item(room, "Tủ")));
		letItBlock();
		deleting.commit();

		assertThatThrownBy(adding::get).cause().isInstanceOf(NotFoundException.class);
		assertThat(rows("reg_item")).isZero();
	}

	@Test
	void occupancyAddedDuringAPersonDeleteIsRefused() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		OpenTransaction<Void> deleting = holdOpen(() -> {
			this.people.delete(CTX, an, 1, null);
			return null;
		});
		Future<Occupancy> adding = inBackground(() -> this.occupancies.create(CTX, stay(room, an)));
		letItBlock();
		deleting.commit();

		assertThatThrownBy(adding::get).cause().isInstanceOf(NotFoundException.class);
		assertThat(rows("reg_occupancy")).isZero();
	}

	@Test
	void personDeleteSeesAnOccupancyAddedConcurrently() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		OpenTransaction<Occupancy> adding = holdOpen(() -> this.occupancies.create(CTX, stay(room, an)));
		Future<Void> deleting = inBackground(() -> {
			this.people.delete(CTX, an, 1, null);
			return null;
		});
		letItBlock();
		adding.commit();

		assertThatThrownBy(deleting::get).cause()
			.isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.getBody().getDetail())
				.contains("1 current or upcoming occupancies"));
		assertThat(this.people.get(WS, an).version()).isEqualTo(1);
	}

	@Test
	void overlappingOccupanciesCreatedTogetherLeaveOne() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		OpenTransaction<Occupancy> first = holdOpen(() -> this.occupancies.create(CTX, stay(room, an)));
		Future<Occupancy> second = inBackground(() -> this.occupancies.create(CTX, stay(room, an)));
		letItBlock();
		first.commit();

		assertThatThrownBy(second::get).cause()
			.isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.getBody().getDetail())
				.contains("already has an overlapping occupancy"));
		assertThat(rows("reg_occupancy")).isEqualTo(1);
	}

	@Test
	void personDeleteSeesAnItemAssignedConcurrently() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		OpenTransaction<Item> adding = holdOpen(() -> this.items.create(CTX, owned(room, an, "Tủ")));
		Future<Void> deleting = inBackground(() -> {
			this.people.delete(CTX, an, 1, null);
			return null;
		});
		letItBlock();
		adding.commit();

		assertThatThrownBy(deleting::get).cause()
			.isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.getBody().getDetail())
				.contains("and 1 items"));
		assertThat(this.people.get(WS, an).version()).isEqualTo(1);
	}

	@Test
	void itemAssignedDuringAPersonDeleteIsRefused() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		OpenTransaction<Void> deleting = holdOpen(() -> {
			this.people.delete(CTX, an, 1, null);
			return null;
		});
		Future<Item> adding = inBackground(() -> this.items.create(CTX, owned(room, an, "Tủ")));
		letItBlock();
		deleting.commit();

		assertThatThrownBy(adding::get).cause().isInstanceOf(NotFoundException.class);
		assertThat(rows("reg_item")).isZero();
	}

	@Test
	void twoOwnersAddedTogetherCannotExceedOneHundredPercent() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		long binh = person("Bình");
		OpenTransaction<Ownership> first = holdOpen(() -> this.ownerships.create(CTX, owns(room, an, "60")));
		Future<Ownership> second = inBackground(() -> this.ownerships.create(CTX, owns(room, binh, "60")));
		letItBlock();
		first.commit();

		assertThatThrownBy(second::get).cause()
			.isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.getBody().getDetail())
				.contains("would total 120%"));
		assertThat(rows("reg_ownership")).isEqualTo(1);
	}

	@Test
	void placeDeleteSeesAnOwnershipAddedConcurrently() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		OpenTransaction<Ownership> adding = holdOpen(() -> this.ownerships.create(CTX, owns(room, an, null)));
		Future<Void> deleting = inBackground(() -> {
			this.places.delete(CTX, room, 1, null);
			return null;
		});
		letItBlock();
		adding.commit();

		assertThatThrownBy(deleting::get).cause()
			.isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.getBody().getDetail())
				.contains("1 ownerships"));
	}

	@Test
	void ownershipAddedDuringAPersonDeleteIsRefused() throws Exception {
		long room = place(null, "P101");
		long an = person("An");
		OpenTransaction<Void> deleting = holdOpen(() -> {
			this.people.delete(CTX, an, 1, null);
			return null;
		});
		Future<Ownership> adding = inBackground(() -> this.ownerships.create(CTX, owns(room, an, null)));
		letItBlock();
		deleting.commit();

		assertThatThrownBy(adding::get).cause().isInstanceOf(NotFoundException.class);
		assertThat(rows("reg_ownership")).isZero();
	}

	@Test
	void sameDocumentCreatedTogetherLeavesOnePerson() throws Exception {
		OpenTransaction<Person> first = holdOpen(() -> this.people.create(CTX, withDocument("An")));
		Future<Person> second = inBackground(() -> this.people.create(CTX, withDocument("Bình")));
		letItBlock();
		first.commit();

		assertThatThrownBy(second::get).cause()
			.isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.getBody().getDetail())
				.isEqualTo("Another person already has this document"));
		assertThat(rows("reg_person")).isEqualTo(1);
	}

	/**
	 * Một transaction đã chốt snapshot, rồi danh mục đổi và commit, rồi transaction đó mới nạp cache:
	 * cache phải nhận bản mới, không phải bản của snapshot cũ.
	 */
	@Test
	void catalogChangeCommittedMidTransactionIsNotCachedStale() throws Exception {
		long room = place(null, "P101");
		Map<String, Object> floor = Map.of("floor_no", 3);

		inTransaction(() -> {
			this.places.get(WS, room); // SELECT thường: chốt snapshot của transaction này
			inBackground(() -> this.fieldDefs.create(CTX, new FieldDefCreateRequest(FieldTarget.PLACE, 0L, "floor_no",
					"Tầng", FieldDataType.NUMBER, null, false, 10)))
				.get();
			return this.attributeValidation.validate(WS, FieldTarget.PLACE, this.type, floor);
		});

		assertThat(this.attributeValidation.validate(WS, FieldTarget.PLACE, this.type, floor)).containsEntry("floor_no", 3);
	}

	private long place(Long parentId, String name) {
		return this.places.create(CTX, newPlace(parentId, name)).id();
	}

	private PlaceCreateRequest newPlace(Long parentId, String name) {
		return new PlaceCreateRequest(parentId, this.type, name, null, null, null, null, null, null, null);
	}

	private long person(String fullName) {
		return this.people
			.create(CTX, new PersonRequest(fullName, null, null, null, null, null, null, null, null, null, null))
			.id();
	}

	private static PersonRequest withDocument(String fullName) {
		return new PersonRequest(fullName, null, null, "CCCD", "001", null, null, null, null, null, null);
	}

	/** Cư trú từ hôm nay, chưa có ngày cuối. */
	private OccupancyCreateRequest stay(long placeId, long personId) {
		return new OccupancyCreateRequest(placeId, personId, Relation.RESIDENT, this.businessDate.today(), null, null);
	}

	private OwnershipCreateRequest owns(long placeId, long personId, String share) {
		return new OwnershipCreateRequest(placeId, personId, (share != null) ? new java.math.BigDecimal(share) : null,
				this.businessDate.today(), null, null, null);
	}

	private static ItemRequest owned(long placeId, long ownerId, String name) {
		return new ItemRequest(placeId, ownerId, null, name, null, null, null, null, null, null, null, null, null, null);
	}

	private static ItemRequest item(long placeId, String name) {
		return new ItemRequest(placeId, null, null, name, null, null, null, null, null, null, null, null, null, null);
	}

}
