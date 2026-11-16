# Find and Recover Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Từ bất kỳ trang nào tìm được một địa điểm, người hay món đồ bằng Ctrl+K; xem và lọc đồ đạc của cả không gian; lấy lại thứ đã xoá từ một trang Thùng rác hoặc bằng nút "Hoàn tác"; xem cả một toà nhà dưới dạng bảng.

**Architecture:** Backend chỉ thêm hai cách đọc: `GET /items` cho cả không gian có lọc trạng thái, và `GET /trash` đọc các dòng đang bị xoá mềm (nhờ `KeysetQuery.onlyDeleted()` mới ở `platform-common`), cùng bốn index. Mọi thứ còn lại là frontend dùng API đã có: ô tìm gộp kết quả của các API tìm theo tên và theo mã; hoàn tác gọi lệnh khôi phục với `version` lúc xoá cộng 1; bảng "Cả bên trong" đọc `/places/{id}/subtree`. Trang Thùng rác nằm trong `features/activity`, vì cả hai trang cùng khôi phục thứ đã xoá và phải làm cũ danh sách của nhau.

**Tech Stack:** như `2026-11-16-auto-codes.md`; `cmdk` (đã có trong `package.json`, qua `components/ui/command.tsx`). Không thêm thư viện.

**Spec:** `docs/superpowers/specs/2026-11-16-find-and-recover-design.md`. Các số mục trong kế hoạch này trỏ vào spec đó.

**Phụ thuộc:** độc lập với hai đợt trước về nghiệp vụ. Viết theo giả định chúng đã chạy: migration là V6, `GET /items` đã có `ownerId`, kết quả tìm có mã do hệ thống cấp. Nếu chưa: dùng số V kế tiếp chưa dùng và bỏ `ownerId` khỏi thông báo ở Task 1.

## Global Constraints

- Như Global Constraints của `2026-11-16-auto-codes.md` (quy ước backend, lệnh chạy IT, npm qua Node của Windows, mỗi task một commit).
- Không API tìm mới. Ô tìm chỉ gọi `/places/search`, `/people/search`, `/items/search`, mỗi request `limit=5`.
- Ô tìm chờ 300 ms sau phím cuối rồi mới gọi; chuỗi rỗng sau khi cắt khoảng trắng thì không gọi.
- Thùng rác không có xoá hẳn. Khôi phục dùng đúng ba lệnh `restore` đã có; thông báo từ chối của backend hiện nguyên văn.
- Hoàn tác: nút hiện 8 giây; gửi `version` = `version` lúc xoá + 1; 409 thì báo "Không hoàn tác được: bản ghi đã thay đổi" và không thử lại.
- Thông báo lỗi backend cố định: `Give at most one of placeId, subtreeOf, categoryId or ownerId`; `status can only be combined with categoryId or no scope`.
- Nhãn: mục thanh bên "Đồ đạc", "Thùng rác"; lọc trạng thái "Chưa gỡ" (mặc định), "Đang dùng", "Hỏng", "Cất kho", "Đã gỡ"; nhóm ô tìm "Khớp mã", "Địa điểm", "Người", "Đồ đạc", "Đi tới".
- `places`, `people`, `items` không nhập `search` hay `activity`.

## Review Focus

1. Gõ tiếng Việt bằng bộ gõ (mỗi dấu viết lại chữ vừa gõ) làm ô tìm đổi giá trị dồn dập: chỉ một lượt request sau khi ngừng gõ. Test: `typingFastSendsOneRoundOfRequests` (Task 7).
2. Câu trả lời chậm của chuỗi cũ về sau câu trả lời của chuỗi mới: kết quả đang hiện vẫn là của chuỗi mới. Test: `slowAnswerForAnOldTermIsNotShown` (Task 7).
3. Bấm "Hoàn tác" sau khi người khác đã khôi phục hoặc sửa bản ghi, hoặc bấm hai lần: không khôi phục nhầm, báo rõ, chỉ một request. Test: `undoAfterSomeoneElseChangedItSaysSo`, `undoRunsOnlyOnce` (Task 5).
4. Khôi phục từ thùng rác một địa điểm có cha còn bị xoá: hiện lý do của backend, dòng vẫn ở lại. Test: `blockedRestoreShowsTheReasonAndKeepsTheRow` (Task 4).
5. Đường dẫn trang Đồ đạc mang giá trị lọc lạ (`?status=XYZ`, do sửa tay hay bản cũ): bỏ qua, không gửi lên để nhận 400. Test: `unknownStatusInTheUrlIsIgnored` (Task 3).

