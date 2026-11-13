package com.home_platform.registry.place;

import java.util.List;

import com.home_platform.common.mapping.PlatformMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PlatformMapperConfig.class)
public interface PlaceMapper {

	@Mapping(target = ".", source = "place")
	PlaceDetailResponse toDetail(PlaceView view);

	PlaceResponse toResponse(Place place);

	List<PlaceResponse> toResponses(List<Place> places);

}
