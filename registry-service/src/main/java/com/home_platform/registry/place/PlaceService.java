package com.home_platform.registry.place;

import java.util.List;
import java.util.Map;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.history.ChangeAction;
import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.id.IdGenerator;
import com.home_platform.common.web.ConflictException;
import com.home_platform.registry.fielddef.AttributeValidation;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Địa điểm của một không gian. Mỗi lần ghi để lại một version (loại bản ghi {@value #ENTITY_TYPE})
 * trong cùng transaction. Lệnh sửa không bao giờ đổi chỗ đứng (cha, {@code path}, {@code depth}).
 */
@Service
public class PlaceService {

	public static final String ENTITY_TYPE = "PLACE";

	private final PlaceRepository repository;

	private final PlaceTypeService placeTypes;

	private final AttributeValidation attributeValidation;

	private final EntityHistory history;

	private final IdGenerator idGenerator;

	PlaceService(PlaceRepository repository, PlaceTypeService placeTypes, AttributeValidation attributeValidation,
			EntityHistory history, IdGenerator idGenerator) {
		this.repository = repository;
		this.placeTypes = placeTypes;
		this.attributeValidation = attributeValidation;
		this.history = history;
		this.idGenerator = idGenerator;
	}

	/** @throws com.home_platform.common.web.NotFoundException nếu không có hoặc đã xoá */
	public Place get(long workspaceId, long id) {
		return this.repository.getById(workspaceId, id);
	}

	public PlaceView view(long workspaceId, long id) {
		Place place = get(workspaceId, id);
		List<Long> ids = place.pathIds();
		return new PlaceView(place, this.repository.refs(workspaceId, ids.subList(0, ids.size() - 1)));
	}

	@Transactional
	public Place create(WorkspaceContext ctx, PlaceCreateRequest request) {
		Place parent = (request.parentId() != null) ? get(ctx.workspaceId(), request.parentId()) : null;
		int depth = (parent != null) ? parent.depth() + 1 : 0;
		if (depth > Place.MAX_DEPTH) {
			throw tooDeep();
		}
		this.placeTypes.requireActive(ctx.workspaceId(), request.placeTypeId());
		Map<String, Object> attributes = validAttributes(ctx, request.placeTypeId(), request.attributes());
		long id = this.idGenerator.nextId();
		String path = ((parent != null) ? parent.path() : "/") + id + "/";
		Place place = new Place(0, id, request.parentId(), request.placeTypeId(), path, depth, request.code(),
				request.name(), request.description(), request.status(), attributes, request.coverFileId(), null, 0,
				null, 0, 0, null);
		Place stored = this.repository.insert(ctx, place);
		record(ctx, stored, ChangeAction.CREATE, request.note());
		return stored;
	}

	@Transactional
	public Place update(WorkspaceContext ctx, long id, PlaceUpdateRequest request) {
		Place current = get(ctx.workspaceId(), id);
		if (current.placeTypeId() != request.placeTypeId()) {
			this.placeTypes.requireActive(ctx.workspaceId(), request.placeTypeId());
		}
		Map<String, Object> attributes = validAttributes(ctx, request.placeTypeId(), request.attributes());
		Place stored = this.repository.update(ctx, current.withVersion(request.version())
			.withPlaceTypeId(request.placeTypeId())
			.withCode(request.code())
			.withName(request.name())
			.withDescription(request.description())
			.withStatus(request.status())
			.withAttributes(attributes)
			.withCoverFileId(request.coverFileId()));
		record(ctx, stored, ChangeAction.UPDATE, request.note());
		return stored;
	}

	private Map<String, Object> validAttributes(WorkspaceContext ctx, long placeTypeId, Map<String, Object> values) {
		return this.attributeValidation.validate(ctx.workspaceId(), FieldTarget.PLACE, placeTypeId, values);
	}

	private void record(WorkspaceContext ctx, Place place, ChangeAction action, String note) {
		this.history.record(ctx, ENTITY_TYPE, place.id(), place.version(), action, PlaceSnapshot.of(place), note);
	}

	static ConflictException tooDeep() {
		return new ConflictException("Places can be nested at most 20 levels deep");
	}

}
