package com.home_platform.registry.person;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.history.ChangeAction;
import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.history.EntityVersion;
import com.home_platform.common.history.EntityVersionSummary;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.ConflictException;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.common.web.NotFoundException;
import com.home_platform.registry.support.Text;
import com.home_platform.registry.fielddef.AttributeValidation;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.support.BusinessDate;
import org.springframework.dao.DuplicateKeyException;
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

	/**
	 * Như {@link #get}, và giữ riêng người này tới hết transaction: không lệnh nào xoá được người
	 * đó, và hai lệnh ghi cư trú của cùng một người chạy lần lượt chứ không cùng lúc.
	 * @throws NotFoundException nếu không có hoặc đã xoá
	 */
	public Person getForUpdate(long workspaceId, long id) {
		return findForUpdate(workspaceId, id).orElseThrow(() -> NotFoundException.of("Person", id));
	}

	/** Như {@link #getForUpdate}, trả rỗng thay vì ném lỗi khi người đó không có hoặc đã xoá. */
	public Optional<Person> findForUpdate(long workspaceId, long id) {
		return this.repository.findByIdForUpdate(workspaceId, id);
	}

	@Transactional
	public Person create(WorkspaceContext ctx, PersonRequest request) {
		requireUniqueDocument(ctx.workspaceId(), 0, request.idType(), request.idNumber());
		Person person = new Person(0, 0, request.fullName(), request.phone(), request.email(), request.idType(),
				request.idNumber(), request.dateOfBirth(), request.gender(), request.note(),
				validAttributes(ctx, request.attributes()), request.avatarFileId(), null, 0, null, 0, 0, null);
		Person stored = rejectingSharedDocument(() -> this.repository.insert(ctx, person));
		record(ctx, stored, ChangeAction.CREATE, request.versionNote());
		return stored;
	}

	@Transactional
	public Person update(WorkspaceContext ctx, long id, PersonUpdateRequest request) {
		Person current = get(ctx.workspaceId(), id);
		Person stored = applyContent(ctx, current, request);
		record(ctx, stored, ChangeAction.UPDATE, request.versionNote());
		return stored;
	}

	/**
	 * Đường ghi nội dung chung của {@link #update} và {@link #restoreVersion}: kiểm tra giấy tờ và
	 * {@code attributes}, rồi ghi mọi trường sửa được. Không ghi version; hàm gọi tự ghi. Khôi phục
	 * version đi qua đây bằng một {@link PersonUpdateRequest} dựng từ snapshot, nên thêm một trường
	 * sửa được mà quên phần khôi phục thì không biên dịch được.
	 */
	private Person applyContent(WorkspaceContext ctx, Person current, PersonUpdateRequest request) {
		requireUniqueDocument(ctx.workspaceId(), current.id(), request.idType(), request.idNumber());
		Person changed = current.withVersion(request.version())
			.withFullName(request.fullName())
			.withPhone(request.phone())
			.withEmail(request.email())
			.withIdType(request.idType())
			.withIdNumber(request.idNumber())
			.withDateOfBirth(request.dateOfBirth())
			.withGender(request.gender())
			.withNote(request.note())
			.withAttributes(validAttributes(ctx, request.attributes()))
			.withAvatarFileId(request.avatarFileId());
		return rejectingSharedDocument(() -> this.repository.update(ctx, changed));
	}

	/**
	 * Xoá mềm; dành cho hồ sơ nhập nhầm hoặc không còn dùng.
	 * @throws ConflictException nếu người đó còn cư trú hoặc sở hữu hiện tại hay sắp tới, hoặc còn là
	 * chủ của món đồ chưa gỡ
	 */
	@Transactional
	public void delete(WorkspaceContext ctx, long id, int version, String note) {
		// Giữ riêng trước khi đếm: câu đếm bên dưới mới thấy mọi dòng cư trú đã commit của người này.
		getForUpdate(ctx.workspaceId(), id);
		int occupancies = this.repository.countCurrentOccupancies(ctx.workspaceId(), id, this.businessDate.today());
		int ownerships = this.repository.countCurrentOwnerships(ctx.workspaceId(), id, this.businessDate.today());
		int items = this.repository.countOwnedItems(ctx.workspaceId(), id);
		if (occupancies + ownerships + items > 0) {
			throw new ConflictException("Person " + id + " still has " + occupancies + " current or upcoming occupancies, "
					+ ownerships + " ownerships and " + items + " items");
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
		Person stored = rejectingSharedDocument(() -> this.repository.restore(ctx, id, request.version()));
		record(ctx, stored, ChangeAction.RESTORE, request.note());
		return stored;
	}

	/** Tên của các người (kể cả đã xoá) theo {@code id}. */
	public Map<Long, String> names(long workspaceId, List<Long> ids) {
		return this.repository.names(workspaceId, ids);
	}

	public CursorPage<Person> list(long workspaceId, CursorRequest page) {
		return this.repository.list(workspaceId, page);
	}

	public CursorPage<Person> searchByName(long workspaceId, String prefix, CursorRequest page) {
		return this.repository.searchByName(workspaceId, Text.likePrefix(required(Text.trimToNull(prefix), "name")),
				page);
	}

	/** Bỏ khoảng trắng và dấu chấm trong từ khoá như khi lưu, rồi tìm theo tiền tố. */
	public CursorPage<Person> searchByPhone(long workspaceId, String prefix, CursorRequest page) {
		return this.repository.searchByPhone(workspaceId,
				Text.likePrefix(required(PersonFields.phone(prefix), "phone")), page);
	}

	public CursorPage<Person> findByIdNumber(long workspaceId, String idNumber, CursorRequest page) {
		return this.repository.findByIdNumber(workspaceId, required(Text.trimToNull(idNumber), "idNumber"), page);
	}

	/** Các version của một người, mới nhất trước; thấy cả khi người đó đã bị xoá. */
	public CursorPage<EntityVersionSummary> versions(long workspaceId, long id, CursorRequest page) {
		this.repository.findByIdIncludingDeleted(workspaceId, id).orElseThrow(() -> NotFoundException.of("Person", id));
		return this.history.list(workspaceId, ENTITY_TYPE, id, page);
	}

	public EntityVersion version(long workspaceId, long id, int versionNo) {
		return this.history.get(workspaceId, ENTITY_TYPE, id, versionNo);
	}

	public PersonSnapshot snapshotOf(EntityVersion version) {
		return this.history.snapshotAs(version, PersonSnapshot.class);
	}

	/**
	 * Khôi phục nội dung một version cũ như một lần sửa bình thường. Nội dung cũ phải hợp lệ lúc
	 * này (giấy tờ không trùng người khác, {@code attributes} hợp với mục tuỳ biến hiện tại).
	 * @throws ConflictException nếu người đó đang bị xoá
	 */
	@Transactional
	public Person restoreVersion(WorkspaceContext ctx, long id, int versionNo, PersonVersionRestoreRequest request) {
		long workspaceId = ctx.workspaceId();
		Person current = this.repository.findById(workspaceId, id)
			.orElseThrow(() -> this.repository.findByIdIncludingDeleted(workspaceId, id).isPresent()
					? new ConflictException("Restore person " + id + " before restoring an old version")
					: NotFoundException.of("Person", id));
		PersonSnapshot old = snapshotOf(version(workspaceId, id, versionNo));
		String note = EntityHistory.restoredNote(versionNo, request.note());
		Person stored = applyContent(ctx, current,
				new PersonUpdateRequest(request.version(), old.fullName(), old.phone(), old.email(), old.idType(),
						old.idNumber(), old.dateOfBirth(), old.gender(), old.note(), old.attributes(),
						old.avatarFileId(), note));
		record(ctx, stored, ChangeAction.UPDATE, note);
		return stored;
	}

	private static String required(String value, String name) {
		if (value == null) {
			throw new BadRequestException(name + " must not be blank");
		}
		return value;
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

	/**
	 * Chạy một lệnh ghi lên bảng người. {@link #requireUniqueDocument} đã kiểm tra trước, nhưng hai
	 * lệnh chạy cùng lúc đều có thể qua được bước đó; khi ấy khoá duy nhất
	 * {@code uk_reg_person_live_document} của database từ chối lệnh đến sau.
	 * @throws ConflictException nếu một người khác chưa xoá vừa được ghi với cùng giấy tờ
	 */
	private static Person rejectingSharedDocument(Supplier<Person> write) {
		try {
			return write.get();
		}
		catch (DuplicateKeyException ex) {
			throw new ConflictException("Another person already has this document");
		}
	}

	Map<String, Object> validAttributes(WorkspaceContext ctx, Map<String, Object> values) {
		return this.attributeValidation.validate(ctx.workspaceId(), FieldTarget.PERSON, 0, values);
	}

	void record(WorkspaceContext ctx, Person person, ChangeAction action, String note) {
		this.history.record(ctx, ENTITY_TYPE, person.id(), person.version(), action, PersonSnapshot.of(person), note);
	}

}
