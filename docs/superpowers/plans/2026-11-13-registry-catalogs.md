# Registry Catalogs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bước 1 của logic nghiệp vụ: danh mục của registry gồm loại địa điểm, nhóm đồ, mục thông tin tuỳ biến (kèm bộ kiểm tra `attributes`) và thao tác khởi tạo không gian.

**Architecture:** Mỗi danh mục là một package trong `registry-service` với ba tầng controller, service, repository (kế thừa `WorkspaceRepository`), DTO riêng và mapper MapStruct. Danh sách đọc qua cache Caffeine theo `workspace_id`; mọi lệnh ghi xoá cache ngay và xoá lần nữa sau khi transaction commit. Bộ kiểm tra `attributes` là một hàm thuần để test nhanh, bọc trong một bean đọc mục tuỳ biến từ cache.

**Tech Stack:** Spring Boot 4.1.2, Java 21, `platform-common` (đợt trước), MapStruct 1.6.3, Lombok `@With`, Jakarta Validation, MySQL 8.0.36 (UAT), JUnit 6 + AssertJ + MockMvc.

**Spec:** `docs/superpowers/specs/2026-11-13-business-rules-design.md` (mục 3, 5.1, 5.2, 5.3, 5.8). Thiết kế database: `docs/superpowers/specs/2026-11-11-database-design.md`.

## Global Constraints

- Truy vấn bằng `JdbcClient` trong lớp `@Repository`; mọi câu SQL lọc `workspace_id` trước. Không JPA. Không sửa `platform-common`.
- Package: `com.home_platform.registry.<nghiệp vụ>` (`catalog`, `placetype`, `itemcategory`, `fielddef`, `setup`). Controller gọi service, service gọi repository; controller không dùng repository (luật ArchUnit đã chạy trong `./mvnw test`).
- Request và response là record riêng, không trả record của bảng. Mapper: `@Mapper(config = PlatformMapperConfig.class)`.
- Chú thích tiếng Việt; tên lớp, hàm, thông báo lỗi tiếng Anh; thụt bằng tab, dùng `this.`.
- Chuỗi vào được cắt khoảng trắng hai đầu; trường tuỳ chọn rỗng sau khi cắt thành `null`. Độ dài tối đa bằng độ dài cột.
- Lỗi: 400 dữ liệu sai; 404 không có hoặc thuộc không gian khác; 409 vi phạm luật (trùng mã, đã lưu trữ, đang được dùng). Trùng khoá duy nhất được bắt và ném `ConflictException` với thông báo nêu giá trị trùng.
- Danh mục không xoá mềm; ngừng dùng bằng `ARCHIVED`. Ba bảng này không có `version`: người sửa sau thắng.
- Cache: tên `placeTypes`, `itemCategories`, `fieldDefs`; khoá là `workspaceId`.
- Test `*IT` chạy trên MySQL UAT, kế thừa `RegistryIT`, dùng `WS_A = -1`, `WS_B = -2`, `USER = -101`, mọi thứ rollback; không để lại dữ liệu đã commit.
- Mỗi task kết thúc bằng một commit, dùng tên và email từ `git.exe config` của Windows (git trong WSL không có danh tính); push bằng `/mnt/c/Program Files/Git/cmd/git.exe push origin main` sau Task 6.

**Lệnh chạy** (từ gốc repo):

- Test thường của registry: `./mvnw -pl registry-service -am test -Dtest=<Lớp> -Dsurefire.failIfNoSpecifiedTests=false`
- Một lớp IT: `./mvnw -pl registry-service -am verify -Dit.test=<Lớp> -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false`

## Review Focus

