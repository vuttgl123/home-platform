package com.home_platform.registry.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.home_platform.registry.item.ItemStatus;
import com.home_platform.registry.occupancy.Relation;
import com.home_platform.registry.person.Gender;
import com.home_platform.registry.place.PlaceStatus;
import org.junit.jupiter.api.Test;

/** Nhãn trong file phải trùng nhãn trên giao diện. */
class ExportLabelsTests {

	@Test
	void itemStatuses() {
		assertThat(ExportLabels.itemStatus(ItemStatus.IN_USE)).isEqualTo("Đang dùng");
		assertThat(ExportLabels.itemStatus(ItemStatus.BROKEN)).isEqualTo("Hỏng");
		assertThat(ExportLabels.itemStatus(ItemStatus.STORED)).isEqualTo("Cất kho");
		assertThat(ExportLabels.itemStatus(ItemStatus.REMOVED)).isEqualTo("Đã gỡ");
	}

	@Test
	void placeStatuses() {
		assertThat(ExportLabels.placeStatus(PlaceStatus.ACTIVE)).isEqualTo("Đang dùng");
		assertThat(ExportLabels.placeStatus(PlaceStatus.INACTIVE)).isEqualTo("Tạm ngưng");
	}

	@Test
	void relations() {
		assertThat(ExportLabels.relation(Relation.OWNER)).isEqualTo("Chủ");
		assertThat(ExportLabels.relation(Relation.RESIDENT)).isEqualTo("Người ở");
		assertThat(ExportLabels.relation(Relation.GUEST)).isEqualTo("Khách");
		assertThat(ExportLabels.relation(Relation.STAFF)).isEqualTo("Nhân viên");
		assertThat(ExportLabels.relation(Relation.OTHER)).isEqualTo("Khác");
	}

	@Test
	void genders() {
		assertThat(ExportLabels.gender(Gender.MALE)).isEqualTo("Nam");
		assertThat(ExportLabels.gender(Gender.FEMALE)).isEqualTo("Nữ");
		assertThat(ExportLabels.gender(Gender.OTHER)).isEqualTo("Khác");
		assertThat(ExportLabels.gender(null)).isNull();
	}

	@Test
	void yesNo() {
		assertThat(ExportLabels.yesNo(true)).isEqualTo("Có");
		assertThat(ExportLabels.yesNo(false)).isEqualTo("Không");
	}

}
