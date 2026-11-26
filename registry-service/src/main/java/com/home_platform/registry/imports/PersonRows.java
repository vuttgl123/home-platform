package com.home_platform.registry.imports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.home_platform.registry.export.AttributeColumns;
import com.home_platform.registry.export.ExcelLayout;
import com.home_platform.registry.export.ExportLabels;
import com.home_platform.registry.imports.ImportLookupRepository.KnownPerson;
import com.home_platform.registry.person.Gender;
import com.home_platform.registry.person.Person;
import com.home_platform.registry.person.PersonFields;
import com.home_platform.registry.person.PersonRequest;
import com.home_platform.registry.person.PersonService;
import org.springframework.stereotype.Component;

/** Sheet Người (spec N2): mỗi dòng là một lần tạo người như ở form. */
@Component
class PersonRows implements SheetRows {

	static final String LIVING_AT_NOTICE = "Cột Đang ở chỉ để xem, bị bỏ qua; khai cư trú ở sheet Cư trú";

	private static final Map<String, String> COLUMNS = Map.of("fullName", "Họ tên", "phone", "Điện thoại", "email",
			"Email", "idType", "Loại giấy tờ", "idNumber", "Số giấy tờ", "dateOfBirth", "Ngày sinh", "gender",
			"Giới tính", "note", "Ghi chú");

	private final PersonService people;

	private final RowRunner runner;

	PersonRows(PersonService people, RowRunner runner) {
		this.people = people;
		this.runner = runner;
	}

	@Override
	public SheetKind kind() {
		return SheetKind.PERSON;
	}

	@Override
	public List<RowReport> importAll(ImportSheet sheet, ImportRun run) {
		if (sheet.rows().stream().anyMatch(row -> !row.cell(ExcelLayout.LIVING_AT).isBlank())) {
			run.notice(LIVING_AT_NOTICE);
		}
		AttributeColumns attributes = run.attributes(SheetKind.PERSON);
		Map<String, String> columns = Columns.withAttributes(COLUMNS, attributes);
		Set<String> knownNames = new HashSet<>();
		Set<String> knownPhones = new HashSet<>();
		for (KnownPerson person : run.knownPeople()) {
			knownNames.add(ImportText.key(person.fullName()));
			if (person.phone() != null) {
				knownPhones.add(person.phone());
			}
		}
		Map<String, Integer> nameRows = new HashMap<>();
		Map<String, Integer> phoneRows = new HashMap<>();
		PersonIndex index = PersonIndex.of(run);
		List<RowReport> reports = new ArrayList<>();
		for (ImportRow row : sheet.rows()) {
			RowReader reader = new RowReader(row);
			String fullName = reader.requiredText("Họ tên");
			String phone = PersonFields.phone(reader.text("Điện thoại"));
			String idNumber = reader.text("Số giấy tờ");
			PersonRequest request = new PersonRequest(fullName, phone, reader.text("Email"),
					reader.text("Loại giấy tờ"), idNumber, reader.date("Ngày sinh"),
					reader.choice("Giới tính", GENDERS, GENDER_LABELS), reader.text("Ghi chú"),
					AttributeCells.read(reader, attributes, null, null), null, run.versionNote());
			this.runner.validate(request, reader, columns);
			Person created = this.runner.run(reader, columns, () -> this.people.create(run.ctx(), request));
			if (created != null) {
				warnDuplicates(reader, fullName, request.phone(), knownNames, knownPhones, nameRows, phoneRows);
			}
			index.addRow(row.number(), (created != null) ? created.id() : null, fullName, request.phone(),
					request.idNumber());
			reports.add(reader.report(fullName, null));
		}
		return reports;
	}

	/** Cảnh báo "có vẻ đã có" (spec N7): trùng tên hay số điện thoại với người đã có, hoặc với dòng trước. */
	private static void warnDuplicates(RowReader reader, String fullName, String phone, Set<String> knownNames,
			Set<String> knownPhones, Map<String, Integer> nameRows, Map<String, Integer> phoneRows) {
		String nameKey = ImportText.key(fullName);
		if (knownNames.contains(nameKey)) {
			reader.warning("Họ tên", "Đã có người tên '" + fullName + "'");
		}
		else if (nameRows.containsKey(nameKey)) {
			reader.warning("Họ tên", "Trùng tên với dòng " + nameRows.get(nameKey));
		}
		nameRows.putIfAbsent(nameKey, reader.number());
		if (phone == null) {
			return;
		}
		if (knownPhones.contains(phone)) {
			reader.warning("Điện thoại", "Đã có người có số điện thoại " + phone);
		}
		else if (phoneRows.containsKey(phone)) {
			reader.warning("Điện thoại", "Trùng số điện thoại với dòng " + phoneRows.get(phone));
		}
		phoneRows.putIfAbsent(phone, reader.number());
	}

	private static final Map<String, Gender> GENDERS = new LinkedHashMap<>();

	private static final List<String> GENDER_LABELS = new ArrayList<>();

	static {
		for (Gender gender : Gender.values()) {
			GENDERS.put(ImportText.key(ExportLabels.gender(gender)), gender);
			GENDERS.put(ImportText.key(gender.name()), gender);
			GENDER_LABELS.add(ExportLabels.gender(gender));
		}
	}

}
