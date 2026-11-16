package com.home_platform.registry.ownership;

import com.home_platform.common.mapping.PlatformMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PlatformMapperConfig.class)
public interface OwnershipMapper {

	@Mapping(target = ".", source = "ownership")
	OwnershipResponse toResponse(OwnershipView view);

}
