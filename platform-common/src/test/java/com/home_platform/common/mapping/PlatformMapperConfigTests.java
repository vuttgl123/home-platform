package com.home_platform.common.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PlatformMapperConfigTests {

	@Test
	void mapperIsGeneratedAsSpringBean() {
		new ApplicationContextRunner().withUserConfiguration(SampleMapperImpl.class)
			.run(context -> assertThat(context.getBean(SampleMapper.class).toResponse(new SampleSource(7, "A")))
				.isEqualTo(new SampleResponse(7, "A")));
	}

}

@Mapper(config = PlatformMapperConfig.class)
interface SampleMapper {

	SampleResponse toResponse(SampleSource source);

}

record SampleSource(long id, String name) {
}

record SampleResponse(long id, String name) {
}
