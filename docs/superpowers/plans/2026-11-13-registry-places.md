# Registry Places Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bước 2 của logic nghiệp vụ: địa điểm lồng nhiều cấp với tạo, sửa, di chuyển, xoá mềm, khôi phục, địa chỉ, các cách đọc, lịch sử version, khôi phục về version cũ và hoạt động gần đây của không gian.

**Architecture:** Package `com.home_platform.registry.place` theo ba tầng như bước 1. `PlaceRepository` kế thừa `WorkspaceRepository`; địa chỉ có khoá `(workspace_id, place_id)` nên `PlaceAddressRepository` viết SQL tay với `EntityMetadata`. Mỗi lần ghi gọi `EntityHistory.record` trong cùng transaction với snapshot `PlaceSnapshot` (địa điểm kèm địa chỉ). Hoạt động gần đây đọc thẳng `reg_entity_version` trong package `history` của registry.

**Tech Stack:** như bước 1; dùng thêm `EntityHistory`, `KeysetQuery`, `CursorCodec`, `IdGenerator` của `platform-common`.

**Spec:** `docs/superpowers/specs/2026-11-13-business-rules-design.md` mục 3, 5.4, 5.5, 5.10. Thiết kế database: `docs/superpowers/specs/2026-11-11-database-design.md` (bảng `reg_place`, `reg_place_address`, `reg_entity_version`, mục 7).

## Global Constraints

- Mọi ràng buộc chung của kế hoạch bước 1 (`docs/superpowers/plans/2026-11-13-registry-catalogs.md`) vẫn áp dụng: `JdbcClient` trong `@Repository`, `workspace_id` trước tiên, request và response riêng, MapStruct, chú thích tiếng Việt, cắt khoảng trắng, mã lỗi 400/404/409, test `*IT` kế thừa `RegistryIT` trên UAT và rollback, commit mỗi task, push sau task cuối.
- Không sửa `platform-common`. Chỉ thêm `platform.history.table: reg_entity_version` vào `registry-service/src/main/resources/application.yaml`.
- Mọi lệnh ghi lên địa điểm (sửa, di chuyển, đặt hoặc gỡ địa chỉ, xoá, khôi phục, khôi phục version) nhận `version` client đã đọc; lệch thì 409 "Place <id> was changed by someone else" (thông báo sẵn của `WorkspaceRepository`). Body JSON mang `version` và `note` (tuỳ chọn, tối đa 255); `DELETE` mang chúng ở tham số URL.
- Lệnh sửa không bao giờ đổi `parentId`, `path`, `depth`; chỉ "di chuyển" đổi chúng.
- `entityType` của version là `"PLACE"`. Mỗi thao tác ghi đúng một version cho địa điểm được thao tác (di chuyển: không ghi version cho cây con).
- 20 cấp: `depth` từ 0 đến 19.
- Thông báo lỗi tiếng Anh, giữ đúng chữ trong kế hoạch này vì test so sánh nguyên văn.

**Lệnh chạy:** như bước 1. `platform-common` đã cài vào `~/.m2`; nếu `~/.m2` thiếu thì chạy `./mvnw -q -N install && ./mvnw -q -pl platform-common install -DskipTests` trước.

## Review Focus

1. Di chuyển một địa điểm có cây con đã xoá mềm, rồi khôi phục cây con đó: nó phải nằm đúng chỗ mới, không phải chỗ cũ. Test: `deletedDescendantsMoveToo` (Task 3).
2. Client giữ bản cũ của một phòng trước khi toà nhà bị di chuyển, rồi sửa phòng đó: phải nhận 409, không ghi đè `path` mới. Test: `staleChildEditAfterMoveIsAConflict` (Task 3).
3. Tìm theo tên có ký tự `%` hoặc `_` (ví dụ "50%") phải tìm đúng nghĩa đen. Test: `searchTreatsWildcardsLiterally` (Task 5).
4. Phân trang hoạt động gần đây khi nhiều version có cùng `changed_at` (cùng mili giây) không sót, không lặp. Test: `activityPagesWithoutGapsWhenTimesAreEqual` (Task 6).
5. Khôi phục về một version mà loại địa điểm lúc đó nay đã lưu trữ, hoặc `attributes` lúc đó nay không hợp lệ: trả 409 hoặc 400 rõ ràng, không ghi gì. Test: `restoreOfInvalidOldContentWritesNothing` (Task 6).

---

### Task 1: Tạo, xem, sửa địa điểm

**Files:**
- Modify: `registry-service/src/main/resources/application.yaml` (thêm `platform.history.table: reg_entity_version`)
- Create: `registry-service/src/main/java/com/home_platform/registry/place/{PlaceStatus,Place,PlaceRepository,PlaceSnapshot,PlaceService,PlaceController,PlaceMapper,PlaceCreateRequest,PlaceUpdateRequest,PlaceResponse,PlaceDetailResponse,PlaceRef}.java`
- Test: `registry-service/src/test/java/com/home_platform/registry/place/{PlaceTestData,PlaceIT}.java`

