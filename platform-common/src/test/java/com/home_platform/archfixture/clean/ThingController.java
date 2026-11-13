package com.home_platform.archfixture.clean;

import com.home_platform.common.context.WorkspaceContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ThingController {

	private final ThingService service;

	public ThingController(ThingService service) {
		this.service = service;
	}

	@GetMapping("/things/{code}")
	public Thing get(WorkspaceContext ctx, @PathVariable String code) {
		return this.service.find(ctx, code).orElseThrow();
	}

}
