package com.home_platform.registry.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PlacePathsTests {

	private final PlacePaths paths = PlacePaths.of(List.of(new PlaceNode(1, null, "Toà A"),
			new PlaceNode(2, 1L, "Tầng 3"), new PlaceNode(3, 2L, "P301"), new PlaceNode(4, null, "Toà B")));

	@Test
	void pathJoinsNamesFromTheRoot() {
		assertThat(this.paths.pathOf(3)).isEqualTo("Toà A / Tầng 3 / P301");
		assertThat(this.paths.pathOf(2)).isEqualTo("Toà A / Tầng 3");
		assertThat(this.paths.pathOf(1)).isEqualTo("Toà A");
	}

	@Test
	void rootHasNoParentPath() {
		assertThat(this.paths.parentPathOf(1)).isEmpty();
		assertThat(this.paths.parentPathOf(4)).isEmpty();
		assertThat(this.paths.parentPathOf(3)).isEqualTo("Toà A / Tầng 3");
	}

	@Test
	void unknownIdIsEmpty() {
		assertThat(this.paths.pathOf(99)).isEmpty();
		assertThat(this.paths.parentPathOf(99)).isEmpty();
	}

	/** Cha không có trong danh sách: đường dẫn bắt đầu từ chính địa điểm đó. */
	@Test
	void missingParentStartsAtTheNode() {
		PlacePaths orphan = PlacePaths.of(List.of(new PlaceNode(5, 4L, "Lạc")));

		assertThat(orphan.pathOf(5)).isEqualTo("Lạc");
		assertThat(orphan.parentPathOf(5)).isEmpty();
	}

	/** Dữ liệu hỏng tạo vòng cha con: vẫn trả về, không lặp vô hạn. */
	@Test
	void aCycleEndsInsteadOfLooping() {
		PlacePaths cycle = PlacePaths.of(List.of(new PlaceNode(1, 2L, "A"), new PlaceNode(2, 1L, "B")));

		assertThat(cycle.pathOf(1)).contains("A");
	}

}