**Interfaces:**
- Consumes: `PlaceTypeService.requireActive/get`, `AttributeValidation.validate`, `FieldTarget.PLACE` (bước 1); `WorkspaceRepository`, `EntityHistory`, `ChangeAction`, `IdGenerator` (platform-common).
- Produces:

```java
public enum PlaceStatus { ACTIVE, INACTIVE }

@With
public record Place(long workspaceId, long id, Long parentId, long placeTypeId, String path, int depth, String code,
		String name, String description, PlaceStatus status, @JsonColumn Map<String, Object> attributes,
		Long coverFileId, Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version,
		Instant deletedAt) {
	public static final int MAX_DEPTH = 19;
	/** Các id trong path, từ gốc tới chính nó. */
	public List<Long> pathIds()
}

/** Snapshot ghi vào version: nội dung, không có cột kiểm toán và version (đã nằm trên dòng version). */
public record PlaceSnapshot(long id, Long parentId, long placeTypeId, String path, int depth, String code,
		String name, String description, PlaceStatus status, Map<String, Object> attributes, Long coverFileId,
		Instant deletedAt) {                               // Task 2 thêm thành phần cuối AddressSnapshot address
	public static PlaceSnapshot of(Place place)            // Task 2 đổi thành of(Place place, PlaceAddress address)
}

public record PlaceRef(long id, String name) {}

@Service
public class PlaceService {
	public static final String ENTITY_TYPE = "PLACE";
	public Place get(long workspaceId, long id)                       // 404 nếu không có hoặc đã xoá
	public PlaceView view(long workspaceId, long id)                  // Place + tổ tiên (+ địa chỉ từ Task 2)
	public Place create(WorkspaceContext ctx, PlaceCreateRequest request)
	public Place update(WorkspaceContext ctx, long id, PlaceUpdateRequest request)
}

public record PlaceView(Place place, List<PlaceRef> ancestors) {}  // Task 2 thêm address, effectiveAddress
```

  - `PlaceRepository`: `List<PlaceRef> refs(long workspaceId, List<Long> ids)` (id và tên, theo thứ tự id truyền vào).
  - `PlaceCreateRequest(Long parentId, Long placeTypeId, String code, String name, String description, PlaceStatus status, Map<String, Object> attributes, Long coverFileId, String note)`: `placeTypeId` `@NotNull`; `name` `@NotBlank @Size(max = 255)`; `code` `@Size(max = 50)`; `description` `@Size(max = 10000)`; `note` `@Size(max = 255)`; `status` null thành ACTIVE.
  - `PlaceUpdateRequest(Integer version, Long placeTypeId, String code, String name, String description, PlaceStatus status, Map<String, Object> attributes, Long coverFileId, String note)`: `version`, `placeTypeId`, `status` `@NotNull`; còn lại như trên.
  - Tạo: cha (nếu có) qua `get` (404); `parent.depth() + 1 > MAX_DEPTH` thì `ConflictException("Places can be nested at most 20 levels deep")`. `id = idGenerator.nextId()`; `path` = `path` của cha (hoặc `/`) + `id` + `/`. Loại qua `requireActive`. `attributes` qua `AttributeValidation`. Ghi version CREATE.
  - Sửa: đọc dòng hiện tại; loại khác loại cũ thì `requireActive`, giống thì chấp nhận dù đã lưu trữ; đặt các trường sửa được, giữ `version` client gửi; `repository.update`. Ghi version UPDATE.
  - `PlaceResponse` (dùng cho danh sách): `id, parentId, placeTypeId, code, name, status, depth, coverFileId, version`. `PlaceDetailResponse`: mọi trường của `Place` trừ `workspaceId`, cộng `ancestors` (`List<PlaceRef>`, từ gốc tới cha); Task 2 thêm `address`, `effectiveAddress`.
  - HTTP: `POST /places` 201 → detail; `GET /places/{id}` → detail; `PUT /places/{id}` → detail.
  - `PlaceTestData` (test): `long type(String code)` tạo loại qua `PlaceTypeService`; `JSON` dựng body; dùng chung cho các IT của package.

- [ ] **Step 1: Viết test hỏng** (`PlaceIT extends RegistryIT`).

