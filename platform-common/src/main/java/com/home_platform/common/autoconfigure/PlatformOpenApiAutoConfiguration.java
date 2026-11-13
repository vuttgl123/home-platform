package com.home_platform.common.autoconfigure;

import com.home_platform.common.context.UserContext;
import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.openapi.PlatformOperationCustomizer;
import com.home_platform.common.openapi.ProblemDetailCustomizer;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.common.web.PagingProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Tài liệu API ({@code /swagger-ui.html}, {@code /v3/api-docs}) cho mọi service: tên lấy từ
 * {@code spring.application.name}, người gọi và phân trang hiện đúng dạng client phải gửi,
 * {@code long} hiện là chuỗi như JSON thật trả ra.
 */
@AutoConfiguration(after = PlatformWebAutoConfiguration.class)
@ConditionalOnClass(SpringDocUtils.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PlatformOpenApiAutoConfiguration {

	public PlatformOpenApiAutoConfiguration() {
		// Cấu hình chung của springdoc là tĩnh; chỉ chạy khi điều kiện ở trên thoả.
		SpringDocUtils.getConfig()
			.addRequestWrapperToIgnore(WorkspaceContext.class, UserContext.class, CursorRequest.class)
			.replaceWithSchema(Long.class, new StringSchema())
			.replaceWithSchema(long.class, new StringSchema());
	}

	@Bean
	@ConditionalOnMissingBean
	OpenAPI platformOpenApi(Environment environment) {
		return new OpenAPI().info(new Info().title(environment.getProperty("spring.application.name", "API")));
	}

	@Bean
	@ConditionalOnMissingBean
	PlatformOperationCustomizer platformOperationCustomizer(PagingProperties paging) {
		return new PlatformOperationCustomizer(paging);
	}

	@Bean
	@ConditionalOnMissingBean
	ProblemDetailCustomizer problemDetailCustomizer() {
		return new ProblemDetailCustomizer();
	}

}
