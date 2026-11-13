package com.home_platform.common.autoconfigure;

import java.time.Clock;

import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.id.IdGenerator;
import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.json.StorageJson;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.JdbcClientAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Phần truy cập dữ liệu. Chỉ có hiệu lực ở service có {@link JdbcClient}, tức là có kết nối
 * database. Lịch sử version chỉ bật khi service đặt {@code platform.history.table}.
 */
@AutoConfiguration(after = { JdbcClientAutoConfiguration.class, PlatformCommonAutoConfiguration.class })
public class PlatformJdbcAutoConfiguration {

	@Bean
	@ConditionalOnBean(JdbcClient.class)
	@ConditionalOnMissingBean
	RepositorySupport repositorySupport(JdbcClient jdbc, IdGenerator idGenerator, Clock clock, StorageJson json) {
		return new RepositorySupport(jdbc, idGenerator, clock, json);
	}

	@Bean
	@ConditionalOnBean(JdbcClient.class)
	@ConditionalOnProperty("platform.history.table")
	@ConditionalOnMissingBean
	EntityHistory entityHistory(RepositorySupport support, ApplicationEventPublisher events, Environment environment) {
		return new EntityHistory(support, events, environment.getProperty("platform.history.table"));
	}

}
