package com.home_platform.registry.person;

import java.util.List;

import com.home_platform.common.history.EntityVersion;
import com.home_platform.common.history.EntityVersionSummary;
import com.home_platform.common.mapping.PlatformMapperConfig;
import com.home_platform.registry.occupancy.OccupancyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PlatformMapperConfig.class)
public interface PersonMapper {

	PersonDetailResponse toDetail(Person person, List<OccupancyResponse> currentOccupancies);

	PersonResponse toResponse(Person person);

	PersonVersionSummaryResponse toVersionSummary(EntityVersionSummary summary);

	@Mapping(target = "snapshot", source = "content")
	@Mapping(target = "note", source = "version.note")
	PersonVersionResponse toVersion(EntityVersion version, PersonSnapshot content);

}
