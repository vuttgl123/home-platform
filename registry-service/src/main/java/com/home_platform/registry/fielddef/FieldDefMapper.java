package com.home_platform.registry.fielddef;

import java.util.List;

import com.home_platform.common.mapping.PlatformMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PlatformMapperConfig.class)
public interface FieldDefMapper {

	@Mapping(target = "required", source = "isRequired")
	FieldDefResponse toResponse(FieldDef fieldDef);

	List<FieldDefResponse> toResponses(List<FieldDef> fieldDefs);

}
