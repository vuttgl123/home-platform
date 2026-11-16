# Auto Codes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mã của địa điểm và đồ đạc do hệ thống cấp khi tạo, không sửa được bằng tay và không bao giờ trùng trong một không gian; mỗi không gian tự đặt dạng mã; bản ghi cũ chưa có mã được cấp bằng một nút.

**Architecture:** Package mới `com.home_platform.registry.code` giữ mẫu mã (`reg_code_rule`, là danh mục, đọc qua cache) và bộ đếm theo dãy (`reg_code_counter`). `PlaceService` và `ItemService` gọi `CodeIssuer` ngay trong transaction của lệnh tạo, nên lệnh tạo hỏng thì số được trả lại. Khoá duy nhất `(workspace_id, code)` trên `reg_place` và `reg_item` là chốt cuối. Frontend bỏ ô "Mã" khỏi hai form, thêm thao tác "Cấp mã mới" và phần "Mã tự cấp" trong trang Danh mục.

**Tech Stack:** Java 21, Spring Boot 4.1, JdbcClient, MapStruct, Flyway, JUnit 5 (backend); React 19, TanStack Query, React Hook Form + Zod, Vitest + MSW (frontend). Không thêm thư viện.

**Spec:** `docs/superpowers/specs/2026-11-16-auto-codes-design.md`. Các số mục ("mục 5.3") trong kế hoạch này trỏ vào spec đó.

## Global Constraints

- Backend theo mục "Quy ước" của `README.md` gốc: ba tầng, SQL viết tay, mọi query lọc `workspace_id` trước, hàm của repository nhận `workspaceId` (hoặc `ctx`) đầu tiên, `long` ra JSON là chuỗi, thông báo lỗi tiếng Anh.
- Dạng mã: `prefix` 1 đến 10 ký tự `A-Z0-9`, bắt đầu bằng chữ, lưu viết hoa; `digits` 3 đến 9; mặc định có sẵn địa điểm `DD`, đồ đạc `TS`, 6 chữ số, không theo năm. Mã = `prefix` + `-` + (năm 4 chữ số + `-` nếu theo năm) + số thêm số 0 cho đủ `digits`; số dài hơn `digits` thì in đủ.
- Bộ đếm chỉ tăng bên trong transaction của lệnh đang cấp mã. Không dùng transaction riêng, không dùng `MAX(code) + 1`.
- `code` không còn là thành phần của `PlaceCreateRequest`, `PlaceUpdateRequest`, `ItemRequest`, `ItemUpdateRequest`. Client gửi `code` thì bị bỏ qua, không lỗi.
- Ghi chú version cố định: `Code assigned` (bản ghi trước đó chưa có mã), `Code changed from <mã cũ>` (cấp mã mới).
- Lô cấp mã cho bản ghi cũ: 100 bản ghi mỗi lần gọi, một transaction.
- File migration đã chạy không sửa lại. Hai file `application-uat.yaml`, `application-prod.yaml` không bao giờ được commit.
- Chạy một lớp IT: `./mvnw -pl registry-service verify -Dit.test=<Lớp> -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false`. `./mvnw test` xanh ở cuối mỗi task backend.
- Frontend: npm chạy qua Node của Windows (`cmd.exe /c "npm test"` khi đứng ở `frontend/`); `id` là chuỗi; test viết trước; `npm test`, `npm run build`, `npm run lint` sạch ở cuối mỗi task frontend.
- Mỗi task một commit; push `main` sau task cuối.

## Review Focus

1. Hai lệnh tạo cùng dãy chạy cùng lúc không ra cùng một mã. Test: `twoCreatesInOneSeriesGetConsecutiveCodes` (Task 3).
2. Một mã gõ tay từ trước trùng đúng số bộ đếm sắp cấp, kể cả khi bản ghi mang nó đã bị xoá: bộ đếm nhảy qua. Test: `counterSkipsCodesAlreadyTaken` (Task 3).
3. Client cũ vẫn gửi `code` trong lệnh sửa: mã không đổi, không lỗi. Test: `updateIgnoresASentCode` (Task 3).
4. Migration gặp mã đang trùng trên dữ liệu thật: bản tạo sớm nhất giữ mã, các bản còn lại trống mã, không dòng nào mất. Kiểm bằng số đếm trước và sau migration và test `noTwoRecordsShareACode` (Task 1).
5. Bấm "Cấp mã" hai lần liên tiếp, hoặc hai người cùng bấm: không bản ghi nào nhận hai mã, không version thừa. Test: `secondRunAssignsNothingTwice`, `twoBackfillsAtOnceAssignEachRecordOnce` (Task 5).

## File Structure

```
registry-service/src/main/resources/db/migration/V3__auto_codes.sql      bảng mẫu, bảng đếm, bỏ mã trùng, khoá duy nhất
registry-service/src/main/java/com/home_platform/registry/code/
  CodeTarget.java            enum PLACE, ITEM
  CodeFormat.java            dạng mã: dựng dãy và mã; chọn mẫu áp dụng. Thuần, không Spring
  CodeRule.java              một dòng reg_code_rule
  CodeRuleRepository.java    đọc, đặt, bỏ mẫu
  CodeCounterRepository.java tăng bộ đếm của một dãy
  CodeRuleService.java       mẫu của một không gian, qua cache
  CodeIssuer.java            cấp mã kế tiếp
  CodeRuleController.java    /code-rules
  CodeRuleRequest.java, CodeRuleResponse.java, CodeRuleView.java
  CodeReissueRequest.java    thân của hai lệnh reissue-code
  CodeBackfillService.java   đếm và cấp mã cho bản ghi cũ
  CodeController.java        /codes/status, /codes/backfill
  CodeStatusResponse.java, CodeBackfillResponse.java
frontend/src/components/common/ReissueCodeDialog.tsx                      hộp xác nhận cấp mã mới, dùng cho cả địa điểm và đồ
frontend/src/features/catalog/codeRules.ts                                ví dụ mã, kiểm tra tiền tố, mã loại suy từ tên
frontend/src/features/catalog/components/CodeRuleSection.tsx              phần "Mã tự cấp"
```