---

### Task 1: Backend, đồ đạc của cả không gian và lọc theo trạng thái

**Files:** tạo `registry-service/src/main/resources/db/migration/V6__list_and_trash_indexes.sql`; sửa `registry-service/src/main/java/com/home_platform/registry/item/{ItemRepository,ItemService,ItemController}.java`; test `item/ItemListIT.java`, `schema/RegistrySchemaIT.java`.

**Interfaces:**
- Produces:

```java
// ItemRepository: thay byCategory bằng
CursorPage<Item> all(long workspaceId, ItemStatus status, Long categoryId, CursorRequest page)
	// status null: removed_at IS NULL; khác null: status = :status
	// categoryId khác null: thêm category_id = :categoryId.  Luôn theo tên.
// ItemService: thay byCategory bằng
public CursorPage<ItemView> all(long workspaceId, ItemStatus status, Long categoryId, CursorRequest page)   // 404 nếu nhóm không có
// GET /items: thêm tham số status (ItemStatus, tuỳ chọn)
```

- Migration: bốn index đúng như mục 5 của spec (hai index `deleted_at` còn lại dùng ở Task 2; gộp một file để chỉ một lần đổi bảng).
- `ItemController.list`: hơn một phạm vi trong `placeId`, `subtreeOf`, `categoryId`, `ownerId` thì 400; `status` đi cùng `placeId`, `subtreeOf` hoặc `ownerId` thì 400; không phạm vi hoặc chỉ `categoryId` thì gọi `service.all(ws, status, categoryId, page)`. Hành vi của `placeId` (+ `removed`), `subtreeOf`, `ownerId` không đổi.

- [ ] Test `ItemListIT`: `wholeWorkspaceListsCurrentItemsByName` (hai phòng, bốn món, một đã gỡ, một đã xoá: trả hai món còn lại theo tên, có `placeName`), `statusFilterNarrowsTheWholeWorkspace` (`status=BROKEN`), `removedStatusListsRemovedItems`, `statusCombinesWithCategory`, `categoryAloneStillListsCurrentItems` (test `byCategoryListsCurrentItems` có sẵn vẫn xanh), `statusWithAPlaceIs400`, `twoScopesAre400` (thay `exactlyOneFilter`), `unknownStatusIs400`, `wholeWorkspacePagesWithoutSkippingOrRepeating` (5 món, `limit=2`, đi hết ba trang), `otherWorkspaceSeesNothing`, `wholeWorkspaceListsUseAnIndex` (`EXPLAIN` của ba dạng: không lọc, có `status`, có `categoryId`; không dạng nào có `type = ALL`).
- [ ] Test `RegistrySchemaIT`: thêm vào `indexesMatchDesign`: `reg_item.idx_reg_item_status` = `workspace_id, status, name`; `reg_item.idx_reg_item_deleted`, `reg_place.idx_reg_place_deleted`, `reg_person.idx_reg_person_deleted` = `workspace_id, deleted_at`.
- [ ] Hỏng → viết → PASS; `./mvnw -pl registry-service test` xanh. Commit `List items across the workspace with a status filter`.

### Task 2: Backend, thùng rác

**Files:** sửa `platform-common/src/main/java/com/home_platform/common/jdbc/KeysetQuery.java`, `registry-service/.../place/{PlaceRepository,PlaceService}.java`, `.../person/{PersonRepository,PersonService}.java`, `.../item/{ItemRepository,ItemService}.java`; tạo `registry-service/src/main/java/com/home_platform/registry/trash/{TrashType,TrashEntry,TrashResponse,TrashMapper,TrashService,TrashController}.java`; test `platform-common/.../jdbc/{KeysetQueryTests,KeysetQueryIT}.java`, `registry-service/.../trash/TrashIT.java`.

