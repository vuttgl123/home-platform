package com.home_platform.common.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.home_platform.common.context.ContextResolver;
import com.home_platform.common.context.HeaderContextResolver;
import com.home_platform.common.context.UserContext;
import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.PagingProperties;
import com.home_platform.common.web.RequestLogFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

class PlatformWebAutoConfigurationTests {

	private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PlatformWebAutoConfiguration.class));

	@Test
	void providesHeaderResolverByDefault() {
		this.runner.run(context -> assertThat(context.getBean(ContextResolver.class))
			.isInstanceOf(HeaderContextResolver.class));
	}

	@Test
	void serviceCanReplaceTheResolver() {
		ContextResolver custom = new ContextResolver() {
			@Override
			public UserContext resolveUser(HttpServletRequest request) {
				return new UserContext(1);
			}

			@Override
			public WorkspaceContext resolveWorkspace(HttpServletRequest request) {
				return new WorkspaceContext(2, 1);
			}
		};
		this.runner.withBean(ContextResolver.class, () -> custom)
			.run(context -> assertThat(context.getBean(ContextResolver.class)).isSameAs(custom));
	}

	@Test
	void pagingLimitsDefaultTo20And100AndCanBeChanged() {
		this.runner.run(context -> assertThat(context.getBean(PagingProperties.class))
			.isEqualTo(new PagingProperties(20, 100)));
		this.runner.withPropertyValues("platform.paging.default-limit=10", "platform.paging.max-limit=50")
			.run(context -> assertThat(context.getBean(PagingProperties.class))
				.isEqualTo(new PagingProperties(10, 50)));
	}

	@Test
	void registersTheRequestLogFilter() {
		this.runner.run(context -> {
			FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);
			assertThat(registration.getFilter()).isInstanceOf(RequestLogFilter.class);
			assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 10);
		});
	}

	@Test
	void notActiveOutsideAWebApplication() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(PlatformWebAutoConfiguration.class))
			.run(context -> assertThat(context).doesNotHaveBean(ContextResolver.class));
	}

}
