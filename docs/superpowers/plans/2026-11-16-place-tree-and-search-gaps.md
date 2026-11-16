# Place Tree and Search Gaps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bù những chỗ backend còn thiếu mà frontend đang phải đoán (nút nào có con, kết quả tìm thuộc toà nào, lỗi 409 nào là "đã có người sửa", hoạt động gần đây thuộc bản ghi tên gì), rồi cho cây địa điểm đi được bằng bàn phím.

**Architecture:** Bốn thay đổi nhỏ ở `platform-common` và `registry-service`, mỗi cái thêm trường vào response chứ không đổi trường cũ, nên frontend cũ vẫn chạy. Sau đó frontend sinh lại kiểu (`npm run api:sync`) và dùng các trường mới; phần đoán cũ được bỏ.

**Tech Stack:** Java 21, Spring Boot 4.1, JdbcClient, MapStruct, JUnit 5 (backend); React 19, TanStack Query, Vitest + MSW (frontend). Không thêm thư viện.

**Spec:** `docs/superpowers/specs/2026-11-13-business-rules-design.md` (mục 3, 5.4, 5.10), `docs/superpowers/specs/2026-11-13-frontend-structure-design.md` (mục 5.4, 6). Nguồn của các việc: mục "Để lại cho backend" và "Deferred minors" của `2026-11-13-frontend-foundation-places.md`.

## Global Constraints

- Backend: theo mục "Quy ước" của `README.md` gốc (ba tầng, `WorkspaceRepository`, SQL viết tay, mọi query lọc `workspace_id` trước, `long` ra JSON là chuỗi). Thêm trường vào response, không đổi hay bỏ trường cũ.
- Mỗi truy vấn mới chạy một lần cho cả trang, không một lần cho từng dòng.
- `./mvnw test` phải xanh ở cuối mỗi task backend; các `*IT` mới chạy riêng bằng `-Dit.test=` như `dev-environment` ghi.
- Frontend: như Global Constraints của `2026-11-13-frontend-foundation-places.md` (npm qua Node của Windows, `id` là chuỗi, test trước, `npm test` + `npm run build` + `npm run lint` sạch).
- Mỗi task một commit; push `main` sau task cuối.

## Review Focus

1. Một nhánh chỉ còn con đã xoá mềm thì là lá. Test: `parentsWithChildrenIgnoresDeletedChildren` (Task 2).
2. Tìm "P301" khi hai toà đều có P301: người dùng phân biệt được hai kết quả. Test: `searchResultsShowTheirAncestors` (Task 5).
3. Lỗi 409 vì khoá dòng ("being changed by someone else; try again") không bị nhận nhầm là xung đột version. Test: `lockTimeoutIsNotAVersionConflict` (Task 1).
4. Hoạt động của một bản ghi đã xoá vẫn có tên. Test: `activityNamesDeletedRecords` (Task 4).
5. Trên cây, phím mũi tên không làm mất chỗ đang đứng khi một nhánh đang tải. Test: `arrowKeysSkipLoadingRows` (Task 6).

---

### Task 1: Mã máy đọc cho lỗi "đã có người sửa"

**Files:** sửa `platform-common/.../web/ConflictException.java`, `.../jdbc/WorkspaceRepository.java` (hàm `stale`), `.../openapi/ProblemDetailCustomizer.java`; `registry-service/.../place/PlaceService.java:132`, `.../item/ItemService.java:146`; test `platform-common/.../web/ConflictExceptionTests.java`, `.../openapi/ProblemDetailCustomizerTests.java`, `registry-service/.../place/PlaceIT.java`.

**Interfaces:**
- Produces: `ConflictException.versionConflict(String entityName, long id)` — 409, `detail` như cũ (`"<Entity> <id> was changed by someone else"`), thêm thuộc tính `code: "VERSION_CONFLICT"`. Hằng `ConflictException.VERSION_CONFLICT = "VERSION_CONFLICT"`. Schema `ProblemDetail` trong OpenAPI có thêm `code` (string).

- [ ] Test: `staleCarriesVersionConflictCode` (thuộc tính `code`, `detail` giữ nguyên), `otherConflictsHaveNoCode` (`new ConflictException("...")` không có `code`), `lockTimeoutIsNotAVersionConflict` (`ApiExceptionHandler.handleLockFailure` không có `code`), `problemDetailSchemaHasCode`; trong `PlaceIT`, test version lệch có sẵn kiểm thêm `$.code == "VERSION_CONFLICT"`.
- [ ] Hỏng → thay ba chỗ tự dựng thông báo bằng `ConflictException.versionConflict(...)` → PASS. Commit `Mark version conflicts with a machine-readable code`.

### Task 2: Danh sách địa điểm cho biết nút nào có con

