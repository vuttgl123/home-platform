# Overview and Export Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mở không gian là thấy ngay tình hình; tải dữ liệu ra file Excel mở đúng trên Excel tiếng Việt; in hồ sơ một địa điểm; lọc trang Hoạt động theo hành động, ngày và người làm; so sánh hai version của một bản ghi.

**Architecture:** Backend thêm hai package chỉ đọc. `overview` có `GET /overview`: các câu `COUNT` trên index sẵn có. `export` có `GET /exports/*.xlsx`: đọc theo lô bằng keyset, ghi luồng bằng `fastexcel`, mọi lô nằm trong một transaction chỉ đọc nên các sheet khớp nhau. Lọc hoạt động là một record `ChangeFilter` đi vào `EntityHistory.listRecent` ở `platform-common`. Phần còn lại là frontend: feature mới `overview`, `exports` (nút tải), `print` (trang in, ghép dữ liệu từ places, people, ownership, items qua `index.ts`), bộ lọc ở `activity`, hộp so sánh ở `history`. Không có migration.

**Tech Stack:** như `2026-11-16-auto-codes.md`; thêm `org.dhatim:fastexcel` 0.20.2 (main) và `org.dhatim:fastexcel-reader` 0.20.2 (chỉ test) cho registry-service. Frontend không thêm thư viện.

**Spec:** `docs/superpowers/specs/2026-11-17-overview-and-export-design.md`. Số mục trong kế hoạch trỏ vào spec đó.

## Global Constraints

- Như Global Constraints của `2026-11-16-auto-codes.md` (quy ước backend, lệnh chạy IT, npm qua Node của Windows, `./mvnw test` hay `npm test`/`build`/`lint` xanh cuối mỗi task, mỗi task một commit, push `main` sau task cuối).
- Luật kiến trúc (`PlatformArchitectureRules`): chỉ lớp `@Repository` chạy SQL, hàm public của repository nhận `workspaceId` đầu tiên, controller chỉ gọi service.
- "Còn hiệu lực hôm nay" = `start_date <= :today AND (end_date IS NULL OR end_date >= :today)`, bản ghi chưa xoá; `today` lấy từ `BusinessDate.today()`.
- Nhãn trong file Excel trùng nhãn giao diện:
  - trạng thái đồ: `IN_USE` "Đang dùng", `BROKEN` "Hỏng", `STORED` "Cất kho", `REMOVED` "Đã gỡ";
  - trạng thái địa điểm: `ACTIVE` "Đang dùng", `INACTIVE` "Tạm ngưng";
  - quan hệ: `OWNER` "Chủ", `RESIDENT` "Người ở", `GUEST` "Khách", `STAFF` "Nhân viên", `OTHER` "Khác";
  - giới tính: `MALE` "Nam", `FEMALE` "Nữ", `OTHER` "Khác";
  - mục kiểu BOOLEAN: "Có"/"Không". Loại giấy tờ là chữ tự do, ghi nguyên.
- Tên sheet: "Địa điểm", "Người", "Cư trú", "Sở hữu", "Đồ đạc". Tiêu đề cột đúng như bảng ở spec mục 3.2. Ô ngày định dạng `dd/MM/yyyy`. Đường dẫn tên ngăn bằng ` / `; danh sách "Đang ở" ngăn bằng `; `.
- Tên file dự phòng từ server: `home-platform-<workspace|items|people>-<yyyy-MM-dd>.xlsx`. Tên frontend đặt: `<tên không gian bỏ dấu>-<toan-bo|do-dac|nguoi>-<yyyy-MM-dd>.xlsx`. Bỏ dấu bằng NFD, bỏ dấu thanh, `đ`→`d`, chữ thường, chuỗi ký tự khác `a-z0-9` thành một `-`, bỏ `-` ở hai đầu; rỗng thì `khong-gian`.
- Thông báo lỗi backend cố định: `from must be before to`.
- Nhãn frontend: mục thanh bên "Tổng quan" (đứng đầu); nút "Tải Excel" (tổng quan), "Xuất Excel" (trang Đồ đạc, Người); menu địa điểm "In hồ sơ"; trang in "In", "Kể cả bên trong", "In lúc …"; Hoạt động "Hành động", "Chỉ việc tôi làm", "Từ ngày", "Đến ngày", "Bỏ lọc", "Không có hoạt động khớp bộ lọc", "Từ ngày phải trước hoặc bằng Đến ngày"; Lịch sử "So sánh", "Chỉ hiện trường khác", "Hai version này giống nhau".