```java
@Test void createsATopLevelPlace()
	// POST {placeTypeId, name " Toà A ", code "A"} -> 201; $.id chuỗi; $.parentId null; $.depth 0;
	// $.path "/<id>/"; $.name "Toà A"; $.status "ACTIVE"; $.version 1; $.ancestors rỗng
@Test void createsAChildWithPathAndAncestors()
	// A -> B (cha A) -> C (cha B): C.depth 2, C.path "/A/B/C/"; GET C -> ancestors [{A.id, "Toà A"}, {B.id, "Tầng 1"}]
@Test void nestingStopsAtTwentyLevels()
	// dựng chuỗi 20 cấp (depth 0..19) -> tạo con của cấp 19 -> 409 "Places can be nested at most 20 levels deep"
@Test void parentMustExistInThisWorkspace()     // parentId không có, hoặc là địa điểm của WS_B -> 404
@Test void archivedTypeIsRejectedForNewPlaces() // 409 "PlaceType <id> is archived"
@Test void attributesAreValidated()
	// mục area NUMBER cho mọi PLACE: {"area":"x"} -> 400, $.errors[0].field "attributes.area"; {"area":42} -> lưu 42
@Test void createRecordsVersionOne()
	// EntityHistory.get(WS_A, "PLACE", id, 1): action CREATE, snapshot.name, snapshot.path;
	// snapshot không có createdAt, updatedAt, version
@Test void updateWritesFieldsAndBumpsVersion()
	// PUT {version 1, name "B", placeTypeId cùng loại, status INACTIVE, note "đổi tên"} -> $.version 2, $.name "B";
	// version 2: action UPDATE, changedFields chứa "name" và "status", note "đổi tên"
@Test void updateWithStaleVersionIsAConflict()   // PUT version 1 hai lần -> lần hai 409
@Test void updateNeverMovesThePlace()            // body có "parentId", "path", "depth" -> bị bỏ qua, chỗ đứng không đổi
@Test void keepingAnArchivedTypeIsAllowedButSwitchingToOneIsNot()
	// tạo với loại X, lưu trữ X: PUT giữ X -> 200; tạo loại Y rồi lưu trữ, PUT sang Y -> 409
@Test void otherWorkspaceCannotSeeOrEdit()       // GET/PUT với WS_B -> 404
```

- [ ] **Step 2: Chạy** `-Dit.test=PlaceIT`. Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết các lớp ở Interfaces.** `Place.pathIds()` tách `path` theo `/`.
- [ ] **Step 4: Chạy lại.** Mong đợi: PASS; rồi `./mvnw -pl registry-service verify` PASS.
- [ ] **Step 5: Commit** `Add places: create, read, update with versions`.

---

### Task 2: Địa chỉ

**Files:**
- Create: `registry-service/src/main/java/com/home_platform/registry/support/Geohash.java`
- Create: `registry-service/src/main/java/com/home_platform/registry/place/{PlaceAddress,PlaceAddressRepository,AddressSnapshot,AddressRequest,AddressResponse,EffectiveAddressResponse,PlaceAddressRequest}.java`
- Modify: `PlaceService`, `PlaceView`, `PlaceCreateRequest` (thêm `AddressRequest address`), `PlaceDetailResponse`, `PlaceController`, `PlaceMapper`, `PlaceSnapshot` (thêm `AddressSnapshot address`, `of(Place, PlaceAddress)`)
- Test: `registry-service/src/test/java/com/home_platform/registry/support/GeohashTests.java`, `.../place/PlaceAddressIT.java`

**Interfaces:**
- Consumes: Task 1.
- Produces:

