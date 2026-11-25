package com.home_platform.media.file;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.home_platform.media.file.PendingFileRepository.Pending;
import com.home_platform.media.storage.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dọn file tải lên hỏng giữa chừng (spec Đợt 7, M9): dòng PENDING quá 1 giờ bị xoá cùng nội dung
 * trong kho, mỗi giờ một lần. Xoá kho trước, dòng sau: lỗi giữa chừng thì lần sau dọn tiếp.
 */
@Component
public class PendingCleanup {

	/** Tuổi tối thiểu của một file PENDING trước khi bị coi là bỏ dở. */
	static final Duration ABANDONED_AFTER = Duration.ofHours(1);

	/** Số file dọn nhiều nhất mỗi lần. */
	static final int BATCH = 500;

	private static final Logger logger = LoggerFactory.getLogger(PendingCleanup.class);

	private final PendingFileRepository pending;

	private final FileStorage storage;

	private final Clock clock;

	PendingCleanup(PendingFileRepository pending, FileStorage storage, Clock clock) {
		this.pending = pending;
		this.storage = storage;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
	void run() {
		int removed = removeOlderThan(ABANDONED_AFTER);
		if (removed > 0) {
			logger.info("Removed {} unfinished uploads", removed);
		}
	}

	/** Xoá các file PENDING cũ hơn {@code age}; trả số dòng đã xoá. */
	public int removeOlderThan(Duration age) {
		LocalDateTime cutoff = LocalDateTime.ofInstant(this.clock.instant().minus(age), ZoneOffset.UTC);
		int removed = 0;
		for (Pending file : this.pending.olderThan(cutoff, BATCH)) {
			this.storage.delete(file.storageKey());
			this.storage.delete(UploadService.thumbnailKey(file.storageKey()));
			removed += this.pending.delete(file) ? 1 : 0;
		}
		return removed;
	}

}