## Review Focus

1. Không gian có địa điểm đã xoá, cư trú đã xoá, đồ đã xoá: không con số nào trên trang tổng quan hay dòng nào trong file Excel tính tới chúng. Test: `deletedRecordsAreNotCounted` (Task 2), `deletedRecordsAreNotExported` (Task 3, Task 4).
2. Một người ở hai phòng cùng lúc: đếm một lần ở "người đang ở", nhưng mỗi phòng đều tính là có người. Test: `personInTwoRoomsCountsOnceButFillsBoth` (Task 2).
3. Hai mục tuỳ biến khác loại có cùng nhãn ("Diện tích" của Phòng và của Căn hộ): hai cột phân biệt được, giá trị không lẫn cột. Test: `sameLabelOnTwoTypesGetsTwoColumns` (Task 3).
4. "Đến ngày" chọn đúng hôm nay: thay đổi lúc 23:59 hôm nay vẫn hiện. Đổi giờ theo máy người dùng không làm lệch ngày. Test: `toDateIncludesTheWholeDay` (Task 7).
5. Chọn version mới trước rồi version cũ sau: cột trái vẫn là version cũ. Test: `olderVersionIsAlwaysOnTheLeft` (Task 8).

---

### Task 1: Lọc hoạt động ở backend

**Files:** tạo `platform-common/src/main/java/com/home_platform/common/history/ChangeFilter.java`; sửa `.../common/history/EntityHistory.java`, `registry-service/.../history/{ActivityService,ActivityController}.java`; test `platform-common/.../history/EntityHistoryIT.java`, `registry-service/.../history/ActivityIT.java`.

**Interfaces:**
- Produces:

```java
/** Lọc của danh sách hoạt động; mọi trường null là không lọc. {@code to} không tính mốc. */
public record ChangeFilter(String entityType, ChangeAction action, Long changedBy, Instant from, Instant to) {
	public static ChangeFilter none()
}
// EntityHistory: thay chữ ký cũ
public CursorPage<EntityChange> listRecent(long workspaceId, ChangeFilter filter, CursorRequest page)
// ActivityService
public CursorPage<ActivityEntry> list(long workspaceId, ChangeFilter filter, CursorRequest page)
// GET /activity?entityType=&action=&changedBy=&from=&to=   (action: ChangeAction; changedBy: long; from/to: Instant ISO-8601)
```

- `from` ≥ `to` (cả hai có) → 400 `from must be before to`, kiểm ở `ActivityService`. Giá trị `action` lạ, `from` không phải Instant → 400 do binding (kiểm trong IT).
- Điều kiện SQL thêm vào sau `workspace_id`: `entity_type = :entityType`, `action = :action`, `changed_by = :changedBy`, `changed_at >= :from`, `changed_at < :to`. Thứ tự sắp và cursor giữ nguyên.

- [ ] Test `EntityHistoryIT`: sửa `listRecentIsNewestFirstAcrossEntities` sang chữ ký mới (`ChangeFilter.none()`, `new ChangeFilter("ITEM", null, null, null, null)`). Thêm `listRecentFiltersByActionUserAndTime` (ba version ở các giờ khác nhau, hai người: mỗi lọc riêng và gộp chỉ trả đúng dòng; `to` bằng đúng `changed_at` của một dòng thì loại dòng đó, `from` bằng đúng thì giữ).
- [ ] Test `ActivityIT`: `filtersByAction`, `filtersByChangedBy` (`asA` sửa một chỗ, `asB` sửa một chỗ), `filtersByTimeRange`, `filtersCombine`, `filteredListPagesWithoutGaps` (5 dòng khớp xen giữa dòng không khớp, `limit=2`, đi hết ba trang), `fromAfterToIs400`, `unknownActionIs400`, `badInstantIs400`. `filtersByEntityType` có sẵn vẫn xanh.
- [ ] Hỏng → viết → PASS. `./mvnw -q -pl platform-common install -DskipTests` trước khi chạy IT của registry. `./mvnw -pl platform-common,registry-service test` xanh. Commit `Filter activity by action, user and time`.

### Task 2: Trang tổng quan ở backend

**Files:** tạo `registry-service/src/main/java/com/home_platform/registry/overview/{OverviewRepository,OverviewService,OverviewController,OverviewResponse,PlaceTypeCount}.java`; test `.../overview/OverviewIT.java`.

