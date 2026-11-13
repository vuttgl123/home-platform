package com.home_platform.identity;

import com.home_platform.common.test.PlatformArchitectureRules;
import org.junit.jupiter.api.Test;

/** Quy ước chung của dự án (xem {@link PlatformArchitectureRules}) áp lên code của service này. */
class ArchitectureTests {

	@Test
	void followsPlatformRules() {
		PlatformArchitectureRules.check("com.home_platform.identity");
	}

}
