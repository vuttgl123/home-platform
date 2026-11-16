package com.home_platform.registry.code;

import com.home_platform.registry.support.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cấp mã mới cho một địa điểm hay món đồ.
 *
 * @param version {@code version} đã đọc; lệch thì 409
 * @param note ghi chú của version; bỏ trống thì ghi mã cũ
 */
public record CodeReissueRequest(@NotNull Integer version, @Size(max = 255) String note) {

	public CodeReissueRequest {
		note = Text.trimToNull(note);
	}

	/** Ghi chú của version: của người dùng, nếu không thì mã cũ (hoặc "Code assigned" khi chưa có mã). */
	public String noteFor(String oldCode) {
		if (this.note != null) {
			return this.note;
		}
		return (oldCode != null) ? "Code changed from " + oldCode : CodeIssuer.CODE_ASSIGNED;
	}

}
