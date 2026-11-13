package com.home_platform.registry.setup;

import com.home_platform.common.context.WorkspaceContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Client gọi ngay sau khi tạo một không gian bên identity. */
@RestController
class SetupController {

	private final SetupService service;

	SetupController(SetupService service) {
		this.service = service;
	}

	@PostMapping("/setup")
	SetupResponse setup(WorkspaceContext ctx) {
		return new SetupResponse(this.service.setup(ctx));
	}

}
