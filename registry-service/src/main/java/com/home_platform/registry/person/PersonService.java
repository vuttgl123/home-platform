package com.home_platform.registry.person;

import java.util.Map;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.history.ChangeAction;
import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.ConflictException;
import com.home_platform.common.web.NotFoundException;
import com.home_platform.registry.fielddef.AttributeValidation;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.support.BusinessDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hồ sơ người của một không gian. Mỗi lần ghi để lại một version (loại {@value #ENTITY_TYPE}).
 * Hai người chưa xoá không được trùng cặp loại và số giấy tờ.
 */
@Service
public class PersonService {

	public static final String ENTITY_TYPE = "PERSON";

	private final PersonRepository repository;

	private final AttributeValidation attributeValidation;

	private final EntityHistory history;

	private final BusinessDate businessDate;

	PersonService(PersonRepository repository, AttributeValidation attributeValidation, EntityHistory history,
			BusinessDate businessDate) {
		this.repository = repository;
		this.attributeValidation = attributeValidation;
		this.history = history;
		this.businessDate = businessDate;
	}

	/** @throws NotFoundException nếu không có hoặc đã xoá */
	public Person get(long workspaceId, long id) {
		return this.repository.getById(workspaceId, id);
	}

	@Transactional
	public Person create(WorkspaceContext ctx, PersonRequest request) {
		requireUniqueDocument(ctx.workspaceId(), 0, request.idType(), request.idNumber());
		Person person = new Person(0, 0, request.fullName(), request.phone(), request.email(), request.idType(),
				request.idNumber(), request.dateOfBirth(), request.gender(), request.note(),
				validAttributes(ctx, request.attributes()), request.avatarFileId(), null, 0, null, 0, 0, null);
		Person stored = this.repository.insert(ctx, person);
		record(ctx, stored, ChangeAction.CREATE, request.versionNote());
		return stored;
	}

	@Transactional
	public Person update(WorkspaceContext ctx, long id, PersonUpdateRequest request) {
		Person current = get(ctx.workspaceId(), id);
		requireUniqueDocument(ctx.workspaceId(), id, request.idType(), request.idNumber());
		Person stored = this.repository.update(ctx, current.withVersion(request.version())
			.withFullName(request.fullName())
			.withPhone(request.phone())
			.withEmail(request.email())
			.withIdType(request.idType())
			.withIdNumber(request.idNumber())
			.withDateOfBirth(request.dateOfBirth())
			.withGender(request.gender())
			.withNote(request.note())
			.withAttributes(validAttributes(ctx, request.attributes()))
			.withAvatarFileId(request.avatarFileId()));
		record(ctx, stored, ChangeAction.UPDATE, request.versionNote());
		return stored;
	}

	/**
	 * Xoá mềm; dành cho hồ sơ nhập nhầm hoặc không còn dùng.
	 * @throws ConflictException nếu người đó còn cư trú hiện tại hoặc sắp tới
	 */
	@Transactional
	public void delete(WorkspaceContext ctx, long id, int version, String note) {
		get(ctx.workspaceId(), id);
		int occupancies = this.repository.countCurrentOccupancies(ctx.workspaceId(), id, this.businessDate.today());
		if (occupancies > 0) {
			throw new ConflictException(
					"Person " + id + " still has " + occupancies + " current or upcoming occupancies");
		}
		Person stored = this.repository.softDelete(ctx, id, version);
		record(ctx, stored, ChangeAction.DELETE, note);
	}

	/**
	 * Bỏ xoá. Trong lúc người này bị xoá, người khác có thể đã được ghi với cùng giấy tờ; khi đó 409.
	 */
	@Transactional
	public Person restore(WorkspaceContext ctx, long id, PersonRestoreRequest request) {
		Person deleted = this.repository.findByIdIncludingDeleted(ctx.workspaceId(), id)
			.orElseThrow(() -> NotFoundException.of("Person", id));
		if (deleted.deletedAt() != null) {
			requireUniqueDocument(ctx.workspaceId(), id, deleted.idType(), deleted.idNumber());
		}
		Person stored = this.repository.restore(ctx, id, request.version());
		record(ctx, stored, ChangeAction.RESTORE, request.note());
		return stored;
	}

	/**
	 * @throws BadRequestException nếu chỉ có một trong hai trường giấy tờ
	 * @throws ConflictException nếu một người khác chưa xoá đã mang giấy tờ này
	 */
	void requireUniqueDocument(long workspaceId, long selfId, String idType, String idNumber) {
		if ((idType == null) != (idNumber == null)) {
			throw new BadRequestException("idType and idNumber must be given together");
		}
		if (idType == null) {
			return;
		}
		this.repository.findByDocument(workspaceId, idType, idNumber)
			.filter(other -> other.id() != selfId)
			.ifPresent(other -> {
				throw new ConflictException("Person " + other.id() + " already has this document");
			});
	}

	Map<String, Object> validAttributes(WorkspaceContext ctx, Map<String, Object> values) {
		return this.attributeValidation.validate(ctx.workspaceId(), FieldTarget.PERSON, 0, values);
	}

	void record(WorkspaceContext ctx, Person person, ChangeAction action, String note) {
		this.history.record(ctx, ENTITY_TYPE, person.id(), person.version(), action, PersonSnapshot.of(person), note);
	}

}