**Interfaces:**
- Produces:

```java
// KeysetQuery
/** Chỉ các dòng đã xoá mềm, thay cho điều kiện mặc định "chưa xoá". */
public KeysetQuery<T> onlyDeleted()        // IllegalStateException nếu bảng không có deleted_at
// PlaceRepository, PersonRepository, ItemRepository
CursorPage<X> deleted(long workspaceId, CursorRequest page)     // keyset().onlyDeleted().orderByDesc("deleted_at")
// PlaceService, PersonService, ItemService
public CursorPage<X> deleted(long workspaceId, CursorRequest page)
// trash
public enum TrashType { PLACE, PERSON, ITEM }
public record TrashEntry(TrashType entityType, long id, String name, String code, String context, Instant deletedAt,
		long deletedBy, int version)
public record TrashResponse(TrashType entityType, long id, String name, String code, String context, Instant deletedAt,
		long deletedBy, int version)
// TrashService
public CursorPage<TrashEntry> list(long workspaceId, TrashType type, CursorRequest page)
// GET /trash?entityType=   -> CursorPage<TrashResponse>
```

- `deletedBy` là `updatedBy` của dòng (lần ghi cuối của một dòng đang bị xoá là lần xoá); `version` là `version` hiện tại của dòng.
- `context`: địa điểm thì tên của cha (`null` ở cấp cao nhất), đồ thì tên chỗ đặt, người thì `null`. Tên lấy theo lô bằng `PlaceService.names` (gồm cả địa điểm đã xoá): một truy vấn cho mỗi trang.
- `code`: của địa điểm và đồ; người thì `null`. `name` của người là `fullName`.
- `entityType` thiếu hoặc lạ: 400 (tham số kiểu `TrashType`, bắt buộc).

- [ ] Test `KeysetQueryTests`: `onlyDeletedSwapsTheDeletedFilter` (câu SQL có `deleted_at IS NOT NULL`, không có `deleted_at IS NULL`), `onlyDeletedNeedsADeletedAtColumn`. `KeysetQueryIT`: `onlyDeletedPagesThroughDeletedRowsNewestFirst`.
- [ ] Test `TrashIT extends RegistryIT`:
  - `listsDeletedPlacesNewestFirst`: xoá A rồi B: thứ tự B, A; mỗi dòng có `name`, `code`, `deletedAt`, `deletedBy` = `USER`, `version` 2.
  - `deletedChildShowsItsParent`, `topLevelPlaceHasNoContext`, `deletedItemShowsItsPlace`, `deletedPersonHasNeitherCodeNorContext`.
  - `liveRecordsAreNotInTheTrash`, `restoredRecordLeavesTheTrash`.
  - `versionFromTheTrashRestoresTheRecord`: lấy `version` của dòng, `POST /places/{id}/restore` với `version` đó trả 200.
  - `typesAreListedSeparately` (xoá một địa điểm và một người: `entityType=PLACE` chỉ có địa điểm).
  - `entityTypeIsRequiredAndChecked` (thiếu: 400; `OCCUPANCY`: 400), `trashPages` (`limit=1` có `nextCursor`, trang hai là dòng còn lại), `otherWorkspaceSeesAnEmptyTrash`, `trashListsUseAnIndex` (`EXPLAIN` không có `type = ALL`).
- [ ] Hỏng → viết → PASS; `./mvnw test` xanh; chạy `-pl registry-service -am … -Dit.test='TrashIT,KeysetQueryIT' -Dfailsafe.failIfNoSpecifiedTests=false`. Cập nhật `README.md` gốc: dòng `/items` (cả không gian, `status`), thêm dòng `GET /trash`. Commit `List soft-deleted records as a trash`.

### Task 3: Frontend, trang "Đồ đạc" của cả không gian

