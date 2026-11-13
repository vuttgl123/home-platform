package com.home_platform.identity.workspace;

import com.home_platform.common.mapping.PlatformMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PlatformMapperConfig.class)
public interface WorkspaceMapper {

	@Mapping(target = ".", source = "workspace")
	WorkspaceResponse toResponse(MemberWorkspace memberWorkspace);

}
