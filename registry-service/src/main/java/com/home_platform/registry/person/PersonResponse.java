package com.home_platform.registry.person;

/** Một người trong danh sách. */
public record PersonResponse(long id, String fullName, String phone, String idType, String idNumber, Gender gender,
		int version) {
}