1. Ghi xong rồi đọc danh sách ngay trong cùng không gian phải thấy thay đổi, không bị cache cũ che. Test: `listReflectsWritesImmediately` (Task 1), `evictsNowAndAgainAfterCommit` (Task 1).
2. Dùng `id` loại hoặc nhóm của không gian khác làm `typeId` của mục tuỳ biến phải nhận 404, không tạo được. Test: `typeOfAnotherWorkspaceIsNotFound` (Task 3).
3. Mã hoặc tên chỉ khác hoa thường, khác dấu (`hotel` với `HOTEL`, `Nội thất` với `noi that`) phải bị coi là trùng (409). Test: `codeIsUppercasedAndDuplicatesConflict` (Task 1), `namesDifferingOnlyInCaseOrAccentsConflict` (Task 2).
4. Giá trị mục tuỳ biến đúng nghĩa nhưng sai kiểu JSON (`"5"` cho NUMBER, `"true"` cho BOOLEAN, `"2026-02-30"` cho DATE) phải bị từ chối, không tự đổi kiểu. Test: `valuesOfTheWrongJsonTypeAreRejected` (Task 4).
5. Gọi khởi tạo lần hai, hoặc sau khi người dùng đã xoá một loại mặc định, không được tạo lại gì. Test: `secondCallDoesNothing`, `doesNotRecreateWhatTheUserRemoved` (Task 5).

---

### Task 1: Loại địa điểm, cùng phần dùng chung của danh mục

**Files:**
- Create: `registry-service/src/main/java/com/home_platform/registry/catalog/{CatalogStatus,Text,CatalogCache}.java`
- Create: `registry-service/src/main/java/com/home_platform/registry/placetype/{PlaceType,PlaceTypeRepository,PlaceTypeService,PlaceTypeController,PlaceTypeMapper,PlaceTypeCreateRequest,PlaceTypeUpdateRequest,PlaceTypeResponse}.java`
- Create (test): `registry-service/src/test/java/com/home_platform/registry/RegistryIT.java`
- Test: `registry-service/src/test/java/com/home_platform/registry/catalog/{TextTests,CatalogCacheTests}.java`, `.../placetype/PlaceTypeIT.java`

**Interfaces:**
- Consumes: `WorkspaceRepository`, `RepositorySupport`, `WorkspaceContext`, `NotFoundException.of`, `ConflictException`, `PlatformMapperConfig`, `AbstractRepositoryIT` (platform-common).
- Produces:

```java
public enum CatalogStatus { ACTIVE, ARCHIVED }

public final class Text {
	/** null giữ null; cắt khoảng trắng; chuỗi rỗng sau khi cắt thành null. */
	public static String trimToNull(String value)
}

@Component
public class CatalogCache {
	public static final String PLACE_TYPES = "placeTypes";
	public static final String ITEM_CATEGORIES = "itemCategories";
	public static final String FIELD_DEFS = "fieldDefs";
	public CatalogCache(CacheManager cacheManager)
	/** Xoá khoá workspaceId của cache ngay, và xoá lần nữa sau commit nếu đang trong transaction. */
	public void evict(String cacheName, long workspaceId)
}

@With
public record PlaceType(long workspaceId, long id, String code, String name, String description, int sortOrder,
		CatalogStatus status, Instant createdAt, Instant updatedAt) {}

public record PlaceTypeCreateRequest(String code, String name, String description, Integer sortOrder) {}
public record PlaceTypeUpdateRequest(String name, String description, Integer sortOrder) {}
public record PlaceTypeResponse(long id, String code, String name, String description, int sortOrder,
		CatalogStatus status, Instant createdAt, Instant updatedAt) {}

@Service
public class PlaceTypeService {
	@Cacheable(cacheNames = CatalogCache.PLACE_TYPES, key = "#workspaceId")
	public List<PlaceType> list(long workspaceId)                   // sort_order, rồi name
	public PlaceType get(long workspaceId, long id)                 // 404 "PlaceType <id> not found"
	public boolean anyExists(long workspaceId)
	/** 404 nếu không có; 409 "PlaceType <id> is archived" nếu đã lưu trữ. */
	public PlaceType requireActive(long workspaceId, long id)
	public PlaceType create(WorkspaceContext ctx, PlaceTypeCreateRequest request)
	public PlaceType update(WorkspaceContext ctx, long id, PlaceTypeUpdateRequest request)
	public PlaceType archive(WorkspaceContext ctx, long id)
	public PlaceType unarchive(WorkspaceContext ctx, long id)
	public void delete(WorkspaceContext ctx, long id)
}

// test
@AutoConfigureMockMvc
public abstract class RegistryIT extends AbstractRepositoryIT {
	@Autowired protected MockMvc mvc;
	/** Thêm header X-Workspace-Id = WS_A (hoặc WS_B), X-User-Id = USER vào request của MockMvc. */
	protected static MockHttpServletRequestBuilder asA(MockHttpServletRequestBuilder request)
	protected static MockHttpServletRequestBuilder asB(MockHttpServletRequestBuilder request)
	// @BeforeEach: xoá sạch mọi cache, vì dữ liệu của test trước đã rollback
}
```

  - Request: các chuỗi đi qua `Text.trimToNull` trong hàm khởi tạo thu gọn của record; `code` thêm `toUpperCase(Locale.ROOT)`. Ràng buộc: `code` `@NotBlank @Size(max = 50) @Pattern(regexp = "[A-Z0-9_]+")`, `name` `@NotBlank @Size(max = 100)`, `description` `@Size(max = 500)`. `sortOrder` null thành 0.
  - Repository: `List<PlaceType> listAll(long workspaceId)`, `boolean anyExists(long workspaceId)`, `boolean isUsed(long workspaceId, long placeTypeId)` (có dòng `reg_place` nào, kể cả đã xoá mềm, dùng loại này).
  - Trùng `code`: bắt `DuplicateKeyException`, ném `ConflictException("Place type code <CODE> already exists")`.
  - `delete` khi `isUsed`: `ConflictException("PlaceType <id> is used by places; archive it instead")`. Xoá bằng `deletePermanently`.
  - Mọi hàm ghi có `@Transactional` và gọi `catalogCache.evict(PLACE_TYPES, ctx.workspaceId())`.
  - HTTP (`/place-types`): `GET` danh sách; `GET /{id}`; `POST` trả 201 kèm body; `PUT /{id}`; `POST /{id}/archive`; `POST /{id}/unarchive`; `DELETE /{id}` trả 204. Body request có `@Valid`.

