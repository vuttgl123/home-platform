package com.home_platform.registry.imports;

import static com.home_platform.registry.imports.XlsxBuilder.workbook;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import com.home_platform.registry.itemcategory.ItemCategoryRequest;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thời gian xem trước một file đủ {@value WorkbookReader#MAX_ROWS} dòng (spec Đợt 9, mục 4.2: dưới 60
 * giây trên máy dev). Chậm, nên chỉ chạy khi có {@code -Dimport.timing=true}. Transaction của test dài
 * hơn mặc định 30 giây, và log SQL tắt: in hàng chục nghìn câu SQL qua kênh của failsafe làm test treo.
 */
@EnabledIfSystemProperty(named = "import.timing", matches = "true")
@Transactional(timeout = 300)
@TestPropertySource(properties = "logging.level.org.springframework.jdbc.core.JdbcTemplate=INFO")
class ImportTimingIT extends ImportTestData {

	private static final Logger log = LoggerFactory.getLogger(ImportTimingIT.class);

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	ItemCategoryService categories;

	@Test
	void fullFilePreviewsInUnderAMinute() throws Exception {
		this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("TOA", "Toà", null, 1));
		this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("PHONG", "Phòng", null, 2));
		this.categories.create(CTX_A, new ItemCategoryRequest("Điện máy", 1));
		XlsxBuilder file = workbook().sheet("Địa điểm", "Mã", "Tên", "Loại", "Nằm trong");
		for (int building = 1; building <= 6; building++) {
			file.row("T" + building, "Toà " + building, "Toà", null);
			for (int room = 1; room <= 99; room++) {
				file.row("T" + building + "-" + room, "Phòng " + room, "Phòng", "T" + building);
			}
		}
		file.sheet("Người", "Họ tên", "Điện thoại");
		for (int person = 1; person <= 1_200; person++) {
			file.row("Người số " + person, "09" + String.format("%08d", person));
		}
		file.sheet("Đồ đạc", "Tên", "Nhóm", "Ở đâu", "Chủ");
		for (int item = 1; item <= 1_200; item++) {
			int building = 1 + item % 6;
			int room = 1 + item % 99;
			file.row("Món " + item, "Điện máy", "Toà " + building + " / Phòng " + room, "Người số " + (1 + item % 1_200));
		}
		byte[] bytes = file.build();

		long started = System.nanoTime();
		String report = report(preview(bytes));
		Duration took = Duration.ofNanos(System.nanoTime() - started);

		log.info("Previewing {} rows took {} ms", WorkbookReader.MAX_ROWS, took.toMillis());
		assertThat((Integer) read(report, "$.totals.rows")).isEqualTo(WorkbookReader.MAX_ROWS);
		assertThat((Integer) read(report, "$.totals.errors")).isZero();
		assertThat(took).isLessThan(Duration.ofSeconds(60));
	}

}
