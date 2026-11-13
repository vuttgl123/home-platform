# Registry People and Occupancy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bước 3 của logic nghiệp vụ: hồ sơ người (tạo, sửa, xoá, khôi phục, tìm, lịch sử version) và cư trú (ai ở đâu, từ khi nào tới khi nào).

**Architecture:** Hai package `com.home_platform.registry.person` và `...occupancy`, ba tầng như bước 2. Người có version (`entityType` `PERSON`) như địa điểm. Cư trú không có version: lịch sử của nó là chính các dòng với ngày bắt đầu, ngày kết thúc. `PersonRepository` tự đếm cư trú bằng SQL để `person` không phụ thuộc `occupancy`; `occupancy` dùng `PersonService` và `PlaceService` để kiểm tra người và địa điểm.

**Tech Stack:** như bước 2.

**Spec:** `docs/superpowers/specs/2026-11-13-business-rules-design.md` mục 3, 5.6, 5.7, 5.10.

## Global Constraints

- Mọi ràng buộc chung của kế hoạch bước 2 (`docs/superpowers/plans/2026-11-13-registry-places.md`) vẫn áp dụng: `version` cho mọi lệnh ghi lên người và cư trú (body, hoặc tham số URL với `DELETE`), `note` tuỳ chọn cho version của người, thông báo lỗi tiếng Anh đúng chữ, test `*IT` trên UAT và rollback, commit mỗi task, push sau task cuối.
- "Hôm nay" lấy từ `BusinessDate`. `end_date` là ngày cuối cùng còn ở. Trạng thái suy ra: `CURRENT` khi `start_date` ≤ hôm nay và (`end_date` trống hoặc ≥ hôm nay); `UPCOMING` khi `start_date` > hôm nay; `PAST` khi `end_date` < hôm nay.
- Hai khoảng ngày `[s1, e1]` và `[s2, e2]` (ngày cuối trống là vô hạn) giao nhau khi `s1 ≤ e2` và `s2 ≤ e1`.

## Review Focus

1. Sửa ngày của một dòng cư trú không được tự đụng vào chính nó khi kiểm tra trùng thời gian. Test: `editingADateDoesNotConflictWithItself` (Task 3).
2. Hai người trùng giấy tờ khi một người đã xoá: không chặn. Test: `deletedPersonDoesNotBlockTheSameDocument` (Task 1).
3. Số điện thoại gõ có dấu cách hay dấu chấm vẫn tìm ra. Test: `phoneSearchIgnoresSpacesAndDots` (Task 2).
4. Người ở cả toà nhà: cư trú ở phòng của toà nhà đã xoá mềm không hiện. Test: `subtreeOccupantsSkipDeletedPlaces` (Task 4).
5. Khôi phục version cũ của người có số giấy tờ nay đã thuộc người khác: 409, không ghi gì. Test: `restoreVersionChecksDocumentDuplicates` (Task 2).

---

### Task 1: Người: tạo, xem, sửa, xoá, khôi phục

**Files:**
- Create: `registry-service/src/main/java/com/home_platform/registry/person/{Gender,Person,PersonRepository,PersonSnapshot,PersonService,PersonController,PersonMapper,PersonRequest,PersonUpdateRequest,PersonRestoreRequest,PersonResponse,PersonDetailResponse}.java`
- Test: `registry-service/src/test/java/com/home_platform/registry/person/{PersonTestData,PersonIT}.java`

**Interfaces:**

