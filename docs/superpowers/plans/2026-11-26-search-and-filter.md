# Search and Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tìm chữ (chứa, không phân biệt hoa thường và dấu) và lọc theo mục tuỳ biến trên Người, Đồ đạc và trang Địa điểm mới dạng bảng; Ctrl+K mở các trang đó với `?q=`.

**Architecture:** Backend: gói mới `registry/search`:
- `TextSearch` sinh `LIKE %q%`;
- `AttributeConditions` đọc `attr=khoá:phép:giá trị`, kiểm theo mục tuỳ biến đang dùng, sinh điều kiện SQL đọc JSON `attributes` (mọi giá trị qua tham số).

Mỗi repository có một câu danh sách nhận bộ lọc, dùng `KeysetQuery.where`. Trang Địa điểm gọi `GET /places/list` (trong `CapacityController`) và trả `VacancyResponse` như Chỗ trống.

Frontend:
- mô-đun lọc mục tuỳ biến dùng chung trong `features/catalog`;
- trang `features/places/pages/PlacesPage.tsx`;
- trang Người và Đồ đạc thêm bộ lọc trên URL;
- `SearchPalette` có nhóm "Tìm trong nội dung".

**Tech Stack:** như `2026-11-26-excel-import.md` (Spring Boot 4, JdbcClient, MySQL 8; React 19, TanStack Query, msw, vitest).

**Spec:** `docs/superpowers/specs/2026-11-26-search-and-filter-design.md`.

## Global Constraints

- Lệnh Maven, npm, commit, push: như `2026-11-26-excel-import.md` (Global Constraints).
  - Nhánh `batch-11-search-filter`.
  - `mvnw verify` cả repo thì chuyển hướng log bên trong `cmd.exe` ra file `D:\…`.
  - Dừng registry preview trước `verify`.
  - Chạy cả bộ test frontend khi stack preview đã tắt.
- Không có migration. Không đổi hành vi của các API cũ khi không gửi tham số mới.
- Hằng số:
  - `TextSearch.MIN_LENGTH = 2`;
  - `AttributeConditions.MAX_CONDITIONS = 3`;
  - tham số chữ là `text`;
  - tham số của điều kiện thứ `i`: `a{i}_path`, `a{i}_v`, `a{i}_t{j}`.
