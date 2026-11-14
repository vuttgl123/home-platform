package com.home_platform.common.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.home_platform.common.CommonIT;
import org.junit.jupiter.api.Test;

/** Các giới hạn chờ trong {@code platform-common-defaults.properties} tới được phiên làm việc của database. */
class ConnectionDefaultsIT extends CommonIT {

	@Test
	void sessionWaitsAtMostFiveSecondsForALock() {
		Integer seconds = this.jdbc.sql("SELECT @@SESSION.innodb_lock_wait_timeout").query(Integer.class).single();

		assertThat(seconds).isEqualTo(5);
	}

}