**Files:** `frontend/src/api/schema/*` (sinh lại); tạo `frontend/src/features/items/pages/ItemsPage.tsx`; sửa `frontend/src/features/items/{api.ts,queries.ts,index.ts}`, `frontend/src/app/{router.tsx,layout/AppLayout.tsx}`, `frontend/src/test/itemBackend.ts`; test `features/items/ItemsPage.test.tsx`.

**Interfaces:**
- Consumes: Task 1 (backend đang chạy bản mới để `npm run api:sync`).
- Produces:

```ts
// items/api.ts
export type ItemFilter = { status: ItemStatus | null; categoryId: string | null }
listAllItems(ws: string, filter: ItemFilter, cursor?: string): Promise<CursorPage<ItemSummary>>
// items/queries.ts
itemKeys.everywhere(ws, filter)        // ['items', ws, 'list', 'all', status ?? 'current', categoryId ?? 'any']
useAllItems(ws, filter): CursorList<ItemSummary>
// items/pages
export function ItemsPage(): JSX.Element      // route '/w/:workspaceId/items', đặt trước 'items/:itemId'
```

  - Thanh bên: `SECTIONS` thành Người, Đồ đạc (`PackageIcon`), Danh mục, Hoạt động.
  - Trang: tiêu đề "Đồ đạc", mô tả "Mọi món đồ của không gian này. Thêm đồ từ tab Đồ đạc của một địa điểm."; hai `NativeSelect` có nhãn "Trạng thái" và "Nhóm" (nhóm đã lưu trữ có đuôi "(đã lưu trữ)"); `ItemList` với `showPlace`, và `removed` khi đang lọc "Đã gỡ".
  - Bộ lọc nằm trên URL: `?status=` (một trong bốn giá trị; "Chưa gỡ" là không có tham số), `?category=<id>`; giá trị lạ hoặc `category` không qua `isId` thì coi như không có.
  - Trống: có lọc thì "Không có món nào khớp bộ lọc."; không lọc thì "Chưa có món đồ nào."
  - `itemBackend`: `GET /items` không phạm vi, có `status`, có `categoryId`.

- [ ] Test `ItemsPage.test.tsx`: `listsEveryCurrentItemWithItsPlace`, `filteringByStatusUpdatesTheUrlAndTheRequest` (chọn "Hỏng": URL có `status=BROKEN`, request có `status=BROKEN`), `filteringByCategory`, `removedFilterShowsRemovalDates`, `openingAFilteredUrlAppliesTheFilter`, `unknownStatusInTheUrlIsIgnored` (request không có `status`, ô chọn hiện "Chưa gỡ"), `emptyResultSaysWhy`, `sidebarLinksToTheItemsPage`, `itemPageStillOpensUnderTheSamePrefix` (`/w/1/items/9001` vẫn là trang món đồ).
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the workspace items page`.

### Task 4: Frontend, trang "Thùng rác"

**Files:** tạo `frontend/src/features/activity/{pages/TrashPage.tsx,components/RestoreButton.tsx}`; sửa `frontend/src/features/activity/{types.ts,api.ts,queries.ts,index.ts,components/ActivityRow.tsx,pages/ActivityPage.tsx}`, `frontend/src/app/{router.tsx,layout/AppLayout.tsx}`; tạo `frontend/src/test/trashBackend.ts`; test `features/activity/TrashPage.test.tsx`, `features/activity/ActivityPage.test.tsx`.

**Interfaces:**
- Consumes: Task 2.
- Produces:

```ts
// activity/types.ts
export type TrashEntry = Omit<Present<S['TrashResponse'], 'entityType' | 'id' | 'name' | 'deletedAt' | 'deletedBy' | 'version'>, 'entityType'>
  & { entityType: EntityType }