---

### Task 1: Bảng mẫu mã, bộ đếm và khoá duy nhất trên mã

**Files:** tạo `registry-service/src/main/resources/db/migration/V3__auto_codes.sql`; sửa `registry-service/src/test/java/com/home_platform/registry/schema/RegistrySchemaIT.java`, `registry-service/src/test/java/com/home_platform/registry/CommittedDataIT.java` (danh sách `WIPE`).

- [ ] **Đếm trước khi đổi dữ liệu.** Trước khi tạo file migration, thêm tạm vào `RegistrySchemaIT` một test in ra, cho từng bảng `reg_place` và `reg_item`: tổng số dòng, số nhóm `(workspace_id, code)` có hơn một dòng, và số dòng sẽ bị bỏ trống mã (tổng của `COUNT(*) - 1` trên các nhóm đó). Chạy `-Dit.test=RegistrySchemaIT#<tên test>`, ghi sáu con số vào báo cáo của task, rồi xoá test tạm. Nếu số dòng bị ảnh hưởng lớn hơn 0 thì **dừng và báo người dùng** kèm danh sách `workspace_id`, `code` của các nhóm trùng, trước khi làm bước sau (spec mục 5.5).
- [ ] Viết `V3__auto_codes.sql`:
  1. Hai bảng `reg_code_rule`, `reg_code_counter` đúng như mục 6 của spec.
  2. Cho `reg_place` rồi `reg_item`: bỏ trống mã trùng, giữ dòng có `id` nhỏ nhất của mỗi nhóm:

     ```sql
     UPDATE reg_place p
     JOIN (SELECT workspace_id, code, MIN(id) AS keep_id FROM reg_place
           WHERE code IS NOT NULL GROUP BY workspace_id, code HAVING COUNT(*) > 1) d
       ON d.workspace_id = p.workspace_id AND d.code = p.code AND p.id <> d.keep_id
     SET p.code = NULL;
     ```

  3. `ALTER TABLE reg_place DROP KEY idx_reg_place_code, ADD UNIQUE KEY uk_reg_place_code (workspace_id, code);` và tương tự `uk_reg_item_code`.
- [ ] Test (`RegistrySchemaIT`): sửa `registryTablesExist` (thêm `reg_code_counter`, `reg_code_rule` đúng thứ tự chữ cái); trong `indexesMatchDesign` thay hai dòng `idx_reg_*_code` bằng `uk_reg_place_code`, `uk_reg_item_code`; thêm
  - `codeIsUniquePerWorkspaceIncludingDeletedRows`: hai địa điểm cùng `code` trong `WS_A` ném `DuplicateKeyException` kể cả khi dòng đầu có `deleted_at`; cùng `code` ở `WS_B` thì được; hai dòng `code` NULL thì được. Làm tương tự với `reg_item`.
  - `codeComparisonIgnoresCase`: `p-0001` sau `P-0001` ném `DuplicateKeyException`.
  - `noTwoRecordsShareACode`: như `noTwoLivePeopleShareADocument`: đếm nhóm `(workspace_id, code)` có hơn một dòng (`code IS NOT NULL`, tính cả dòng đã xoá) trong `reg_place` và `reg_item`, cả hai bằng 0. Đây là điều kiện để thêm được khoá duy nhất, và là luật nó giữ sau đó. (Câu bỏ mã trùng không thử riêng được: bảng thật đã có khoá duy nhất, còn bảng tạm của MySQL không cho đọc hai lần trong một câu.)
  - `primaryKeysStartWithWorkspaceId`: thêm kiểm `primaryKey("reg_code_rule")` = `workspace_id, target, type_id` và `primaryKey("reg_code_counter")` = `workspace_id, target, series`.
- [ ] `CommittedDataIT.WIPE`: thêm `DELETE FROM reg_code_counter WHERE workspace_id = :ws` và `DELETE FROM reg_code_rule WHERE workspace_id = :ws` (không có khoá ngoại, đặt cuối danh sách).
- [ ] Chạy `RegistrySchemaIT`: hỏng trước khi có migration, PASS sau. Chạy lại test đếm tạm (rồi xoá): tổng số dòng của hai bảng bằng đúng số đã ghi ở bước đầu, số nhóm trùng bằng 0. Commit `Add code rule and counter tables and make codes unique`.

### Task 2: Mẫu mã và bộ cấp mã

**Files:** tạo trong `registry-service/src/main/java/com/home_platform/registry/code/`: `CodeTarget.java`, `CodeFormat.java`, `CodeRule.java`, `CodeRuleRepository.java`, `CodeCounterRepository.java`, `CodeRuleService.java`, `CodeIssuer.java`, `CodeRuleController.java`, `CodeRuleRequest.java`, `CodeRuleResponse.java`, `CodeRuleView.java`; sửa `catalog/CatalogCache.java`, `placetype/{PlaceTypeService,PlaceTypeRepository}.java`, `itemcategory/{ItemCategoryService,ItemCategoryRepository}.java`; test `code/CodeFormatTests.java`, `code/CodeRuleIT.java`.

**Interfaces:**
- Produces:

```java
public enum CodeTarget { PLACE, ITEM }

/** Dạng mã. Thuần, không phụ thuộc Spring. */
public record CodeFormat(String prefix, int digits, boolean yearly) {
	public static CodeFormat builtIn(CodeTarget target);          // PLACE: DD,6,false   ITEM: TS,6,false
	public String series(int year);                               // "P-"  hoặc "TS-2026-"
	public String code(int year, long number);                    // series + số thêm 0 cho đủ digits; không cắt
	/** Mẫu riêng của typeId, không có thì mẫu chung (typeId 0), không có nữa thì builtIn. */
	public static CodeFormat resolve(List<CodeRule> stored, CodeTarget target, long typeId);
}

public record CodeRule(long workspaceId, CodeTarget target, long typeId, String prefix, int digits, boolean yearly,
		Instant createdAt, Instant updatedAt) { public CodeFormat format(); }

// CodeRuleRepository (@Repository; SQL viết tay theo mẫu PlaceAddressRepository vì khoá không phải (workspace_id, id))
List<CodeRule> listAll(long workspaceId)                          // kết thúc bằng CatalogCache.LATEST_COMMITTED
void upsert(long workspaceId, CodeTarget target, long typeId, CodeFormat format)   // INSERT … ON DUPLICATE KEY UPDATE, đặt updated_at
boolean delete(long workspaceId, CodeTarget target, long typeId)

// CodeCounterRepository (@Repository)
long next(long workspaceId, CodeTarget target, String series)
	// INSERT INTO reg_code_counter (…, last_no) VALUES (…, 1) ON DUPLICATE KEY UPDATE last_no = last_no + 1
	// rồi SELECT last_no của đúng dòng đó. Ném IllegalTransactionStateException nếu không có transaction đang mở.

// CodeRuleService (@Service)
@Cacheable(cacheNames = CatalogCache.CODE_RULES, key = "#workspaceId") List<CodeRule> stored(long workspaceId)
List<CodeRuleView> list(long workspaceId)                         // mẫu đã lưu + dòng typeId 0 có sẵn cho đích chưa đặt
CodeRuleView put(WorkspaceContext ctx, CodeTarget target, long typeId, CodeRuleRequest request)
void delete(WorkspaceContext ctx, CodeTarget target, long typeId)
record CodeRuleView(CodeTarget target, long typeId, CodeFormat format, boolean builtIn)

// CodeIssuer (@Service)
/** Mã kế tiếp theo mẫu đang áp dụng. Phải gọi trong transaction của lệnh sẽ ghi mã đó. */
String issue(long workspaceId, CodeTarget target, long typeId, Predicate<String> taken)

// API
record CodeRuleRequest(@NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9]{0,9}") String prefix,
		@NotNull @Min(3) @Max(9) Integer digits, boolean yearly)      // compact constructor: trim, viết hoa prefix
record CodeRuleResponse(CodeTarget target, long typeId, String prefix, int digits, boolean yearly, boolean builtIn)
// CatalogCache
public static final String CODE_RULES = "codeRules";
```

- `CodeIssuer.issue`: lấy mẫu bằng `CodeFormat.resolve(rules.stored(workspaceId), target, typeId)`; năm là `businessDate.today().getYear()`; lặp tối đa 1000 lần: `n = counters.next(...)`, dựng mã, `taken.test(mã)` sai thì trả về. Hết 1000 lần: `ConflictException("No free code in series " + series + " after 1000 attempts; change the code rule")`.
- `CodeIssuer` gọi `rules.stored(...)` qua bean (không tự gọi trong cùng lớp) để cache có hiệu lực. Caffeine tự tạo cache theo tên, không cần đăng ký ở đâu khác.
- `CodeRuleService.put`: `typeId` khác 0 phải là loại địa điểm (PLACE) hoặc nhóm đồ (ITEM) của không gian: gọi `placeTypes.get` hoặc `categories.get`, không có thì 404 của chính hàm đó; loại đã lưu trữ vẫn đặt mẫu được. Ghi xong `catalogCache.evict(CODE_RULES, workspaceId)`.
- `CodeRuleService.delete`: không có mẫu đã lưu thì `NotFoundException("Code rule " + target + "/" + typeId + " not found")`.
- `PlaceTypeService.delete` và `ItemCategoryService.delete`: xoá luôn mẫu riêng. Thêm `deleteOwnCodeRule(long workspaceId, long typeId)` vào `PlaceTypeRepository` (`target = 'PLACE'`) và `ItemCategoryRepository` (`target = 'ITEM'`), cạnh `deleteOwnFields`, và `evict(CODE_RULES)`.
- `CodeRuleController` (`/code-rules`): `GET` trả `List<CodeRuleResponse>` (PLACE trước ITEM; trong mỗi đích, `typeId` 0 trước); `PUT /{target}/{typeId}` trả `CodeRuleResponse`; `DELETE /{target}/{typeId}` trả 204.

- [ ] Test `CodeFormatTests`:
  - `plainCodePadsToDigits`: `new CodeFormat("DD", 6, false).code(2026, 1)` = `"DD-000001"`, `series(2026)` = `"DD-"`.
  - `overflowKeepsAllDigits`: `new CodeFormat("P", 3, false).code(2026, 1000)` = `"P-1000"`.
  - `yearlyPutsTheYearInTheSeries`: `new CodeFormat("TS", 4, true)`: `series(2026)` = `"TS-2026-"`, `code(2026, 1)` = `"TS-2026-0001"`, `series(2027)` = `"TS-2027-"`.
  - `ownRuleBeatsSharedBeatsBuiltIn`: ba trường hợp của `resolve` với `typeId` 7: có mẫu của 7; chỉ có mẫu `typeId` 0; không có gì (PLACE ra `DD`, ITEM ra `TS`).
  - `ruleOfTheOtherTargetIsIgnored`: mẫu ITEM/0 không áp dụng cho PLACE.
