package com.home_platform.registry.placetype;

import java.util.List;

import com.home_platform.common.mapping.PlatformMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = PlatformMapperConfig.class)
public interface PlaceTypeMapper {

	PlaceTypeResponse toResponse(PlaceType placeType);

	List<PlaceTypeResponse> toResponses(List<PlaceType> placeTypes);

}