**Files:** sửa `registry-service/.../place/{PlaceRepository,PlaceService,PlaceController,PlaceMapper,PlaceResponse}.java`; tạo `.../place/PlaceListItem.java`; test `PlaceListIT.java`.

**Interfaces:**
- Produces:

```java
// PlaceRepository
Set<Long> parentsWithChildren(long workspaceId, Collection<Long> placeIds)
	// SELECT DISTINCT parent_id ... WHERE workspace_id = ? AND parent_id IN (...) AND deleted_at IS NULL; rỗng thì không chạy SQL
// PlaceService
CursorPage<PlaceListItem> withChildFlags(long workspaceId, CursorPage<Place> page)
record PlaceListItem(Place place, boolean hasChildren)
// PlaceResponse thêm thành phần cuối: boolean hasChildren
```

  - Mọi API trả `CursorPage<PlaceResponse>` (cấp cao nhất, con, cây con, theo loại, theo khu vực, tìm) đi qua `withChildFlags`. Một truy vấn thêm cho mỗi trang, dùng index có sẵn trên `(workspace_id, parent_id, ...)`; kiểm bằng `EXPLAIN` trong test.

- [ ] Test (`PlaceListIT`): `childrenReportWhetherTheyHaveChildren`, `parentsWithChildrenIgnoresDeletedChildren`, `emptyPageRunsNoExtraQuery`, `parentsWithChildrenUsesAnIndex` (`EXPLAIN` không có `type = ALL`).
- [ ] Hỏng → viết → PASS; `./mvnw -pl registry-service test` xanh. Commit `Tell list callers which places have children`.

### Task 3: Kết quả tìm địa điểm kèm tổ tiên

**Files:** sửa `PlaceRepository`, `PlaceService`, `PlaceController` (`/places/search`), `PlaceMapper`; tạo `.../place/PlaceSearchResponse.java`; test `PlaceListIT.java`.

**Interfaces:**
- Produces:

```java
record PlaceSearchResponse(long id, Long parentId, long placeTypeId, String code, String name, PlaceStatus status,
		int depth, boolean hasChildren, int version, List<PlaceRef> ancestors)   // ancestors từ gốc tới cha
// PlaceRepository
Map<Long, String> namesOf(long workspaceId, Collection<Long> ids)   // một truy vấn; gồm cả dòng đã xoá mềm
```

  - `ancestors` lấy từ `path` của từng kết quả; gom mọi `id` tổ tiên của cả trang rồi gọi `namesOf` một lần.
  - Chỉ `/places/search` đổi kiểu response; các danh sách khác giữ `PlaceResponse`.

- [ ] Test: `searchResultsCarryAncestorsFromRootToParent`, `topLevelResultHasNoAncestors`, `ancestorNamesAreLoadedInOneQuery` (đếm câu lệnh bằng `CountingDataSource` hoặc log JDBC của test-jar).
- [ ] Hỏng → viết → PASS. Commit `Return ancestors with place search results`.

### Task 4: Hoạt động gần đây kèm tên bản ghi

**Files:** sửa `registry-service/.../history/{ActivityService,ActivityController,ActivityMapper,ActivityResponse}.java`; thêm `namesOf` vào `person/PersonRepository.java` và `item/ItemRepository.java` (cùng chữ ký với Task 3, `full_name` với người); test `history/ActivityIT.java`.

**Interfaces:**
- Produces: `ActivityResponse` thêm `String entityName` (tên lúc này, kể cả bản ghi đã xoá; `null` nếu dòng đã bị xoá hẳn). `ActivityService.list` trả `CursorPage<ActivityEntry>` với `record ActivityEntry(EntityChange change, String entityName)`; mỗi trang gọi `namesOf` tối đa một lần cho mỗi loại bản ghi có mặt.

- [ ] Test: `activityCarriesEntityNames` (địa điểm, người, đồ đạc trong cùng một trang), `activityNamesDeletedRecords`, `activityAfterRenameShowsTheCurrentName`.
- [ ] Hỏng → viết → PASS; `./mvnw test` xanh. Commit `Name the records in recent activity`.

### Task 5: Frontend dùng các trường mới

**Files:** `frontend/src/api/schema/*` (sinh lại), `frontend/src/api/errors.ts`, `frontend/src/features/places/{types.ts,api.ts,queries.ts}`, `components/{PlaceTreeNode,PlaceSearchResults,MovePlaceDialog}.tsx`; tạo `frontend/src/features/places/components/{PlacePicker.tsx,AncestorTrail.tsx}`; test `errors.test.ts`, `PlaceTree.test.tsx`, `PlaceActions.test.tsx`, `PlacePicker.test.tsx`.

