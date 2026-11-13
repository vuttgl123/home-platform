package com.home_platform.common.autoconfigure;

import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;

import com.home_platform.common.id.IdGenerator;
import com.home_platform.common.id.TimeSortedIdGenerator;
import com.home_platform.common.json.LongAsStringCustomizer;
import com.home_platform.common.json.StorageJson;
import com.home_platform.common.web.ApiExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * Những thứ mọi service đều có sẵn khi phụ thuộc vào platform-common: bộ sinh id, đồng hồ,
 * hai quy ước JSON, bộ xử lý lỗi của API và cache. Service không phải khai báo gì thêm.
 */
@AutoConfiguration(beforeName = "org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration")
@EnableCaching
@PropertySource("classpath:platform-common-defaults.properties")
public class PlatformCommonAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(PlatformCommonAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	IdGenerator idGenerator(Environment environment) {
		Integer nodeId = environment.getProperty("platform.id.node-id", Integer.class);
		if (nodeId == null) {
			nodeId = ThreadLocalRandom.current().nextInt(TimeSortedIdGenerator.MAX_NODE_ID + 1);
			log.info("platform.id.node-id is not set, using random node id {}. "
					+ "Set it to a distinct value per instance when running more than one.", nodeId);
		}
		return new TimeSortedIdGenerator(nodeId);
	}

	/** Nguồn thời gian chung, giờ UTC. Test thay bean này để cố định thời điểm. */
	@Bean
	@ConditionalOnMissingBean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	@ConditionalOnMissingBean
	StorageJson storageJson() {
		return new StorageJson();
	}

	@Bean
	@ConditionalOnMissingBean
	LongAsStringCustomizer longAsStringCustomizer() {
		return new LongAsStringCustomizer();
	}

	@Bean
	@ConditionalOnMissingBean
	ApiExceptionHandler apiExceptionHandler() {
		return new ApiExceptionHandler();
	}

}
