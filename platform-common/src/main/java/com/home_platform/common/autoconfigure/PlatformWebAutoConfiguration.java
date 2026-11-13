package com.home_platform.common.autoconfigure;

import java.util.List;

import com.home_platform.common.context.ContextArgumentResolver;
import com.home_platform.common.context.ContextResolver;
import com.home_platform.common.context.HeaderContextResolver;
import com.home_platform.common.web.CursorRequestArgumentResolver;
import com.home_platform.common.web.PagingProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Phần dành cho ứng dụng web: controller nhận được người gọi và yêu cầu phân trang như các
 * tham số bình thường.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(PagingProperties.class)
public class PlatformWebAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ContextResolver contextResolver() {
		return new HeaderContextResolver();
	}

	@Bean
	WebMvcConfigurer platformArgumentResolvers(ContextResolver contextResolver, PagingProperties paging) {
		return new WebMvcConfigurer() {
			@Override
			public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
				resolvers.add(new ContextArgumentResolver(contextResolver));
				resolvers.add(new CursorRequestArgumentResolver(paging));
			}
		};
	}

}
