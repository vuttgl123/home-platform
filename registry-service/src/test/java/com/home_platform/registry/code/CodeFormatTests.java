package com.home_platform.registry.code;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CodeFormatTests {

	@Test
	void plainCodePadsToDigits() {
		CodeFormat format = new CodeFormat("DD", 6, false);
		assertThat(format.code(2026, 1)).isEqualTo("DD-000001");
		assertThat(format.series(2026)).isEqualTo("DD-");
	}

	@Test
	void overflowKeepsAllDigits() {
		assertThat(new CodeFormat("P", 3, false).code(2026, 1000)).isEqualTo("P-1000");
	}

	@Test
	void yearlyPutsTheYearInTheSeries() {
		CodeFormat format = new CodeFormat("TS", 4, true);
		assertThat(format.series(2026)).isEqualTo("TS-2026-");
		assertThat(format.code(2026, 1)).isEqualTo("TS-2026-0001");
		assertThat(format.series(2027)).isEqualTo("TS-2027-");
	}

	@Test
	void ownRuleBeatsSharedBeatsBuiltIn() {
		List<CodeRule> both = List.of(rule(CodeTarget.PLACE, 0, "A"), rule(CodeTarget.PLACE, 7, "P"));
		assertThat(CodeFormat.resolve(both, CodeTarget.PLACE, 7).prefix()).isEqualTo("P");

		List<CodeRule> shared = List.of(rule(CodeTarget.PLACE, 0, "A"), rule(CodeTarget.PLACE, 8, "Q"));
		assertThat(CodeFormat.resolve(shared, CodeTarget.PLACE, 7).prefix()).isEqualTo("A");

		assertThat(CodeFormat.resolve(List.of(), CodeTarget.PLACE, 7)).isEqualTo(new CodeFormat("DD", 6, false));
		assertThat(CodeFormat.resolve(List.of(), CodeTarget.ITEM, 7)).isEqualTo(new CodeFormat("TS", 6, false));
	}

	@Test
	void ruleOfTheOtherTargetIsIgnored() {
		assertThat(CodeFormat.resolve(List.of(rule(CodeTarget.ITEM, 0, "X")), CodeTarget.PLACE, 0).prefix())
			.isEqualTo("DD");
	}

	private static CodeRule rule(CodeTarget target, long typeId, String prefix) {
		return new CodeRule(-1, target, typeId, prefix, 4, false, null, null);
	}

}
