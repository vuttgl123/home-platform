package com.home_platform.common.mapping;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * Cấu hình chung cho mọi mapper của MapStruct: mapper là bean của Spring, và build báo lỗi
 * nếu một trường của kiểu đích không được gán. Nhờ vậy thêm một cột mà quên đưa vào response
 * thì biết ngay lúc build.
 *
 * <p>Cách dùng: {@code @Mapper(config = PlatformMapperConfig.class)}.
 */
@MapperConfig(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PlatformMapperConfig {

}
