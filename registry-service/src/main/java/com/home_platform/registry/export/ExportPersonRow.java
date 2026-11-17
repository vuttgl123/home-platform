package com.home_platform.registry.export;

import java.time.LocalDate;
import java.util.Map;

import com.home_platform.registry.person.Gender;

/** Một người cho file Excel. */
public record ExportPersonRow(long id, String fullName, String phone, String email, String idType, String idNumber,
		LocalDate dateOfBirth, Gender gender, String note, Map<String, Object> attributes) {

	/** Mốc để đọc các dòng đứng sau dòng này. */
	PersonCursor cursor() {
		return new PersonCursor(this.fullName, this.id);
	}

}