// activity/api.ts
listTrash(ws: string, entityType: EntityType, cursor?: string): Promise<CursorPage<TrashEntry>>
// activity/queries.ts
export const trashKeys = { all: (ws) => ['trash', ws], list: (ws, entityType) => ['trash', ws, entityType] }
useTrash(ws, entityType): CursorList<TrashEntry>
// activity/components/RestoreButton.tsx  (tách từ ActivityRow)
export function RestoreButton(props: { ws: string; entityType: EntityType; entityId: string; version: number; name: string }): JSX.Element
// activity/pages
export function TrashPage(): JSX.Element      // route '/w/:workspaceId/trash'
```

  - `RestoreButton`: hành vi như đoạn đang nằm trong `ActivityRow`; khôi phục xong, hoặc gặp 409 `version` lệch, làm cũ cả `activityKeys.all(ws)` lẫn `trashKeys.all(ws)`. `ActivityRow` dùng lại nó; các test có sẵn của trang Hoạt động phải xanh.
  - Thanh bên: thêm "Thùng rác" (`Trash2Icon`) ở cuối `SECTIONS`.
  - `TrashPage`: tiêu đề "Thùng rác", mô tả "Những thứ đang bị xoá. Khôi phục để đưa chúng trở lại đúng chỗ cũ."; ba tab "Địa điểm", "Người", "Đồ đạc" trên URL (`?type=`, mặc định `PLACE`); mỗi dòng: tên, mã (nếu có), ngữ cảnh ("trong <tên cha>" với địa điểm, "ở <tên chỗ đặt>" với đồ), "xoá lúc <ngày giờ>", "Bạn" hoặc "Người dùng khác", `RestoreButton`. Trống: "Không có gì đang bị xoá."
  - Trang Hoạt động: dưới mô tả thêm liên kết "Chỉ cần lấy lại thứ đã xoá? Mở Thùng rác."

- [ ] Test `TrashPage.test.tsx`: `listsDeletedPlacesWithTheirParent`, `tabsSwitchTheKindAndTheUrl`, `restoringRemovesTheRowAndSaysSo` (thân `{version}` bằng `version` của dòng; thông báo "Đã khôi phục <tên>"), `blockedRestoreShowsTheReasonAndKeepsTheRow` (409 `Restore the parent place 12 first`), `staleRowIsRefreshedAfterAVersionConflict`, `emptyTrashSaysSo`, `sidebarLinksToTheTrash`.
- [ ] Test `ActivityPage.test.tsx`: `linksToTheTrash`; `restoringFromActivityAlsoRefreshesTheTrash`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the trash page`.

### Task 5: Frontend, "Hoàn tác" sau khi xoá

**Files:** tạo `frontend/src/components/common/deletedToast.ts`; sửa `frontend/src/features/places/{queries.ts,components/DeletePlaceDialog.tsx}`, `frontend/src/features/people/{queries.ts,components/DeletePersonDialog.tsx}`, `frontend/src/features/items/{queries.ts,components/DeleteItemDialog.tsx}`; test `components/common/deletedToast.test.tsx`, `features/places/PlaceActions.test.tsx`, `features/people/PersonPage.test.tsx`, `features/items/ItemLifecycle.test.tsx`.

**Interfaces:**
- Produces:

```ts
// components/common/deletedToast.ts
/** Thông báo "Đã xoá <name>" có nút "Hoàn tác" trong 8 giây. `undo` khôi phục bản ghi và ném ApiError khi bị từ chối. */
export function deletedToast(options: { name: string; undo: () => Promise<void> }): void
// places/queries.ts
export function undoDeletePlace(queryClient: QueryClient, ws: string, id: string, deletedVersion: number): Promise<PlaceDetail>
// people/queries.ts
export function undoDeletePerson(queryClient: QueryClient, ws: string, id: string, deletedVersion: number): Promise<PersonDetail>
// items/queries.ts
export function undoDeleteItem(queryClient: QueryClient, ws: string, id: string, deletedVersion: number): Promise<ItemDetail>
```

  - Ba hàm `undoDelete…` gọi hàm `restore…` của `api.ts` với `deletedVersion + 1`, rồi làm mới cache như hook khôi phục của feature đó (`refreshAfterWrite(…, 'self')`, `refreshAfterPersonWrite`, `refreshItems`). Là hàm thường, không phải hook: hộp thoại xoá đã đóng và trang đã rời đi khi người dùng bấm "Hoàn tác".
  - `deletedToast`: `toast.add` với `timeout: 8000` và `actionProps` (nhãn "Hoàn tác"). Bấm: đóng thông báo đó ngay (nên không bấm được lần hai), chạy `undo`; xong thì thông báo "Đã khôi phục <name>"; `ApiError.isVersionConflict` hoặc 409 khác thì thông báo lỗi "Không hoàn tác được: bản ghi đã thay đổi" kèm `detail`; lỗi khác hiện `detail`.
  - Ba hộp thoại xoá thay `toast.add({ title: 'Đã xoá …' })` bằng `deletedToast`; `undo` của chúng gọi `undoDelete…` rồi `navigate` về trang của bản ghi (`placeUrl`, `/w/:ws/people/:id`, `/w/:ws/items/:id`).

