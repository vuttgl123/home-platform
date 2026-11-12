package com.home_platform.common.id;

import java.time.Clock;
import java.time.Instant;

/**
 * Id 64 bit gồm ba phần, từ cao xuống thấp: 41 bit số mili giây kể từ 2026-01-01 (UTC),
 * 10 bit mã node, 12 bit số thứ tự trong cùng một mili giây.
 *
 * <p>Hai instance chạy cùng lúc phải có mã node khác nhau, nếu không chúng có thể sinh
 * trùng id.
 */
public final class TimeSortedIdGenerator implements IdGenerator {

	static final long EPOCH_MILLIS = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();

	private static final int NODE_BITS = 10;

	private static final int SEQUENCE_BITS = 12;

	public static final int MAX_NODE_ID = (1 << NODE_BITS) - 1;

	private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;

	private final long nodeId;

	private final Clock clock;

	private long lastMillis = -1;

	private long sequence;

	public TimeSortedIdGenerator(int nodeId) {
		this(nodeId, Clock.systemUTC());
	}

	public TimeSortedIdGenerator(int nodeId, Clock clock) {
		if (nodeId < 0 || nodeId > MAX_NODE_ID) {
			throw new IllegalArgumentException("nodeId must be between 0 and " + MAX_NODE_ID + ", got " + nodeId);
		}
		this.nodeId = nodeId;
		this.clock = clock;
	}

	@Override
	public synchronized long nextId() {
		// Đồng hồ hệ thống có thể bị chỉnh lùi; không bao giờ đi lùi theo nó.
		long millis = Math.max(this.clock.millis(), this.lastMillis);
		if (millis == this.lastMillis) {
			this.sequence = (this.sequence + 1) & SEQUENCE_MASK;
			if (this.sequence == 0) {
				// Hết 4096 số của mili giây này: mượn mili giây kế tiếp thay vì chờ.
				millis++;
			}
		}
		else {
			this.sequence = 0;
		}
		this.lastMillis = millis;
		return ((millis - EPOCH_MILLIS) << (NODE_BITS + SEQUENCE_BITS)) | (this.nodeId << SEQUENCE_BITS) | this.sequence;
	}

}
