package com.home_platform.registry.occupancy;

import com.home_platform.common.mapping.PlatformMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PlatformMapperConfig.class)
public interface OccupancyMapper {

	@Mapping(target = ".", source = "occupancy")
	OccupancyResponse toResponse(OccupancyView view);

}