**Interfaces:**
- Consumes: `CodeBackfillService.status(long workspaceId)` → `CodeStatusResponse(int placesWithoutCode, int itemsWithoutCode)`; `BusinessDate.today()`.
- Produces:

```java
public record PlaceTypeCount(long typeId, String name, int total, int occupied)
public record OverviewResponse(int places, List<PlaceTypeCount> placeTypes, int people, int peopleLiving,
		Map<ItemStatus, Integer> itemsByStatus, MissingCodes missingCodes) {
	public record MissingCodes(int places, int items) {}
}
// OverviewRepository
int countPlaces(long workspaceId)
List<PlaceTypeCount> placeTypeCounts(long workspaceId, LocalDate today)   // chỉ loại có ít nhất một chỗ; theo sort_order, tên
int countPeople(long workspaceId)
int countPeopleLiving(long workspaceId, LocalDate today)               // COUNT(DISTINCT person_id)
Map<ItemStatus, Integer> countItemsByStatus(long workspaceId)          // đủ bốn khoá, thiếu thì 0
// GET /overview → OverviewResponse   (typeId ra JSON là chuỗi như mọi long)
```

- `occupied` của một loại = số địa điểm khác nhau (chưa xoá) thuộc loại đó có ít nhất một dòng cư trú còn hiệu lực hôm nay, dòng cư trú và người chưa xoá. `peopleLiving` cùng điều kiện, đếm người. Mọi câu đếm chỉ tính bản ghi `deleted_at IS NULL`.

- [ ] Test `OverviewIT` (không gian âm riêng):
  - `emptyWorkspaceIsAllZero`: bốn khoá trạng thái đồ đều 0, `placeTypes` rỗng.
  - `countsPlacesByTypeWithOccupied`: 3 Phòng, 2 có người, 1 Tầng không người → Phòng `total=3, occupied=2`, Tầng `total=1, occupied=0`.
  - `onlyOccupanciesValidTodayCount`: cư trú kết thúc hôm qua, cư trú bắt đầu ngày mai, cư trú kết thúc đúng hôm nay; chỉ dòng cuối được tính.
  - `personInTwoRoomsCountsOnceButFillsBoth`.
  - `deletedRecordsAreNotCounted`: địa điểm, người, cư trú, đồ đã xoá.
  - `itemsByStatusCountsEachStatus`.
  - `missingCodesMatchCodeStatus`: bằng đúng hai số của `GET /codes/status`.
  - `otherWorkspaceSeesNothing`.
- [ ] Hỏng → viết → PASS; `./mvnw -pl registry-service test` xanh. Commit `Add the workspace overview endpoint`.

### Task 3: Xuất Excel ở backend, phần khung, sheet Địa điểm và Đồ đạc

**Files:** sửa `registry-service/pom.xml` (hai phụ thuộc); tạo `registry-service/src/main/java/com/home_platform/registry/export/{ExportRepository,ExportService,ExportController,PlacePaths,ExportLabels,AttributeColumns,SheetWriter,PlaceSheet,ItemSheet}.java`; test `.../export/{ExportItemsIT,PlacePathsTests,AttributeColumnsTests,XlsxReader}.java` (`XlsxReader` là tiện ích test đọc lại file bằng `fastexcel-reader`).

**Interfaces:**
- Consumes: `FieldDefService.listAll(long workspaceId)`, `ItemStatus`, `PlaceStatus`, `BusinessDate`.
- Produces:

```java
// PlacePaths: dựng từ (id, parentId, name) của mọi địa điểm chưa xoá
static PlacePaths of(List<PlaceNode> nodes)            // record PlaceNode(long id, Long parentId, String name)
String pathOf(long placeId)                            // "Toà A / Tầng 3 / P301"; id lạ → ""
String parentPathOf(long placeId)                      // cột "Nằm trong" của sheet Địa điểm: đường dẫn của cha; gốc → ""
// AttributeColumns: cột mục tuỳ biến của một đích
static AttributeColumns of(List<FieldDef> defs, FieldTarget target, Map<Long, String> typeNames)
List<String> headers()                                 // nhãn; nhãn trùng → "Nhãn (Tên loại)"; mục áp mọi loại (typeId 0) giữ nhãn trần
void write(SheetWriter row, Long typeId, Map<String, Object> attributes)   // ô trống khi mục không áp cho typeId
// SheetWriter: bọc org.dhatim.fastexcel.Worksheet; ghi theo dòng
void text(String), number(Number), date(LocalDate), blank(); void nextRow()
// ExportRepository: keyset theo lô
List<PlaceNode> placeNodes(long workspaceId)
List<ExportPlaceRow> places(long workspaceId, String afterPath, int limit)          // sắp theo path
List<ExportItemRow> items(long workspaceId, ItemStatus status, Long categoryId, ItemCursor after, int limit)
	// sắp theo path của địa điểm, tên, id; status null → removed_at IS NULL như GET /items
// ExportService (@Transactional(readOnly = true))
void writeItems(long workspaceId, ItemStatus status, Long categoryId, OutputStream out)
void writePlacesAndItems(long workspaceId, Workbook workbook)   // dùng lại ở Task 4
// GET /exports/items.xlsx?status=&categoryId=   Content-Type application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
//   Content-Disposition: attachment; filename="home-platform-items-<ngày>.xlsx"; filename*=UTF-8''…
```

- Kích thước lô: 500. Mỗi sheet: dòng đầu là tiêu đề, cố định dòng đầu, bật lọc tự động trên vùng có dữ liệu.
- Cột mục tuỳ biến: mục `ACTIVE`, theo `sort_order` rồi `id`. NUMBER ghi ô số; DATE ghi ô ngày; BOOLEAN ghi "Có"/"Không"; TEXT và SELECT ghi chữ.
- Lọc của `items.xlsx` theo đúng luật của `GET /items` không phạm vi; nhóm không có → 404.

- [ ] Test `PlacePathsTests`: `pathJoinsNamesFromTheRoot`, `rootHasNoParentPath`, `unknownIdIsEmpty`.
- [ ] Test `AttributeColumnsTests`: `columnsFollowSortOrder`, `sameLabelOnTwoTypesGetsTwoColumns`, `fieldForAnotherTypeIsBlank`, `valuesGetTheirCellType` (số, ngày, Có/Không).
- [ ] Test `ExportItemsIT`:
  - `itemsSheetHasHeadersAndRows`: tên sheet "Đồ đạc", tiêu đề đúng thứ tự, "Ở đâu" là đường dẫn đầy đủ, trạng thái "Hỏng", ngày đặt là ô ngày, số lượng là ô số, cột "Chủ" là tên người.
  - `itemsExportFollowsTheListFilter`: `status=BROKEN`; `categoryId`; không lọc thì không có món đã gỡ.
  - `deletedRecordsAreNotExported`.
  - `moreThanOneBatchKeepsEveryRowOnce`: 501 món, đúng 501 dòng, không trùng mã.
  - `responseNamesTheFile`: header `Content-Disposition` có `home-platform-items-`, `.xlsx`.
  - `unknownCategoryIs404`.
  - `otherWorkspaceExportsNothing`.
- [ ] Hỏng → viết → PASS; `./mvnw -pl registry-service test` xanh. Commit `Export items to Excel`.

### Task 4: Xuất Excel ở backend, sheet Người, Cư trú, Sở hữu và file cả không gian

**Files:** tạo `registry-service/.../export/{PersonSheet,OccupancySheet,OwnershipSheet}.java`; sửa `ExportRepository`, `ExportService`, `ExportController`, `ExportLabels`; test `.../export/ExportWorkspaceIT.java`.

**Interfaces:**
- Consumes: Task 3 (`PlacePaths`, `AttributeColumns`, `SheetWriter`, `writePlacesAndItems`).
- Produces:

```java
// ExportRepository
List<ExportPersonRow> people(long workspaceId, PersonCursor after, int limit)                  // theo full_name, id
Map<Long, List<Long>> currentPlacesOf(long workspaceId, Collection<Long> personIds, LocalDate today)  // một truy vấn cho cả lô
List<ExportOccupancyRow> occupancies(long workspaceId, StayCursor after, int limit)            // theo path địa điểm, start_date, id
List<ExportOwnershipRow> ownerships(long workspaceId, StayCursor after, int limit)
// ExportService
void writePeople(long workspaceId, OutputStream out)
void writeWorkspace(long workspaceId, OutputStream out)      // năm sheet theo thứ tự: Địa điểm, Người, Cư trú, Sở hữu, Đồ đạc
// GET /exports/people.xlsx, GET /exports/workspace.xlsx
```