- [ ] **Step 1: Viết test hỏng.**

```java
// TextTests
@Test void trimsAndTurnsBlankIntoNull()   // trimToNull(null) null; ("  a b  ") "a b"; ("   ") null

// CatalogCacheTests (ConcurrentMapCacheManager, không cần Spring context)
@Test void evictsNowAndAgainAfterCommit()
	// put(-1, x); TransactionSynchronizationManager.initSynchronization(); evict("placeTypes", -1)
	// -> cache không còn khoá -1; put lại (-1, y); gọi afterCommit() của mọi synchronization đã đăng ký
	// -> cache lại không còn khoá -1; finally clearSynchronization()
@Test void evictsOnlyThatWorkspace()      // khoá -2 vẫn còn sau evict(..., -1)

// PlaceTypeIT extends RegistryIT
@Test void createsAndReadsBack()
	// POST {"code":" hotel ","name":" Khách sạn ","sortOrder":2} -> 201; $.id là chuỗi; $.code "HOTEL";
	// $.name "Khách sạn"; $.status "ACTIVE"; $.description không có hoặc null; GET /{id} trả đúng như vậy
@Test void codeIsUppercasedAndDuplicatesConflict()
	// tạo "HOTEL" rồi "hotel" -> 409, $.detail "Place type code HOTEL already exists"
@Test void invalidInputIs400WithFieldErrors()
	// code "HO-TEL" -> 400, $.errors[*].field chứa "code"; name "   " -> 400, chứa "name"; code dài 51 -> 400
@Test void listIsOrderedBySortOrderThenName()
	// tạo (B,0), (A,0), (C,-1) -> danh sách tên [C, A, B]
@Test void listReflectsWritesImmediately()
	// GET danh sách (rỗng, nạp cache) -> POST -> GET có 1; PUT đổi tên -> GET thấy tên mới; DELETE -> GET rỗng
@Test void updateChangesNameDescriptionSortOrderButNotCode()
	// PUT {"name":"X","description":"  ","sortOrder":5} -> name X, description null, sortOrder 5, code giữ nguyên
@Test void archiveAndUnarchive()
	// archive -> status ARCHIVED; requireActive -> ConflictException "PlaceType <id> is archived"; unarchive -> ACTIVE
@Test void deleteUnusedTypeAndRefuseUsedOne()
	// xoá loại chưa dùng -> 204, GET -> 404; chèn một reg_place của WS_A dùng loại khác (SQL tay qua jdbc,
	// path '/<id>/', depth 0, status ACTIVE, các cột chung) -> DELETE loại đó -> 409
@Test void otherWorkspaceSeesNothing()
	// tạo trong WS_A; GET /{id} với header WS_B -> 404; GET danh sách WS_B rỗng; PUT/DELETE từ WS_B -> 404
```