```java
public enum Gender { MALE, FEMALE, OTHER }

@With
public record Person(long workspaceId, long id, String fullName, String phone, String email, String idType,
		String idNumber, LocalDate dateOfBirth, Gender gender, String note,
		@JsonSerialize(contentUsing = PlainNumbers.class) @JsonColumn Map<String, Object> attributes,
		Long avatarFileId, Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version,
		Instant deletedAt) {}

public record PersonSnapshot(long id, String fullName, String phone, String email, String idType, String idNumber,
		LocalDate dateOfBirth, Gender gender, String note, Map<String, Object> attributes, Long avatarFileId,
		Instant deletedAt) {                                      // attributes có contentUsing PlainNumbers
	public static PersonSnapshot of(Person person)
}

@Service
public class PersonService {
	public static final String ENTITY_TYPE = "PERSON";
	public Person get(long workspaceId, long id)
	public Person create(WorkspaceContext ctx, PersonRequest request)
	public Person update(WorkspaceContext ctx, long id, PersonUpdateRequest request)
	public void delete(WorkspaceContext ctx, long id, int version, String note)
	public Person restore(WorkspaceContext ctx, long id, PersonRestoreRequest request)
}
```

  - `PersonRequest(String fullName, String phone, String email, String idType, String idNumber, LocalDate dateOfBirth, Gender gender, String note, Map<String, Object> attributes, Long avatarFileId, String versionNote)`; `PersonUpdateRequest` có thêm `Integer version` (`@NotNull`) ở đầu. Ràng buộc: `fullName` `@NotBlank @Size(max = 150)`; `phone` `@Size(max = 20)` sau khi bỏ khoảng trắng và dấu chấm; `email` `@Email @Size(max = 255)`; `idType` `@Size(max = 20)`, viết hoa; `idNumber` `@Size(max = 30)`; `dateOfBirth` `@PastOrPresent`; `note` `@Size(max = 10000)`; `versionNote` `@Size(max = 255)`. Chuỗi rỗng thành null.
  - Có một trong `idType`, `idNumber` mà thiếu cái kia: `BadRequestException("idType and idNumber must be given together")`.
  - Trùng cặp (`idType`, `idNumber`) với một người khác chưa xoá: `ConflictException("Person <id cũ> already has this document")`. `PersonRepository.findByDocument(long workspaceId, String idType, String idNumber)` dùng index `(workspace_id, id_number)`.
  - `attributes` qua `AttributeValidation` với `FieldTarget.PERSON`, `typeId` 0.
  - Xoá: `PersonRepository.countCurrentOccupancies(long workspaceId, long personId, LocalDate today)` (chưa xoá, `end_date` trống hoặc ≥ hôm nay) khác 0: `ConflictException("Person <id> still has <n> current or upcoming occupancies")`.
  - Version `CREATE`, `UPDATE`, `DELETE`, `RESTORE` với `PersonSnapshot`.
  - HTTP: `POST /people` 201; `GET /people/{id}`; `PUT /people/{id}`; `DELETE /people/{id}?version=&note=` 204; `POST /people/{id}/restore` (body `{version, note}`). `PersonDetailResponse` là mọi trường trừ `workspaceId` (Task 4 thêm `currentOccupancies`).

- [ ] **Step 1: Viết test hỏng** (`PersonIT`).

```java
@Test void createsAPersonWithNormalizedFields()
	// {"fullName": " Nguyễn Văn A ", "phone": "090 123.4567", "idType": "cccd", "idNumber": "001", "gender": "MALE"}
	// -> 201; fullName "Nguyễn Văn A"; phone "0901234567"; idType "CCCD"; version 1; version 1 CREATE
@Test void documentFieldsComeInPairs()           // chỉ idNumber -> 400 "idType and idNumber must be given together"
@Test void sameDocumentIsAConflict()             // hai người CCCD 001 -> 409 "Person <id> already has this document"
@Test void deletedPersonDoesNotBlockTheSameDocument()
@Test void futureBirthDateAndBadEmailAre400()
@Test void attributesAreValidatedAgainstPersonFields()   // mục PERSON nickname TEXT: {"nickname": 1} -> 400
@Test void updateWritesFieldsAndChecksVersion()  // PUT version 1 -> version 2, UPDATE; PUT version 1 lần nữa -> 409
@Test void updateKeepsOwnDocument()              // sửa tên, giữ nguyên CCCD 001 -> 200 (không coi là trùng với chính mình)
@Test void deleteIsBlockedByCurrentOrUpcomingOccupancy()
	// chèn bằng SQL một reg_place và reg_occupancy end_date NULL -> 409 "Person <id> still has 1 current or upcoming occupancies";
	// occupancy đã kết thúc hôm qua -> xoá được
@Test void deleteAndRestoreWithVersions()        // DELETE -> 204, GET 404, version 2 DELETE; restore {version 2} -> version 3 RESTORE
@Test void otherWorkspaceSeesNothing()
```

