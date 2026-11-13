package com.home_platform.registry.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeohashTests {

	@Test
	void matchesKnownValues() {
		assertThat(Geohash.encode(57.64911, 10.40744, 11)).isEqualTo("u4pruydqqvj");
		assertThat(Geohash.encode(42.6, -5.6, 5)).isEqualTo("ezs42");
	}

	@Test
	void precisionNineIsAPrefixOfLonger() {
		assertThat(Geohash.encode(21.028511, 105.804817, 12)).startsWith(Geohash.encode(21.028511, 105.804817, 9))
			.hasSize(12);
	}

}