- Tìm chữ (S1):
  - `TextSearch.pattern(q)`: cắt hai đầu (`Text.trimToNull`); dưới 2 ký tự thì `null`; còn lại là `"%" + escape + "%"`, thoát `\`, `%`, `_` như `Text.likePrefix`.
  - Cột tìm:
    - Người: `full_name`, `phone`, `email`, `id_number`, `note`;
    - Đồ đạc: `name`, `code`, `description`;
    - Địa điểm: `name`, `code`, `description`.
- Mục tuỳ biến (S2):
  - Dạng `khoá:phép:giá trị`, tách ở hai dấu `:` đầu (giá trị có thể chứa `:`).
  - Phép `has eq gte lte set unset`; với `set`, `unset` thì giá trị bỏ qua, có thể rỗng.
  - Phép theo kiểu:
    - TEXT `has eq set unset`;
    - NUMBER `eq gte lte set unset`;
    - DATE `gte lte set unset`;
    - BOOLEAN `eq set unset`;
    - SELECT `eq set unset`.
- Chữ lỗi 400 của backend (tiếng Anh như các lỗi 400 khác; giao diện không hiện vì đã lọc trước):
  - "At most 3 attribute conditions";
  - "attr must be key:op:value";
  - "Unknown field: {khoá}";
  - "Operator {phép} does not apply to field {khoá}";
  - "Invalid value for field {khoá}: {giá trị}";
  - "rootId needs living to be true or empty".
- URL của trang (S6):
  - `?q=`, `?type=`, `?status=`, `?root=`, `?room=1`, `?living=yes|no`, `?category=`;
  - `?f=khoá~phép~giá trị`, lặp tối đa 3, tách ở hai dấu `~` đầu.
- Chữ giao diện:
  - Nhãn phép:
    - `has` "chứa";
    - `eq` "bằng" ("là" với Có/Không và Lựa chọn);
    - `gte` "từ";
    - `lte` "đến";
    - `set` "có giá trị";
    - `unset` "trống".
  - Nút và mục:
    - nút "Thêm điều kiện", nút bỏ có `aria-label="Bỏ điều kiện {nhãn mục}"`;
    - mục thanh bên "Địa điểm";
    - Ctrl+K: nhóm "Tìm trong nội dung", dòng `Tìm “{chữ}” trong Người` / `… trong Đồ đạc` / `… trong Địa điểm`.

## Review Focus

1. **Giá trị chứa dấu ngăn.** Lọc "giờ mở cửa chứa 8:30", hay giá trị có `~`, vẫn đi nguyên vẹn từ URL tới SQL. Test:
   - `valueKeepsItsColons` (Task 1);
   - `valueKeepsItsTildes` (Task 5).
2. **Đường dẫn chép từ trước khi một mục bị lưu trữ.** Trang mở bình thường, điều kiện của mục đó bị bỏ qua, không gửi lên nên không 400. Test: `archivedFieldInTheUrlIsIgnored` (Task 6).
3. **Cùng khoá ở hai loại khác kiểu.** Ví dụ `dien_tich` là số ở Phòng và chữ ở Kho. Lọc "từ 20" khớp Phòng đủ điều kiện, không 400, không khớp Kho. Test: `sameKeyOnTwoTypesIsJudgedByEachType` (Task 2).
4. **Đồ chưa có nhóm.** Với mục của một nhóm: `eq` không khớp đồ chưa có nhóm, `unset` khớp. Test: `uncategorizedItemsAreBlankForCategoryFields` (Task 4).
5. **Một người ở hai nơi cùng lúc.** Lọc "Đang ở" hay "Toà A" hiện người đó một lần. Test: `personLivingInTwoPlacesAppearsOnce` (Task 3).

---

### Task 1: Bộ dựng điều kiện tìm và lọc (S1, S2)

**Files:**
- Create: `registry-service/src/main/java/com/home_platform/registry/search/TextSearch.java`, `AttributeOp.java`, `AttributeCondition.java`, `AttributeConditions.java`, `SqlCondition.java`, `package-info.java` (theo quy ước gói của repo, nếu các gói khác có)
- Test: `registry-service/src/test/java/com/home_platform/registry/search/TextSearchTests.java`, `AttributeConditionsTests.java`

**Interfaces:**
- Produces:
  - `TextSearch`:
    - `static final int MIN_LENGTH = 2`;
    - `static final String PARAM = "text"`;
    - `static String pattern(String q)`, trả `null` nếu không tìm;
    - `static SqlCondition anyColumnContains(String pattern, String... columns)`, sinh `(c1 LIKE :text OR c2 LIKE :text …)` với `Map.of("text", pattern)`.
  - `record SqlCondition(String sql, Map<String, Object> params)`.
  - `enum AttributeOp { HAS, EQ, GTE, LTE, SET, UNSET }`:
    - `static AttributeOp fromCode(String)`: mã viết thường, sai thì `null`;
    - `boolean needsValue()`: false với SET, UNSET.
  - `record AttributeCondition(String key, AttributeOp op, String value)`.
  - `AttributeConditions`:
    - `static final int MAX_CONDITIONS = 3`;
    - `static List<AttributeCondition> parse(List<String> raw)`: `null` hay rỗng thì `List.of()`; ném `BadRequestException`;
    - `static List<SqlCondition> toSql(List<AttributeCondition> conditions, List<FieldDef> defs, String typeColumn)`:
      - `defs` là mọi mục của đích, mọi trạng thái; hàm tự lọc `ACTIVE`;
      - `typeColumn` là `null` với Người, `"place_type_id"`, `"category_id"`.

- [ ] **Step 1: Viết test đơn vị**

`TextSearchTests`:
- `pattern("  ab ")` = `"%ab%"`;
- `pattern("a")` = `null`;
- `pattern(" ")` = `null`;
- `pattern(null)` = `null`;
- `pattern("5%_\\")` = `"%5\\%\\_\\\\%"`;
- `anyColumnContains("%ab%", "name", "code").sql()` = `"(name LIKE :text OR code LIKE :text)"`, params `{text=%ab%}`.

`AttributeConditionsTests`. Dùng `FieldDef` dựng tay; `def(key, type, typeId, options…)` là helper trong test.
- `parsesKeyOpAndValue`: `"dien_tich:gte:20"` → `(dien_tich, GTE, "20")`.
- `valueKeepsItsColons`: `"gio_mo:has:8:30"` → value `"8:30"`.
- `setNeedsNoValue`: `"cccd_het_han:set:"` và `"cccd_het_han:set"` → `(cccd_het_han, SET, "")`.
- `fourConditionsAreRejected`: message "At most 3 attribute conditions".
- `malformedIsRejected`: `"dien_tich"` và `"dien_tich:zz:1"` → "attr must be key:op:value".
- `unknownOrArchivedKeyIsRejected`: "Unknown field: x".
- `operatorMustFitTheType`: `has` trên NUMBER → "Operator has does not apply to field dien_tich".
- `valuesAreCheckedByType`: tất cả → "Invalid value for field …: …".
  - NUMBER `"abc"`;
  - DATE `"2026-13-01"`;
  - BOOLEAN `"maybe"`;
  - SELECT ngoài `options`;
  - TEXT rỗng với `has`.
- `numberBecomesADecimalParameter`: NUMBER mục chung `gte 20.5`:
  - params `a0_path = "$.dien_tich"`;
  - `a0_v = new BigDecimal("20.5")`;
  - không có `a0_t0`.
- `typedFieldRestrictsByType`: mục của loại 7 → params có `a0_t0 = 7L` và sql chứa `place_type_id = :a0_t0`.
- `textHasEscapesLikeCharacters`: `has 5%` → `a0_v = "%5\\%%"`.
- `sameKeyKeepsOnlyDefsTheValueFits`: `dien_tich` NUMBER loại 7 và TEXT loại 8, `gte 20` → chỉ có `a0_t0 = 7L`. `has abc` trên cùng cặp → chỉ có loại 8.

- [ ] **Step 2: Chạy, thấy FAIL (chưa có lớp).**

Run: `-pl registry-service -am test -Dtest=TextSearchTests,AttributeConditionsTests -Dsurefire.failIfNoSpecifiedTests=false`
Expected: lỗi biên dịch, thiếu `TextSearch`.

- [ ] **Step 3: Viết code.** `toSql` cho điều kiện `i`:
  - `P = :a{i}_path`, giá trị là `"$." + key`. Khoá đã đúng mẫu `[a-z][a-z0-9_]*` khi tạo mục.
  - Mỗi mục `d` (đang dùng, đúng khoá, hợp phép và hợp giá trị) cho một vế `(typeMatch AND valueCond)`. `typeMatch` là:
    - `TRUE` khi `typeColumn == null` hoặc `d.typeId() == 0`;
    - ngược lại `typeColumn = :a{i}_t{j}`.
  - Các vế nối bằng `OR`. Không mục nào hợp thì:
    - có mục đúng khoá nhưng sai phép: lỗi phép;
    - hợp phép nhưng sai giá trị: lỗi giá trị.
  - `UNSET` là `NOT (` các vế của `SET` `)`: bản ghi khác loại cũng là trống.
  - Biểu thức (`J = JSON_EXTRACT(attributes, P)`, `U = JSON_UNQUOTE(J)`):

| Kiểu, phép | `valueCond` | `a{i}_v` |
|---|---|---|
| mọi kiểu, `set` | `J IS NOT NULL AND U <> ''` | — |
| TEXT `has` / `eq` | `CONVERT(U USING utf8mb4) LIKE :v` / `= :v` | `TextSearch.pattern`-kiểu chứa / chữ đã cắt |
| NUMBER `eq gte lte` | `JSON_TYPE(J) IN ('INTEGER','UNSIGNED INTEGER','DECIMAL','DOUBLE') AND CAST(J AS DECIMAL(65,10)) {= >= <=} :v` | `BigDecimal` |
| DATE `gte lte` | `U {>= <=} :v` | `LocalDate.parse(...).toString()` |
| BOOLEAN `eq true` / `eq false` | `COALESCE(J = CAST('true' AS JSON), FALSE)` / `NOT COALESCE(J = CAST('true' AS JSON), FALSE)` | — |
| SELECT `eq` | `U = :v` | đúng một trong `options` |

  `CONVERT(… USING utf8mb4)` cho collation mặc định của utf8mb4, cũng là collation các cột tạo bằng `DEFAULT CHARSET=utf8mb4`. Nhờ vậy chữ trong JSON so giống cột thường: không phân biệt hoa thường và dấu.

- [ ] **Step 4: Chạy lại** → PASS.
- [ ] **Step 5: Commit** "Build text and custom-field conditions for list filters".

### Task 2: Trang Địa điểm, phía API (S3)

**Files:**
- Modify:
  - `place/PlaceRepository.java`: thêm `list`;
  - `place/PlaceService.java`: thêm `list`, đổi `rootId` thành `path`;
  - `capacity/CapacityService.java`: thêm `list`; tách phần dựng `VacancyResponse` khỏi `vacancies` thành `private CursorPage<VacancyResponse> rows(long ws, CursorPage<Place> page)` dùng chung;
  - `capacity/CapacityController.java`: `GET /places/list`;
  - `README.md`: dòng API `/places`.
- Create: `place/PlaceFilter.java`
- Test: `registry-service/src/test/java/com/home_platform/registry/place/PlaceFilterIT.java` (extends `OccupancyTestData`, như `VacancyIT`)

**Interfaces:**
- Consumes: Task 1.
- Produces:
  - `record PlaceFilter(String text, Long placeTypeId, PlaceStatus status, String rootPath, boolean hasRoom, List<SqlCondition> attributes)`.
  - `PlaceRepository.list(long workspaceId, PlaceFilter filter, LocalDate today, CursorRequest page)`:
    - theo `name`;
    - `rootPath`: hậu duệ, không gồm gốc, như `vacancies`;
    - `hasRoom`: `capacity IS NOT NULL AND PEOPLE_LIVING_HERE < capacity`.
  - `PlaceService.list(long ws, String q, Long typeId, PlaceStatus status, Long rootId, boolean hasRoom, List<String> attr, CursorRequest page)`: dựng `PlaceFilter`; lấy mục từ `FieldDefService.listAll` lọc `target == PLACE`.
  - `CapacityService.list(...)` cùng tham số, trả `CursorPage<VacancyResponse>`.
  - `GET /places/list?q=&typeId=&status=&rootId=&hasRoom=&attr=&cursor=&limit=` (`attr` lặp).

- [ ] **Step 1: Viết IT** (`PlaceFilterIT`):
- `rowsCarryTheTableColumns`: `code`, `name`, `placeTypeName`, `status`, `ancestors[*].name`, `capacity`, `occupantCount`.
- `textMatchesNameCodeAndDescriptionIgnoringCaseAndAccents`:
  - `q=hue` khớp tên "Nhà Huế";
  - `q=p-0` khớp mã "P-001";
  - `q=ban cong` khớp mô tả "Có ban công".
- `likeCharactersAreLiteral`: `q=50%` chỉ khớp "Giảm 50%", không khớp "500".
- `oneLetterIsIgnored`: `q=a` trả mọi địa điểm.
- `filtersByTypeStatusAndBuilding`: `rootId` không gồm chính toà; `status=INACTIVE`.
- `hasRoomNeedsCapacityAndAFreeSeat`:
  - phòng sức chứa 2 có 1 người: có;
  - đủ người: không;
  - chưa đặt sức chứa: không;
  - sức chứa 0: không.
- `numberDateBooleanSelectAndTextFilters`: mỗi kiểu một mục chung, mỗi phép một lần gọi:
  - `gte`, `lte`, `eq`;
  - `has` "hue" khớp "Huế";
  - `eq false` khớp ô trống;
  - `set`, `unset`.
- `typedFieldOnlyMatchesItsType`: `set` chỉ khớp loại của mục; `unset` khớp cả loại khác.
- `sameKeyOnTwoTypesIsJudgedByEachType` (Review Focus 3).
- `conditionsCombineWithAnd`: hai điều kiện, chỉ bản ghi khớp cả hai.
- `badFiltersAre400`: khoá lạ; giá trị sai; 4 điều kiện.
- `pagesByNameAndHidesDeleted`: `limit=2`, cursor, địa điểm đã xoá không hiện.

- [ ] **Step 2: Chạy, thấy FAIL** (404 vì chưa có `/places/list`).

Run: `-pl registry-service -am verify -Dit.test=PlaceFilterIT -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false` (log ra file).

- [ ] **Step 3: Viết code** như Interfaces.
  - `/places/list` là đường cố định nên đứng trước `/{id}`, như `/places/vacancies`.
  - `rootId` sai hay đã xoá thì 404, như `vacancies`.
  - README: thêm dòng `/places/list` cạnh `/places/vacancies`.
- [ ] **Step 4: Chạy lại `PlaceFilterIT` và `VacancyIT`** → PASS.
- [ ] **Step 5: Commit** "List places with text, building, room and custom-field filters".

### Task 3: Lọc người (S4)

**Files:**
- Modify:
  - `person/PersonRepository.java`: thay `list(ws, page)` bằng `list(ws, PersonFilter, LocalDate today, page)`;
  - `person/PersonService.java`;
  - `person/PersonController.java`: `GET /people` nhận `q`, `living`, `rootId`, `attr`;
  - `README.md`: dòng `/people`.
- Create: `person/PersonFilter.java`
- Test: `person/PersonFilterIT.java` (extends `OccupancyTestData`)

**Interfaces:**
- Consumes: Task 1.
- Produces:
  - `record PersonFilter(String text, Boolean living, String rootPath, List<SqlCondition> attributes)`.
  - `PersonService.list(long ws, String q, Boolean living, Long rootId, List<String> attr, CursorRequest page)`: theo `full_name`.
  - Điều kiện đang ở là `EXISTS`, không JOIN, nên một người chỉ ra một dòng:
    `EXISTS (SELECT 1 FROM reg_occupancy o JOIN reg_place pl ON pl.workspace_id = o.workspace_id AND pl.id = o.place_id WHERE o.workspace_id = reg_person.workspace_id AND o.person_id = reg_person.id AND o.deleted_at IS NULL AND pl.deleted_at IS NULL AND o.start_date <= :today AND (o.end_date IS NULL OR o.end_date >= :today) [AND pl.path LIKE :rootPrefix])`.
    - `living=false` là `NOT EXISTS` (không có `rootPrefix`).
    - `rootId` kèm `living=false` thì 400 "rootId needs living to be true or empty". `rootId` tự nghĩa là đang ở trong toà, gồm chính toà: `rootPrefix = path + "%"`.

- [ ] **Step 1: Viết IT:**
- `textMatchesPhoneEmailIdNumberAndNote`: mỗi cột một người.
  - `q=0905` theo số điện thoại;
  - `q=gmail` theo email;
  - `q=0790` theo số giấy tờ;
  - `q=nuoi meo` khớp ghi chú "Nuôi mèo".
- `livingYesAndNo`: người đang ở / đã chuyển đi / chưa ở đâu.
- `buildingMeansLivingThereOrInside`:
  - ở chính toà: có;
  - ở phòng con: có;
  - ở toà khác: không.
- `personLivingInTwoPlacesAppearsOnce` (Review Focus 5).
- `livingFalseWithBuildingIs400`.
- `customFieldsFilterPeople`:
  - `que_quan:has:hue` khớp "Huế";
  - `cccd_het_han:lte:2026-12-31`.
- `noParametersListsEveryoneAsBefore`: như `PersonListIT` cũ.

- [ ] **Step 2: Chạy → FAIL.**
- [ ] **Step 3: Viết code** như Interfaces; mọi chỗ khác gọi `PersonRepository.list(ws, page)` cũ (export, nếu có) chuyển sang `new PersonFilter(null, null, null, List.of())`; README.
- [ ] **Step 4: Chạy `PersonFilterIT`, `PersonListIT`, `PersonIT`, `ExportWorkspaceIT`** → PASS.
- [ ] **Step 5: Commit** "Filter people by text, where they live and custom fields".

### Task 4: Lọc đồ đạc (S5)

**Files:**
- Modify:
  - `item/ItemRepository.java`: `all(ws, status, categoryId, page)` thành `all(ws, ItemFilter, page)`;
  - `item/ItemService.java`: `all`;
  - `item/ItemController.java`: `GET /items` nhận `q`, `rootId`, `attr`;
  - `README.md`: dòng `/items`.
- Create: `item/ItemFilter.java`
- Test: `item/ItemFilterIT.java` (extends `ItemTestData`)

**Interfaces:**
- Consumes: Task 1.
- Produces:
  - `record ItemFilter(ItemStatus status, Long categoryId, String text, String rootPath, List<SqlCondition> attributes)`.
    - `status == null` vẫn là `removed_at IS NULL`.
    - `rootPath` gồm chính toà, cùng biểu thức như `inSubtree`, nhưng tên tham số là `rootPrefix`.
  - `ItemService.all(long ws, ItemStatus status, Long categoryId, String q, Long rootId, List<String> attr, CursorRequest page)`.
    - `typeColumn = "category_id"`.
    - Đồ có `category_id IS NULL` không khớp vế của mục theo nhóm, vì `NULL = :t` là không đúng.
  - Controller: `q`, `rootId`, `attr` chỉ đi với "không phạm vi" hoặc `categoryId`, như `status`. Đi với `placeId`, `subtreeOf`, `ownerId` thì 400, sửa câu báo thành "status, q, rootId and attr can only be combined with categoryId or no scope".

- [ ] **Step 1: Viết IT:**
- `textMatchesNameCodeAndDescription`.
- `buildingIncludesItsOwnPlace`: đồ đặt ở chính toà và ở phòng con đều có.
- `combinesWithStatusAndCategory`.
- `categoryFieldOnlyMatchesItsCategory`.
- `uncategorizedItemsAreBlankForCategoryFields` (Review Focus 4).
- `textWithAPlaceScopeIs400`: `placeId` + `q`.
- `pages`.

- [ ] **Step 2: Chạy → FAIL.**
- [ ] **Step 3: Viết code.** Mọi chỗ gọi `repository.all` cũ (export, nếu có) chuyển sang `new ItemFilter(status, categoryId, null, null, List.of())`. README.
- [ ] **Step 4: Chạy `ItemFilterIT`, `ItemListIT`, `ExportWorkspaceIT`** → PASS.
- [ ] **Step 5: Commit** "Filter items by text, building and custom fields".

### Task 5: Đồng bộ kiểu API và bộ lọc mục tuỳ biến dùng chung

**Files:**
- Modify: `frontend/src/api/schema/registry.json`, `registry.d.ts` (sinh lại); `frontend/src/features/catalog/index.ts`
- Create: `frontend/src/features/catalog/attributeFilters.ts`, `frontend/src/features/catalog/components/AttributeFilters.tsx`
- Test: `frontend/src/features/catalog/attributeFilters.test.ts`, `frontend/src/features/catalog/AttributeFilters.test.tsx`

**Interfaces:**
- Produces (xuất qua `@/features/catalog`):
  - Kiểu:
    - `type AttributeOp = 'has' | 'eq' | 'gte' | 'lte' | 'set' | 'unset'`;
    - `type AttributeCondition = { key: string; op: AttributeOp; value: string }`.
  - Hằng:
    - `OPS_BY_TYPE: Record<FieldDef['dataType'], AttributeOp[]>`, theo Global Constraints;
    - `opLabel(op, dataType): string`;
    - `MAX_CONDITIONS = 3`.
  - `parseConditions(raw: string[], defs: FieldDef[]): AttributeCondition[]`:
    - giá trị `?f=`, tách ở hai `~` đầu;
    - bỏ khoá không thuộc mục `ACTIVE` trong `defs`;
    - bỏ phép không hợp kiểu;
    - lấy tối đa 3.
  - `toUrlValues(conditions): string[]`.
  - `toApiValues(conditions): string[]`: `key:op:value`; bỏ điều kiện cần giá trị mà giá trị rỗng.
  - `filterableDefs(defs: FieldDef[], typeId: string | null): FieldDef[]`:
    - mục `ACTIVE`, `typeId === '0'` hoặc đúng loại đã chọn; chưa chọn loại thì mọi mục `ACTIVE`;
    - mỗi `fieldKey` một mục, giữ mục có `sortOrder` nhỏ nhất.
  - `<AttributeFilters defs={FieldDef[]} value={AttributeCondition[]} onChange={(next) => void} />`:
    - mỗi điều kiện một hàng: chọn mục, chọn phép (theo kiểu), ô giá trị, nút bỏ;
    - ô giá trị theo kiểu: TEXT chữ, NUMBER `type="number"`, DATE `type="date"`, BOOLEAN chọn Có/Không, SELECT chọn từ `options`;
    - phép `set`/`unset` thì không có ô giá trị;
    - đổi mục thì phép về phép đầu của kiểu mới, giá trị rỗng;
    - nút "Thêm điều kiện" thêm mục đầu tiên, tắt khi đủ 3 hay không có mục nào.

- [ ] **Step 1: Đồng bộ kiểu.**
  - Build: `-pl registry-service -am package -DskipTests`.
  - Chạy jar registry bằng Windows java 21 với `--server.port=18082`, từ `.superpowers/preview`.
  - Trong `frontend/`: `cmd.exe /c "set HP_REGISTRY_PORT=18082&& npm run api:sync -- --only=registry"`. Expected: `registry.d.ts` có `/places/list`, và `/people`, `/items` có `q`, `attr`.
  - Dừng jar.
  - `FieldDef` của frontend thêm `'options'` vào `Present` nếu chưa có.
- [ ] **Step 2: Viết test.**

`attributeFilters.test.ts`:
- `parsesAndWritesTheUrlForm`: khứ hồi `dien_tich~gte~20`.
- `valueKeepsItsTildes`: `ghi_chu~has~a~b` → value `a~b`.
- `unknownKeysAndWrongOpsAreDropped`.
- `atMostThree`.
- `apiFormSkipsIncompleteConditions`: `has` rỗng bị bỏ; `set` rỗng vẫn gửi `k:set:`.
- `filterableDefsNarrowByType`.

`AttributeFilters.test.tsx`:
- `addingAConditionPicksTheFirstField`.
- `operatorsFollowTheFieldType`: NUMBER có "từ", "đến"; DATE không có "bằng".
- `valueInputFollowsTheType`: DATE là ô ngày; SELECT có lựa chọn; `set` không có ô.
- `addIsDisabledAtThree`.
- `removeDropsTheCondition`: theo `aria-label`.

- [ ] **Step 3: Chạy** `cmd.exe /c "npx vitest run src/features/catalog/attributeFilters.test.ts src/features/catalog/AttributeFilters.test.tsx"` → FAIL.
- [ ] **Step 4: Viết code**, theo kiểu `NativeSelect`, `Input`, `Button` đang dùng ở `ItemsPage`.
- [ ] **Step 5: Chạy lại → PASS; build và lint 0 lỗi** (kiểu mới không làm vỡ chỗ gọi cũ).
- [ ] **Step 6: Commit** "Sync API types and add the shared custom-field filter".

### Task 6: Trang Địa điểm (S3, S6)

**Files:**
- Create:
  - `frontend/src/features/places/pages/PlacesPage.tsx`;
  - `frontend/src/features/places/components/PlaceListTable.tsx`;
  - `frontend/src/features/places/components/PlaceFilters.tsx`;
  - `frontend/src/features/places/placeFilter.ts`: đọc và ghi URL.
- Modify:
  - `features/places/api.ts`: `listPlaces`;
  - `features/places/types.ts`: `PlaceRow`, `PlaceFilter`;
  - `features/places/queries.ts`: `placeKeys.filtered`, `usePlaceList`; làm mới trong `refreshAfterWrite` và `useDeletePlace`;
  - `features/places/index.ts`;
  - `features/people/queries.ts`: `refreshOccupancies` làm mới `['places', ws, 'filtered']`, viết thẳng như `['vacancies', ws]`;
  - `app/router.tsx`: `places` → `<PlacesPage />`;
  - `app/layout/AppLayout.tsx`: mục `{ path: 'places', label: 'Địa điểm', icon: Building2Icon }` ngay sau "Việc cần làm".
- Test: `frontend/src/features/places/PlacesPage.test.tsx`; `app/app.test.tsx` (mục thanh bên)

**Interfaces:**
- Produces:
  - `type PlaceRow`: như `Vacancy` của `features/vacancies`, `id = placeId`; định nghĩa lại trong `places/types.ts` để không import vòng.
  - `type PlaceFilter = { q: string; placeTypeId: string | null; status: 'ACTIVE' | 'INACTIVE' | null; rootId: string | null; hasRoom: boolean; conditions: AttributeCondition[] }`.
  - `placeKeys.filtered(ws, filter)` = `['places', ws, 'filtered', filter]`; `placeKeys.filteredAll(ws)` = `['places', ws, 'filtered']`.
  - `usePlaceList(ws, filter): CursorList<PlaceRow>`.
  - Trang đọc URL rồi cho qua `useDebouncedValue(searchParams.toString(), 300)` trước khi gọi API. Ô chữ vẫn gõ ngay trên URL (`replace: true`).
  - Lọc mục tuỳ biến dùng `filterableDefs(useAllFieldDefs(ws,'PLACE').data, placeTypeId)`.
  - Toà dùng `useTopLevelPlaces`, Loại dùng `usePlaceTypes`.
  - Bảng: cột Mã, Tên (link `/w/:ws/places/:id`), Loại, Nằm trong (`›`), Sức chứa, Đang ở, Trạng thái. Trên điện thoại dùng `PHONE_LABEL` như `VacancyList`.
  - Không có dòng nào: "Không có địa điểm nào khớp bộ lọc".

- [ ] **Step 1: Viết test** (MSW bắt query của `/api/registry/places/list`):
- `showsTheTableColumns`.
- `eachFilterGoesToTheUrlAndTheRequest`:
  - gõ chữ → `?q=` và `q`;
  - chọn loại → `?type=` và `typeId`;
  - chọn trạng thái;
  - chọn toà → `?root=` và `rootId`;
  - tích "Còn chỗ" → `?room=1` và `hasRoom=true`;
  - thêm điều kiện → `?f=` và `attr`.
- `urlFiltersAreReadOnLoad`.
- `archivedFieldInTheUrlIsIgnored` (Review Focus 2): request không có `attr`.
- `strangeValuesAreIgnored`: `?type=abc&status=X`.
- `loadsMore`.
- `app.test`: thanh bên có "Địa điểm" ngay sau "Việc cần làm".

- [ ] **Step 2: Chạy → FAIL.**
- [ ] **Step 3: Viết code.**
- [ ] **Step 4: Chạy file test, rồi cả `npm test`** → PASS.
- [ ] **Step 5: Commit** "Add the places table with filters".

### Task 7: Trang Người (S4, S6)

**Files:**
- Modify:
  - `features/people/api.ts`: `listPeople(ws, filter, cursor)` gửi `q`, `living`, `rootId`, `attr`;
  - `features/people/types.ts`: `PersonFilter`;
  - `features/people/queries.ts`: `personKeys.filtered`, `usePeopleList` thay `usePeople`; `refreshOccupancies` làm mới thêm `personKeys.lists(ws)`;
  - `features/people/pages/PeoplePage.tsx`.
- Test: `frontend/src/features/people/PeoplePage.test.tsx`

**Interfaces:**
- Produces:
  - `type PersonFilter = { q: string; living: 'yes' | 'no' | null; rootId: string | null; conditions: AttributeCondition[] }`.
  - `personKeys.filtered(ws, filter)` = `['people', ws, 'list', 'filtered', filter]`.
  - Trang:
    - một ô tìm `aria-label="Tìm người"`, placeholder "Tên, số điện thoại, email, giấy tờ, ghi chú…", thay ô "Tìm theo";
    - chọn "Cư trú": "Mọi người" / "Đang ở" / "Không ở đâu";
    - chọn "Toà" (`useTopLevelPlaces`): chọn toà thì `living` thành `yes`; "Không ở đâu" thì xoá và tắt ô Toà;
    - bộ lọc mục tuỳ biến (`PERSON`).
  - URL: `?q=`, `?living=`, `?root=`, `?f=`.
  - `usePeopleSearch` giữ nguyên cho `PersonPicker`.

- [ ] **Step 1: Sửa và thêm test:**
- `searchBoxSendsQ`: thay các test "Tìm theo" cũ, có chủ đích.
- `livingFilterSendsLiving`: `living=true` / `false`.
- `choosingABuildingMeansLivingThere`.
- `notLivingAnywhereDisablesTheBuilding`.
- `customFieldConditionSendsAttr`.
- `urlFiltersAreReadOnLoad`.
- `emptyWorkspaceStillShowsTheAddButton`: giữ hành vi cũ.

- [ ] **Step 2: Chạy → FAIL.**
- [ ] **Step 3: Viết code.**
- [ ] **Step 4: Chạy `PeoplePage.test`, `PersonPicker`/`PersonChoiceField` test, rồi cả `npm test`** → PASS.
- [ ] **Step 5: Commit** "Search people in every field and filter by where they live".

### Task 8: Trang Đồ đạc (S5, S6)

**Files:**
- Modify:
  - `features/items/api.ts`: `ItemFilter` thêm `q`, `rootId`, `conditions`; `listAllItems` gửi chúng;
  - `features/items/pages/ItemsPage.tsx`.
- Test: `frontend/src/features/items/ItemsPage.test.tsx`

**Interfaces:**
- Produces:
  - `type ItemFilter = { status: ItemStatus | null; categoryId: string | null; q: string; rootId: string | null; conditions: AttributeCondition[] }`.
  - Trang:
    - thêm ô tìm `aria-label="Tìm đồ"`, placeholder "Tên, mã, mô tả…";
    - thêm "Toà" (`useTopLevelPlaces`);
    - thêm bộ lọc mục tuỳ biến: `filterableDefs(ITEM defs, categoryId)`.
  - URL thêm `?q=`, `?root=`, `?f=`.
  - Nút Xuất Excel vẫn chỉ gửi `status`, `categoryId`. Ruling: xuất theo bộ lọc mới không nằm trong spec.

- [ ] **Step 1: Thêm test:**
- `searchBoxSendsQ`.
- `buildingSendsRootId`.
- `categoryNarrowsTheCustomFields`.
- `exportKeepsStatusAndCategoryOnly`.
- `urlFiltersAreReadOnLoad`.

- [ ] **Step 2: Chạy → FAIL.** **Step 3: Viết code.** **Step 4: Chạy file test, rồi cả `npm test`** → PASS.
- [ ] **Step 5: Commit** "Search items and filter by building and custom fields".

### Task 9: Ctrl+K, xem bằng mắt, tài liệu (S7)

**Files:**
- Modify: `frontend/src/features/search/components/SearchPalette.tsx`; `README.md`, `frontend/README.md` nếu có mục trang; spec, trạng thái thành "đã xây"
- Test: `frontend/src/features/search/SearchPalette.test.tsx`

**Interfaces:**
- `SHORTCUTS` thêm `{ label: 'Địa điểm', path: 'places' }` sau "Người".
- Khi `typed.length >= 2`, cuối danh sách có nhóm "Tìm trong nội dung" gồm ba `CommandItem` (`value` `content:people`…). Chọn thì `go(\`/w/${ws}/people?q=${encodeURIComponent(typed)}\`)`, tương tự `items`, `places`. Dùng `typed`, không chờ debounce.
- Nhóm này hiện cả khi không có kết quả tên, mã. "Không tìm thấy gì." chỉ là dòng phụ.

- [ ] **Step 1: Test:**
- `contentSearchOpensTheListWithQ`: gõ "bảo hành", chọn "Tìm “bảo hành” trong Đồ đạc", URL là `/w/1/items?q=b%E1%BA%A3o%20h%C3%A0nh`.
- `oneLetterShowsNoContentSearch`.
- `placesIsAShortcut`.

- [ ] **Step 2: FAIL → code → PASS.**
- [ ] **Step 3: Xem bằng mắt** (stack preview, `seed.mjs`): trang Địa điểm, Người, Đồ đạc có bộ lọc mục tuỳ biến ở 1440 và 390, sáng và tối; Ctrl+K có nhóm mới. Dừng stack, xoá `chrome-profile`.
- [ ] **Step 4: Kiểm cả repo.** `npm test`, build, lint; `mvnw -B verify` (log ra file bên Windows) → xanh.
- [ ] **Step 5: Commit** "Search content from the palette and document the filters".