- [ ] **Step 2: Chạy** `TextTests,CatalogCacheTests` rồi IT `PlaceTypeIT`. Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết các lớp ở Interfaces.** `CatalogCache.evict` dùng `TransactionSynchronizationManager.isSynchronizationActive()` để đăng ký một `TransactionSynchronization` có `afterCommit`.
- [ ] **Step 4: Chạy lại cả ba lớp.** Mong đợi: PASS. Rồi `./mvnw -pl registry-service -am test`: PASS (gồm `ArchitectureTests`).
- [ ] **Step 5: Commit** `Add place types to registry`.

---

### Task 2: Nhóm đồ

**Files:**
- Create: `registry-service/src/main/java/com/home_platform/registry/itemcategory/{ItemCategory,ItemCategoryRepository,ItemCategoryService,ItemCategoryController,ItemCategoryMapper,ItemCategoryRequest,ItemCategoryResponse}.java`
- Test: `registry-service/src/test/java/com/home_platform/registry/itemcategory/ItemCategoryIT.java`

**Interfaces:**
- Consumes: `CatalogStatus`, `Text`, `CatalogCache`, `RegistryIT` (Task 1).
- Produces:

```java
@With
public record ItemCategory(long workspaceId, long id, String name, int sortOrder, CatalogStatus status,
		Instant createdAt, Instant updatedAt) {}
public record ItemCategoryRequest(String name, Integer sortOrder) {}   // dùng cho cả tạo và sửa
public record ItemCategoryResponse(long id, String name, int sortOrder, CatalogStatus status,
		Instant createdAt, Instant updatedAt) {}

@Service
public class ItemCategoryService {
	@Cacheable(cacheNames = CatalogCache.ITEM_CATEGORIES, key = "#workspaceId")
	public List<ItemCategory> list(long workspaceId)                 // sort_order, rồi name
	public ItemCategory get(long workspaceId, long id)
	public ItemCategory requireActive(long workspaceId, long id)     // 404 / 409 "ItemCategory <id> is archived"
	public ItemCategory create(WorkspaceContext ctx, ItemCategoryRequest request)
	public ItemCategory update(WorkspaceContext ctx, long id, ItemCategoryRequest request)
	public ItemCategory archive(WorkspaceContext ctx, long id)
	public ItemCategory unarchive(WorkspaceContext ctx, long id)
	public void delete(WorkspaceContext ctx, long id)
}
```

  - Giống Task 1, khác ở: `name` `@NotBlank @Size(max = 100)`; trùng tên: `ConflictException("Item category <name> already exists")`; đang dùng là có dòng `reg_item` (kể cả đã xoá mềm) với `category_id` này. HTTP: `/item-categories`, cùng bộ đường dẫn như Task 1.

- [ ] **Step 1: Viết test hỏng** (`ItemCategoryIT extends RegistryIT`).

```java
@Test void createsListsUpdatesAndDeletes()       // 201, $.id chuỗi; danh sách theo sortOrder rồi tên; PUT đổi tên; DELETE 204
@Test void namesDifferingOnlyInCaseOrAccentsConflict()
	// "Nội thất" rồi "noi that" -> 409 detail "Item category noi that already exists"; "  " -> 400
@Test void archivedCategoryIsNotActive()         // archive -> requireActive ném ConflictException; unarchive -> dùng lại được
@Test void usedCategoryCannotBeDeleted()
	// chèn bằng SQL tay một reg_place và một reg_item của WS_A dùng nhóm này -> DELETE 409
@Test void otherWorkspaceSeesNothing()           // như Task 1
```