- [ ] Test `CodeRuleIT extends RegistryIT`:
  - `freshWorkspaceListsTheTwoBuiltInRules`: `GET /code-rules` trả đúng hai dòng: `{target: PLACE, typeId: "0", prefix: DD, digits: 6, yearly: false, builtIn: true}` và ITEM với `TS`.
  - `putStoresAndUppercasesThePrefix`: `PUT /code-rules/PLACE/0` thân `{"prefix": " p ", "digits": 4, "yearly": false}` trả `prefix: P, builtIn: false`; `GET` thấy như vậy.
  - `invalidRulesAreRejected`: `prefix` lần lượt `1A`, `A-B`, `ABCDEFGHIJK`, rỗng trả 400 có lỗi của trường `prefix`; `digits` 2 và 10 trả 400 có lỗi của trường `digits`.
  - `ruleForAnUnknownTypeIs404`: `PUT /code-rules/PLACE/999` và `PUT /code-rules/ITEM/<id một loại địa điểm>` đều 404.
  - `unknownTargetIs400`: `PUT /code-rules/PERSON/0`.
  - `deleteReturnsToTheBuiltInRule`: đặt rồi `DELETE /code-rules/ITEM/0` trả 204, `GET` lại thấy `TS`, `builtIn: true`; `DELETE` lần nữa trả 404.
  - `deletingAPlaceTypeDeletesItsRule`, `deletingACategoryDeletesItsRule`.
  - `otherWorkspaceSeesOnlyBuiltInRules`: đặt mẫu ở `WS_A`, `asB(get("/code-rules"))` vẫn là hai dòng có sẵn.
- [ ] Hỏng → viết → PASS; `./mvnw -pl registry-service test` xanh (gồm `ArchitectureTests`). Commit `Add code rules and the code issuer`.

### Task 3: Địa điểm và đồ đạc nhận mã khi tạo; mã bị khoá

**Files:** sửa `place/{PlaceCreateRequest,PlaceUpdateRequest,PlaceService,PlaceRepository}.java`, `item/{ItemRequest,ItemUpdateRequest,ItemService,ItemRepository}.java`; sửa test `item/ItemTestData.java`, `occupancy/OccupancyTestData.java`, `ConcurrentWritesIT.java` (ba chỗ dựng request theo vị trí, bỏ đối số `code`), `place/{PlaceIT,PlaceListIT}.java`, `item/{ItemIT,ItemListIT,ItemLifecycleIT}.java`; tạo test `place/PlaceCodeIT.java`, `item/ItemCodeIT.java`, `registry/CodeConcurrencyIT.java`.

**Interfaces:**
- Consumes: `CodeIssuer.issue(long, CodeTarget, long, Predicate<String>)`, `CodeTarget` (Task 2).
- Produces:

```java
// bỏ thành phần `code` khỏi bốn record request; ItemRequest.atPlace bỏ theo
// PlaceRepository, ItemRepository
boolean codeExists(long workspaceId, String code)      // mọi dòng, kể cả đã xoá; dùng uk_reg_*_code
```

- `PlaceService.create`: sau khi kiểm tra cha, loại và `attributes`, ngay trước `insert`: `codes.issue(ws, CodeTarget.PLACE, request.placeTypeId(), code -> repository.codeExists(ws, code))`. `ItemService.create`: tương tự với `CodeTarget.ITEM` và `categoryId` (không nhóm thì 0). Lệnh thay (`replace`) đi qua `create` nên món mới tự có mã.
- `applyContent` của cả hai service bỏ `.withCode(...)`; `restoreVersion` dựng request không có mã, nên mã giữ nguyên.
- `insert` ném `DuplicateKeyException` (không thể xảy ra nếu bộ đếm đúng, nhưng khoá duy nhất là chốt cuối): đổi thành `ConflictException("Code " + code + " is already in use; try again")`.

- [ ] Test `PlaceCodeIT extends PlaceTestData`:
  - `createIssuesConsecutiveCodes`: hai lần `POST /places` trả `code` `DD-000001`, `DD-000002`.
  - `createIgnoresASentCode`: thân có `"code": "X"` vẫn trả `DD-000001`.
  - `typeRuleWinsOverTheSharedRule`: đặt PLACE/0 `{A,3}` và PLACE/<loại phòng> `{P,4}`; tạo một phòng ra `P-0001`, tạo một toà ra `A-001`.
  - `typesSharingAPrefixShareOneSeries`: hai loại cùng mẫu `{P,4}`; tạo mỗi loại một địa điểm ra `P-0001`, `P-0002`.
  - `changingThePrefixStartsANewSeriesAndBackContinuesTheOld`: `P` cấp 1, 2; đổi sang `PH` cấp `PH-0001`; đổi lại `P` cấp `P-0003`.
  - `counterSkipsCodesAlreadyTaken`: chèn bằng SQL một địa điểm có `code = 'DD-000001'` và `deleted_at` khác NULL; `POST /places` trả `DD-000002`.
  - `yearlySeriesIgnoresLastYearsCounter`: đặt PLACE/0 `{TS,4,yearly}`; chèn bằng SQL bộ đếm `(WS_A, 'PLACE', 'TS-<năm nay trừ 1>-', 7)`; tạo ra `TS-<năm nay>-0001` (năm lấy từ `BusinessDate`).
  - `updateIgnoresASentCode`: `PUT /places/{id}` có `"code": "HACK"` trả 200 và `code` vẫn là mã đã cấp; version 2 không có `code` trong `changedFields`.
  - `changingTheTypeKeepsTheCode`.
  - `otherWorkspaceCountsOnItsOwn`: `WS_A` tạo hai địa điểm, `WS_B` tạo một: `WS_B` nhận `DD-000001`.
