package com.home_platform.registry.item;

import com.home_platform.common.mapping.PlatformMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PlatformMapperConfig.class)
public interface ItemMapper {

	@Mapping(target = ".", source = "item")
	ItemDetailResponse toDetail(ItemView view);

	@Mapping(target = ".", source = "item")
	ItemResponse toResponse(ItemView view);

}