- [ ] **Step 2: Chạy IT.** Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết các lớp.**
- [ ] **Step 4: Chạy lại.** Mong đợi: PASS.
- [ ] **Step 5: Commit** `Add item categories to registry`.

---

### Task 3: Mục thông tin tuỳ biến

**Files:**
- Create: `registry-service/src/main/java/com/home_platform/registry/fielddef/{FieldTarget,FieldDataType,FieldDef,FieldDefRepository,FieldDefService,FieldDefController,FieldDefMapper,FieldDefCreateRequest,FieldDefUpdateRequest,FieldDefResponse}.java`
- Test: `registry-service/src/test/java/com/home_platform/registry/fielddef/FieldDefIT.java`

**Interfaces:**
- Consumes: `PlaceTypeService.requireActive`, `ItemCategoryService.requireActive` (Task 1, 2); `CatalogCache`, `Text`, `CatalogStatus`.
- Produces:

```java
public enum FieldTarget { PLACE, PERSON, ITEM }
public enum FieldDataType { TEXT, NUMBER, BOOLEAN, DATE, SELECT }

@With
public record FieldDef(long workspaceId, long id, FieldTarget target, long typeId, String fieldKey, String label,
		FieldDataType dataType, @JsonColumn List<String> options, boolean isRequired, int sortOrder,
		CatalogStatus status, Instant createdAt, Instant updatedAt) {}

public record FieldDefCreateRequest(FieldTarget target, Long typeId, String fieldKey, String label,
		FieldDataType dataType, List<String> options, Boolean required, Integer sortOrder) {}
public record FieldDefUpdateRequest(String label, List<String> options, Boolean required, Integer sortOrder) {}
public record FieldDefResponse(long id, FieldTarget target, long typeId, String fieldKey, String label,
		FieldDataType dataType, List<String> options, boolean required, int sortOrder, CatalogStatus status,
		Instant createdAt, Instant updatedAt) {}

@Service
public class FieldDefService {
	/** Mọi mục của không gian, mọi trạng thái; nguồn cho bộ kiểm tra attributes (Task 4). */
	@Cacheable(cacheNames = CatalogCache.FIELD_DEFS, key = "#workspaceId")
	public List<FieldDef> listAll(long workspaceId)
	/** typeId null: mọi mục của target; khác null: mục có type_id bằng 0 hoặc bằng typeId. Theo sort_order, rồi label. */
	public List<FieldDef> list(long workspaceId, FieldTarget target, Long typeId)
	public FieldDef get(long workspaceId, long id)
	public FieldDef create(WorkspaceContext ctx, FieldDefCreateRequest request)
	public FieldDef update(WorkspaceContext ctx, long id, FieldDefUpdateRequest request)
	public FieldDef archive(WorkspaceContext ctx, long id)
	public FieldDef unarchive(WorkspaceContext ctx, long id)
}
```

  - Ràng buộc request: `target`, `dataType` `@NotNull`; `fieldKey` `@NotBlank @Size(max = 50) @Pattern(regexp = "[a-z][a-z0-9_]*")`; `label` `@NotBlank @Size(max = 100)`. `typeId` null thành 0; `required` null thành false; `sortOrder` null thành 0. Mapper ánh xạ `isRequired` sang `required`.
  - `options`: mỗi phần tử qua `Text.trimToNull`. SELECT: 1 đến 100 phần tử, mỗi phần tử khác null và tối đa 100 ký tự, không trùng khi so không phân biệt hoa thường; sai thì `BadRequestException("options must be 1 to 100 distinct non-blank values of at most 100 characters")`. Kiểu khác: `options` null hoặc rỗng thì lưu null; có phần tử thì `BadRequestException("options are only allowed for SELECT")`.
  - `typeId`: PERSON mà khác 0 thì `BadRequestException("typeId must be 0 for PERSON")`. PLACE khác 0 thì `placeTypeService.requireActive`; ITEM khác 0 thì `itemCategoryService.requireActive`.
  - Repository: `boolean keyOverlaps(long workspaceId, FieldTarget target, long typeId, String fieldKey)`: đã có mục (mọi trạng thái) cùng `target`, `field_key` và `type_id` bằng 0, bằng `typeId`, hoặc `typeId` là 0. Chồng phạm vi: `ConflictException("Field <fieldKey> already exists for this <target in lower case>")`. Bắt thêm `DuplicateKeyException` cho trường hợp tranh chấp, cùng thông báo.
  - Không có xoá. HTTP (`/field-defs`): `GET ?target=PLACE&typeId=5` (`target` bắt buộc); `GET /{id}`; `POST` 201; `PUT /{id}`; `POST /{id}/archive`; `POST /{id}/unarchive`.