- [ ] Test `ItemCodeIT extends ItemTestData`: `createIssuesConsecutiveCodes` (`TS-000001`, `TS-000002`), `createIgnoresASentCode`, `categoryRuleWinsOverTheSharedRule`, `itemWithoutCategoryUsesTheSharedRule`, `replacementGetsItsOwnCode` (món cũ giữ mã của nó), `updateIgnoresASentCode`, `placesAndItemsCountSeparately` (cùng tiền tố `X` cho hai đích: địa điểm đầu `X-000001`, món đầu cũng `X-000001`).
- [ ] Test `CodeConcurrencyIT extends CommittedDataIT`:
  - `twoCreatesInOneSeriesGetConsecutiveCodes`: `holdOpen` lệnh tạo thứ nhất, `inBackground` lệnh thứ hai, `letItBlock`, commit; hai mã là `DD-000001` và `DD-000002`, `rows("reg_place")` = 2.
  - `failedCreateGivesItsNumberBack`: tạo A (`DD-000001`); tạo B với địa chỉ chỉ có `latitude` (ném `BadRequestException` sau khi mã đã cấp); tạo C nhận `DD-000002`.
- [ ] Sửa test cũ: `PlaceIT` và `ItemIT` bỏ `code` gõ tay khỏi thân và không còn kiểm giá trị gõ tay; `ItemLifecycleIT` bỏ `"code": "SN-2"`; `PlaceListIT` và `ItemListIT` (tìm đúng mã, không phải tiền tố) đổi sang mã được cấp: đặt mẫu `{P,3}`, tạo 10 bản ghi, tìm `p-001` chỉ ra đúng bản ghi mang `P-001`, không ra `P-010`.
- [ ] Hỏng → viết → PASS; `./mvnw -pl registry-service test` xanh; chạy `-Dit.test='Place*IT,Item*IT,CodeConcurrencyIT,ConcurrentWritesIT,Occupancy*IT'`. Commit `Issue codes on create and lock them`.

### Task 4: Cấp mã mới cho một bản ghi

**Files:** tạo `code/CodeReissueRequest.java`; sửa `place/{PlaceService,PlaceController}.java`, `item/{ItemService,ItemController}.java`; test thêm vào `place/PlaceCodeIT.java`, `item/ItemCodeIT.java`.

**Interfaces:**
- Produces:

```java
public record CodeReissueRequest(@NotNull Integer version, @Size(max = 255) String note)   // note: Text.trimToNull
// PlaceService
@Transactional public Place reissueCode(WorkspaceContext ctx, long id, CodeReissueRequest request)
// ItemService
@Transactional public Item reissueCode(WorkspaceContext ctx, long id, CodeReissueRequest request)
// POST /places/{id}/reissue-code -> PlaceDetailResponse;  POST /items/{id}/reissue-code -> ItemDetailResponse
```

- So `version` trước khi cấp số: lệch thì `ConflictException.versionConflict(...)` ngay. Mẫu lấy theo loại (nhóm) hiện tại của bản ghi. Ghi chú version: `request.note()` nếu có; không thì `Code changed from <mã cũ>`, hoặc `Code assigned` khi mã cũ trống.
- Món đã gỡ: `requireNotRemoved` (409 `Item <id> is removed`).

- [ ] Test: `reissueGivesTheNextCodeAndRecordsIt` (mã `DD-000001` thành `DD-000002`; version 2 là `UPDATE`, `changedFields` = `["code"]`, `note` = `Code changed from DD-000001`), `reissueOnARecordWithoutCodeNotesCodeAssigned`, `reissueKeepsTheUsersNote`, `reissueFollowsTheCurrentTypesRule` (đặt mẫu riêng sau khi tạo), `oldCodeNoLongerFindsTheRecord` (`/places/search?code=` mã cũ rỗng, mã mới ra đúng một), `oldCodeIsNeverIssuedAgain` (cấp mã mới rồi tạo bản ghi khác: không bản ghi nào nhận lại mã cũ), `reissueChecksTheVersion` (409, `code` = `VERSION_CONFLICT`, mã không đổi), `restoringAVersionFromBeforeAReissueKeepsTheNewCode` (tạo, cấp mã mới, khôi phục version 1 mà snapshot còn mang mã cũ: mã hiện tại vẫn là mã mới); với đồ thêm `removedItemCannotGetANewCode`.
- [ ] Hỏng → viết → PASS. Commit `Add reissuing a code`.

### Task 5: Cấp mã cho bản ghi cũ

**Files:** tạo `code/{CodeBackfillService,CodeController,CodeStatusResponse,CodeBackfillResponse}.java`; sửa `place/{PlaceService,PlaceRepository}.java`, `item/{ItemService,ItemRepository}.java`; test `code/CodeBackfillIT.java`.

**Interfaces:**
- Produces:

```java
// PlaceRepository, ItemRepository
int countWithoutCode(long workspaceId)                       // chưa xoá, code IS NULL; đồ: thêm removed_at IS NULL
List<Long> idsWithoutCode(long workspaceId, int limit)       // cùng điều kiện, ORDER BY id, LIMIT
// PlaceService, ItemService
int countWithoutCode(long workspaceId)
@Transactional int assignMissingCodes(WorkspaceContext ctx, int limit)   // trả số bản ghi vừa được cấp
// CodeBackfillService
static final int BATCH = 100;
CodeStatusResponse status(long workspaceId)
@Transactional CodeBackfillResponse backfill(WorkspaceContext ctx)
record CodeStatusResponse(int placesWithoutCode, int itemsWithoutCode)
record CodeBackfillResponse(int placesAssigned, int itemsAssigned, int placesRemaining, int itemsRemaining)
// GET /codes/status, POST /codes/backfill
```

