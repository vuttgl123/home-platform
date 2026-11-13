package com.home_platform.common;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.home_platform.common.jdbc.SampleDocRepository;
import com.home_platform.common.jdbc.SampleTagRepository;
import com.home_platform.common.test.AbstractRepositoryIT;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Khung cho test {@code *IT} của chính module này. platform-common không có bảng riêng, nên
 * mỗi test tự tạo bảng tạm: bảng tạm chỉ tồn tại trong kết nối của test, dữ liệu trong đó
 * được rollback cùng transaction của test, và database không bị thêm bảng thật nào.
 *
 * <p>Mọi lớp con dùng chung một bộ chú thích ở đây, nên Spring chỉ dựng một application
 * context (và một nhóm kết nối) cho tất cả.
 */
@ActiveProfiles("uat")
@TestPropertySource(properties = { "platform.history.table=tmp_entity_version",
		"spring.datasource.hikari.maximum-pool-size=2" })
@Import({ CommonIT.FixedClock.class, SampleDocRepository.class, SampleTagRepository.class })
public abstract class CommonIT extends AbstractRepositoryIT {

	/** Thời điểm mà repository ghi vào các cột thời gian: giờ của {@link FixedClock}, cắt tới mili giây. */
	protected static final Instant NOW = Instant.parse("2026-11-12T06:40:00.123Z");

	@BeforeEach
	void createTemporaryTables() {
		this.jdbc.sql("""
				CREATE TEMPORARY TABLE IF NOT EXISTS tmp_sample_doc (
				  workspace_id BIGINT NOT NULL, id BIGINT NOT NULL, parent_id BIGINT NULL, name VARCHAR(255) NOT NULL,
				  status VARCHAR(20) NOT NULL, pinned TINYINT(1) NOT NULL, rating INT NULL, price DECIMAL(12,2) NULL,
				  valid_from DATE NULL, attributes JSON NULL, created_at DATETIME(3) NOT NULL, created_by BIGINT NOT NULL,
				  updated_at DATETIME(3) NOT NULL, updated_by BIGINT NOT NULL, version INT NOT NULL, deleted_at DATETIME(3) NULL,
				  PRIMARY KEY (workspace_id, id), KEY idx_tmp_doc_parent (workspace_id, parent_id, name)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""").update();
		this.jdbc.sql("""
				CREATE TEMPORARY TABLE IF NOT EXISTS tmp_sample_tag (
				  workspace_id BIGINT NOT NULL, id BIGINT NOT NULL, code VARCHAR(50) NOT NULL, name VARCHAR(100) NOT NULL,
				  created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
				  PRIMARY KEY (workspace_id, id), UNIQUE KEY uk_tmp_tag_code (workspace_id, code)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""").update();
		this.jdbc.sql("""
				CREATE TEMPORARY TABLE IF NOT EXISTS tmp_entity_version (
				  workspace_id BIGINT NOT NULL, entity_type VARCHAR(20) NOT NULL, entity_id BIGINT NOT NULL,
				  version_no INT NOT NULL, action VARCHAR(10) NOT NULL, snapshot JSON NOT NULL, changed_fields JSON NULL,
				  changed_by BIGINT NOT NULL, changed_at DATETIME(3) NOT NULL, note VARCHAR(255) NULL,
				  PRIMARY KEY (workspace_id, entity_type, entity_id, version_no)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""").update();
	}

	/** Đồng hồ đứng yên, có phần micro giây để thử việc cắt tới mili giây. */
	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClock {

		@Bean
		Clock clock() {
			return Clock.fixed(Instant.parse("2026-11-12T06:40:00.123456Z"), ZoneOffset.UTC);
		}

	}

}
