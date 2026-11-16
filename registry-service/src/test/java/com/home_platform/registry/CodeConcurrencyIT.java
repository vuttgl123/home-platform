package com.home_platform.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.concurrent.Future;

import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.registry.code.CodeBackfillResponse;
import com.home_platform.registry.code.CodeBackfillService;
import com.home_platform.registry.place.AddressRequest;
import com.home_platform.registry.place.Place;
import com.home_platform.registry.place.PlaceCreateRequest;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Cấp mã khi nhiều lệnh chạy cùng lúc, và khi một lệnh hỏng. Dữ liệu được commit thật. */
class CodeConcurrencyIT extends CommittedDataIT {

	@Autowired
	PlaceService places;

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	CodeBackfillService backfill;

	@Autowired
	EntityHistory history;

	long type;

	@BeforeEach
	void placeType() {
		this.type = this.placeTypes.create(CTX, new PlaceTypeCreateRequest("T", "T", null, null)).id();
	}

	@Test
	void twoCreatesInOneSeriesGetConsecutiveCodes() throws Exception {
		OpenTransaction<Place> first = holdOpen(() -> this.places.create(CTX, newPlace("A", null)));
		Future<Place> second = inBackground(() -> this.places.create(CTX, newPlace("B", null)));
		letItBlock();
		first.commit();

		assertThat(first.result().code()).isEqualTo("DD-000001");
		assertThat(second.get().code()).isEqualTo("DD-000002");
		assertThat(rows("reg_place")).isEqualTo(2);
	}

	@Test
	void failedCreateGivesItsNumberBack() throws Exception {
		assertThat(this.places.create(CTX, newPlace("A", null)).code()).isEqualTo("DD-000001");
		AddressRequest latitudeOnly = new AddressRequest(null, null, null, null, null, null, null, null, "x",
				new BigDecimal("21.0"), null);
		assertThatThrownBy(() -> this.places.create(CTX, newPlace("B", latitudeOnly)))
			.isInstanceOf(BadRequestException.class);

		assertThat(this.places.create(CTX, newPlace("C", null)).code()).isEqualTo("DD-000002");
	}

	@Test
	void twoBackfillsAtOnceAssignEachRecordOnce() throws Exception {
		for (long id = 9100; id < 9103; id++) {
			this.jdbc.sql("INSERT INTO reg_place (workspace_id, id, place_type_id, path, name, created_by, updated_by) "
					+ "VALUES (:ws, :id, :type, :path, 'old', :user, :user)")
				.param("ws", WS).param("id", id).param("type", this.type).param("path", "/" + id + "/")
				.param("user", USER).update();
		}

		OpenTransaction<CodeBackfillResponse> first = holdOpen(() -> this.backfill.backfill(CTX));
		Future<CodeBackfillResponse> second = inBackground(() -> this.backfill.backfill(CTX));
		letItBlock();
		first.commit();

		assertThat(first.result().placesAssigned() + second.get().placesAssigned()).isEqualTo(3);
		assertThat(this.jdbc.sql("SELECT COUNT(DISTINCT code) FROM reg_place WHERE workspace_id = :ws AND code IS NOT NULL")
			.param("ws", WS).query(Integer.class).single()).isEqualTo(3);
		for (long id = 9100; id < 9103; id++) {
			assertThat(this.history.list(WS, "PLACE", id, new CursorRequest(null, 10)).items()).hasSize(1);
		}
	}

	private PlaceCreateRequest newPlace(String name, AddressRequest address) {
		return new PlaceCreateRequest(null, this.type, name, null, null, null, null, address, null);
	}

}