- [ ] **Step 1: Viết test hỏng** (`FieldDefIT extends RegistryIT`).

```java
@Test void createsASelectFieldForOnePlaceType()
	// loại HOTEL; POST {target PLACE, typeId HOTEL.id, fieldKey "view", label "Hướng", dataType SELECT,
	//   options [" Biển ","Phố"], required true} -> 201; $.options ["Biển","Phố"]; $.required true; $.typeId là chuỗi
@Test void keyMayNotOverlapAcrossScopes()
	// area_m2 cho mọi PLACE rồi area_m2 cho APARTMENT -> 409 "Field area_m2 already exists for this place";
	// ngược lại (riêng APARTMENT trước, rồi chung) -> 409; cùng khoá cho hai loại khác nhau -> được;
	// cùng khoá cho PERSON khi đã có ở PLACE -> được
@Test void optionsMustMatchTheDataType()
	// SELECT không options -> 400; SELECT options ["a","A"] -> 400; NUMBER có options -> 400; TEXT options [] -> 201, options null
@Test void personFieldsApplyToEveryone()        // PERSON typeId 7 -> 400 "typeId must be 0 for PERSON"
@Test void typeOfAnotherWorkspaceIsNotFound()   // PLACE typeId = id loại của WS_B -> 404; ITEM với id nhóm của WS_B -> 404
@Test void archivedTypeCannotGetNewFields()     // loại đã lưu trữ -> 409
@Test void updateChangesOnlyLabelOptionsRequiredAndSortOrder()
	// PUT {label, options, required, sortOrder}; fieldKey, target, typeId, dataType giữ nguyên
@Test void listFiltersByTargetAndType()
	// PLACE/0 "a", PLACE/HOTEL "b", PLACE/HOUSE "c", ITEM/0 "d":
	// ?target=PLACE -> a,b,c; ?target=PLACE&typeId=HOTEL -> a,b; ?target=ITEM -> d; thiếu target -> 400
@Test void archiveKeepsTheFieldInListAll()       // archive -> listAll vẫn có, status ARCHIVED
```

- [ ] **Step 2: Chạy IT.** Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết các lớp.**
- [ ] **Step 4: Chạy lại.** Mong đợi: PASS.
- [ ] **Step 5: Commit** `Add custom field definitions to registry`.

---

### Task 4: Kiểm tra `attributes`

**Files:**
- Create: `registry-service/src/main/java/com/home_platform/registry/fielddef/{AttributeValidator,AttributeValidation}.java`
- Test: `registry-service/src/test/java/com/home_platform/registry/fielddef/{AttributeValidatorTests,AttributeValidationIT}.java`

**Interfaces:**
- Consumes: `FieldDef`, `FieldTarget`, `FieldDataType`, `FieldDefService.listAll` (Task 3); `BadRequestException`, `ApiExceptionHandler.ERRORS_PROPERTY`.
- Produces (bước 2, 3, 4 của logic nghiệp vụ gọi `AttributeValidation.validate` khi tạo và sửa địa điểm, người, đồ đạc):