- [ ] Test `deletedToast.test.tsx`: `showsUndoNextToTheMessage`, `undoRunsOnlyOnce`, `successSaysRestored`, `conflictSaysTheRecordChanged`.
- [ ] Test `PlaceActions.test.tsx`: `undoRestoresTheDeletedPlaceAndReturnsToIt` (thân `POST /places/301/restore` là `{version: 4}` khi xoá ở `version` 3; URL trở lại trang địa điểm), `undoAfterSomeoneElseChangedItSaysSo`. `PersonPage.test.tsx`: `undoRestoresTheDeletedPerson`. `ItemLifecycle.test.tsx`: `undoRestoresTheDeletedItem`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Offer undo after deleting a place, person or item`.

### Task 6: Frontend, bảng "Cả bên trong" ở tab Bên trong

**Files:** sửa `frontend/src/features/places/{api.ts,queries.ts,components/PlaceChildrenTab.tsx}`, `frontend/src/test/placeBackend.ts`; tạo `frontend/src/features/places/components/PlaceSubtreeTable.tsx`; test `features/places/PlaceSubtree.test.tsx`.

**Interfaces:**
- Produces:

```ts
// places/api.ts
listSubtree(ws: string, placeId: string, cursor?: string): Promise<CursorPage<PlaceSummary>>   // GET /places/{id}/subtree, limit 50
// places/queries.ts
placeKeys.subtrees(ws)                 // ['places', ws, 'subtree']
placeKeys.subtree(ws, placeId)         // ['places', ws, 'subtree', placeId]
useSubtree(ws, placeId, enabled): CursorList<PlaceSummary>
// components
export function PlaceSubtreeTable(props: { ws: string; place: PlaceDetail }): JSX.Element
```

  - `refreshAfterWrite` (phạm vi `'self'`, `'descendants'`) và `useDeletePlace` làm cũ thêm `placeKeys.subtrees(ws)`; phạm vi `'tree'` đã làm cũ cả `placeKeys.all(ws)`.
  - `PlaceChildrenTab`: khi địa điểm có chỗ con, hiện công tắc "Cả bên trong" (cùng kiểu với tab Người ở); bật thì thay danh sách con bằng `PlaceSubtreeTable`.
  - Bảng: cột Tên (thụt `depth - place.depth - 1` bậc, mỗi bậc 1 rem; tên là liên kết tới trang của nó), Loại (tên từ `usePlaceTypes`), Mã, Trạng thái ("Đang dùng" / "Tạm ngưng"). Thứ tự giữ nguyên như backend trả (theo `path`). "Tải thêm" khi còn.
  - `placeBackend`: `GET /places/:id/subtree` trả mọi hậu duệ chưa xoá, cha trước con.

- [ ] Test `PlaceSubtree.test.tsx`: `switchIsHiddenForAPlaceWithoutChildren`, `listsEveryDescendantParentsBeforeChildren` (Toà A có Tầng 1 có P101, P102: thứ tự Tầng 1, P101, P102), `rowsAreIndentedByDepth`, `rowsShowTypeCodeAndStatus`, `subtreeLoadsOnlyWhenSwitchedOn` (không có request `subtree` trước khi bật), `loadsMoreDescendants`, `addingAChildRefreshesTheTable`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Show a whole subtree as a table`.

### Task 7: Frontend, ô tìm chung Ctrl+K

