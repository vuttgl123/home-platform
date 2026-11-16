package com.home_platform.registry.code;

import java.time.Instant;

/** Một dòng của {@code reg_code_rule}: mẫu mã của một đích, chung ({@code typeId} 0) hoặc của một loại, nhóm. */
public record CodeRule(long workspaceId, CodeTarget target, long typeId, String prefix, int digits, boolean yearly,
		Instant createdAt, Instant updatedAt) {

	public CodeFormat format() {
		return new CodeFormat(this.prefix, this.digits, this.yearly);
	}

}
