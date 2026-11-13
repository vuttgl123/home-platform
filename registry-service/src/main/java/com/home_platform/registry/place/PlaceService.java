package com.home_platform.registry.place;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.history.ChangeAction;
import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.history.EntityVersion;
import com.home_platform.common.history.EntityVersionSummary;
import com.home_platform.common.id.IdGenerator;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.ConflictException;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.common.web.NotFoundException;
import com.home_platform.registry.catalog.Text;
import com.home_platform.registry.fielddef.AttributeValidation;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.placetype.PlaceTypeService;
import com.home_platform.registry.support.BusinessDate;
import com.home_platform.registry.support.Geohash;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Địa điểm của một không gian. Mỗi lần ghi để lại một version (loại bản ghi {@value #ENTITY_TYPE})
 * trong cùng transaction. Lệnh sửa không bao giờ đổi chỗ đứng (cha, {@code path}, {@code depth}).
 */
@Service
public class PlaceService {

	public static final String ENTITY_TYPE = "PLACE";

	private static final int GEOHASH_PRECISION = 9;

	private final PlaceRepository repository;

	private final PlaceTypeService placeTypes;

	private final AttributeValidation attributeValidation;

	private final EntityHistory history;

	private final IdGenerator idGenerator;

	private final PlaceAddressRepository addresses;

	private final Clock clock;

	private final BusinessDate businessDate;

	PlaceService(PlaceRepository repository, PlaceTypeService placeTypes, AttributeValidation attributeValidation,
			EntityHistory history, IdGenerator idGenerator, PlaceAddressRepository addresses, Clock clock,
			BusinessDate businessDate) {
		this.businessDate = businessDate;
		this.repository = repository;
		this.addresses = addresses;
		this.clock = clock;
		this.placeTypes = placeTypes;
		this.attributeValidation = attributeValidation;
		this.history = history;
		this.idGenerator = idGenerator;
	}

	/** @throws com.home_platform.common.web.NotFoundException nếu không có hoặc đã xoá */
	public Place get(long workspaceId, long id) {
		return this.repository.getById(workspaceId, id);
	}

	/** Địa điểm có tồn tại và chưa bị xoá không. */
	public boolean isAlive(long workspaceId, long id) {
		return this.repository.findById(workspaceId, id).isPresent();
	}

	/** Tên của các địa điểm (kể cả đã xoá) theo {@code id}; id không có thì không có trong kết quả. */
	public Map<Long, String> names(long workspaceId, List<Long> ids) {
		return this.repository.refs(workspaceId, ids)
			.stream()
			.collect(Collectors.toMap(PlaceRef::id, PlaceRef::name, (a, b) -> a));
	}

	public PlaceView view(long workspaceId, long id) {
		Place place = get(workspaceId, id);
		List<Long> ids = place.pathIds();
		Map<Long, PlaceAddress> byPlace = this.addresses.findAll(workspaceId, ids)
			.stream()
			.collect(Collectors.toMap(PlaceAddress::placeId, Function.identity()));
		Long from = null;
		for (int i = ids.size() - 1; i >= 0 && from == null; i--) {
			from = byPlace.containsKey(ids.get(i)) ? ids.get(i) : null;
		}
		return new PlaceView(place, this.repository.refs(workspaceId, ids.subList(0, ids.size() - 1)),
				byPlace.get(id), (from != null) ? byPlace.get(from) : null, from);
	}

	/**
	 * Đưa một địa điểm cùng cả cây con sang cha khác, hoặc lên cấp cao nhất. Chỉ địa điểm được
	 * chuyển có version mới; các hậu duệ được tăng {@code version} nhưng không ghi snapshot.
	 */
	@Transactional
	public Place move(WorkspaceContext ctx, long id, PlaceMoveRequest request) {
		Place current = get(ctx.workspaceId(), id);
		if (current.version() != request.version()) {
			throw new ConflictException("Place " + id + " was changed by someone else");
		}
		if (Objects.equals(request.parentId(), current.parentId())) {
			return current;
		}
		Place parent = (request.parentId() != null) ? get(ctx.workspaceId(), request.parentId()) : null;
		if (parent != null && parent.path().startsWith(current.path())) {
			throw new ConflictException("A place cannot be moved into itself or its descendants");
		}
		int depth = (parent != null) ? parent.depth() + 1 : 0;
		String path = ((parent != null) ? parent.path() : "/") + id + "/";
		int deepest = this.repository.maxDepthUnder(ctx.workspaceId(), current.path());
		if (deepest - current.depth() + depth > Place.MAX_DEPTH) {
			throw tooDeep();
		}
		Instant now = this.clock.instant().truncatedTo(ChronoUnit.MILLIS);
		this.repository.moveDescendants(ctx, current.path(), path, depth - current.depth(), now);
		Place stored = this.repository.update(ctx, current.withParentId(request.parentId()).withPath(path).withDepth(depth));
		record(ctx, stored, address(ctx.workspaceId(), id), ChangeAction.UPDATE, request.note());
		return stored;
	}

	/**
	 * Xoá mềm. Chỉ được khi không còn địa điểm con, người đang hoặc sắp ở, đồ đạc chưa gỡ.
	 * @throws ConflictException nếu còn một trong ba thứ đó, kèm số lượng từng thứ
	 */
	@Transactional
	public void delete(WorkspaceContext ctx, long id, int version, String note) {
		get(ctx.workspaceId(), id);
		int children = this.repository.countChildren(ctx.workspaceId(), id);
		int occupancies = this.repository.countCurrentOccupancies(ctx.workspaceId(), id, this.businessDate.today());
		int items = this.repository.countCurrentItems(ctx.workspaceId(), id);
		if (children + occupancies + items > 0) {
			throw new ConflictException("Place " + id + " still has " + children + " child places, " + occupancies
					+ " occupancies and " + items + " items");
		}
		Place stored = this.repository.softDelete(ctx, id, version);
		record(ctx, stored, address(ctx.workspaceId(), id), ChangeAction.DELETE, note);
	}

	/**
	 * Bỏ xoá.
	 * @throws ConflictException nếu chưa bị xoá, nếu cha đang bị xoá, hoặc {@code version} lệch
	 */
	@Transactional
	public Place restore(WorkspaceContext ctx, long id, PlaceRestoreRequest request) {
		Place deleted = this.repository.findByIdIncludingDeleted(ctx.workspaceId(), id)
			.orElseThrow(() -> NotFoundException.of("Place", id));
		if (deleted.deletedAt() != null && deleted.parentId() != null
				&& this.repository.findById(ctx.workspaceId(), deleted.parentId()).isEmpty()) {
			throw new ConflictException("Restore the parent place " + deleted.parentId() + " first");
		}
		Place stored = this.repository.restore(ctx, id, request.version());
		record(ctx, stored, address(ctx.workspaceId(), id), ChangeAction.RESTORE, request.note());
		return stored;
	}

	public CursorPage<Place> topLevel(long workspaceId, CursorRequest page) {
		return this.repository.topLevel(workspaceId, page);
	}

	/** @throws NotFoundException nếu cha không có hoặc đã xoá */
	public CursorPage<Place> children(long workspaceId, long parentId, CursorRequest page) {
		get(workspaceId, parentId);
		return this.repository.children(workspaceId, parentId, page);
	}

	/** Mọi hậu duệ chưa xoá, không gồm gốc. */
	public CursorPage<Place> subtree(long workspaceId, long rootId, CursorRequest page) {
		return this.repository.subtree(workspaceId, get(workspaceId, rootId).path(), page);
	}

	public CursorPage<Place> byType(long workspaceId, long placeTypeId, CursorRequest page) {
		return this.repository.byType(workspaceId, placeTypeId, page);
	}

	/** Tên bắt đầu bằng {@code prefix}; {@code %} và {@code _} trong prefix được hiểu theo nghĩa đen. */
	public CursorPage<Place> searchByName(long workspaceId, String prefix, CursorRequest page) {
		String cleaned = Text.trimToNull(prefix);
		if (cleaned == null) {
			throw new BadRequestException("name must not be blank");
		}
		return this.repository.searchByName(workspaceId, Text.likePrefix(cleaned), page);
	}

	public CursorPage<Place> findByCode(long workspaceId, String code, CursorRequest page) {
		String cleaned = Text.trimToNull(code);
		if (cleaned == null) {
			throw new BadRequestException("code must not be blank");
		}
		return this.repository.findByCode(workspaceId, cleaned, page);
	}

	public CursorPage<Place> byArea(long workspaceId, String provinceCode, String wardCode, CursorRequest page) {
		String province = Text.trimToNull(provinceCode);
		if (province == null) {
			throw new BadRequestException("province must not be blank");
		}
		return this.repository.byArea(workspaceId, province, Text.trimToNull(wardCode), page);
	}

	/**
	 * Các version của một địa điểm, mới nhất trước; thấy cả khi địa điểm đã bị xoá.
	 * @throws NotFoundException nếu địa điểm chưa từng có
	 */
	public CursorPage<EntityVersionSummary> versions(long workspaceId, long id, CursorRequest page) {
		this.repository.findByIdIncludingDeleted(workspaceId, id).orElseThrow(() -> NotFoundException.of("Place", id));
		return this.history.list(workspaceId, ENTITY_TYPE, id, page);
	}

	public EntityVersion version(long workspaceId, long id, int versionNo) {
		return this.history.get(workspaceId, ENTITY_TYPE, id, versionNo);
	}

	public PlaceSnapshot snapshotOf(EntityVersion version) {
		return this.history.snapshotAs(version, PlaceSnapshot.class);
	}

	/**
	 * Khôi phục nội dung của một version cũ, như một lần sửa bình thường: version mới mang nội
	 * dung cũ, không version nào bị ghi đè. Chỉ nội dung và địa chỉ; không đổi chỗ đứng. Nội dung
	 * cũ phải hợp lệ như một lần sửa (loại còn dùng được, {@code attributes} hợp với mục tuỳ biến
	 * hiện tại); nếu không thì không ghi gì.
	 * @throws ConflictException nếu địa điểm đang bị xoá
	 */
	@Transactional
	public Place restoreVersion(WorkspaceContext ctx, long id, int versionNo, PlaceVersionRestoreRequest request) {
		long workspaceId = ctx.workspaceId();
		Place current = this.repository.findById(workspaceId, id)
			.orElseThrow(() -> this.repository.findByIdIncludingDeleted(workspaceId, id).isPresent()
					? new ConflictException("Restore place " + id + " before restoring an old version")
					: NotFoundException.of("Place", id));
		PlaceSnapshot old = snapshotOf(version(workspaceId, id, versionNo));
		if (old.placeTypeId() != current.placeTypeId()) {
			this.placeTypes.requireActive(workspaceId, old.placeTypeId());
		}
		Map<String, Object> attributes = validAttributes(ctx, old.placeTypeId(), old.attributes());
		PlaceAddress address = (old.address() != null) ? fromSnapshot(id, old.address()) : null;
		Place stored = this.repository.update(ctx, current.withVersion(request.version())
			.withPlaceTypeId(old.placeTypeId())
			.withCode(old.code())
			.withName(old.name())
			.withDescription(old.description())
			.withStatus(old.status())
			.withAttributes(attributes)
			.withCoverFileId(old.coverFileId()));
		if (address != null) {
			this.addresses.upsert(workspaceId, address);
		}
		else {
			this.addresses.delete(workspaceId, id);
		}
		String note = "Restored from version " + versionNo + ((request.note() != null) ? ": " + request.note() : "");
		record(ctx, stored, address, ChangeAction.UPDATE, (note.length() > 255) ? note.substring(0, 255) : note);
		return stored;
	}

	private PlaceAddress fromSnapshot(long placeId, AddressSnapshot address) {
		Instant now = this.clock.instant().truncatedTo(ChronoUnit.MILLIS);
		return new PlaceAddress(0, placeId, address.countryCode(), address.provinceCode(), address.provinceName(),
				address.districtCode(), address.districtName(), address.wardCode(), address.wardName(),
				address.street(), address.fullAddress(), address.latitude(), address.longitude(), address.geohash(),
				now, now);
	}

	/** Địa chỉ của chính địa điểm; {@code null} nếu không có. */
	public PlaceAddress address(long workspaceId, long placeId) {
		return this.addresses.find(workspaceId, placeId).orElse(null);
	}

	/** Đặt (tạo hoặc thay) địa chỉ. Là một lần sửa địa điểm: kiểm tra và tăng {@code version}. */
	@Transactional
	public Place setAddress(WorkspaceContext ctx, long id, PlaceAddressRequest request) {
		Place current = get(ctx.workspaceId(), id);
		PlaceAddress address = toAddress(id, request.address());
		Place stored = this.repository.update(ctx, current.withVersion(request.version()));
		this.addresses.upsert(ctx.workspaceId(), address);
		record(ctx, stored, address, ChangeAction.UPDATE, request.note());
		return stored;
	}

	/** Gỡ địa chỉ. Là một lần sửa địa điểm: kiểm tra và tăng {@code version}. */
	@Transactional
	public Place removeAddress(WorkspaceContext ctx, long id, int version, String note) {
		Place current = get(ctx.workspaceId(), id);
		if (address(ctx.workspaceId(), id) == null) {
			throw new NotFoundException("Place " + id + " has no address");
		}
		Place stored = this.repository.update(ctx, current.withVersion(version));
		this.addresses.delete(ctx.workspaceId(), id);
		record(ctx, stored, null, ChangeAction.UPDATE, note);
		return stored;
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
		PlaceAddress address = (request.address() != null) ? toAddress(id, request.address()) : null;
		if (address != null) {
			this.addresses.upsert(ctx.workspaceId(), address);
		}
		record(ctx, stored, address, ChangeAction.CREATE, request.note());
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
		record(ctx, stored, address(ctx.workspaceId(), id), ChangeAction.UPDATE, request.note());
		return stored;
	}

	private Map<String, Object> validAttributes(WorkspaceContext ctx, long placeTypeId, Map<String, Object> values) {
		return this.attributeValidation.validate(ctx.workspaceId(), FieldTarget.PLACE, placeTypeId, values);
	}

	private void record(WorkspaceContext ctx, Place place, PlaceAddress address, ChangeAction action, String note) {
		this.history.record(ctx, ENTITY_TYPE, place.id(), place.version(), action, PlaceSnapshot.of(place, address),
				note);
	}

	/**
	 * Địa chỉ sẽ lưu: toạ độ đi theo cặp, làm tròn 6 chữ số thập phân như cột {@code DECIMAL(9,6)},
	 * và {@code geohash} 9 ký tự (khoảng 5 m) tính từ toạ độ.
	 */
	private PlaceAddress toAddress(long placeId, AddressRequest request) {
		if ((request.latitude() == null) != (request.longitude() == null)) {
			throw new BadRequestException("latitude and longitude must be given together");
		}
		BigDecimal latitude = round(request.latitude());
		BigDecimal longitude = round(request.longitude());
		String geohash = (latitude != null)
				? Geohash.encode(latitude.doubleValue(), longitude.doubleValue(), GEOHASH_PRECISION) : null;
		Instant now = this.clock.instant().truncatedTo(ChronoUnit.MILLIS);
		return new PlaceAddress(0, placeId, request.countryCode(), request.provinceCode(), request.provinceName(),
				request.districtCode(), request.districtName(), request.wardCode(), request.wardName(),
				request.street(), request.fullAddress(), latitude, longitude, geohash, now, now);
	}

	private static BigDecimal round(BigDecimal value) {
		return (value != null) ? value.setScale(6, RoundingMode.HALF_UP) : null;
	}

	static ConflictException tooDeep() {
		return new ConflictException("Places can be nested at most 20 levels deep");
	}

}