**Files:** tạo `frontend/src/features/search/{index.ts,groupResults.ts,components/SearchPalette.tsx,components/useSearchShortcut.ts}`; sửa `frontend/src/features/places/{api.ts,queries.ts,index.ts}`, `frontend/src/features/people/{api.ts,queries.ts,index.ts}`, `frontend/src/features/items/{api.ts,queries.ts,index.ts}`, `frontend/src/app/layout/AppLayout.tsx`; test `features/search/groupResults.test.ts`, `features/search/SearchPalette.test.tsx`.

**Interfaces:**
- Produces:

```ts
// places (export qua index.ts)
usePlaceQuickFind(ws: string, term: string): UseQueryResult<{ byCode: PlaceSearchResult[]; byName: PlaceSearchResult[] }, ApiError>
// people
usePersonQuickFind(ws: string, term: string): UseQueryResult<PersonSummary[], ApiError>
// items
useItemQuickFind(ws: string, term: string): UseQueryResult<{ byCode: ItemSummary[]; byName: ItemSummary[] }, ApiError>
// search/groupResults.ts
export type SearchRow = {
  key: string                       // 'PLACE:301'
  kind: 'PLACE' | 'PERSON' | 'ITEM'
  title: string
  detail: string | null             // địa điểm: 'Toà A › Tầng 3'; người: số điện thoại; đồ: 'ở <chỗ đặt>'
  code: string | null
  removed: boolean
  href: string
}
export function groupResults(ws: string, found: {
  places?: { byCode: PlaceSearchResult[]; byName: PlaceSearchResult[] }
  people?: PersonSummary[]
  items?: { byCode: ItemSummary[]; byName: ItemSummary[] }
}): { codeMatches: SearchRow[]; places: SearchRow[]; people: SearchRow[]; items: SearchRow[] }
// search/components
export function SearchPalette(props: { ws: string; open: boolean; onOpenChange: (open: boolean) => void }): JSX.Element
export function useSearchShortcut(onOpen: () => void): void       // Ctrl+K và ⌘K; preventDefault
```

  - Ba hook `…QuickFind`: `enabled` khi `term !== ''`; mỗi hook gọi song song các request của loại nó với `limit=5` (địa điểm: `name`, `code`; đồ: `name`, `code`; người: `name`, và `phone` khi `term` khớp `/^\d{3,}$/`, gộp không lặp theo `id`). Khoá cache nằm dưới khoá danh sách của feature (`placeKeys.searches(ws)`, `personKeys.lists(ws)`, `itemKeys.lists(ws)`) cộng `'quick'` và `term`, nên lệnh ghi nào đang làm cũ danh sách cũng làm cũ kết quả tìm.
  - `groupResults`: `codeMatches` là địa điểm rồi đồ khớp mã; một bản ghi có trong `byCode` thì bị bỏ khỏi nhóm theo tên của loại nó.
  - `SearchPalette`: `CommandDialog` với `shouldFilter={false}` (kết quả đã do backend lọc); ô nhập có nhãn "Tìm địa điểm, người, đồ đạc"; giá trị đã cắt khoảng trắng đi qua `useDebouncedValue(…, 300)`. Chuỗi rỗng: nhóm "Đi tới" với Người, Đồ đạc, Danh mục, Hoạt động, Thùng rác. Có chuỗi: các nhóm theo thứ tự ở Global Constraints, nhóm rỗng không hiện; mọi nhóm rỗng và không còn request nào đang chạy thì "Không tìm thấy gì."; hook nào lỗi thì nhóm của nó hiện "Không tìm được <tên nhóm>". Dòng đồ đã gỡ có nhãn "Đã gỡ". Chọn một dòng: `navigate(href)` rồi đóng và xoá chuỗi.
  - `AppLayout`: nút "Tìm…" kèm `Kbd` "Ctrl K" trên thanh đầu trang, cạnh `SidebarTrigger`; `useSearchShortcut` mở ô tìm; `SearchPalette` nhận `key={current.id}` để đổi không gian thì bắt đầu lại.

