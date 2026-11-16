package com.home_platform.registry.code;

public record CodeRuleResponse(CodeTarget target, long typeId, String prefix, int digits, boolean yearly,
		boolean builtIn) {

	static CodeRuleResponse of(CodeRuleView view) {
		return new CodeRuleResponse(view.target(), view.typeId(), view.format().prefix(), view.format().digits(),
				view.format().yearly(), view.builtIn());
	}

}
