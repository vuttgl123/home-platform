package com.home_platform.registry.history;

import com.home_platform.common.mapping.PlatformMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = PlatformMapperConfig.class)
public interface ActivityMapper {

	ActivityResponse toResponse(ActivityEntry entry);

}
