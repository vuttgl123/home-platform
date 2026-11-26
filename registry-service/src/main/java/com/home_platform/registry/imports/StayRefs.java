package com.home_platform.registry.imports;

import com.home_platform.registry.imports.PersonIndex.PersonRef;
import com.home_platform.registry.imports.PlaceIndex.PlaceRef;

/**
 * Địa điểm và người của một dòng Cư trú hay Sở hữu (spec N4). "Mã địa điểm" có chữ thì dùng nó; cả
 * hai cột có chữ mà chỉ tới hai nơi khác nhau là lỗi.
 *
 * @param placeId {@code null} khi không dùng được (lỗi đã ghi)
 * @param personId {@code null} khi không dùng được (lỗi đã ghi)
 */
record StayRefs(Long placeId, String placePath, Long personId, String personName) {

	static final String PLACE = "Địa điểm";

	static final String PLACE_CODE = "Mã địa điểm";

	static final String PERSON = "Người";

	static StayRefs read(RowReader reader, ImportRun run) {
		PlaceIndex places = PlaceIndex.of(run);
		String code = reader.text(PLACE_CODE);
		String path = reader.text(PLACE);
		Long placeId = null;
		String placePath = (path != null) ? path : code;
		if (code == null && path == null) {
			reader.error(PLACE, ImportMessages.REQUIRED);
		}
		else {
			String column = (code != null) ? PLACE_CODE : PLACE;
			PlaceRef ref = places.resolve((code != null) ? code : path);
			String problem = PlaceIndex.problem(ref);
			if (problem != null) {
				reader.error(column, problem);
			}
			else {
				PlaceIndex.Found found = (PlaceIndex.Found) ref;
				placeId = found.id();
				placePath = found.path();
				if (code != null && path != null && places.resolve(path) instanceof PlaceIndex.Found other
						&& other.id() != found.id()) {
					reader.error(PLACE_CODE, "Mã địa điểm và Địa điểm chỉ tới hai nơi khác nhau");
					placeId = null;
				}
			}
		}
		String person = reader.requiredText(PERSON);
		Long personId = null;
		String personName = person;
		if (person != null) {
			PersonRef ref = PersonIndex.of(run).resolve(person);
			String problem = PersonIndex.problem(ref);
			if (problem != null) {
				reader.error(PERSON, problem);
			}
			else {
				PersonIndex.Found found = (PersonIndex.Found) ref;
				personId = found.id();
				personName = found.name();
			}
		}
		return new StayRefs(placeId, placePath, personId, personName);
	}

	boolean resolved() {
		return this.placeId != null && this.personId != null;
	}

	/** "người · địa điểm" cho báo cáo. */
	String label() {
		if (this.personName == null) {
			return this.placePath;
		}
		return (this.placePath != null) ? this.personName + " · " + this.placePath : this.personName;
	}

}
