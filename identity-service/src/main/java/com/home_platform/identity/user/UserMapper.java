package com.home_platform.identity.user;

import com.home_platform.common.mapping.PlatformMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = PlatformMapperConfig.class)
public interface UserMapper {

	UserResponse toResponse(User user);

}
