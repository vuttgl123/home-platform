package com.home_platform.registry.history;

import com.home_platform.common.mapping.PlatformMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PlatformMapperConfig.class)
public interface ActivityMapper {

	@Mapping(target = ".", source = "change")
	ActivityResponse toResponse(ActivityEntry entry);

}
