package com.home_platform.common.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class TimeSortedIdGeneratorTests {

	private static final Instant T = Instant.parse("2026-11-12T02:00:00Z");

	@Test
	void idsArePositiveAndStrictlyIncreasing() {
		IdGenerator generator = new TimeSortedIdGenerator(1);
		long previous = 0;
		for (int i = 0; i < 10_000; i++) {
			long id = generator.nextId();
			assertThat(id).isGreaterThan(previous);
			previous = id;
		}
	}

	@Test
	void idsAreUniqueAcrossThreads() throws Exception {
		IdGenerator generator = new TimeSortedIdGenerator(1);
		int threads = 8;
		int perThread = 20_000;
		Set<Long> ids = ConcurrentHashMap.newKeySet();
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			List<Future<?>> futures = new ArrayList<>();
			for (int t = 0; t < threads; t++) {
				futures.add(pool.submit(() -> {
					for (int i = 0; i < perThread; i++) {
						ids.add(generator.nextId());
					}
				}));
			}
			for (Future<?> future : futures) {
				future.get(30, TimeUnit.SECONDS);
			}
		}
		finally {
			pool.shutdownNow();
		}
		assertThat(ids).hasSize(threads * perThread);
	}

	@Test
	void idEmbedsCreationTimeAndNodeId() {
		IdGenerator generator = new TimeSortedIdGenerator(5, Clock.fixed(T, ZoneOffset.UTC));
		long id = generator.nextId();
		assertThat((id >> 22) + TimeSortedIdGenerator.EPOCH_MILLIS).isEqualTo(T.toEpochMilli());
		assertThat((id >> 12) & 0x3FF).isEqualTo(5L);
	}

	@Test
	void sameInstantOnTwoNodesGivesDifferentIds() {
		Clock clock = Clock.fixed(T, ZoneOffset.UTC);
		assertThat(new TimeSortedIdGenerator(1, clock).nextId()).isNotEqualTo(new TimeSortedIdGenerator(2, clock).nextId());
	}

	@Test
	void staysIncreasingWhenClockGoesBackwards() {
		MutableClock clock = new MutableClock(T);
		IdGenerator generator = new TimeSortedIdGenerator(1, clock);
		long before = generator.nextId();
		clock.now = T.minusSeconds(60);
		assertThat(generator.nextId()).isGreaterThan(before);
	}

	@Test
	void moreIdsThanOneMillisecondHoldsStayIncreasing() {
		IdGenerator generator = new TimeSortedIdGenerator(1, Clock.fixed(T, ZoneOffset.UTC));
		long previous = 0;
		for (int i = 0; i < 10_000; i++) {
			long id = generator.nextId();
			assertThat(id).isGreaterThan(previous);
			previous = id;
		}
	}

	@Test
	void rejectsNodeIdOutsideTenBits() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TimeSortedIdGenerator(1024));
		assertThatIllegalArgumentException().isThrownBy(() -> new TimeSortedIdGenerator(-1));
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		private MutableClock(Instant now) {
			this.now = now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return this.now;
		}

	}

}