```java
public final class Geohash {
	/** Mã geohash base32 chuẩn. */
	public static String encode(double latitude, double longitude, int precision)
}

@With
public record PlaceAddress(long workspaceId, long placeId, String countryCode, String provinceCode,
		String provinceName, String districtCode, String districtName, String wardCode, String wardName,
		String street, String fullAddress, BigDecimal latitude, BigDecimal longitude, String geohash,
		Instant createdAt, Instant updatedAt) {}

public record AddressSnapshot(String countryCode, String provinceCode, String provinceName, String districtCode,
		String districtName, String wardCode, String wardName, String street, String fullAddress,
		BigDecimal latitude, BigDecimal longitude, String geohash) {
	public static AddressSnapshot of(PlaceAddress address)          // null -> null
}

public record AddressRequest(String countryCode, String provinceCode, String provinceName, String districtCode,
		String districtName, String wardCode, String wardName, String street, String fullAddress,
		BigDecimal latitude, BigDecimal longitude) {}
public record PlaceAddressRequest(Integer version, @Valid AddressRequest address, String note) {}

public record PlaceView(Place place, List<PlaceRef> ancestors, PlaceAddress address,
		PlaceAddress effectiveAddress, Long effectiveAddressFrom) {}

// PlaceService thêm
public Place setAddress(WorkspaceContext ctx, long id, PlaceAddressRequest request)
public Place removeAddress(WorkspaceContext ctx, long id, int version, String note)
public PlaceAddress address(long workspaceId, long placeId)          // null nếu không có
```

  - `PlaceAddressRepository` (SQL tay qua `EntityMetadata.of(PlaceAddress.class, json)`): `Optional<PlaceAddress> find(long workspaceId, long placeId)`; `List<PlaceAddress> findAll(long workspaceId, List<Long> placeIds)`; `void upsert(long workspaceId, PlaceAddress address)` (`INSERT ... ON DUPLICATE KEY UPDATE` mọi cột trừ `created_at`); `boolean delete(long workspaceId, long placeId)`.
  - `AddressRequest`: `fullAddress` `@NotBlank @Size(max = 500)`; `countryCode` `@Pattern(regexp = "[A-Za-z]{2}")`, viết hoa, null thành `VN`; mã `@Size(max = 10)`; tên `@Size(max = 100)`; `street` `@Size(max = 255)`; `latitude` `@DecimalMin("-90") @DecimalMax("90")`, `longitude` `@DecimalMin("-180") @DecimalMax("180")`. Có một trong hai toạ độ mà thiếu cái kia: `BadRequestException("latitude and longitude must be given together")`. Toạ độ làm tròn 6 chữ số thập phân (HALF_UP) trước khi lưu; `geohash = Geohash.encode(lat, lng, 9)`.
  - Đặt hoặc gỡ địa chỉ: `repository.update` của địa điểm với `version` client gửi (tăng version, cập nhật `updated_*`, không đổi trường nào khác), ghi địa chỉ, rồi version UPDATE với snapshot có địa chỉ mới. Gỡ khi không có địa chỉ: `NotFoundException("Place <id> has no address")`.
  - Tạo kèm `address` trong `PlaceCreateRequest`: ghi địa chỉ trước khi ghi version CREATE.
  - Địa chỉ hiệu lực: địa chỉ của chính nó, nếu không thì của tổ tiên sâu nhất có địa chỉ (một lần `findAll` với `pathIds()`); `effectiveAddressFrom` là `id` địa điểm sở hữu địa chỉ đó; không có thì cả hai null.
  - `PlaceDetailResponse` thêm `address` và `effectiveAddress` (`EffectiveAddressResponse(long fromPlaceId, AddressResponse address)`).
  - HTTP: `PUT /places/{id}/address` (body `PlaceAddressRequest`) → detail; `DELETE /places/{id}/address?version=&note=` → detail.

- [ ] **Step 1: Viết test hỏng.**

```java
// GeohashTests
@Test void matchesKnownValues()     // encode(57.64911, 10.40744, 11) = "u4pruydqqvj"; encode(42.6, -5.6, 5) = "ezs42"
@Test void precisionNineIsAPrefixOfLonger()   // encode(21.028511, 105.804817, 9) là tiền tố của bản 12 ký tự

// PlaceAddressIT
@Test void setsReplacesAndRemovesTheAddressAsPlaceVersions()
	// PUT address {version 1, address {fullAddress "1 Tràng Tiền", provinceCode "01", latitude 21.0245, longitude 105.8575}}
	// -> $.version 2, $.address.countryCode "VN", $.address.geohash 9 ký tự; version 2 snapshot.address.fullAddress;
	// PUT lần nữa (version 2) đổi fullAddress -> version 3; DELETE (version 3) -> version 4, $.address null,
	// snapshot.address null; DELETE lần nữa -> 404 "Place <id> has no address"
@Test void coordinatesComeInPairsAndInRange()
	// chỉ latitude -> 400 "latitude and longitude must be given together"; latitude 91 -> 400; country "VNM" -> 400
@Test void coordinatesAreRoundedToSixDecimals()   // 21.0245678 -> 21.024568
@Test void childUsesTheNearestAncestorsAddress()
	// A có địa chỉ, B (con A) không, C (con B) có: GET B -> address null, effectiveAddress.fromPlaceId = A;
	// GET C -> effectiveAddress.fromPlaceId = C; địa điểm không ai có -> effectiveAddress null
@Test void createWithAddressStoresBoth()           // POST kèm address -> version 1 snapshot có address
@Test void staleVersionIsAConflict()               // PUT address với version cũ -> 409, địa chỉ không đổi
```

- [ ] **Step 2: Chạy** `GeohashTests` và `PlaceAddressIT`. Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết code.**
- [ ] **Step 4: Chạy lại** cùng `PlaceIT`. Mong đợi: PASS.
- [ ] **Step 5: Commit** `Add place addresses with effective address`.

---

### Task 3: Di chuyển

**Files:**
- Create: `registry-service/src/main/java/com/home_platform/registry/place/PlaceMoveRequest.java`
- Modify: `PlaceRepository`, `PlaceService`, `PlaceController`
- Test: `registry-service/src/test/java/com/home_platform/registry/place/PlaceMoveIT.java`

**Interfaces:**
- Consumes: Task 1, 2.
- Produces:

