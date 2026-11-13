package com.home_platform.common.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.jdbc.RepositorySupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.simple.JdbcClient;

class PlatformJdbcAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PlatformCommonAutoConfiguration.class, CacheAutoConfiguration.class,
				PlatformJdbcAutoConfiguration.class));

	@Test
	void repositorySupportNeedsAJdbcClient() {
		this.runner.run(context -> assertThat(context).doesNotHaveBean(RepositorySupport.class));
		this.runner.withBean(JdbcClient.class, () -> mock(JdbcClient.class))
			.run(context -> assertThat(context).hasSingleBean(RepositorySupport.class));
	}

	@Test
	void historyIsOffWithoutTheProperty() {
		this.runner.withBean(JdbcClient.class, () -> mock(JdbcClient.class))
			.run(context -> assertThat(context).doesNotHaveBean(EntityHistory.class));
	}

	@Test
	void historyIsOnWithTheProperty() {
		this.runner.withBean(JdbcClient.class, () -> mock(JdbcClient.class))
			.withPropertyValues("platform.history.table=reg_entity_version")
			.run(context -> assertThat(context).hasSingleBean(EntityHistory.class));
	}

	@Test
	void historyNeedsAJdbcClient() {
		this.runner.withPropertyValues("platform.history.table=reg_entity_version")
			.run(context -> assertThat(context).doesNotHaveBean(EntityHistory.class));
	}

	@Test
	void strangeTableNameStopsStartup() {
		this.runner.withBean(JdbcClient.class, () -> mock(JdbcClient.class))
			.withPropertyValues("platform.history.table=x; DROP")
			.run(context -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("platform.history.table"));
	}

}
