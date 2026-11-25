package com.home_platform.registry.person;

/**
 * Một người trong danh sách.
 *
 * @param avatarFileId ảnh đại diện (file bên media), để danh sách hiện ảnh thay chữ cái đầu
 */
public record PersonResponse(long id, String fullName, String phone, String idType, String idNumber, Gender gender,
		Long avatarFileId, int version) {
}
