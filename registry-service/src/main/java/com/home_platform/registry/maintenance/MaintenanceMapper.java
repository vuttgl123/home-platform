package com.home_platform.registry.maintenance;

import com.home_platform.common.mapping.PlatformMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = PlatformMapperConfig.class)
public interface MaintenanceMapper {

	MaintenanceResponse toResponse(Maintenance row);

}
