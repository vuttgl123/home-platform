package com.home_platform.common.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneOffset;

import com.home_platform.common.id.IdGenerator;
import com.home_platform.common.json.StorageJson;
import com.home_platform.common.web.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

class PlatformCommonAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(PlatformCommonAutoConfiguration.class, CacheAutoConfiguration.class));

	@Test
	void providesIdGeneratorAndExceptionHandler() {
		this.runner.run(context -> {
			assertThat(context).hasSingleBean(IdGenerator.class);
			assertThat(context).hasSingleBean(ApiExceptionHandler.class);
		});
	}

	@Test
	void providesUtcClockAndStorageJson() {
		this.runner.run(context -> {
			assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneOffset.UTC);
			assertThat(context).hasSingleBean(StorageJson.class);
		});
	}

	@Test
	void configuredNodeIdIsUsed() {
		this.runner.withPropertyValues("platform.id.node-id=7").run(context -> {
			long id = context.getBean(IdGenerator.class).nextId();
			assertThat((id >> 12) & 0x3FF).isEqualTo(7L);
		});
	}

	@Test
	void databaseWaitsAreBounded() {
		this.runner.run(context -> {
			Environment environment = context.getEnvironment();
			String driver = "spring.datasource.hikari.data-source-properties.";
			assertThat(environment.getProperty(driver + "connectTimeout")).isEqualTo("5000");
			assertThat(environment.getProperty(driver + "socketTimeout")).isEqualTo("60000");
			assertThat(environment.getProperty(driver + "sessionVariables")).isEqualTo("innodb_lock_wait_timeout=5");
			assertThat(environment.getProperty("spring.transaction.default-timeout")).isEqualTo("30s");
		});
	}

	@Test
	void cachingUsesCaffeineWithSizeLimitAndExpiry() {
		this.runner.run(context -> {
			CacheManager cacheManager = context.getBean(CacheManager.class);
			assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
			CaffeineCache cache = (CaffeineCache) cacheManager.getCache("any");
			assertThat(cache.getNativeCache().policy().eviction()).isPresent();
			assertThat(cache.getNativeCache().policy().expireAfterWrite()).isPresent();
		});
	}

}