- Cư trú, Sở hữu: mọi dòng chưa xoá (cả đã kết thúc). "Tỷ lệ (%)" là ô số, trống khi không ghi tỷ lệ.
- Cột "Đang ở" của Người: đường dẫn đầy đủ của các nơi còn hiệu lực hôm nay, theo đường dẫn, ngăn bằng `; `.

- [ ] Test `ExportWorkspaceIT`:
  - `workspaceFileHasFiveSheetsInOrder`.
  - `placesSheetShowsParentPathAndAttributes`: "Nằm trong" là đường dẫn của cha, trạng thái "Tạm ngưng", cột mục tuỳ biến của Phòng.
  - `peopleSheetListsWhereEachPersonLivesToday`: hai nơi ngăn `; `; cư trú đã hết hạn không ghi; giới tính "Nữ".
  - `occupancySheetUsesVietnameseRelations`: "Người ở", ngày kết thúc trống khi còn ở.
  - `ownershipSheetHasShareAsNumber`.
  - `deletedRecordsAreNotExported`: người, cư trú, sở hữu đã xoá.
  - `peopleExportIsOneSheet`.
- [ ] Hỏng → viết → PASS; `./mvnw -pl registry-service test` xanh. Commit `Export the whole workspace to Excel`.

### Task 5: Nút tải Excel ở frontend

**Files:** chạy `api:sync` (bật identity và registry như các đợt trước) để có `/overview`, `/exports/*`, tham số mới của `/activity` trong `src/api/schema/registry.ts`; tạo `frontend/src/api/download.ts`, `frontend/src/features/exports/{index.ts,fileName.ts,components/ExportButton.tsx}`; sửa `features/workspaces/{queries.ts,index.ts}`, `features/items/pages/ItemsPage.tsx`, `features/people/pages/PeoplePage.tsx`, `api/index.ts`; test `src/api/download.test.ts`, `src/features/exports/{fileName.test.ts,ExportButton.test.tsx}`, thêm vào `ItemsPage` và `PeoplePage` test có sẵn.

**Interfaces:**
- Produces:

```ts
// api/download.ts: fetch tại lúc gọi (MSW), header workspaceHeaders(ws); lỗi → ApiError.from(status, thân JSON hoặc null); mạng → ApiError.network()
export async function downloadFile(ws: string, path: string, query: Record<string, string | undefined>, fileName: string): Promise<void>
// features/workspaces
export function useWorkspaceName(ws: string): string | undefined   // từ useMyWorkspaces
// features/exports
export type ExportKind = 'workspace' | 'items' | 'people'
export function exportFileName(workspaceName: string | undefined, kind: ExportKind, today: Date): string
export function ExportButton(props: { ws: string; kind: ExportKind; query?: Record<string, string | undefined>; label: string }): JSX.Element
```

- `ExportButton`: khi đang tải thì nút bị khoá và có vòng xoay; lỗi thì `toast.error(error.detail)`. Đường dẫn tương ứng `/exports/workspace.xlsx`, `/exports/items.xlsx`, `/exports/people.xlsx`. Đoạn tên file theo `kind`: `toan-bo`, `do-dac`, `nguoi`.
- Trang Đồ đạc: nút "Xuất Excel" gửi `status` và `categoryId` đang chọn. Trang Người: nút "Xuất Excel".

- [ ] Test `fileName.test.ts`: `"Chung cư Hòa Bình"` → `chung-cu-hoa-binh-do-dac-2026-11-17.xlsx`; `"Đảo  Ngọc!"` → `dao-ngoc-…`; tên rỗng hoặc undefined → `khong-gian-…`.
- [ ] Test `download.test.ts`: `sendsWorkspaceHeadersAndQuery` (bỏ khoá có giá trị undefined); `savesTheBlobUnderTheGivenName` (theo dõi `URL.createObjectURL` và `click` của thẻ `a`, kiểm `download`); `errorBecomesApiError` (404 có `detail`).
- [ ] Test `ExportButton.test.tsx`: `disabledWhileDownloading`, `showsTheErrorDetail`.
- [ ] Test trang: `itemsExportSendsTheCurrentFilter` (`?status=BROKEN` → request có `status=BROKEN`); `peoplePageHasAnExportButton`.
- [ ] Hỏng → viết → PASS; `npm test`, `npm run build`, `npm run lint` sạch. Commit `Add Excel download buttons`.

