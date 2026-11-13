package com.home_platform.registry.itemcategory;

import java.util.List;

import com.home_platform.common.mapping.PlatformMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = PlatformMapperConfig.class)
public interface ItemCategoryMapper {

	ItemCategoryResponse toResponse(ItemCategory category);

	List<ItemCategoryResponse> toResponses(List<ItemCategory> categories);

}