```java
public final class AttributeValidator {
	/**
	 * @param applicable các mục áp dụng cho bản ghi (đã lọc theo target và type), mọi trạng thái
	 * @return attributes đã bỏ các khoá mang giá trị null; null nếu không còn khoá nào
	 * @throws BadRequestException "Invalid attributes", thuộc tính errors = [{field, message}]
	 */
	public static Map<String, Object> validate(List<FieldDef> applicable, Map<String, Object> attributes)
}

@Component
public class AttributeValidation {
	/** Lấy mục từ FieldDefService.listAll (qua cache), giữ mục cùng target có type_id 0 hoặc typeId, rồi gọi AttributeValidator. */
	public Map<String, Object> validate(long workspaceId, FieldTarget target, long typeId, Map<String, Object> attributes)
}
```

  - Luật (spec 5.3), mỗi lỗi một phần tử `{"field": "attributes.<khoá>", "message": ...}`:

| Trường hợp | `message` |
|---|---|
| Khoá không thuộc mục áp dụng nào | `is not a defined field` |
| Mục ACTIVE kiểu TEXT, giá trị không phải `String` hoặc dài hơn 1000 | `must be text of at most 1000 characters` |
| NUMBER, giá trị không phải `Number` | `must be a number` |
| BOOLEAN, không phải `Boolean` | `must be true or false` |
| DATE, không phải `String` đọc được bằng `LocalDate.parse` | `must be a date in YYYY-MM-DD format` |
| SELECT, không phải `String` thuộc `options` (so đúng từng ký tự) | `must be one of the options` |
| Mục ACTIVE `isRequired` mà thiếu, null hoặc chuỗi chỉ có khoảng trắng | `is required` |

  - Mục ARCHIVED: khoá hợp lệ, giá trị giữ nguyên, không kiểm tra, không bắt buộc.
  - `attributes` null coi như rỗng. Thứ tự lỗi: theo thứ tự khoá gửi lên, rồi các mục bắt buộc còn thiếu theo `sortOrder`.

- [ ] **Step 1: Viết test hỏng.**

```java
// AttributeValidatorTests: dựng FieldDef trong bộ nhớ
@Test void acceptsValidValuesOfEveryType()
	// {"note":"x","area":42.5,"floors":3,"pet":true,"built":"2020-01-31","view":"Biển"} -> trả lại đúng map đó
@Test void valuesOfTheWrongJsonTypeAreRejected()
	// area "5", pet "true", built "2026-02-30", built 20200131, view "biển", note 7 -> 400, đủ 6 lỗi với field attributes.<khoá>
@Test void unknownKeyIsRejected()                // {"color":"red"} -> errors [{attributes.color, is not a defined field}]
@Test void requiredFieldsMustBePresent()         // view bắt buộc: thiếu, null, "  " -> mỗi lần một lỗi "is required"
@Test void archivedFieldIsKeptButNotChecked()    // mục old ARCHIVED, bắt buộc, NUMBER: {"old":"text"} hợp lệ; thiếu old cũng hợp lệ
@Test void nullValuesAreDroppedAndEmptyBecomesNull()  // {"note":null} -> null; null -> null; {} -> null
@Test void textLongerThan1000IsRejected()
@Test void errorsAreAttachedToTheProblemDetail()
	// ex là BadRequestException; ex.getBody().getDetail() = "Invalid attributes";
	// ex.getBody().getProperties().get("errors") là List<Map> với khoá "field" và "message"

// AttributeValidationIT extends RegistryIT
@Test void usesFieldsOfTheTargetAndTypeOnly()
	// PLACE/0 "a" NUMBER, PLACE/HOTEL "b" TEXT, ITEM/0 "c": validate(WS_A, PLACE, HOTEL.id, {a:1,b:"x"}) hợp lệ;
	// validate(WS_A, PLACE, HOUSE.id, {b:"x"}) -> 400 b không thuộc mục nào; {c:1} với PLACE -> 400
@Test void seesAFieldRightAfterItIsCreated()     // validate (nạp cache) -> tạo mục mới -> validate dùng được khoá mới
```

- [ ] **Step 2: Chạy** `AttributeValidatorTests` và IT. Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết hai lớp.**
- [ ] **Step 4: Chạy lại.** Mong đợi: PASS.
- [ ] **Step 5: Commit** `Validate custom attributes against field definitions`.

---

### Task 5: Khởi tạo không gian

