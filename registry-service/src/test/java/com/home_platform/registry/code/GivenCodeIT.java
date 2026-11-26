package com.home_platform.registry.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.home_platform.common.web.ConflictException;
import com.home_platform.registry.item.Item;
import com.home_platform.registry.item.ItemRequest;
import com.home_platform.registry.item.ItemService;
import com.home_platform.registry.item.ItemTestData;
import com.home_platform.registry.place.Place;
import com.home_platform.registry.place.PlaceCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Lệnh tạo với mã cho trước, chỉ dùng khi nhập từ Excel (spec Đợt 9, N3). */
class GivenCodeIT extends ItemTestData {

	@Autowired
	ItemService items;

	@Test
	void givenCodeIsKept() {
		assertThat(placeWith("P301").code()).isEqualTo("P301");
		assertThat(itemWith("TV-01").code()).isEqualTo("TV-01");
	}

	@Test
	void givenCodeTakenEvenByADeletedRowIsAConflict() {
		Place taken = placeWith("P301");
		this.places.delete(CTX_A, taken.id(), taken.version(), null);

		assertThatThrownBy(() -> placeWith("p301")).isInstanceOf(ConflictException.class)
			.hasMessageContaining("Code p301 is already in use");
		itemWith("Q1");
		assertThatThrownBy(() -> itemWith("Q1")).isInstanceOf(ConflictException.class)
			.hasMessageContaining("Code Q1 is already in use");
	}

	@Test
	void blankGivenCodeIsIssuedAsUsual() throws Exception {
		codeRule("PLACE", this.type, "P", 3, false);

		assertThat(placeWith(null).code()).isEqualTo("P-001");
	}

	@Test
	void issuedCodesSkipAGivenCode() throws Exception {
		codeRule("PLACE", this.type, "P", 3, false);
		placeWith("P-002");

		assertThat(placeWith(null).code()).isEqualTo("P-001");
		assertThat(placeWith(null).code()).isEqualTo("P-003");
	}

	private Place placeWith(String code) {
		return this.places.create(CTX_A,
				new PlaceCreateRequest(null, this.type, "Phòng", null, null, null, null, null, null, null), code);
	}

	private Item itemWith(String code) {
		return this.items.create(CTX_A, new ItemRequest(this.room, null, null, "Tivi", null, null, null, null, null,
				null, null, null, null, null), code);
	}

}