- `assignMissingCodes`: với từng `id`, đọc lại kèm khoá riêng (`findByIdForUpdate`); dòng đã xoá hoặc đã có mã (lệnh khác vừa cấp) thì bỏ qua; còn lại cấp mã theo loại (nhóm) hiện tại, `update` với `version` vừa đọc, ghi version `UPDATE` ghi chú `Code assigned`.
- `backfill`: địa điểm trước (tối đa `BATCH`), phần còn lại của `BATCH` cho đồ đạc; các số "còn lại" đếm sau khi cấp.

- [ ] Test `CodeBackfillIT extends RegistryIT` (bản ghi không mã chèn thẳng bằng SQL, vì API không còn tạo được bản ghi không mã):
  - `statusCountsLiveRecordsWithoutCode`: 2 địa điểm không mã, 1 địa điểm không mã đã xoá, 1 món không mã, 1 món không mã đã gỡ: `{placesWithoutCode: 2, itemsWithoutCode: 1}`.
  - `backfillAssignsOldestFirstWithOneVersionEach`: ba địa điểm `id` tăng dần nhận `DD-000001..3` theo đúng thứ tự `id`; mỗi cái có `version` 2 và một version `UPDATE` ghi chú `Code assigned`.
  - `backfillUsesEachRecordsOwnRule`: mẫu riêng của loại phòng áp dụng cho phòng, mẫu chung cho phần còn lại.
  - `backfillSkipsDeletedPlacesAndRemovedItems`: chúng vẫn không có mã, `version` không đổi.
  - `backfillStopsAtOneHundred`: 101 địa điểm và 5 món không mã: lần một `{100, 0, 1, 5}`, lần hai `{1, 5, 0, 0}`.
  - `secondRunAssignsNothingTwice`: chạy lại sau khi hết: `{0, 0, 0, 0}`, không bản ghi nào có version thứ ba.
  - `backfillLeavesOtherWorkspacesAlone`.
- [ ] Test `CodeConcurrencyIT`: `twoBackfillsAtOnceAssignEachRecordOnce` (ba địa điểm không mã; `holdOpen` lượt cấp thứ nhất, lượt thứ hai ở nền, `letItBlock`, commit: tổng `placesAssigned` của hai lượt là 3, ba mã khác nhau, mỗi địa điểm có đúng một version `Code assigned`).
- [ ] Hỏng → viết → PASS; `./mvnw test` xanh. Cập nhật `README.md` gốc: thêm dòng `/code-rules`, `/codes` vào bảng Registry, sửa dòng `/places` và `/items` (mã do hệ thống cấp, `POST /{id}/reissue-code`). Commit `Assign codes to records that have none`.

### Task 6: Frontend, form không còn ô mã

**Files:** `frontend/src/api/schema/*` (sinh lại); sửa `frontend/src/features/places/components/{placeForm.ts,PlaceFormDialog.tsx}`, `frontend/src/features/items/components/{itemForm.ts,ItemFields.tsx,ItemFormDialog.tsx,ReplaceItemDialog.tsx}`, `frontend/src/test/{placeBackend.ts,itemBackend.ts}`; test `features/places/PlaceForm.test.tsx`, `features/items/ItemForm.test.tsx`, `features/items/ItemLifecycle.test.tsx`.

**Interfaces:**
- Consumes: Task 2 đến 5 (backend đang chạy bản mới để `npm run api:sync`).
- Produces: `PlaceFormValues` và `ItemFormValues` không còn `code`; `itemPayload` không trả `code`.

  - `npm run api:sync` với hai service đang chạy; commit cả JSON lẫn `.d.ts`.
  - Form thêm: dòng chữ nhỏ "Mã được cấp khi lưu." ở chỗ ô mã cũ. Form sửa: dòng chỉ đọc "Mã <mã> · do hệ thống cấp, không sửa được"; bản ghi chưa có mã thì không hiện dòng nào.
  - Thông báo sau khi thêm: `Đã thêm <tên> (<mã>)`.
  - `placeBackend` và `itemBackend`: lệnh tạo tự gán `code` (`DD-00000N`, `TS-00000N` theo thứ tự tạo) và bỏ qua `code` trong thân; lệnh sửa không đổi `code`.