- [ ] **Step 2: Chạy.** Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết code.**
- [ ] **Step 4: Chạy lại.** Mong đợi: PASS.
- [ ] **Step 5: Commit** `Add people: create, update, delete, restore with versions`.

---

### Task 2: Người: tìm và lịch sử

**Files:**
- Create: `registry-service/src/main/java/com/home_platform/registry/person/{PersonVersionResponse,PersonVersionSummaryResponse,PersonVersionRestoreRequest}.java`
- Modify: `PersonRepository`, `PersonService`, `PersonController`, `PersonMapper`
- Test: `registry-service/src/test/java/com/home_platform/registry/person/{PersonListIT,PersonVersionIT}.java`

**Interfaces:**

```java
// PersonService, mỗi danh sách trả CursorPage<Person>, chỉ người chưa xoá
public CursorPage<Person> list(long workspaceId, CursorRequest page)                      // theo full_name
public CursorPage<Person> searchByName(long workspaceId, String prefix, CursorRequest page)  // full_name LIKE prefix%
public CursorPage<Person> searchByPhone(long workspaceId, String prefix, CursorRequest page) // phone LIKE prefix% sau chuẩn hoá, theo phone
public CursorPage<Person> findByIdNumber(long workspaceId, String idNumber, CursorRequest page)
public CursorPage<EntityVersionSummary> versions(long workspaceId, long id, CursorRequest page)
public EntityVersion version(long workspaceId, long id, int versionNo)
public PersonSnapshot snapshotOf(EntityVersion version)
public Person restoreVersion(WorkspaceContext ctx, long id, int versionNo, PersonVersionRestoreRequest request)
```

  - Thoát ký tự `LIKE` như `PlaceService.searchByName`; đưa hàm thoát thành `public static String likePrefix(String value)` trong `registry.catalog.Text` và cho `PlaceService` dùng lại.
  - HTTP: `GET /people` (danh sách); `GET /people/search?name=` | `?phone=` | `?idNumber=` (đúng một; nếu không `BadRequestException("Give exactly one of name, phone or idNumber")`); `GET /people/{id}/versions`; `GET /people/{id}/versions/{no}`; `POST /people/{id}/versions/{no}/restore`.
  - Khôi phục version: như địa điểm (bỏ xoá trước, nếu không 409 "Restore person <id> before restoring an old version"); kiểm tra lại cặp giấy tờ và `attributes`; note "Restored from version <n>".

- [ ] **Step 1: Viết test hỏng.**

```java
// PersonListIT
@Test void listsByNameAndPages()
@Test void searchByNamePrefixIgnoresCaseAndAccents()
@Test void phoneSearchIgnoresSpacesAndDots()       // lưu 0901234567; tìm "090 123" -> có
@Test void idNumberSearchMatchesExactly()
@Test void searchNeedsExactlyOneCriterion()
// PersonVersionIT
@Test void listsAndShowsVersions()
@Test void restoresAnOldVersion()                   // tên cũ quay lại, note "Restored from version 1"
@Test void restoreVersionChecksDocumentDuplicates()
	// P có CCCD 001 (v1), đổi sang 002 (v2); Q tạo với CCCD 001; restore v1 của P -> 409; P vẫn version 2
@Test void restoringADeletedPersonIsAConflict()
```

