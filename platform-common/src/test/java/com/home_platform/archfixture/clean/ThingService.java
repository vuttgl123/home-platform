package com.home_platform.archfixture.clean;

import java.util.Optional;

import com.home_platform.common.context.WorkspaceContext;
import org.springframework.stereotype.Service;

@Service
public class ThingService {

	private final ThingRepository things;

	public ThingService(ThingRepository things) {
		this.things = things;
	}

	public Optional<Thing> find(WorkspaceContext ctx, String code) {
		return this.things.findByCode(ctx.workspaceId(), code);
	}

}