```java
public record PlaceMoveRequest(@NotNull Integer version, Long parentId, @Size(max = 255) String note) {}   // parentId null: lên cấp cao nhất

// PlaceService
public Place move(WorkspaceContext ctx, long id, PlaceMoveRequest request)

// PlaceRepository
public int maxDepthUnder(long workspaceId, String path)               // MAX(depth) của mọi dòng có path bắt đầu bằng path, kể cả đã xoá
/** Đổi path, depth của mọi hậu duệ (kể cả đã xoá), trừ chính nó; tăng version, cập nhật updated_at/by. Trả số dòng. */
public int moveDescendants(WorkspaceContext ctx, String oldPath, String newPath, int depthDelta, Instant now)
```

  - Luật: đọc địa điểm (404); cha mới (nếu có) qua `get` (404); cha mới là chính nó hoặc `path` của cha mới bắt đầu bằng `path` của nó: `ConflictException("A place cannot be moved into itself or its descendants")`. Cha mới trùng cha hiện tại: không làm gì, trả địa điểm hiện tại, không ghi version (vẫn kiểm tra `version` và trả 409 nếu lệch). `maxDepthUnder(oldPath) - depth + newDepth > MAX_DEPTH`: `ConflictException("Places can be nested at most 20 levels deep")`.
  - Thứ tự ghi trong một transaction: `moveDescendants` (`UPDATE reg_place SET path = CONCAT(:newPath, SUBSTRING(path, :oldLength + 1)), depth = depth + :delta, version = version + 1, updated_at = :now, updated_by = :userId WHERE workspace_id = :workspaceId AND path LIKE :oldPrefix AND id <> :id`), rồi `repository.update` của chính nó với `parentId`, `path`, `depth` mới, rồi version UPDATE của chính nó.
  - HTTP: `POST /places/{id}/move` → detail.

- [ ] **Step 1: Viết test hỏng** (`PlaceMoveIT`).

```java
@Test void movesASubtree()
	// A, B; A > F > R (F con A, R con F). Move F sang B (version 1) -> F.parentId B, F.path "/B/F/", F.depth 1, F.version 2;
	// GET R -> path "/B/F/R/", depth 2, version 2 (tăng), ancestors [B, F]; history F có version 2 UPDATE với
	// changedFields chứa "parentId", "path"; history R chỉ có version 1
@Test void movesToTheTop()                         // move F với parentId null -> path "/F/", depth 0, R.depth 1
@Test void cannotMoveIntoItselfOrADescendant()     // move F vào F, vào R -> 409 "A place cannot be moved into itself or its descendants"
@Test void movingTooDeepIsRefused()
	// chuỗi 15 cấp X0..X14 và một cây 6 cấp Y0..Y5: move Y0 vào X14 -> 409 "Places can be nested at most 20 levels deep";
	// không dòng nào đổi
@Test void sameParentIsANoOp()                     // move F sang A (cha hiện tại) -> 200, version vẫn 1, không có version 2
@Test void deletedDescendantsMoveToo()
	// xoá mềm R bằng SQL tay (deleted_at) rồi move F sang B -> path của R trong database là "/B/F/R/"
@Test void staleChildEditAfterMoveIsAConflict()
	// đọc R (version 1); move F; PUT R với version 1 -> 409; path của R vẫn "/B/F/R/"
@Test void movingIntoAPlaceOfAnotherWorkspaceIsNotFound()
```

- [ ] **Step 2: Chạy.** Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết code.**
- [ ] **Step 4: Chạy lại** cùng các IT của package `place`. Mong đợi: PASS.
- [ ] **Step 5: Commit** `Add moving places with their subtree`.

---

### Task 4: Xoá và khôi phục

**Files:**
- Create: `registry-service/src/main/java/com/home_platform/registry/support/BusinessDate.java`, `.../place/PlaceRestoreRequest.java`
- Modify: `registry-service/src/main/resources/application.yaml` (thêm `registry.business-zone: Asia/Ho_Chi_Minh`), `PlaceRepository`, `PlaceService`, `PlaceController`
- Test: `registry-service/src/test/java/com/home_platform/registry/support/BusinessDateTests.java`, `.../place/PlaceDeleteIT.java`

**Interfaces:**
- Consumes: Task 1.
- Produces:

```java
@Component
public class BusinessDate {
	public BusinessDate(Clock clock, @Value("${registry.business-zone:Asia/Ho_Chi_Minh}") ZoneId zone)
	/** Ngày hôm nay theo múi giờ nghiệp vụ; dùng cho "đang ở", "còn ở đây". */
	public LocalDate today()
}

public record PlaceRestoreRequest(@NotNull Integer version, @Size(max = 255) String note) {}

// PlaceService
public void delete(WorkspaceContext ctx, long id, int version, String note)
public Place restore(WorkspaceContext ctx, long id, PlaceRestoreRequest request)

// PlaceRepository
public int countChildren(long workspaceId, long placeId)                    // con chưa xoá
public int countCurrentOccupancies(long workspaceId, long placeId, LocalDate today)
	// reg_occupancy chưa xoá, end_date IS NULL OR end_date >= today
public int countCurrentItems(long workspaceId, long placeId)               // reg_item chưa xoá, removed_at IS NULL
```

  - Xoá: đọc (404); đếm ba thứ; có cái nào khác 0 thì `ConflictException("Place <id> still has <a> child places, <b> occupancies and <c> items")`; `repository.softDelete(ctx, id, version)`; version DELETE.
  - Khôi phục: `findByIdIncludingDeleted` (404 nếu không có); chưa xoá thì để `WorkspaceRepository.restore` báo 409 "Place <id> is not deleted"; cha (nếu có) đang bị xoá: `ConflictException("Restore the parent place <parentId> first")`; `repository.restore`; version RESTORE.
  - HTTP: `DELETE /places/{id}?version=&note=` → 204; `POST /places/{id}/restore` (body `PlaceRestoreRequest`) → detail.

