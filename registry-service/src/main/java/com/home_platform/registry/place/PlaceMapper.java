package com.home_platform.registry.place;

import java.util.List;

import com.home_platform.common.mapping.PlatformMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PlatformMapperConfig.class)
public interface PlaceMapper {

	@Mapping(target = ".", source = "place")
	@Mapping(target = "effectiveAddress", expression = "java(effectiveAddress(view))")
	PlaceDetailResponse toDetail(PlaceView view);

	AddressResponse toAddress(PlaceAddress address);

	default EffectiveAddressResponse effectiveAddress(PlaceView view) {
		return (view.effectiveAddress() != null)
				? new EffectiveAddressResponse(view.effectiveAddressFrom(), toAddress(view.effectiveAddress())) : null;
	}

	PlaceResponse toResponse(Place place);

	List<PlaceResponse> toResponses(List<Place> places);

}