### Task 6: Trang tổng quan ở frontend

**Files:** tạo `frontend/src/features/overview/{index.ts,api.ts,queries.ts,types.ts,components/OverviewPage.tsx}`; sửa `app/layout/WorkspaceHomePage.tsx`, `app/layout/AppLayout.tsx`, `features/activity/{index.ts,queries.ts,api.ts}`, tạo `features/activity/components/RecentActivity.tsx`; test `features/overview/OverviewPage.test.tsx`, thêm vào test của `WorkspaceHomePage` và `AppLayout` có sẵn.

**Interfaces:**
- Consumes: `ExportButton` (Task 5), `ITEM_STATUS_LABEL` từ items, `useCodeStatus` không dùng (số chưa có mã đã nằm trong `/overview`).
- Produces:

```ts
export type Overview = RegistrySchemas['OverviewResponse']
export function useOverview(ws: string)          // queryKey ['overview', ws], staleTime: 0
// features/activity
export function RecentActivity(props: { ws: string }): JSX.Element   // 5 dòng đầu của /activity (limit=5), dùng ActivityRow, link "Xem tất cả"
// activity api: listActivity nhận thêm limit tuỳ chọn
```

- `WorkspaceHomePage`: chưa có loại địa điểm → `SetupNotice` như cũ; đang tải → skeleton; `overview.places === 0` → trang trống "Chọn một địa điểm trên cây" như cũ; còn lại → `OverviewPage`.
- `OverviewPage` theo bố cục spec 3.1. Bốn ô số (`places`; `peopleLiving` "/ `people` hồ sơ"; `IN_USE`; `BROKEN`). Hai ô đồ và mọi số trạng thái là `Link` tới `/w/:ws/items?status=…`. Bảng "Chỗ ở theo loại" ghi "`occupied` có người · `total − occupied` trống". "Cần xử lý" chỉ hiện khi `missingCodes.places + missingCodes.items > 0`: dòng "N địa điểm chưa có mã", "M đồ chưa có mã" (bỏ dòng có số 0), nút "Cấp mã" tới `/w/:ws/catalog`. Nút "Tải Excel" là `ExportButton kind="workspace"`.
- `AppLayout`: thêm "Tổng quan" (`LayoutDashboardIcon`) đứng đầu `SECTIONS`, trỏ tới `/w/:ws`, `NavLink` có `end` để không sáng ở mọi trang.

- [ ] Test `OverviewPage.test.tsx`: `showsTheHeadlineNumbers`, `statusNumbersLinkToTheFilteredItemsPage` (`href` có `?status=BROKEN`), `placeTypesShowOccupiedAndVacant` ("2 có người · 1 trống"), `todoSectionHiddenWhenNothingIsMissing`, `todoSectionLinksToCatalog`, `recentActivityShowsFiveRows`.
- [ ] Test `WorkspaceHomePage`: `workspaceWithoutPlacesKeepsTheEmptyState`, `workspaceWithPlacesShowsTheOverview`; test setup có sẵn vẫn xanh.
- [ ] Test `AppLayout`: `overviewLinkIsActiveOnlyOnTheHomePage`.
- [ ] Hỏng → viết → PASS; ba lệnh npm sạch. Commit `Show a workspace overview on the home page`.

### Task 7: Bộ lọc trang Hoạt động

**Files:** sửa `frontend/src/features/activity/{api.ts,queries.ts,types.ts,pages/ActivityPage.tsx}`; tạo `features/activity/filters.ts`; test `features/activity/{filters.test.ts,ActivityPage.test.tsx}`.

**Interfaces:**
- Produces:

```ts
export type ActivityFilter = { type: EntityType | null; action: ChangeAction | null; mine: boolean; from: string | null; to: string | null } // ngày yyyy-MM-dd
export function readFilter(params: URLSearchParams): ActivityFilter      // giá trị lạ → null/false
export function writeFilter(filter: ActivityFilter): URLSearchParams     // bỏ khoá rỗng
/** Tham số gửi lên: from = 00:00 giờ máy của ngày `from`; to = 00:00 giờ máy của ngày sau `to`; ISO-8601. */
export function toQuery(filter: ActivityFilter, me: string | null): { entityType?: string; action?: string; changedBy?: string; from?: string; to?: string }
export function useActivity(ws: string, filter: ActivityFilter): CursorList<…>   // queryKey gồm cả filter
```

