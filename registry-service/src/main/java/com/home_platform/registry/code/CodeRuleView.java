package com.home_platform.registry.code;

/** Một mẫu mã như không gian đang dùng. @param builtIn dạng có sẵn, không gian chưa đặt */
public record CodeRuleView(CodeTarget target, long typeId, CodeFormat format, boolean builtIn) {
}
