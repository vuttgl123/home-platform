package com.home_platform.registry.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.home_platform.registry.place.PlaceStatus;
import org.junit.jupiter.api.Test;

class PlaceOrderTests {

	@Test
	void siblingsSortByNameWithNumbersAsNumbers() {
		List<ExportPlaceRow> rows = List.of(row(1, "/1/", "P10"), row(2, "/2/", "P2"), row(3, "/3/", "Phòng ăn"),
				row(4, "/4/", "Ánh"));

		assertThat(names(PlaceOrder.treeOrder(rows))).containsExactly("Ánh", "P2", "P10", "Phòng ăn");
	}

	@Test
	void childrenFollowTheirParent() {
		List<ExportPlaceRow> rows = List.of(row(1, "/1/", "Toà B"), row(2, "/2/", "Toà A"), row(3, "/1/3/", "Tầng 2"),
				row(4, "/2/4/", "Tầng 1"), row(5, "/1/5/", "Tầng 1"), row(6, "/1/5/6/", "P101"));

		assertThat(names(PlaceOrder.treeOrder(rows))).containsExactly("Toà A", "Tầng 1", "Toà B", "Tầng 1", "P101",
				"Tầng 2");
	}

	@Test
	void sameNamedSiblingsKeepAStableOrder() {
		List<ExportPlaceRow> rows = new ArrayList<>(List.of(row(7, "/7/", "P1"), row(3, "/3/", "P1"), row(5, "/5/", "P1")));
		Collections.reverse(rows);

		assertThat(PlaceOrder.treeOrder(rows)).extracting(ExportPlaceRow::id).containsExactly(3L, 5L, 7L);
	}

	@Test
	void rowsUnderAMissingParentStandAtTheTop() {
		List<ExportPlaceRow> rows = List.of(row(2, "/9/2/", "Mồ côi"), row(1, "/1/", "Gốc"));

		assertThat(names(PlaceOrder.treeOrder(rows))).containsExactly("Gốc", "Mồ côi");
	}

	private static ExportPlaceRow row(long id, String path, String name) {
		return new ExportPlaceRow(id, path, 1, null, name, PlaceStatus.ACTIVE, null, null, Map.of());
	}

	private static List<String> names(List<ExportPlaceRow> rows) {
		return rows.stream().map(ExportPlaceRow::name).toList();
	}

}