- [ ] Test `groupResults.test.ts`: `codeMatchesComeFirstAndAreNotRepeated`, `placesCarryTheirAncestorTrail`, `removedItemsAreFlagged`, `missingKindsGiveEmptyGroups`.
- [ ] Test `SearchPalette.test.tsx` (qua `renderApp`):
  - `ctrlKOpensThePaletteFromAnyPage`, `searchButtonOpensIt`, `emptyBoxOffersShortcuts`.
  - `typingFastSendsOneRoundOfRequests`: gõ `phong` liền một mạch: mỗi API tìm nhận đúng một request, với `name=phong` hoặc `code=phong`.
  - `slowAnswerForAnOldTermIsNotShown`: câu trả lời của `ph` bị giữ chậm, của `phong` về trước: danh sách chỉ có kết quả của `phong`, kể cả sau khi câu chậm về.
  - `resultsAreGroupedByKind`, `aCodeMatchIsShownOnceUnderCodeMatches`, `twoPlacesWithTheSameNameShowDifferentTrails`.
  - `digitsAlsoSearchPhones` (`0912` sinh request `/people/search?phone=0912`), `lettersDoNotSearchPhones`.
  - `oneFailingSearchKeepsTheOthers` (`/items/search` trả 500: nhóm Địa điểm và Người vẫn hiện, có dòng "Không tìm được Đồ đạc").
  - `enterOpensTheRecordAndClosesThePalette`, `nothingFoundSaysSo`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the Ctrl+K search palette`.

### Task 8: Hoàn tất

- [ ] `docs/superpowers/specs/2026-11-13-business-rules-design.md`: mục 5.9 "Đọc" thêm "đồ của cả không gian, lọc theo trạng thái"; mục 5.10 thêm một dòng về thùng rác; mục 9 gạch câu hỏi "thùng rác" (đã trả lời). `docs/superpowers/specs/2026-11-13-frontend-structure-design.md` mục 6: thêm ba route `items`, `trash` và ô tìm. Đổi trạng thái spec của đợt này thành "đã xây".
- [ ] `frontend/README.md`: feature `search` trong cây thư mục; mô tả `activity` thêm Thùng rác; luật phụ thuộc (`search` và `activity` đứng trên ba feature bản ghi); một đoạn "Tìm chung" (Ctrl+K, chờ 300 ms, gộp năm request) và "Hoàn tác" (`deletedToast`, `version + 1`).
- [ ] Chạy thử trên backend thật: Ctrl+K tìm một phòng theo mã và một người theo số điện thoại; trang Đồ đạc lọc "Hỏng"; xoá một món rồi "Hoàn tác"; xoá một địa điểm, mở Thùng rác, khôi phục; bật "Cả bên trong" ở một toà nhà. Chụp màn hình ô tìm và trang Thùng rác. Xoá dữ liệu vừa tạo theo đúng `id`.
- [ ] `./mvnw verify` (chạy nền) xanh; `npm test`, `npm run build`, `npm run lint` sạch. Commit `Document search, the items page, trash and undo` và push `main`.

## Để sau, có lý do

- **Hoàn tác và thùng rác cho dòng cư trú (và sở hữu)**: backend chưa có lệnh khôi phục cho hai bảng đó; thêm khi người dùng xoá nhầm thật.
- **Một danh sách thùng rác trộn cả ba loại**: phân trang trên ba bảng cùng lúc phức tạp hơn nhiều so với ba tab; chờ xem ba tab có đủ không.
- **Tìm ở giữa tên, không dấu, trong mô tả**: cần Elasticsearch, là quyết định hạ tầng của người dùng.
- **Ô tìm nhớ các lần tìm gần đây**: tiện nhưng chưa cần; làm cùng "địa điểm xem gần đây" (F4).
- **Số liệu trên trang Đồ đạc** (bao nhiêu món hỏng): thuộc trang tổng quan (E1), cần API đếm riêng.
- **Thùng rác và trang Hoạt động làm cũ nhau sau "Hoàn tác"**: hàm hoàn tác nằm ở ba feature bản ghi, không biết tới `activity`. Danh sách cũ tối đa 30 giây (`staleTime`); bấm khôi phục trên một dòng cũ thì backend trả 409 và dòng được tải lại.
