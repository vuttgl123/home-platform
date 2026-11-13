package com.home_platform.registry.person;

import java.time.LocalDate;
import java.util.Map;

import com.home_platform.registry.catalog.Text;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * Tạo một người.
 *
 * @param phone bỏ khoảng trắng và dấu chấm trước khi lưu
 * @param idType loại giấy tờ (ví dụ CCCD, PASSPORT), viết hoa; đi cùng {@code idNumber}
 * @param versionNote ghi chú cho version được tạo
 */
public record PersonRequest(@NotBlank @Size(max = 150) String fullName, @Size(max = 20) String phone,
		@Email @Size(max = 255) String email, @Size(max = 20) String idType, @Size(max = 30) String idNumber,
		@PastOrPresent LocalDate dateOfBirth, Gender gender, @Size(max = 10000) String note,
		Map<String, Object> attributes, Long avatarFileId, @Size(max = 255) String versionNote) {

	public PersonRequest {
		fullName = Text.trimToNull(fullName);
		phone = PersonFields.phone(phone);
		email = Text.trimToNull(email);
		idType = PersonFields.idType(idType);
		idNumber = Text.trimToNull(idNumber);
		note = Text.trimToNull(note);
		versionNote = Text.trimToNull(versionNote);
	}

}