- [ ] **Step 1: Viết test hỏng.**

```java
// BusinessDateTests
@Test void todayFollowsTheBusinessZone()
	// Clock cố định 2026-11-13T18:30:00Z, zone Asia/Ho_Chi_Minh -> 2026-11-14; zone UTC -> 2026-11-13

// PlaceDeleteIT
@Test void deletesAndRestoresWithVersions()
	// DELETE ?version=1&note=x -> 204; GET -> 404; history: version 2 DELETE (note "x");
	// POST restore {version 2} -> 200, $.version 3, $.deletedAt null; history version 3 RESTORE
@Test void placeWithChildrenOccupantsOrItemsCannotBeDeleted()
	// A có một con chưa xoá; chèn bằng SQL tay một reg_person, một reg_occupancy end_date NULL và một reg_item chưa gỡ ở A
	// -> 409 "Place <A> still has 1 child places, 1 occupancies and 1 items"
@Test void pastOccupantsAndRemovedItemsDoNotBlock()
	// chỉ có occupancy end_date = hôm qua (theo BusinessDate) và item removed_at có giá trị -> xoá được
@Test void futureOccupantsBlock()                  // occupancy start_date ngày mai, end_date NULL -> 409
@Test void deletedChildDoesNotBlock()              // con đã xoá mềm -> xoá cha được
@Test void restoreNeedsTheParentFirst()
	// xoá con C rồi cha P; restore C -> 409 "Restore the parent place <P> first"; restore P rồi C -> được
@Test void restoreOfALivePlaceIsAConflict()        // 409 "Place <id> is not deleted"
@Test void staleVersionIsAConflict()               // DELETE ?version=7 -> 409, vẫn còn
```

- [ ] **Step 2: Chạy.** Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết code.**
- [ ] **Step 4: Chạy lại.** Mong đợi: PASS.
- [ ] **Step 5: Commit** `Add deleting and restoring places`.

---

### Task 5: Các cách đọc danh sách

**Files:**
- Modify: `PlaceRepository`, `PlaceService`, `PlaceController`
- Test: `registry-service/src/test/java/com/home_platform/registry/place/PlaceListIT.java`

**Interfaces:**
- Consumes: Task 1, 2; `KeysetQuery`, `CursorRequest`, `CursorPage` (platform-common).
- Produces (`PlaceService`, mỗi hàm trả `CursorPage<Place>` và chỉ thấy địa điểm chưa xoá):

```java
public CursorPage<Place> topLevel(long workspaceId, CursorRequest page)                  // parent_id IS NULL, theo name
public CursorPage<Place> children(long workspaceId, long parentId, CursorRequest page)    // 404 nếu cha không có; theo name
public CursorPage<Place> subtree(long workspaceId, long rootId, CursorRequest page)       // hậu duệ, không gồm gốc; theo path
public CursorPage<Place> byType(long workspaceId, long placeTypeId, CursorRequest page)   // theo name
public CursorPage<Place> searchByName(long workspaceId, String prefix, CursorRequest page) // name LIKE prefix%, theo name
public CursorPage<Place> findByCode(long workspaceId, String code, CursorRequest page)    // code = :code, theo id
public CursorPage<Place> byArea(long workspaceId, String provinceCode, String wardCode, CursorRequest page)
```

  - Các hàm theo tên, loại, cha, cây con, mã dùng `keyset()` của `PlaceRepository` với đúng index ở mục 7 của thiết kế database.
  - `searchByName`: prefix qua `Text.trimToNull`; rỗng: `BadRequestException("name must not be blank")`; thoát `\`, `%`, `_` bằng `\` rồi nối `%`.
  - `byArea`: `provinceCode` bắt buộc (400 nếu thiếu); SQL tay trong `PlaceRepository` (`public CursorPage<Place> byArea(long workspaceId, String provinceCode, String wardCode, CursorRequest page)`) join `reg_place_address` với `reg_place` (`deleted_at IS NULL`), lọc `province_code` và `ward_code` nếu có, sắp theo `place_id`, cursor `[place_id]` qua `CursorCodec`. Đặt ở đây vì chỉ `PlaceRepository` có bộ đọc dòng của `Place`, và luật kiến trúc không cho repository mở hàm public không nhận `workspaceId` trước.
  - HTTP, mỗi đường dẫn nhận `cursor`, `limit` và trả `CursorPage<PlaceResponse>`: `GET /places` (cấp cao nhất), `GET /places/{id}/children`, `GET /places/{id}/subtree`, `GET /places/search?name=` hoặc `?code=` (đúng một trong hai, nếu không 400 "Give exactly one of name or code"), `GET /places/by-type/{typeId}`, `GET /places/by-area?province=&ward=`.

- [ ] **Step 1: Viết test hỏng** (`PlaceListIT`).

```java
@Test void topLevelAndChildrenAreSortedByNameAndPaged()
	// cấp cao nhất "B", "A", "C" (và một con của A); limit 2: trang 1 [A, B], trang 2 [C], nextCursor null
	// con của A: "P2", "P1" -> [P1, P2]