**Interfaces:**
- Consumes: Task 1–4.
- Produces:

```ts
// api/errors.ts
get isVersionConflict(): boolean   // code === 'VERSION_CONFLICT'; không có code thì xét đuôi detail như cũ
// places/types.ts
export type PlaceSummary = Present<S['PlaceResponse'], ... | 'hasChildren'>
export type PlaceSearchResult = Present<S['PlaceSearchResponse'], 'id' | 'placeTypeId' | 'name' | 'status' | 'depth' | 'hasChildren' | 'version' | 'ancestors'>
// places/components
export function AncestorTrail(props: { ancestors: PlaceRef[] }): JSX.Element | null   // "Toà A › Tầng 3", chữ nhỏ, mờ
export function PlacePicker(props: { ws: string; exclude: string[]; value: string | null; onChange: (id: string | null, place: PlaceSearchResult | null) => void; allowTop?: boolean }): JSX.Element
	// ô tìm + nhóm radio (tách ra từ MovePlaceDialog); mỗi kết quả: tên, AncestorTrail, mã; exported qua index.ts
	// cho kế hoạch người ở và đồ đạc dùng lại
```

  - Cây: nút có `hasChildren: false` hiện chấm ngay, không mũi tên, không tải con. Địa điểm đang chọn được ghim (dòng thêm) vẫn tính là "có con" cho cha của nó.
  - `npm run api:sync` với hai service đang chạy; commit JSON và `.d.ts`.

- [ ] Test: `versionConflictIsReadFromTheCode`, `messageIsStillAFallback`; `leavesHaveNoArrowBeforeLoading` (không có request `children` cho lá); `searchResultsShowTheirAncestors` (hai "P301" ở hai toà, mỗi dòng có đường dẫn riêng); `movePickerShowsAncestors`. Sửa các test cây cũ cho fixture có `hasChildren`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Use child flags, search ancestors and conflict codes in the frontend`.

### Task 6: Cây đi được bằng bàn phím

**Files:** `frontend/src/features/places/components/{PlaceTree,PlaceTreeNode,treeContext}.tsx`; tạo `frontend/src/features/places/components/useTreeKeyboard.ts`; test `PlaceTreeKeyboard.test.tsx`.

**Interfaces:**
- Produces: `useTreeKeyboard(rootRef: RefObject<HTMLElement>): { onKeyDown: KeyboardEventHandler; focusedId: string | null }` — đọc thứ tự các dòng đang hiện từ DOM (`[data-tree-row]`), không giữ bản sao của cây.

  - Theo mẫu ARIA "tree" (role `tree`/`treeitem`/`group`, `aria-expanded`, `aria-level`, `aria-selected`) với tabindex luân phiên: chỉ một dòng có `tabIndex=0`. Đây là lúc dùng lại role `tree` đã bỏ ở bản trước, vì giờ đã có phím.
  - Phím: ↓/↑ dòng kế/trước đang hiện; → mở nhánh, nếu đã mở thì xuống con đầu; ← đóng nhánh, nếu đã đóng thì lên cha; Home/End dòng đầu/cuối; Enter mở trang của dòng; phím chữ nhảy tới dòng kế có tên bắt đầu bằng chữ đó.
  - Dòng "Đang tải", "Tải thêm", "Không tải được" không phải `treeitem`: phím mũi tên bỏ qua chúng; "Tải thêm" vẫn tới được bằng Tab.
  - Nút mũi tên và liên kết trong dòng có `tabIndex=-1`; chuột vẫn dùng như cũ.

- [ ] Test: `downAndUpMoveBetweenVisibleRows`, `rightOpensThenEntersFirstChild`, `leftClosesThenGoesToParent`, `enterOpensThePage`, `homeAndEndJump`, `typeaheadJumpsByFirstLetter`, `onlyOneRowIsTabbable`, `arrowKeysSkipLoadingRows`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Cập nhật `frontend/README.md` (một đoạn về phím của cây). Commit `Navigate the place tree with the keyboard` và push `main`.

## Để sau, có lý do

- **OpenAPI ghi rõ trường bắt buộc và trường có thể `null`** (để bỏ `Present<...>` ở frontend): phải chú thích từng thành phần của khoảng 40 record response bằng `@Nullable` (JSpecify) rồi viết một `ModelConverter` của springdoc; lợi ích là bỏ được vài dòng khai báo mỗi feature. Làm khi có thêm client thứ hai cần kiểu chính xác.
- **Tên người sửa trong lịch sử**: `changedBy` chỉ là `id`, identity chưa có API đọc tên thành viên. Hiện mỗi không gian chỉ có một thành viên (chưa có mời), nên frontend hiện "Bạn". Làm cùng phần mời thành viên.