- `from` > `to` → hiện "Từ ngày phải trước hoặc bằng Đến ngày" dưới hai ô ngày và không gọi API (`enabled: false`).
- Danh sách rỗng khi có lọc → "Không có hoạt động khớp bộ lọc"; không lọc thì giữ câu rỗng hiện có. "Bỏ lọc" xoá mọi tham số trừ `type`.

- [ ] Test `filters.test.ts`: `unknownValuesAreIgnored` (`?action=XYZ&mine=yes&from=17-11-2026`), `writeThenReadRoundTrips`, `toDateIncludesTheWholeDay` (dùng `vi.useFakeTimers` hoặc so với `new Date(2026, 10, 18).toISOString()`), `mineSendsTheCurrentUser`.
- [ ] Test `ActivityPage.test.tsx`: `filtersAreReadFromTheUrl` (request có `action=DELETE`), `changingAFilterUpdatesTheUrl`, `clearFiltersKeepsTheTypeTab`, `fromAfterToShowsAMessageAndSendsNothing`, `emptyFilteredListSaysSo`.
- [ ] Hỏng → viết → PASS; ba lệnh npm sạch. Commit `Filter the activity page by action, date and user`.

### Task 8: So sánh hai version

**Files:** sửa `frontend/src/features/history/{index.ts,types.ts,components/HistoryTab.tsx,components/VersionDialog.tsx}`; tạo `features/history/components/CompareDialog.tsx`, `features/history/compare.ts`; sửa `features/catalog/{index.ts,components/AttributeView.tsx}` (tách ra `attributeRows`), `features/places/components/PlaceHistoryTab.tsx`, `features/people/components/PersonHistoryTab.tsx`, `features/items/components/ItemHistoryTab.tsx`; test `features/history/{compare.test.ts,HistoryTab.test.tsx}`, các test lịch sử có sẵn.

**Interfaces:**
- Produces:

```ts
// history
export type SnapshotRow = { key: string; label: string; value: ReactNode; raw: unknown }
export type CompareLine = { key: string; label: string; older: ReactNode; newer: ReactNode; changed: boolean }
/** Hợp các dòng theo `key`: thứ tự dòng của version mới, rồi khoá chỉ có ở version cũ. changed khi JSON của raw khác nhau. */
export function compareRows(older: SnapshotRow[], newer: SnapshotRow[]): CompareLine[]
// HistoryTab: prop renderSnapshot được thay bằng
snapshotRows: (snapshot: Snapshot) => SnapshotRow[]
// catalog
export function attributeRows(defs: FieldDef[], values: Record<string, unknown> | null): { key: string; label: string; display: string; raw: unknown }[]
	// key "attr:<fieldKey>"; cùng luật hiện của AttributeView (mục ACTIVE luôn có, mục ARCHIVED chỉ khi còn giá trị)
```

- `VersionDialog` hiện `SnapshotFields` từ `snapshotRows`. Các tab Lịch sử của địa điểm, người, đồ cung cấp `snapshotRows` bằng một hook trong file tab (ví dụ `usePlaceSnapshotRows(ws)` trả về hàm); dòng cố định giữ đúng nhãn và cách hiện đang có (địa chỉ của địa điểm, tên loại, tên nhóm, tên chủ…), rồi tới `attributeRows`.
- Mỗi dòng version có checkbox `aria-label="Chọn v{n} để so sánh"`. Chọn dòng thứ ba thì bỏ dòng được chọn sớm nhất. Nút "So sánh" chỉ bật khi đủ hai.
- `CompareDialog`: tải hai version (`useVersion`); tiêu đề hai cột "v{n} · {ngày giờ} · Bạn|Người dùng khác"; version nhỏ bên trái. Công tắc "Chỉ hiện trường khác" bật sẵn. Dòng khác nhau có `data-changed` và chữ đậm. Không dòng nào khác nhau → "Hai version này giống nhau".

- [ ] Test `compare.test.ts`: `sameValuesAreNotChanged`, `differentValuesAreChanged`, `keyOnlyInOlderIsKept`, `objectValuesCompareByContent` (địa chỉ).
- [ ] Test `HistoryTab.test.tsx`: `compareNeedsExactlyTwo`, `thirdChoiceDropsTheFirstChosen`, `olderVersionIsAlwaysOnTheLeft`, `onlyChangedFieldsByDefault`, `showAllFieldsListsUnchangedToo`, `identicalVersionsSaySo`, `attributeChangeIsShownPerField`.
- [ ] Các test lịch sử có sẵn của địa điểm, người, đồ vẫn xanh (hộp xem một version hiện cùng nội dung).
- [ ] Hỏng → viết → PASS; ba lệnh npm sạch. Commit `Compare two versions side by side`.

