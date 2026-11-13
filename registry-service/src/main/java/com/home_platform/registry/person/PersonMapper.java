package com.home_platform.registry.person;

import com.home_platform.common.mapping.PlatformMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = PlatformMapperConfig.class)
public interface PersonMapper {

	PersonDetailResponse toDetail(Person person);

}
