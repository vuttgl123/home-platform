package com.home_platform.registry.code;

import java.util.List;

import com.home_platform.common.context.WorkspaceContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/code-rules")
class CodeRuleController {

	private final CodeRuleService service;

	CodeRuleController(CodeRuleService service) {
		this.service = service;
	}

	@GetMapping
	List<CodeRuleResponse> list(WorkspaceContext ctx) {
		return this.service.list(ctx.workspaceId()).stream().map(CodeRuleResponse::of).toList();
	}

	@PutMapping("/{target}/{typeId}")
	CodeRuleResponse put(WorkspaceContext ctx, @PathVariable CodeTarget target, @PathVariable long typeId,
			@Valid @RequestBody CodeRuleRequest request) {
		return CodeRuleResponse.of(this.service.put(ctx, target, typeId, request));
	}

	@DeleteMapping("/{target}/{typeId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(WorkspaceContext ctx, @PathVariable CodeTarget target, @PathVariable long typeId) {
		this.service.delete(ctx, target, typeId);
	}

}
