package com.home_platform.registry.person;

import java.util.Objects;
import java.util.stream.Stream;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.history.EntityVersion;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.registry.occupancy.OccupancyMapper;
import com.home_platform.registry.occupancy.OccupancyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/people")
class PersonController {

	private final PersonService service;

	private final PersonMapper mapper;

	private final OccupancyService occupancies;

	private final OccupancyMapper occupancyMapper;

	PersonController(PersonService service, PersonMapper mapper, OccupancyService occupancies,
			OccupancyMapper occupancyMapper) {
		this.service = service;
		this.mapper = mapper;
		this.occupancies = occupancies;
		this.occupancyMapper = occupancyMapper;
	}

	/** Người kèm những nơi đang ở. */
	private PersonDetailResponse detail(WorkspaceContext ctx, Person person) {
		return this.mapper.toDetail(person, this.occupancies.currentOfPerson(ctx.workspaceId(), person.id())
			.stream()
			.map(this.occupancyMapper::toResponse)
			.toList());
	}

	@GetMapping
	CursorPage<PersonResponse> list(WorkspaceContext ctx, CursorRequest page) {
		return this.service.list(ctx.workspaceId(), page).map(this.mapper::toResponse);
	}

	/** Đúng một trong ba: tên bắt đầu bằng, số điện thoại bắt đầu bằng, đúng số giấy tờ. */
	@GetMapping("/search")
	CursorPage<PersonResponse> search(WorkspaceContext ctx, @RequestParam(required = false) String name,
			@RequestParam(required = false) String phone, @RequestParam(required = false) String idNumber,
			CursorRequest page) {
		if (Stream.of(name, phone, idNumber).filter(Objects::nonNull).count() != 1) {
			throw new BadRequestException("Give exactly one of name, phone or idNumber");
		}
		CursorPage<Person> found = (name != null) ? this.service.searchByName(ctx.workspaceId(), name, page)
				: (phone != null) ? this.service.searchByPhone(ctx.workspaceId(), phone, page)
						: this.service.findByIdNumber(ctx.workspaceId(), idNumber, page);
		return found.map(this.mapper::toResponse);
	}

	@GetMapping("/{id}/versions")
	CursorPage<PersonVersionSummaryResponse> versions(WorkspaceContext ctx, @PathVariable long id, CursorRequest page) {
		return this.service.versions(ctx.workspaceId(), id, page).map(this.mapper::toVersionSummary);
	}

	@GetMapping("/{id}/versions/{versionNo}")
	PersonVersionResponse version(WorkspaceContext ctx, @PathVariable long id, @PathVariable int versionNo) {
		EntityVersion version = this.service.version(ctx.workspaceId(), id, versionNo);
		return this.mapper.toVersion(version, this.service.snapshotOf(version));
	}

	@PostMapping("/{id}/versions/{versionNo}/restore")
	PersonDetailResponse restoreVersion(WorkspaceContext ctx, @PathVariable long id, @PathVariable int versionNo,
			@Valid @RequestBody PersonVersionRestoreRequest request) {
		return detail(ctx, this.service.restoreVersion(ctx, id, versionNo, request));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	PersonDetailResponse create(WorkspaceContext ctx, @Valid @RequestBody PersonRequest request) {
		return detail(ctx, this.service.create(ctx, request));
	}

	@GetMapping("/{id}")
	PersonDetailResponse get(WorkspaceContext ctx, @PathVariable long id) {
		return detail(ctx, this.service.get(ctx.workspaceId(), id));
	}

	@PutMapping("/{id}")
	PersonDetailResponse update(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody PersonUpdateRequest request) {
		return detail(ctx, this.service.update(ctx, id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(WorkspaceContext ctx, @PathVariable long id, @RequestParam int version,
			@RequestParam(required = false) String note) {
		this.service.delete(ctx, id, version, note);
	}

	@PostMapping("/{id}/restore")
	PersonDetailResponse restore(WorkspaceContext ctx, @PathVariable long id,
			@Valid @RequestBody PersonRestoreRequest request) {
		return detail(ctx, this.service.restore(ctx, id, request));
	}

}
