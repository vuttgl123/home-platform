package com.home_platform.registry.imports;

import static com.home_platform.registry.imports.XlsxBuilder.workbook;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.person.PersonRequest;
import com.home_platform.registry.person.PersonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ImportPeopleIT extends ImportTestData {

	@Autowired
	PersonService people;

	@Autowired
	FieldDefService fieldDefs;

	@Test
	void previewListsEveryRowAndWritesNothing() throws Exception {
		int people = count("reg_person");
		int versions = count("reg_entity_version");

		String report = report(preview(workbook().sheet("Người", "Họ tên").row("Nguyễn An").row("Trần Bình").build()));

		assertThat((Boolean) read(report, "$.committed")).isFalse();
		assertThat((List<String>) read(report, "$.sheets[0].items[*].status")).containsExactly("OK", "OK");
		assertThat((Integer) read(report, "$.sheets[0].items[0].row")).isEqualTo(2);
		assertThat((String) read(report, "$.sheets[0].items[0].label")).isEqualTo("Nguyễn An");
		assertThat((String) read(report, "$.sheets[0].sheet")).isEqualTo("PERSON");
		assertThat((String) read(report, "$.sheets[0].name")).isEqualTo("Người");
		assertThat((Integer) read(report, "$.totals.rows")).isEqualTo(2);
		assertThat(count("reg_person")).isEqualTo(people);
		assertThat(count("reg_entity_version")).isEqualTo(versions);
	}

	@Test
	void commitCreatesPeopleWithTheImportNote() throws Exception {
		byte[] file = workbook().sheet("Người", "Họ tên", "Ngày sinh", "Giới tính", "Ghi chú", "Điện thoại")
			.row("Nguyễn An", LocalDate.of(1990, 5, 1), "Nữ", "Thuê dài hạn", "0901 234 567")
			.build();

		String report = report(commit("nguoi.xlsx", file));

		assertThat((Boolean) read(report, "$.committed")).isTrue();
		assertThat((String) read(report, "$.fileName")).isEqualTo("nguoi.xlsx");
		long id = personId("Nguyễn An");
		this.mvc.perform(asA(get("/people/" + id)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.dateOfBirth").value("1990-05-01"))
			.andExpect(jsonPath("$.gender").value("FEMALE"))
			.andExpect(jsonPath("$.note").value("Thuê dài hạn"))
			.andExpect(jsonPath("$.phone").value("0901234567"));
		assertThat(versionNote("PERSON", id, 1)).isEqualTo("Nhập từ Excel: nguoi.xlsx");
	}

	@Test
	void oneErrorImportsNothing() throws Exception {
		int people = count("reg_person");
		byte[] file = workbook().sheet("Người", "Họ tên", "Email").row("Nguyễn An", null).row("Trần Bình", "abc").build();

		String report = report(commit(file));

		assertThat((Boolean) read(report, "$.committed")).isFalse();
		assertThat((String) read(report, "$.sheets[0].items[0].status")).isEqualTo("OK");
		assertThat((String) read(report, "$.sheets[0].items[1].status")).isEqualTo("ERROR");
		assertThat((String) read(report, "$.sheets[0].items[1].messages[0].column")).isEqualTo("Email");
		assertThat((String) read(report, "$.sheets[0].items[1].messages[0].text")).isEqualTo("Email không hợp lệ");
		assertThat((String) read(report, "$.sheets[0].items[1].messages[0].level")).isEqualTo("ERROR");
		assertThat((Integer) read(report, "$.totals.errors")).isEqualTo(1);
		assertThat((Integer) read(report, "$.sheets[0].errors")).isEqualTo(1);
		assertThat(count("reg_person")).isEqualTo(people);
	}

	@Test
	void aFailedRowDoesNotSpoilTheNext() throws Exception {
		person("Lê Cúc", "CCCD", "001");
		byte[] file = workbook().sheet("Người", "Họ tên", "Loại giấy tờ", "Số giấy tờ")
			.row("Lê Cúc 2", "CCCD", "001")
			.row("Trần Bình", null, null)
			.build();

		String report = report(preview(file));

		assertThat((List<String>) read(report, "$.sheets[0].items[*].status")).containsExactly("ERROR", "OK");
	}

	@Test
	void serviceErrorsAreTranslated() throws Exception {
		person("Lê Cúc", "CCCD", "001");
		byte[] file = workbook().sheet("Người", "Họ tên", "Loại giấy tờ", "Số giấy tờ", "Ngày sinh")
			.row("Lê Cúc 2", "CCCD", "001", null)
			.row("Trần Bình", null, "002", null)
			.row("Phạm Dung", null, null, LocalDate.now().plusDays(2))
			.build();

		String report = report(preview(file));

		assertThat((String) read(report, "$.sheets[0].items[0].messages[0].text")).isEqualTo("Đã có người dùng giấy tờ này");
		assertThat((String) read(report, "$.sheets[0].items[1].messages[0].text"))
			.isEqualTo("Loại giấy tờ và Số giấy tờ phải ghi cùng nhau");
		assertThat((String) read(report, "$.sheets[0].items[2].messages[0].text")).isEqualTo("Không được sau hôm nay");
		assertThat((String) read(report, "$.sheets[0].items[2].messages[0].column")).isEqualTo("Ngày sinh");
	}

	@Test
	void duplicateNamesAndPhonesWarnButDoNotBlock() throws Exception {
		person("Nguyễn An", null, null);
		byte[] file = workbook().sheet("Người", "Họ tên", "Điện thoại")
			.row("nguyễn  an", null)
			.row("Trần Bình", "0901234567")
			.row("Trần Bình Hai", "0901.234.567")
			.build();

		String report = report(commit(file));

		assertThat((String) read(report, "$.sheets[0].items[0].status")).isEqualTo("WARNING");
		assertThat((String) read(report, "$.sheets[0].items[0].messages[0].text")).isEqualTo("Đã có người tên 'nguyễn an'");
		assertThat((String) read(report, "$.sheets[0].items[0].messages[0].level")).isEqualTo("WARNING");
		assertThat((String) read(report, "$.sheets[0].items[2].messages[0].text"))
			.isEqualTo("Trùng số điện thoại với dòng 3");
		assertThat((Integer) read(report, "$.totals.warnings")).isEqualTo(2);
		assertThat((Boolean) read(report, "$.committed")).isTrue();
	}

	@Test
	void filledLivingAtColumnGetsANotice() throws Exception {
		String filled = report(preview(workbook().sheet("Người", "Họ tên", "Đang ở").row("Nguyễn An", "P301").build()));
		String empty = report(preview(workbook().sheet("Người", "Họ tên", "Đang ở").row("Nguyễn An", null).build()));

		assertThat((List<String>) read(filled, "$.notices"))
			.containsExactly("Cột Đang ở chỉ để xem, bị bỏ qua; khai cư trú ở sheet Cư trú");
		assertThat((List<String>) read(empty, "$.notices")).isEmpty();
	}

	@Test
	void personAttributesAreRead() throws Exception {
		field("home_town", "Quê quán", FieldDataType.TEXT, null, false);
		field("vaccinated", "Đã tiêm", FieldDataType.BOOLEAN, null, false);
		field("blood", "Nhóm máu", FieldDataType.SELECT, List.of("A", "B", "O"), false);
		field("id_expiry", "Hạn CCCD", FieldDataType.DATE, null, true);
		byte[] file = workbook().sheet("Người", "Họ tên", "Quê quán", "Đã tiêm", "Nhóm máu", "Hạn CCCD")
			.row("Nguyễn An", "Huế", "Có", "b", LocalDate.of(2030, 1, 1))
			.row("Trần Bình", null, "không", "X", null)
			.build();

		String failed = report(preview(file));
		assertThat((List<String>) read(failed, "$.sheets[0].items[1].messages[*].text"))
			.containsExactlyInAnyOrder("Giá trị 'X' không hợp lệ; dùng một trong: A, B, O", "Bắt buộc");
		assertThat((List<String>) read(failed, "$.sheets[0].items[1].messages[*].column"))
			.containsExactlyInAnyOrder("Nhóm máu", "Hạn CCCD");

		String report = report(commit(workbook().sheet("Người", "Họ tên", "Quê quán", "Đã tiêm", "Nhóm máu", "Hạn CCCD")
			.row("Nguyễn An", "Huế", "Có", "b", LocalDate.of(2030, 1, 1))
			.build()));
		assertThat((Boolean) read(report, "$.committed")).isTrue();
		this.mvc.perform(asA(get("/people/" + personId("Nguyễn An"))))
			.andExpect(jsonPath("$.attributes.home_town").value("Huế"))
			.andExpect(jsonPath("$.attributes.vaccinated").value(true))
			.andExpect(jsonPath("$.attributes.blood").value("B"))
			.andExpect(jsonPath("$.attributes.id_expiry").value("2030-01-01"));
	}

	@Test
	void fileErrorsComeBackAsReport() throws Exception {
		String report = report(commit("Họ tên\nNguyễn An\n".getBytes(StandardCharsets.UTF_8)));

		assertThat((List<String>) read(report, "$.fileErrors"))
			.containsExactly("Không đọc được file; hãy lưu lại dưới dạng Excel (.xlsx)");
		assertThat((Boolean) read(report, "$.committed")).isFalse();
		assertThat((List<?>) read(report, "$.sheets")).isEmpty();
	}

	@Test
	void oversizedFileIs413() throws Exception {
		byte[] file = new byte[5 * 1024 * 1024 + 1];

		this.mvc.perform(upload("/imports/preview", "big.xlsx", file, WS_A))
			.andExpect(status().isContentTooLarge());
	}

	private void person(String fullName, String idType, String idNumber) {
		this.people.create(CTX_A,
				new PersonRequest(fullName, null, null, idType, idNumber, null, null, null, Map.of(), null, null));
	}

	private void field(String key, String label, FieldDataType type, List<String> options, boolean required) {
		this.fieldDefs.create(CTX_A,
				new FieldDefCreateRequest(FieldTarget.PERSON, null, key, label, type, options, required, 1, null));
	}

}