- [ ] Test `PlaceForm.test.tsx`: `formHasNoCodeBox` (không có `textbox` tên "Mã" ở cả form thêm lẫn form sửa), `createdToastShowsTheIssuedCode` ("Đã thêm Phòng 301 (DD-000001)"), `editShowsTheLockedCode`, `editOfARecordWithoutCodeShowsNoCodeLine`, `submittedBodyHasNoCode` (thân `POST` và `PUT` không có khoá `code`). Sửa các test cũ đang gõ vào ô "Mã".
- [ ] Test `ItemForm.test.tsx`: `itemFormHasNoCodeBox`, `createdItemToastShowsTheIssuedCode`, `submittedItemBodyHasNoCode`; `ItemLifecycle.test.tsx`: `replacementBodyHasNoCode`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Remove the code inputs; codes come from the server`.

### Task 7: Frontend, "Cấp mã mới" trên trang địa điểm và trang món đồ

**Files:** tạo `frontend/src/components/common/ReissueCodeDialog.tsx`; sửa `frontend/src/features/places/{api.ts,queries.ts,components/PlaceActionsMenu.tsx,pages/PlacePage.tsx}`, `frontend/src/features/items/{api.ts,queries.ts,pages/ItemPage.tsx}`, `frontend/src/test/{placeBackend.ts,itemBackend.ts}`; test `features/places/PlaceActions.test.tsx`, `features/items/ItemLifecycle.test.tsx`.

**Interfaces:**
- Produces:

```ts
// places/api.ts, items/api.ts
reissuePlaceCode(ws: string, id: string, version: number): Promise<PlaceDetail>
reissueItemCode(ws: string, id: string, version: number): Promise<ItemDetail>
// queries.ts
useReissuePlaceCode(ws): UseMutationResult<PlaceDetail, ApiError, PlaceDetail>   // onSuccess: refreshAfterWrite(…, 'self')
useReissueItemCode(ws): UseMutationResult<ItemDetail, ApiError, ItemDetail>     // onSuccess: refreshItems
// components/common/ReissueCodeDialog.tsx
export function ReissueCodeDialog(props: {
  open: boolean; onOpenChange: (open: boolean) => void
  name: string; code: string | null
  isPending: boolean; error: ApiError | null
  onConfirm: () => void; onReload: () => void; isReloading: boolean
}): JSX.Element
// PlaceActionsMenu
export type PlaceAction = 'move' | 'address' | 'reissueCode' | 'delete'
export function PlaceActionsMenu(props: { hasCode: boolean; onSelect: (action: PlaceAction) => void }): JSX.Element
```

  - Mục menu: "Cấp mã mới" khi bản ghi có mã, "Cấp mã" khi chưa có. Món đã gỡ không có mục này.
  - Hộp thoại có mã: tiêu đề "Cấp mã mới cho <tên>?", nội dung "Mã hiện tại <mã> sẽ không còn tìm ra bản ghi này. Nhãn đã in theo mã cũ sẽ sai. Mã cũ không được cấp lại cho bản ghi nào khác." Chưa có mã: "Hệ thống sẽ cấp một mã theo dạng mã đang đặt trong Danh mục."
  - Xong: thông báo `Mã mới: <mã>`. 409 "đã có người sửa": `VersionConflictAlert` với "Tải bản mới" (`useFetchFreshPlace` / `useFetchFreshItem`); lỗi khác hiện nguyên văn.
  - Backend giả: `POST …/reissue-code` kiểm `version`, gán mã kế tiếp, tăng `version`.

- [ ] Test `PlaceActions.test.tsx`: `reissueWarnsAboutTheOldCode`, `reissueSendsTheVersionAndShowsTheNewCode` (thân `{version: 3}`, đầu trang hiện mã mới), `recordWithoutCodeOffersToAssignOne` (mục menu là "Cấp mã"), `reissueConflictOffersToReload`.
- [ ] Test `ItemLifecycle.test.tsx`: `reissuesAnItemCode`, `removedItemHasNoReissueAction`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add reissuing a code to the place and item pages`.

### Task 8: Frontend, phần "Mã tự cấp" của trang Danh mục

**Files:** tạo `frontend/src/features/catalog/codeRules.ts`, `frontend/src/features/catalog/components/CodeRuleSection.tsx`; sửa `frontend/src/features/catalog/{types.ts,api.ts,queries.ts,pages/CatalogPage.tsx,components/PlaceTypeSection.tsx}`, `frontend/src/test/catalogBackend.ts`; test `features/catalog/codeRules.test.ts`, `features/catalog/CatalogPage.test.tsx`.

**Interfaces:**
- Produces:

```ts
// types.ts
export type CodeTarget = 'PLACE' | 'ITEM'
export type CodeRule = Present<S['CodeRuleResponse'], 'target' | 'typeId' | 'prefix' | 'digits' | 'yearly' | 'builtIn'>
export type CodeStatus = Present<S['CodeStatusResponse'], 'placesWithoutCode' | 'itemsWithoutCode'>
// api.ts
listCodeRules(ws): Promise<CodeRule[]>
export const codeRuleApi = { put(ws, target, typeId, body), remove(ws, target, typeId) }
getCodeStatus(ws): Promise<CodeStatus>
backfillCodes(ws): Promise<Present<S['CodeBackfillResponse'], 'placesAssigned' | 'itemsAssigned' | 'placesRemaining' | 'itemsRemaining'>>
// queries.ts
catalogKeys.codeRules(ws), catalogKeys.codeStatus(ws)
useCodeRules(ws), useCodeStatus(ws)
useBackfillCodes(ws): { run: () => void; isRunning: boolean; assigned: number; total: number; error: ApiError | null }
// codeRules.ts
export const PREFIX_PATTERN = /^[A-Z][A-Z0-9]{0,9}$/
export function codeExample(rule: { prefix: string; digits: number; yearly: boolean }, year: number): string   // số 1
export function suggestTypeCode(name: string, taken: string[]): string
```

  - `codeExample({prefix: 'TS', digits: 4, yearly: true}, 2026)` = `TS-2026-0001`. Năm lấy từ `todayInBusinessZone()`.
  - `suggestTypeCode`: bỏ dấu (như `suggestFieldKey`, gồm `đ` thành `d`), viết hoa, ký tự khác `A-Z0-9` thành `_`, bỏ `_` hai đầu, cắt 46 ký tự; trùng `taken` (không phân biệt hoa thường) thì thêm `_2`, `_3`…; tên không còn ký tự nào thì trả `''`.
  - `CodeRuleSection`: bảng như mục 8 của spec. Dòng `typeId` `'0'` nhãn "Mọi địa điểm" / "Mọi đồ đạc", có "(có sẵn)" khi `builtIn`; dòng riêng nhãn "Loại: <tên>" / "Nhóm: <tên>" đứng ngay dưới dòng chung của đích nó. Thao tác: "Sửa" (mọi dòng), "Bỏ" (dòng riêng; và dòng chung đã đặt, nhãn "Về mặc định"). "Thêm dạng riêng": chọn đích rồi chọn một loại hoặc nhóm chưa có dạng riêng.
  - Hộp thoại sửa: tiền tố (tự viết hoa khi gõ), số chữ số (`NativeSelect` 3 đến 9), công tắc "Bắt đầu lại mỗi năm", dòng "Ví dụ: <mã>" đổi theo lúc gõ. Tiền tố trùng với một dạng của đích kia: cảnh báo "Địa điểm và đồ đạc sẽ có mã giống nhau", vẫn lưu được.
  - Dải thông báo "Có N địa điểm và M món đồ chưa có mã." kèm nút "Cấp mã" chỉ hiện khi N + M > 0. `useBackfillCodes.run` gọi `backfillCodes` lặp tới khi cả hai số còn lại bằng 0 hoặc một lượt không cấp được bản ghi nào; trong lúc chạy hiện "Đã cấp <assigned> / <total>…" (`total` là tổng hai số của `useCodeStatus` lúc bấm); xong thì thông báo "Đã cấp mã cho <assigned> bản ghi" và `queryClient.invalidateQueries()` (làm cũ mọi cache: danh sách và chi tiết của địa điểm, đồ đạc đều mang mã).
  - Form thêm loại địa điểm (`PlaceTypeSection`): không còn ô mã bắt buộc. Dưới ô tên hiện "Mã: <suggestTypeCode(tên, các mã đang có)>" và liên kết "Tự đặt mã"; bấm thì hiện ô mã như cũ (cùng luật `^[A-Z0-9_]+$`). Gửi mã gợi ý trừ khi người dùng đã tự đặt.
  - `catalogBackend`: thêm `/code-rules` (GET, PUT, DELETE), `/codes/status`, `/codes/backfill` (mỗi lần cấp tối đa `options.backfillBatch ?? 100`).