- [ ] **Step 2–4:** chạy hỏng, viết code, chạy lại PASS (gồm `PlaceListIT` vì đổi hàm thoát).
- [ ] **Step 5: Commit** `Add people search and version history`.

---

### Task 3: Cư trú: ghi

**Files:**
- Create: `registry-service/src/main/java/com/home_platform/registry/occupancy/{Relation,OccupancyStatus,Occupancy,OccupancyRepository,OccupancyService,OccupancyController,OccupancyMapper,OccupancyCreateRequest,OccupancyUpdateRequest,OccupancyEndRequest,OccupancyResponse}.java`
- Test: `registry-service/src/test/java/com/home_platform/registry/occupancy/{OccupancyTestData,OccupancyIT}.java`

**Interfaces:**

```java
public enum Relation { OWNER, RESIDENT, GUEST, STAFF, OTHER }
public enum OccupancyStatus { CURRENT, UPCOMING, PAST;
	public static OccupancyStatus of(LocalDate start, LocalDate end, LocalDate today) }

@With
public record Occupancy(long workspaceId, long id, long placeId, long personId, Relation relation,
		LocalDate startDate, LocalDate endDate, String note, Instant createdAt, long createdBy, Instant updatedAt,
		long updatedBy, int version, Instant deletedAt) {}

@Service
public class OccupancyService {
	public Occupancy get(long workspaceId, long id)
	public Occupancy create(WorkspaceContext ctx, OccupancyCreateRequest request)
	public Occupancy update(WorkspaceContext ctx, long id, OccupancyUpdateRequest request)
	public Occupancy end(WorkspaceContext ctx, long id, OccupancyEndRequest request)
	public void delete(WorkspaceContext ctx, long id, int version)
}
```

  - `OccupancyCreateRequest(Long placeId, Long personId, Relation relation, LocalDate startDate, LocalDate endDate, String note)`: bốn trường đầu `@NotNull`; `note` `@Size(max = 500)`. `OccupancyUpdateRequest(Integer version, Relation relation, LocalDate startDate, LocalDate endDate, String note)`. `OccupancyEndRequest(Integer version, LocalDate endDate)`: `endDate` null thành hôm nay.
  - Địa điểm qua `PlaceService.get`, người qua `PersonService.get` (404 nếu không có hoặc đã xoá). `endDate` trước `startDate`: `BadRequestException("endDate must not be before startDate")`.
  - `OccupancyRepository.overlaps(long workspaceId, long personId, long placeId, LocalDate start, LocalDate end, long excludeId)` (chưa xoá, theo luật giao nhau ở Global Constraints, bỏ qua `excludeId`); có thì `ConflictException("<person> already has an overlapping occupancy at this place")` với `<person>` là `"Person <personId>"`.
  - Kết thúc (chuyển đi): dòng đã có `endDate` thì `ConflictException("Occupancy <id> has already ended")`.
  - Không ghi version vào `reg_entity_version`.
  - HTTP: `POST /occupancies` 201; `GET /occupancies/{id}`; `PUT /occupancies/{id}`; `POST /occupancies/{id}/end`; `DELETE /occupancies/{id}?version=` 204. `OccupancyResponse`: các trường của `Occupancy` trừ `workspaceId`, thêm `status`, `placeName`, `personFullName`.

- [ ] **Step 1: Viết test hỏng** (`OccupancyIT`).

```java
@Test void recordsWhoLivesWhere()                  // POST -> 201, status CURRENT, placeName, personFullName
@Test void statusFollowsTheDates()                 // start ngày mai -> UPCOMING; end hôm qua -> PAST; end hôm nay -> CURRENT
@Test void endBeforeStartIs400()
@Test void overlappingStaysAtTheSamePlaceConflict()
	// [2025-01-01, ∞) rồi [2025-06-01, 2025-07-01] cùng chỗ -> 409; ở chỗ khác cùng lúc -> được;
	// [2024-01-01, 2024-12-31] -> được (không giao)
@Test void editingADateDoesNotConflictWithItself() // PUT đổi startDate của chính dòng đó -> 200
@Test void endingSetsTheLastDay()                  // POST end {version 1} -> endDate hôm nay, version 2; end lần nữa -> 409
@Test void deletedPlaceOrPersonIsNotFound()        // người đã xoá -> 404
@Test void deleteIsSoftAndChecksVersion()          // DELETE ?version=9 -> 409; ?version=1 -> 204, GET 404
@Test void otherWorkspaceSeesNothing()
```

