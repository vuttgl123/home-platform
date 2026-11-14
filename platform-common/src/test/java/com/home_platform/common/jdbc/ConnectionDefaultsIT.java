package com.home_platform.common.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.home_platform.common.CommonIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

/** Các giới hạn chờ trong {@code platform-common-defaults.properties} tới được phiên làm việc của database. */
class ConnectionDefaultsIT extends CommonIT {

	@Autowired
	PlatformTransactionManager transactionManager;

	/** Thuộc tính có trong cấu hình chưa đủ: transaction manager phải thực sự dùng nó. */
	@Test
	void transactionsAreCutOffAfterThirtySeconds() {
		int seconds = ((AbstractPlatformTransactionManager) this.transactionManager).getDefaultTimeout();

		assertThat(seconds).isEqualTo(30);
	}

	@Test
	void sessionWaitsAtMostFiveSecondsForALock() {
		Integer seconds = this.jdbc.sql("SELECT @@SESSION.innodb_lock_wait_timeout").query(Integer.class).single();

		assertThat(seconds).isEqualTo(5);
	}

}
