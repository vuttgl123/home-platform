package com.home_platform.common.autoconfigure;

import java.util.List;

import com.home_platform.common.context.ContextArgumentResolver;
import com.home_platform.common.context.ContextResolver;
import com.home_platform.common.context.HeaderContextResolver;
import com.home_platform.common.web.CursorRequestArgumentResolver;
import com.home_platform.common.web.PagingProperties;
import com.home_platform.common.web.RequestLogFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Phần dành cho ứng dụng web: controller nhận được người gọi và yêu cầu phân trang như các
 * tham số bình thường, và mỗi request có một mã đi theo log của nó.
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

	/** Chạy gần như đầu tiên, để log của mọi filter và controller phía sau đều mang mã request. */
	@Bean
	@ConditionalOnMissingBean(name = "requestLogFilter")
	FilterRegistrationBean<RequestLogFilter> requestLogFilter() {
		FilterRegistrationBean<RequestLogFilter> registration = new FilterRegistrationBean<>(new RequestLogFilter());
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
		return registration;
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