@Test void subtreeListsDescendantsInPathOrderWithoutTheRoot()
	// A > (X > Y), A > Z: subtree(A) có đúng X, Y, Z (thứ tự theo path), không có A
@Test void deletedPlacesAreHidden()                // con đã xoá không có trong children, subtree, search
@Test void byTypeFiltersOnType()
@Test void searchByNamePrefixIgnoresCaseAndAccents()   // "Phòng 101", "phong 102", "Kho": search "PHONG" -> 2 kết quả
@Test void searchTreatsWildcardsLiterally()           // "50% off", "500 m", "5_x": "50%" -> chỉ "50% off"; "5_" -> chỉ "5_x"
@Test void searchByCodeMatchesExactly()               // code "P101" và "P1010": code "p101" -> chỉ P101
@Test void searchNeedsExactlyOneCriterion()           // không có, hoặc có cả hai -> 400
@Test void byAreaUsesTheAddressTable()
	// A tỉnh 01 phường 001, B tỉnh 01 phường 002, C tỉnh 79: province 01 -> A, B; province 01 ward 002 -> B; thiếu province -> 400
@Test void otherWorkspaceIsInvisible()               // mọi danh sách của WS_B rỗng
```

- [ ] **Step 2: Chạy.** Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết code.**
- [ ] **Step 4: Chạy lại.** Mong đợi: PASS.
- [ ] **Step 5: Commit** `Add place listings and search`.

---

### Task 6: Lịch sử, khôi phục version, hoạt động gần đây

**Files:**
- Create: `registry-service/src/main/java/com/home_platform/registry/place/{PlaceVersionRestoreRequest,PlaceVersionResponse,PlaceVersionSummaryResponse}.java`
- Create: `registry-service/src/main/java/com/home_platform/registry/history/{ActivityEntry,ActivityRepository,ActivityService,ActivityController,ActivityResponse}.java`
- Modify: `PlaceService`, `PlaceController`
- Test: `registry-service/src/test/java/com/home_platform/registry/place/PlaceVersionIT.java`, `.../history/ActivityIT.java`

**Interfaces:**
- Consumes: Task 1 đến 4; `EntityHistory` (`list`, `get`, `snapshotAs`), `CursorCodec`.
- Produces:

```java
public record PlaceVersionRestoreRequest(@NotNull Integer version, @Size(max = 255) String note) {}

// PlaceService
public CursorPage<EntityVersionSummary> versions(long workspaceId, long id, CursorRequest page)   // kể cả địa điểm đã xoá; 404 nếu chưa từng có
public EntityVersion version(long workspaceId, long id, int versionNo)
public Place restoreVersion(WorkspaceContext ctx, long id, int versionNo, PlaceVersionRestoreRequest request)

public record ActivityEntry(String entityType, long entityId, int versionNo, ChangeAction action,
		List<String> changedFields, long changedBy, Instant changedAt, String note) {}

