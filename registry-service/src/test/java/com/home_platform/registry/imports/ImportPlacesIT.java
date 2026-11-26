package com.home_platform.registry.imports;

import static com.home_platform.registry.imports.XlsxBuilder.workbook;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.place.PlaceCreateRequest;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ImportPlacesIT extends ImportTestData {

	private static final String[] HEADERS = { "Mã", "Tên", "Loại", "Sức chứa", "Nằm trong", "Trạng thái" };

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	PlaceService places;

	@Autowired
	FieldDefService fieldDefs;

	long building;

	long room;

	@BeforeEach
	void types() {
		this.building = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("TOA", "Toà", null, 1)).id();
		this.room = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("PHONG", "Phòng", null, 2)).id();
	}

	@Test
	void placesNestByPathAndKeepCodes() throws Exception {
		String report = report(commit(workbook().sheet("Địa điểm", HEADERS)
			.row("TA", "Toà A", "Toà", null, null, null)
			.row(null, "Tầng 3", "toà", null, "Toà A", null)
			.row("P301", "P301", "PHONG", 2, "toà a / tầng 3", "Tạm ngưng")
			.build()));

		assertThat((Boolean) read(report, "$.committed")).isTrue();
		Map<String, Object> p301 = place("P301");
		Map<String, Object> floor = place("Tầng 3");
		Map<String, Object> a = place("Toà A");
		assertThat(p301.get("parent_id")).isEqualTo(floor.get("id"));
		assertThat(floor.get("parent_id")).isEqualTo(a.get("id"));
		assertThat(p301).containsEntry("code", "P301").containsEntry("status", "INACTIVE").containsEntry("capacity", 2);
		assertThat(a).containsEntry("code", "TA");
		assertThat((String) floor.get("code")).isNotBlank();
		assertThat((String) read(report, "$.sheets[0].items[2].label")).isEqualTo("P301 · P301");
		assertThat((String) read(report, "$.sheets[0].items[1].code")).isEqualTo(floor.get("code"));
	}

	@Test
	void childBeforeParentInTheFileStillWorks() throws Exception {
		String report = report(commit(workbook().sheet("Địa điểm", HEADERS)
			.row(null, "Tầng 3", "Toà", null, "Toà A", null)
			.row(null, "Toà A", "Toà", null, null, null)
			.build()));

		assertThat((Boolean) read(report, "$.committed")).isTrue();
		assertThat((List<Integer>) read(report, "$.sheets[0].items[*].row")).containsExactly(2, 3);
		assertThat(place("Tầng 3").get("parent_id")).isEqualTo(place("Toà A").get("id"));
	}

	@Test
	void parentByCodeOfAnExistingPlace() throws Exception {
		long a = existing(null, "Toà A", "TA");

		report(commit(workbook().sheet("Địa điểm", HEADERS).row(null, "Tầng 1", "Toà", null, "ta", null).build()));

		assertThat(place("Tầng 1").get("parent_id")).isEqualTo(a);
	}

	@Test
	void codeErrors() throws Exception {
		existing(null, "Toà A", "TA");

		String report = report(preview(workbook().sheet("Địa điểm", HEADERS)
			.row("TA", "Toà B", "Toà", null, null, null)
			.row("x1", "Toà C", "Toà", null, null, null)
			.row("X1", "Toà D", "Toà", null, null, null)
			.row("M".repeat(51), "Toà E", "Toà", null, null, null)
			.build()));

		assertThat(messages(report, 0)).containsExactly("Mã TA đã có");
		assertThat(messages(report, 1)).isEmpty();
		assertThat(messages(report, 2)).containsExactly("Mã X1 lặp với dòng 3");
		assertThat(messages(report, 3)).containsExactly("Dài quá 50 ký tự");
		assertThat((String) read(report, "$.sheets[0].items[3].messages[0].column")).isEqualTo("Mã");
	}

	@Test
	void referenceErrors() throws Exception {
		long store = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("KHO_CU", "Kho cũ", null, 3)).id();
		this.placeTypes.archive(CTX_A, store);
		existing(null, "Toà A", null);
		existing(null, "Toà A", null);

		String report = report(preview(workbook().sheet("Địa điểm", HEADERS)
			.row(null, "P1", "Kho", null, null, null)
			.row(null, "P2", "Kho cũ", null, null, null)
			.row(null, "P3", "Phòng", null, "Toà Z", null)
			.row(null, "P4", "Phòng", null, "Toà A", null)
			.row(null, "P5", null, null, null, null)
			.build()));

		assertThat(messages(report, 0)).containsExactly("Chưa có loại 'Kho' trong Danh mục");
		assertThat(messages(report, 1)).containsExactly("Loại 'Kho cũ' đang ngừng dùng");
		assertThat(messages(report, 2)).containsExactly("Không tìm thấy địa điểm 'Toà Z'");
		assertThat(messages(report, 3)).containsExactly("Có 2 địa điểm 'Toà A'; ghi mã thay cho đường dẫn");
		assertThat(messages(report, 4)).containsExactly("Bắt buộc");
		assertThat((String) read(report, "$.sheets[0].items[2].messages[0].column")).isEqualTo("Nằm trong");
	}

	@Test
	void childOfAFailedRowFails() throws Exception {
		String report = report(preview(workbook().sheet("Địa điểm", HEADERS)
			.row(null, "Toà A", "Kho", null, null, null)
			.row(null, "Tầng 1", "Toà", null, "Toà A", null)
			.build()));

		assertThat(messages(report, 1)).containsExactly("Địa điểm ở dòng 2 (sheet Địa điểm) bị lỗi");
	}

	@Test
	void cycleIsAnError() throws Exception {
		String report = report(preview(workbook().sheet("Địa điểm", HEADERS)
			.row("A", "Nhà A", "Toà", null, "B", null)
			.row("B", "Nhà B", "Toà", null, "A", null)
			.row(null, "Nhà C", "Toà", null, null, null)
			.build()));

		assertThat(messages(report, 0)).containsExactly("Các dòng chỉ tới nhau thành vòng");
		assertThat(messages(report, 1)).containsExactly("Các dòng chỉ tới nhau thành vòng");
		assertThat(messages(report, 2)).isEmpty();
	}

	@Test
	void existingPathWarns() throws Exception {
		existing(null, "Toà A", null);

		String report = report(commit(workbook().sheet("Địa điểm", HEADERS)
			.row(null, "toà a", "Toà", null, null, null)
			.row(null, "Toà B", "Toà", null, null, null)
			.row(null, "Toà B", "Toà", null, null, null)
			.build()));

		assertThat(messages(report, 0)).containsExactly("Đã có địa điểm 'toà a'");
		assertThat((String) read(report, "$.sheets[0].items[0].status")).isEqualTo("WARNING");
		assertThat(messages(report, 2)).containsExactly("Trùng đường dẫn với dòng 3");
		assertThat((Boolean) read(report, "$.committed")).isTrue();
	}

	@Test
	void placeAttributeOfAnotherTypeIsAnError() throws Exception {
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.PLACE, this.room, "area", "Diện tích",
				FieldDataType.NUMBER, null, null, 1, null));

		String report = report(preview(workbook().sheet("Địa điểm", "Tên", "Loại", "Diện tích")
			.row("Toà A", "Toà", 25)
			.row("P1", "Phòng", 25)
			.build()));

		assertThat(messages(report, 0)).containsExactly("Mục 'Diện tích' không áp cho loại Toà");
		assertThat(messages(report, 1)).isEmpty();
	}

	@Test
	void sharedFieldsAreCheckedWhenTheTypeIsWrong() throws Exception {
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.PLACE, null, "postcode", "Mã bưu chính",
				FieldDataType.TEXT, null, true, 1, null));

		String report = report(preview(workbook().sheet("Địa điểm", "Tên", "Loại", "Mã bưu chính")
			.row("Kho sau", "Kho", null)
			.build()));

		assertThat(messages(report, 0)).containsExactlyInAnyOrder("Chưa có loại 'Kho' trong Danh mục", "Bắt buộc");
		assertThat((List<String>) read(report, "$.sheets[0].items[0].messages[*].column"))
			.containsExactlyInAnyOrder("Loại", "Mã bưu chính");
	}

	@Test
	void tooDeepIsTranslated() throws Exception {
		XlsxBuilder file = workbook().sheet("Địa điểm", HEADERS);
		for (int level = 1; level <= 21; level++) {
			file.row("L" + level, "Cấp " + level, "Toà", null, (level == 1) ? null : "L" + (level - 1), null);
		}

		String report = report(preview(file.build()));

		assertThat(messages(report, 19)).isEmpty();
		assertThat(messages(report, 20)).containsExactly("Cây địa điểm không được sâu quá 20 cấp");
	}

	@Test
	void previewShowsTheCodeThatWouldBeIssued() throws Exception {
		codeRule("PLACE", this.building, "P", 3, false);
		byte[] file = workbook().sheet("Địa điểm", HEADERS).row(null, "Toà A", "Toà", null, null, null).build();

		String first = report(preview(file));
		String second = report(preview(file));

		assertThat((String) read(first, "$.sheets[0].items[0].code")).isEqualTo("P-001");
		assertThat((String) read(first, "$.sheets[0].items[0].label")).isEqualTo("P-001 · Toà A");
		assertThat((String) read(second, "$.sheets[0].items[0].code")).isEqualTo("P-001");
	}

	private long existing(Long parentId, String name, String code) {
		return this.places
			.create(CTX_A, new PlaceCreateRequest(parentId, this.building, name, null, null, null, null, null, null, null),
					code)
			.id();
	}

	private Map<String, Object> place(String name) {
		return this.jdbc
			.sql("SELECT id, parent_id, code, status, capacity FROM reg_place WHERE workspace_id = :ws AND name = :name "
					+ "AND deleted_at IS NULL")
			.param("ws", WS_A)
			.param("name", name)
			.query()
			.singleRow();
	}

	private static List<String> messages(String report, int row) {
		return read(report, "$.sheets[0].items[" + row + "].messages[*].text");
	}

}