- [ ] Test `codeRules.test.ts`: `exampleFollowsPrefixDigitsAndYear`, `typeCodeDropsAccentsAndUppercases` ("Toà nhà" ra `TOA_NHA`, "Phòng đôi (VIP)" ra `PHONG_DOI_VIP`), `typeCodeAvoidsTakenCodes` (đã có `TOA_NHA` và `toa_nha_2`: ra `TOA_NHA_3`), `nameWithoutLettersGivesNoCode`.
- [ ] Test `CatalogPage.test.tsx`: `listsBuiltInRulesWithExamples`, `exampleChangesWhileTyping`, `savesARuleForOnePlaceType` (`PUT /code-rules/PLACE/<id>` thân `{prefix: 'P', digits: 4, yearly: false}`), `removingAnOwnRuleFallsBackToTheSharedOne`, `warnsWhenThePrefixIsUsedByTheOtherTarget`, `invalidPrefixIsBlockedBeforeSending`, `backfillBannerIsHiddenWhenEverythingHasACode`, `backfillRunsUntilNothingRemains` (ba lượt 100, 100, 40: đúng ba request, thông báo "Đã cấp mã cho 240 bản ghi"), `backfillStopsWhenARoundAssignsNothing`, `placeTypeCodeComesFromTheName` (thân `POST /place-types` có `code: 'KHO_LANH'` khi chỉ gõ tên "Kho lạnh"), `ownTypeCodeCanStillBeTyped`. Sửa test cũ đang gõ vào ô "Mã" của loại địa điểm.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the code rules section and derive place type codes from names`.

### Task 9: Hoàn tất

- [ ] Sửa `docs/superpowers/specs/2026-11-13-business-rules-design.md`: mục 3 thêm `code` vào danh sách trường do hệ thống quản lý; mục 5.4 và 5.9 bỏ `code` khỏi danh sách trường sửa được và thay luật "mã không bắt buộc duy nhất" bằng một dòng trỏ sang `2026-11-16-auto-codes-design.md`. Đổi trạng thái spec mã tự cấp thành "đã xây".
- [ ] `frontend/README.md`: một đoạn "Mã" (mã do backend cấp, form không có ô mã, "Cấp mã mới", phần "Mã tự cấp"; sau khi cấp mã hàng loạt thì làm cũ mọi cache).
- [ ] Chạy thử trên backend thật trong một không gian thử: đặt mẫu `P`/4 cho loại phòng, tạo hai phòng (thấy `P-0001`, `P-0002`), cấp mã mới cho một phòng, bấm "Cấp mã" khi có bản ghi cũ không mã; chụp màn hình phần "Mã tự cấp". Xoá dữ liệu vừa tạo theo đúng `id`.
- [ ] `./mvnw verify` (chạy nền, khoảng 20 phút) xanh; `npm test`, `npm run build`, `npm run lint` sạch. Commit `Document auto codes` và push `main`.

## Để sau, có lý do

- **Mã cho người (A6)**: bảng mẫu và bảng đếm đã chừa chỗ (`target` là chuỗi), nhưng người đã có số giấy tờ làm khoá tra cứu; làm khi có nhu cầu in thẻ.
- **Mã ghép theo cây (`A-03-301`, A5)**: phải quyết định mã đổi thế nào khi di chuyển địa điểm, đi ngược luật "mã không đổi". Cần một thiết kế riêng.
- **Nhãn QR (A9)**: cần mã ổn định trước, tức là sau đợt này.
- **Nhập mã có sẵn từ sổ sách**: thuộc nhập dữ liệu hàng loạt (F5); khi đó lệnh nhập được phép ghi mã, form thì vẫn không.
- **Cấp mã cho bản ghi cũ chạy nền**: lô 100 mỗi request đủ cho vài nghìn bản ghi; làm khi một không gian có hàng chục nghìn bản ghi không mã.