@Repository
public class ActivityRepository {
	/** Mới nhất trước; entityType null là mọi loại. Cursor: [changedAt, entityType, entityId, versionNo]. */
	public CursorPage<ActivityEntry> list(long workspaceId, String entityType, CursorRequest page)
}
```

  - Khôi phục version: địa điểm phải chưa xoá (`get`, 404; đang xoá thì `ConflictException("Restore place <id> before restoring an old version")` khi `findByIdIncludingDeleted` thấy có). Đọc snapshot bằng `snapshotAs(..., PlaceSnapshot.class)`; áp các trường nội dung: `placeTypeId`, `code`, `name`, `description`, `status`, `attributes`, `coverFileId`, và địa chỉ (đặt hoặc gỡ). Không đổi `parentId`, `path`, `depth`. Kiểm tra như một lần sửa (loại, `attributes`); một lần `repository.update` với `version` client gửi; một version UPDATE với note `"Restored from version <n>"`, nếu client có note thì nối `": <note>"` và cắt còn 255 ký tự.
  - `ActivityRepository`: SQL tay trên `reg_entity_version`, `ORDER BY changed_at DESC, entity_type DESC, entity_id DESC, version_no DESC` (index `(workspace_id, changed_at)` cộng khoá chính đi kèm). Điều kiện sau cursor viết dạng lồng: `changed_at < :t OR (changed_at = :t AND (entity_type < :e OR (entity_type = :e AND (entity_id < :i OR (entity_id = :i AND version_no < :v)))))`. Cursor hỏng hoặc sai số phần tử: `BadRequestException("Invalid cursor")`.
  - HTTP: `GET /places/{id}/versions` (cursor, limit) → `CursorPage<PlaceVersionSummaryResponse>`; `GET /places/{id}/versions/{no}` → `PlaceVersionResponse` (kèm `snapshot`, đổi sang `PlaceSnapshot` trước khi trả để `long` ra chuỗi); `POST /places/{id}/versions/{no}/restore` → detail; `GET /activity?entityType=&cursor=&limit=` → `CursorPage<ActivityResponse>`.

- [ ] **Step 1: Viết test hỏng.**

```java
// PlaceVersionIT
@Test void listsVersionsNewestFirst()          // tạo, sửa hai lần -> [3, 2, 1]; limit 2 -> trang 1 [3, 2], trang 2 [1]
@Test void showsOneVersionWithItsSnapshot()    // GET versions/1 -> $.snapshot.name tên ban đầu, $.snapshot.id là chuỗi
@Test void deletedPlaceKeepsItsHistory()       // xoá -> GET versions vẫn có DELETE ở đầu
@Test void restoresContentButNotPosition()
	// tạo A (tên "Một", địa chỉ X) dưới P1; sửa tên "Hai", gỡ địa chỉ; move sang P2;
	// restore version 1 với version hiện tại -> tên "Một", địa chỉ X, vẫn dưới P2;
	// version mới: action UPDATE, note "Restored from version 1"
@Test void restoreOfInvalidOldContentWritesNothing()
	// version 1 dùng loại T; lưu trữ T sau khi đổi sang loại U -> restore version 1 -> 409; version không tăng
	// mục bắt buộc mới thêm sau version 1 -> restore version 1 -> 400; version không tăng
@Test void restoringADeletedPlaceIsAConflict()  // 409 "Restore place <id> before restoring an old version"
@Test void unknownVersionIsNotFound()           // versions/99 -> 404 "PLACE <id> version 99 not found"

// ActivityIT
@Test void listsChangesNewestFirstAcrossPlaces()  // tạo A, tạo B, sửa A -> [A v2, B v1, A v1]
@Test void filtersByEntityType()                  // entityType=PLACE có; entityType=ITEM rỗng
@Test void activityPagesWithoutGapsWhenTimesAreEqual()
	// chèn bằng SQL tay 7 dòng reg_entity_version của WS_A cùng changed_at, khác entity_id/version_no;
	// đi hết các trang với limit 2 -> đủ 7, không trùng
@Test void otherWorkspaceIsInvisible()
@Test void badCursorIs400()
```

- [ ] **Step 2: Chạy.** Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết code.**
- [ ] **Step 4: Chạy lại.** Mong đợi: PASS.
- [ ] **Step 5: Commit** `Add place history, restore to version and workspace activity`.

---

### Task 7: Tài liệu, kiểm tra toàn bộ, chạy thử

**Files:**
- Modify: `README.md` (bảng "Nghiệp vụ đã có": thêm các đường dẫn `/places...` và `/activity`; ghi rằng mọi lệnh ghi lên địa điểm cần `version`)

- [ ] **Step 1: Cập nhật README.**
- [ ] **Step 2: Kiểm tra toàn bộ.** `./mvnw verify` ở gốc. Mong đợi: `BUILD SUCCESS`, không test nào bị bỏ qua.
- [ ] **Step 3: Đo phân trang trang sâu.** Một IT tạm (xoá sau khi đo) tạo 5.000 địa điểm con của một cha trong transaction rollback và chạy `EXPLAIN ANALYZE` cho `children` sau cursor ở gần cuối và cho `/activity` sau cursor sâu. Mong đợi: thời gian thực tế cỡ trang đầu (dưới 5 ms), không quét lại từ đầu. Ghi kết quả vào sổ.
- [ ] **Step 4: Chạy thử.** Chạy `registry-service`, với `X-Workspace-Id: -9`: `POST /setup`, tạo toà nhà kèm địa chỉ, tạo tầng, xem tầng (có `effectiveAddress`), xem `/activity`; dừng service; xoá mọi dòng của `workspace_id = -9` trong `reg_place_address`, `reg_place`, `reg_entity_version`, `reg_field_def`, `reg_place_type`, `reg_item_category` (theo thứ tự đó), kiểm tra còn 0 dòng.
- [ ] **Step 5: Commit** `Document registry places` **và push** `main`.
