package com.home_platform.media.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Kho file của service: hôm nay là thư mục trên đĩa; S3 hay MinIO sẽ là một bean khác ở đây. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StorageProperties.class)
class StorageConfiguration {

	@Bean
	FileStorage fileStorage(StorageProperties properties) {
		return new LocalFileStorage(properties.root());
	}

}