**Files:**
- Create: `registry-service/src/main/java/com/home_platform/registry/setup/{DefaultCatalog,SetupService,SetupController,SetupResponse}.java`
- Test: `registry-service/src/test/java/com/home_platform/registry/setup/SetupIT.java`

**Interfaces:**
- Consumes: `PlaceTypeService` (`anyExists`, `create`), `ItemCategoryService.create`, `FieldDefService.create` (Task 1 đến 3).
- Produces:

```java
public record SetupResponse(boolean created) {}

@Service
public class SetupService {
	/** Tạo dữ liệu mặc định nếu không gian chưa có loại địa điểm nào; ngược lại không làm gì. */
	@Transactional
	public boolean setup(WorkspaceContext ctx)
}
// POST /setup -> 200 SetupResponse
```

  - `DefaultCatalog` giữ dữ liệu mặc định đúng như spec 5.1, theo thứ tự này, `sortOrder` lần lượt 10, 20, 30...:
    - Loại: `BUILDING` Toà nhà, `FLOOR` Tầng, `ROOM` Phòng, `APARTMENT` Căn hộ, `HOUSE` Nhà ở, `HOTEL` Khách sạn, `OTHER` Khác.
    - Nhóm đồ: Nội thất, Điện tử, Điện lạnh, Thiết bị vệ sinh, Đồ gia dụng, Khác.
    - Mục: PLACE/`HOTEL` `star_rating` "Số sao" NUMBER; PLACE/`APARTMENT` `area_m2` "Diện tích (m²)" NUMBER; không bắt buộc.
  - Hai lần gọi đồng thời: lần thua nhận 409 từ khoá duy nhất; gọi lại thì không làm gì. Ghi điều này trong Javadoc.

- [ ] **Step 1: Viết test hỏng** (`SetupIT extends RegistryIT`).

```java
@Test void createsTheDefaults()
	// POST /setup -> 200 {"created":true}; GET /place-types -> 7 mã theo đúng thứ tự trên;
	// GET /item-categories -> 6 tên; GET /field-defs?target=PLACE -> star_rating (typeId = HOTEL), area_m2 (typeId = APARTMENT)
@Test void secondCallDoesNothing()                // lần hai {"created":false}; vẫn 7 loại
@Test void doesNotRecreateWhatTheUserRemoved()     // setup; xoá OTHER; setup -> false; vẫn 6 loại, không có OTHER
@Test void workspaceWithItsOwnTypesIsLeftAlone()   // tự tạo một loại; setup -> false; chỉ có 1 loại, 0 nhóm đồ
@Test void otherWorkspaceIsUnaffected()            // setup WS_A; GET /place-types của WS_B rỗng
```

- [ ] **Step 2: Chạy IT.** Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết các lớp.**
- [ ] **Step 4: Chạy lại.** Mong đợi: PASS.
- [ ] **Step 5: Commit** `Add workspace setup with default catalogs`.

---

### Task 6: Tài liệu, kiểm tra toàn bộ, chạy thử

**Files:**
- Modify: `README.md` (mục mới "Nghiệp vụ đã có": danh sách đường dẫn của bước này, việc gọi `POST /setup` sau khi tạo không gian, cách dùng `AttributeValidation` cho các bước sau)

- [ ] **Step 1: Cập nhật README.**
- [ ] **Step 2: Kiểm tra toàn bộ.** `./mvnw verify` ở gốc. Mong đợi: `BUILD SUCCESS`, không test nào bị bỏ qua.
- [ ] **Step 3: Chạy thử.** Đóng gói và chạy `registry-service`; với header `X-Workspace-Id: -9`, `X-User-Id: -101`: `POST /setup` trả `{"created":true}`, `GET /place-types` trả 7 loại; `/swagger-ui.html` liệt kê các đường dẫn mới. Dừng service, rồi xoá dữ liệu vừa tạo của `workspace_id = -9` trong `reg_field_def`, `reg_place_type`, `reg_item_category` (ba câu `DELETE ... WHERE workspace_id = -9`), kiểm tra còn 0 dòng.
- [ ] **Step 4: Commit** `Document registry catalogs` **và push** `main`.