### Task 9: In hồ sơ địa điểm

**Files:** tạo `frontend/src/features/print/{index.ts,useAllPages.ts,pages/PlacePrintPage.tsx}`; sửa `app/router.tsx` (route mới bên trong `RequireUser`, ngoài `AppLayout`), `features/places/components/PlaceActionsMenu.tsx`, `features/places/index.ts`, `features/people/index.ts` (xuất `useOccupanciesAtPlace`), `features/ownership/index.ts` (xuất `useOwnershipsAtPlace`), `features/items/index.ts` (xuất `useItemsAtPlace`, `useItemsInSubtree`), `features/catalog/index.ts` nếu thiếu; `src/index.css` (khối `@media print`); test `features/print/PlacePrintPage.test.tsx`, thêm vào `PlaceActions.test.tsx`.

**Interfaces:**
- Consumes: `usePlace`, `usePlaceTypes`, `useFieldDefs`, `AttributeView`, `RELATION_LABEL`, `ITEM_STATUS_LABEL`, các hook danh sách vừa xuất.
- Produces:

```ts
/** Gọi loadMore tới khi hết trang; trả về list cùng cờ done. */
export function useAllPages<T>(list: CursorList<T>): { items: T[]; done: boolean; error: ApiError | null }
export function PlacePrintPage(): JSX.Element     // route /w/:workspaceId/places/:placeId/print
```

- Menu "In hồ sơ" là liên kết `target="_blank"` tới `/w/:ws/places/:id/print`.
- Nội dung theo spec 3.3: đường dẫn từ `ancestors`; bảng người ở (`CURRENT`), bảng chủ sở hữu (`CURRENT`), bảng đồ chưa gỡ. `?subtree=1` đổi bảng đồ sang `useItemsInSubtree` và thêm cột "Ở đâu" (`placeName`). Bảng rỗng ghi "Không có". Cuối trang "In lúc dd/MM/yyyy HH:mm" (giờ mở trang).
- Nút "In" khoá khi chưa tải xong mọi danh sách. Bấm gọi `window.print()`. Lớp `print:hidden` (Tailwind) ẩn nút và công tắc khi in. Dòng bảng có `break-inside: avoid`.

- [ ] Test `PlacePrintPage.test.tsx`: `showsDetailsPeopleOwnersAndItems`, `loadsEveryPageBeforePrinting` (hai trang đồ; nút "In" khoá tới khi có trang hai), `subtreeSwitchUsesTheWholeSubtreeAndAddsWhere`, `printButtonCallsWindowPrint`, `emptyTablesSayNone`, `unknownPlaceShowsNotFound`.
- [ ] Test `PlaceActions.test.tsx`: `printLinkOpensInANewTab`.
- [ ] Hỏng → viết → PASS; ba lệnh npm sạch. Commit `Add a printable place record`.

### Task 10: Tài liệu, chạy thử, push

**Files:** sửa `README.md` (API `/overview`, `/exports/*.xlsx`, tham số mới của `/activity`, phụ thuộc `fastexcel`), `frontend/README.md`, `docs/superpowers/specs/2026-11-13-frontend-structure-design.md` (feature `overview`, `exports`, `print`, route in), đánh dấu spec Đợt 4 "đã duyệt, đã xây".

- [ ] Bật identity, registry và frontend. Trên không gian thử -9001 (dùng chung với `CommittedDataIT`):
  - mở trang tổng quan, chụp màn hình;
  - tải ba file Excel bằng `curl` có header, đọc lại bằng `XlsxReader` hoặc `unzip -p … xl/worksheets/sheet1.xml` để kiểm tên sheet và một dòng;
  - mở trang in, chụp màn hình;
  - so sánh hai version của một địa điểm, chụp màn hình;
  - lọc Hoạt động theo "Xoá".

  Bạn đã cho phép dùng không gian của bạn để thử. Chỉ đọc ở đó, không ghi.
- [ ] `./mvnw test` (cả repo) và `npm test`, `npm run build`, `npm run lint` xanh lần cuối. Commit `Document the overview, Excel export, printing and activity filters`; push `main`.
