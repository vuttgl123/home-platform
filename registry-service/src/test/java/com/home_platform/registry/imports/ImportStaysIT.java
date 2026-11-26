package com.home_platform.registry.imports;

import static com.home_platform.registry.imports.XlsxBuilder.workbook;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Date;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;

import com.home_platform.registry.person.PersonRequest;
import com.home_platform.registry.person.PersonService;
import com.home_platform.registry.place.PlaceCreateRequest;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.placetype.PlaceTypeCreateRequest;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ImportStaysIT extends ImportTestData {

	private static final String[] STAYS = { "Địa điểm", "Mã địa điểm", "Người", "Quan hệ", "Từ ngày", "Đến ngày" };

	private static final String[] OWNERSHIPS = { "Địa điểm", "Mã địa điểm", "Người", "Tỷ lệ (%)", "Từ ngày",
			"Số giấy tờ" };

	@Autowired
	PlaceTypeService placeTypes;

	@Autowired
	PlaceService places;

	@Autowired
	PersonService people;

	long type;

	long p101;

	@BeforeEach
	void data() {
		this.type = this.placeTypes.create(CTX_A, new PlaceTypeCreateRequest("PHONG", "Phòng", null, 1)).id();
		this.p101 = this.places
			.create(CTX_A, new PlaceCreateRequest(null, this.type, "P101", null, null, null, null, null, null, null),
					"P101")
			.id();
	}

	@Test
	void staysAndOwnershipsLinkFileRowsAndExistingRecords() throws Exception {
		long cuc = person("Lê Cúc", null);

		String report = report(commit(workbook().sheet("Địa điểm", "Mã", "Tên", "Loại", "Nằm trong")
			.row("TA", "Toà A", "Phòng", null)
			.row("P301", "P301", "Phòng", "TA")
			.sheet("Người", "Họ tên")
			.row("Trần Bình")
			.sheet("Cư trú", STAYS)
			.row("Toà A / P301", null, "Trần Bình", null, "01/03/2026", null)
			.row(null, "P101", "Lê Cúc", "Khách", "01/02/2026", "28/02/2026")
			.sheet("Sở hữu", OWNERSHIPS)
			.row(null, "P301", "Trần Bình", 50, "01/01/2026", "GCN-1")
			.build()));

		assertThat((Boolean) read(report, "$.committed")).isTrue();
		long binh = personId("Trần Bình");
		long p301 = placeId("P301");
		List<Map<String, Object>> stays = rows("SELECT place_id, person_id, relation, start_date, end_date "
				+ "FROM reg_occupancy WHERE workspace_id = :ws ORDER BY start_date DESC");
		assertThat(stays).hasSize(2);
		assertThat(stays.get(0)).containsEntry("place_id", p301)
			.containsEntry("person_id", binh)
			.containsEntry("relation", "RESIDENT")
			.containsEntry("start_date", Date.valueOf("2026-03-01"));
		assertThat(stays.get(1)).containsEntry("place_id", this.p101)
			.containsEntry("person_id", cuc)
			.containsEntry("relation", "GUEST")
			.containsEntry("end_date", Date.valueOf("2026-02-28"));
		Map<String, Object> ownership = rows("SELECT place_id, person_id, share_percent, document_no FROM reg_ownership "
				+ "WHERE workspace_id = :ws").getFirst();
		assertThat(ownership).containsEntry("place_id", p301).containsEntry("person_id", binh)
			.containsEntry("document_no", "GCN-1");
		assertThat((BigDecimal) ownership.get("share_percent")).isEqualByComparingTo("50");
		assertThat((String) read(report, "$.sheets[2].items[0].label")).isEqualTo("Trần Bình · Toà A / P301");
	}

	@Test
	void ownerRelationIsRejected() throws Exception {
		person("Lê Cúc", null);

		String report = report(preview(workbook().sheet("Cư trú", STAYS)
			.row(null, "P101", "Lê Cúc", "Chủ", "01/02/2026", null)
			.row(null, "P101", "Lê Cúc", "OWNER", "01/03/2026", null)
			.build()));

		assertThat(messages(report, 0)).containsExactly("Quan hệ Chủ đã bỏ; ghi người này ở sheet Sở hữu");
		assertThat(messages(report, 1)).containsExactly("Quan hệ Chủ đã bỏ; ghi người này ở sheet Sở hữu");
	}

	@Test
	void personIsFoundByANumericPhoneCell() throws Exception {
		String report = report(commit(workbook().sheet("Người", "Họ tên", "Điện thoại")
			.row("Trần Bình", 901234567)
			.sheet("Cư trú", STAYS)
			.row(null, "P101", 901234567, null, "01/03/2026", null)
			.build()));

		assertThat((Boolean) read(report, "$.committed")).isTrue();
		long binh = personId("Trần Bình");
		assertThat(rows("SELECT person_id FROM reg_occupancy WHERE workspace_id = :ws").getFirst())
			.containsEntry("person_id", binh);
		assertThat(rows("SELECT phone FROM reg_person WHERE workspace_id = :ws").getFirst())
			.containsEntry("phone", "901234567");
	}

	@Test
	void composedNamesMatch() throws Exception {
		long binh = person("Trần Bình", null);

		report(commit(workbook().sheet("Cư trú", STAYS)
			.row(null, "P101", Normalizer.normalize("Trần Bình", Normalizer.Form.NFD), null, "01/03/2026", null)
			.build()));

		assertThat(rows("SELECT person_id FROM reg_occupancy WHERE workspace_id = :ws").getFirst())
			.containsEntry("person_id", binh);
	}

	@Test
	void ambiguousPersonAsksForPhoneOrDocument() throws Exception {
		person("Nguyễn An", "0901000001");
		long second = person("Nguyễn An", "0901000002");

		String report = report(preview(workbook().sheet("Cư trú", STAYS)
			.row(null, "P101", "Nguyễn An", null, "01/03/2026", null)
			.row(null, "P101", "0901000002", null, "01/03/2026", null)
			.row(null, "P101", "Phạm Dung", null, "01/03/2026", null)
			.build()));

		assertThat(messages(report, 0))
			.containsExactly("Có 2 người khớp 'Nguyễn An'; ghi số điện thoại hoặc số giấy tờ thay cho tên");
		assertThat((String) read(report, "$.sheets[0].items[0].messages[0].column")).isEqualTo("Người");
		assertThat(messages(report, 1)).isEmpty();
		assertThat(messages(report, 2)).containsExactly("Không tìm thấy người 'Phạm Dung'");
		assertThat(second).isPositive();
	}

	@Test
	void codeAndPathMustAgree() throws Exception {
		person("Lê Cúc", null);

		String report = report(preview(workbook().sheet("Địa điểm", "Mã", "Tên", "Loại")
			.row("P301", "P301", "Phòng")
			.sheet("Cư trú", STAYS)
			.row("P101", "P301", "Lê Cúc", null, "01/03/2026", null)
			.build()));

		assertThat(messages(report, 1, 0)).containsExactly("Mã địa điểm và Địa điểm chỉ tới hai nơi khác nhau");
	}

	@Test
	void overlapsInTheFileAreCaught() throws Exception {
		person("Lê Cúc", null);

		String report = report(preview(workbook().sheet("Cư trú", STAYS)
			.row(null, "P101", "Lê Cúc", null, "01/01/2026", "30/06/2026")
			.row(null, "P101", "Lê Cúc", null, "01/03/2026", null)
			.build()));

		assertThat(messages(report, 0)).isEmpty();
		assertThat(messages(report, 1)).containsExactly("Người này đã có một lần ở chồng ngày tại địa điểm này");
	}

	@Test
	void sharesOverAHundredAreCaught() throws Exception {
		person("Lê Cúc", null);
		person("Trần Bình", null);

		String report = report(preview(workbook().sheet("Sở hữu", OWNERSHIPS)
			.row(null, "P101", "Lê Cúc", 60, "01/01/2026", null)
			.row(null, "P101", "Trần Bình", 50, "01/01/2026", null)
			.row(null, "P101", "Trần Bình", "50,555", "01/01/2027", null)
			.build()));

		assertThat(messages(report, 1)).containsExactly("Tổng tỷ lệ sở hữu của địa điểm sẽ là 110% vào ngày 01/01/2026");
		assertThat(messages(report, 2)).containsExactly("Tối đa 2 chữ số thập phân");
	}

	@Test
	void endBeforeStartAndMissingStart() throws Exception {
		person("Lê Cúc", null);

		String report = report(preview(workbook().sheet("Cư trú", STAYS)
			.row(null, "P101", "Lê Cúc", null, "01/03/2026", "01/02/2026")
			.row(null, "P101", "Lê Cúc", null, null, null)
			.build()));

		assertThat(messages(report, 0)).containsExactly("Đến ngày không được trước Từ ngày");
		assertThat(messages(report, 1)).containsExactly("Bắt buộc");
		assertThat((String) read(report, "$.sheets[0].items[1].messages[0].column")).isEqualTo("Từ ngày");
	}

	@Test
	void rowOfAFailedPersonFails() throws Exception {
		String report = report(preview(workbook().sheet("Người", "Họ tên", "Email")
			.row("Trần Bình", "abc")
			.sheet("Cư trú", STAYS)
			.row(null, "P101", "Trần Bình", null, "01/03/2026", null)
			.build()));

		assertThat(messages(report, 1, 0)).containsExactly("Người ở dòng 2 (sheet Người) bị lỗi");
	}

	private long person(String fullName, String phone) {
		return this.people
			.create(CTX_A, new PersonRequest(fullName, phone, null, null, null, null, null, null, Map.of(), null, null))
			.id();
	}

	private long placeId(String code) {
		return this.jdbc.sql("SELECT id FROM reg_place WHERE workspace_id = :ws AND code = :code")
			.param("ws", WS_A)
			.param("code", code)
			.query(Long.class)
			.single();
	}

	private List<Map<String, Object>> rows(String sql) {
		return this.jdbc.sql(sql).param("ws", WS_A).query().listOfRows();
	}

	private static List<String> messages(String report, int row) {
		return messages(report, 0, row);
	}

	private static List<String> messages(String report, int sheet, int row) {
		return read(report, "$.sheets[" + sheet + "].items[" + row + "].messages[*].text");
	}

}
