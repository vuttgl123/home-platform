package com.home_platform.registry.person;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.home_platform.registry.support.PlainNumbers;
import tools.jackson.databind.annotation.JsonSerialize;

/** Nội dung một người ghi vào version; không có cột kiểm toán và {@code version}, như địa điểm. */
public record PersonSnapshot(long id, String fullName, String phone, String email, String idType, String idNumber,
		LocalDate dateOfBirth, Gender gender, String note,
		@JsonSerialize(contentUsing = PlainNumbers.class) Map<String, Object> attributes, Long avatarFileId,
		Instant deletedAt) {

	public static PersonSnapshot of(Person person) {
		return new PersonSnapshot(person.id(), person.fullName(), person.phone(), person.email(), person.idType(),
				person.idNumber(), person.dateOfBirth(), person.gender(), person.note(), person.attributes(),
				person.avatarFileId(), person.deletedAt());
	}

}
