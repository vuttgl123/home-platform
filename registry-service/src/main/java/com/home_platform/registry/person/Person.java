package com.home_platform.registry.person;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.home_platform.common.jdbc.JsonColumn;
import com.home_platform.registry.support.PlainNumbers;
import lombok.With;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Một dòng của {@code reg_person}: hồ sơ một người do chủ không gian ghi lại, không phải tài
 * khoản đăng nhập.
 */
@With
public record Person(long workspaceId, long id, String fullName, String phone, String email, String idType,
		String idNumber, LocalDate dateOfBirth, Gender gender, String note,
		@JsonSerialize(contentUsing = PlainNumbers.class) @JsonColumn Map<String, Object> attributes,
		Long avatarFileId, Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version,
		Instant deletedAt) {
}
