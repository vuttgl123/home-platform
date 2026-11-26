package com.home_platform.registry.imports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.home_platform.registry.imports.ImportLookupRepository.KnownPerson;
import com.home_platform.registry.person.PersonFields;

/**
 * Nhận ra người được nhắc tới (spec N4): giá trị khớp họ tên (không phân biệt hoa thường), số điện
 * thoại (chuẩn hoá như form) hoặc số giấy tờ. Gồm người đã có và các dòng của sheet Người, kể cả dòng
 * lỗi.
 */
final class PersonIndex {

	sealed interface PersonRef permits Found, Missing, Ambiguous, FailedRow {
	}

	record Found(long id, String name) implements PersonRef {
	}

	record Missing(String value) implements PersonRef {
	}

	record Ambiguous(String value, int count) implements PersonRef {
	}

	record FailedRow(SheetKind sheet, int row) implements PersonRef {
	}

	/** @param row dòng của sheet Người; {@code null} là người đã có */
	private record Entry(Long id, String name, Integer row) {
	}

	private final Map<String, List<Entry>> byName = new HashMap<>();

	private final Map<String, List<Entry>> byPhone = new HashMap<>();

	private final Map<String, List<Entry>> byIdNumber = new HashMap<>();

	private PersonIndex() {
	}

	static PersonIndex of(ImportRun run) {
		return run.shared(PersonIndex.class, () -> {
			PersonIndex index = new PersonIndex();
			for (KnownPerson person : run.knownPeople()) {
				index.put(new Entry(person.id(), person.fullName(), null), person.fullName(), person.phone(),
						person.idNumber());
			}
			return index;
		});
	}

	/**
	 * Ghi một dòng của sheet Người.
	 * @param id {@code null} khi dòng lỗi
	 */
	void addRow(int row, Long id, String fullName, String phone, String idNumber) {
		put(new Entry(id, fullName, row), fullName, phone, idNumber);
	}

	private void put(Entry entry, String fullName, String phone, String idNumber) {
		add(this.byName, ImportText.key(fullName), entry);
		add(this.byPhone, PersonFields.phone(phone), entry);
		add(this.byIdNumber, ImportText.key(idNumber), entry);
	}

	PersonRef resolve(String value) {
		String cleaned = ImportText.clean(value);
		Set<Entry> matches = new LinkedHashSet<>();
		matches.addAll(this.byName.getOrDefault(ImportText.key(cleaned), List.of()));
		matches.addAll(this.byPhone.getOrDefault(PersonFields.phone(cleaned), List.of()));
		matches.addAll(this.byIdNumber.getOrDefault(ImportText.key(cleaned), List.of()));
		if (matches.isEmpty()) {
			return new Missing(cleaned);
		}
		if (matches.size() > 1) {
			return new Ambiguous(cleaned, matches.size());
		}
		Entry entry = matches.iterator().next();
		return (entry.id() != null) ? new Found(entry.id(), entry.name()) : new FailedRow(SheetKind.PERSON, entry.row());
	}

	/** Lời nhắn lỗi khi tham chiếu không dùng được; {@code null} khi tìm thấy. */
	static String problem(PersonRef ref) {
		return switch (ref) {
			case Found found -> null;
			case Missing missing -> "Không tìm thấy người '" + missing.value() + "'";
			case Ambiguous ambiguous -> "Có " + ambiguous.count() + " người khớp '" + ambiguous.value()
					+ "'; ghi số điện thoại hoặc số giấy tờ thay cho tên";
			case FailedRow failed -> "Người ở dòng " + failed.row() + " (sheet " + failed.sheet().sheetName() + ") bị lỗi";
		};
	}

	private static void add(Map<String, List<Entry>> map, String key, Entry entry) {
		if (key != null) {
			map.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
		}
	}

}