- [ ] **Step 2–4:** chạy hỏng, viết code, chạy lại PASS.
- [ ] **Step 5: Commit** `Add occupancy records`.

---

### Task 4: Cư trú: đọc, và nơi đang ở của một người

**Files:**
- Modify: `OccupancyRepository`, `OccupancyService`, `OccupancyController`; `person/PersonDetailResponse`, `person/PersonController` (thêm `currentOccupancies`)
- Test: `registry-service/src/test/java/com/home_platform/registry/occupancy/OccupancyListIT.java`

**Interfaces:**

```java
// OccupancyService, mỗi hàm trả CursorPage<OccupancyView>; OccupancyView(Occupancy occupancy, String placeName, String personFullName)
public CursorPage<OccupancyView> atPlace(long workspaceId, long placeId, OccupancyStatus status, CursorRequest page)
	// CURRENT, UPCOMING: theo start_date tăng dần; PAST: theo end_date giảm dần
public CursorPage<OccupancyView> currentInSubtree(long workspaceId, long rootPlaceId, CursorRequest page)
	// địa điểm gốc và mọi hậu duệ chưa xoá; theo start_date
public CursorPage<OccupancyView> ofPerson(long workspaceId, long personId, CursorRequest page)
	// mọi dòng chưa xoá của người, theo start_date giảm dần
public List<OccupancyView> currentOfPerson(long workspaceId, long personId)
```

  - Tên địa điểm và tên người lấy theo lô (`PlaceRepository.refs`, `PersonRepository.refs`), không join từng dòng.
  - HTTP: `GET /occupancies?placeId=&status=` (mặc định CURRENT) | `?subtreeOf=` | `?personId=` (đúng một trong ba, nếu không `BadRequestException("Give exactly one of placeId, subtreeOf or personId")`). `GET /people/{id}` có thêm `currentOccupancies`.

- [ ] **Step 1: Viết test hỏng** (`OccupancyListIT`).

```java
@Test void currentUpcomingAndPastAtAPlace()
@Test void pastIsNewestFirst()
@Test void subtreeShowsEveryoneInTheBuilding()        // người ở phòng P1, P2 của toà A, và ở chính A -> 3 dòng
@Test void subtreeOccupantsSkipDeletedPlaces()
@Test void personHistoryIsNewestFirst()
@Test void personDetailShowsCurrentPlaces()           // GET /people/{id} -> currentOccupancies[*].placeName
@Test void exactlyOneFilter()
```

- [ ] **Step 2–4:** chạy hỏng, viết code, chạy lại PASS.
- [ ] **Step 5: Commit** `Add occupancy listings and current places of a person`.

---

### Task 5: Tài liệu, kiểm tra toàn bộ, chạy thử

- [ ] README: thêm `/people...`, `/occupancies...`.
- [ ] `./mvnw verify` ở gốc: `BUILD SUCCESS`, không test nào bị bỏ qua.
- [ ] Chạy thử với `workspace_id = -9`: setup, toà nhà, phòng, người, cư trú ở phòng; `GET /occupancies?subtreeOf=<toà nhà>` có người đó; `GET /people/{id}` có nơi đang ở. Dừng service, xoá mọi dòng của `-9` (cư trú, người, địa chỉ, địa điểm theo `depth` giảm dần, version, mục tuỳ biến, loại, nhóm), kiểm tra còn 0.
- [ ] Commit `Document registry people and occupancy` và push `main`.
