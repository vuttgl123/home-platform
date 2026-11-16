# Item Ownership Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ghi được một món đồ là của riêng ai (trống là của địa điểm), đổi chủ bằng một thao tác có version, xem được mọi món của một người, và không cho xoá một người còn là chủ của món đang dùng.

**Architecture:** Một cột `owner_person_id` trên `reg_item`, là trường do hệ thống quản lý như `place_id`: đặt khi tạo, đổi bằng `POST /items/{id}/owner`, không nằm trong lệnh sửa và không đổi khi khôi phục version. `ItemService` giữ riêng dòng người khi gán chủ, cùng cách cư trú đang làm, nên lệnh xoá người chạy cùng lúc không lọt. Ở frontend, `items` nhập `PersonPicker` của `people`; chiều ngược lại (tab "Đồ đạc" trên trang người) được `app/router.tsx` ghép qua `PersonTabSlots`, như `PlaceTabSlots`.

**Tech Stack:** như `2026-11-16-auto-codes.md`. Không thêm thư viện.

**Spec:** `docs/superpowers/specs/2026-11-16-ownership-design.md` (mục 4.2, 5.4, 5.5, 6, 7, 8). Các số mục trong kế hoạch này trỏ vào spec đó.

**Phụ thuộc:** chạy sau `2026-11-16-auto-codes.md` (số migration, và `ItemRequest` đã bỏ `code`). Nếu chạy trước, dùng số V kế tiếp chưa dùng và giữ thành phần `code` ở chỗ nó đang đứng.

## Global Constraints

- Như Global Constraints của `2026-11-16-auto-codes.md` (quy ước backend, lệnh chạy IT, npm qua Node của Windows, mỗi task một commit).
- `ownerPersonId` chỉ có trong `ItemRequest` (tạo, món mới khi thay) và `ItemOwnerRequest`. `ItemUpdateRequest` không có; client gửi thì bị bỏ qua.
- Thứ tự giữ dòng khi ghi: địa điểm trước (`PlaceService.getForShare`), người sau (`PersonService.getForUpdate`), giống `OccupancyService.create`. Không đảo.
- Thông báo lỗi cố định: `Restore person <id> first`; `Person <id> still has <n> current or upcoming occupancies and <k> items`; `Give exactly one of placeId, subtreeOf, categoryId or ownerId`.
- Nhãn giao diện: trường là "Chủ"; ô chọn là "Của ai" với hai lựa chọn "Của địa điểm này", "Của một người"; thao tác là "Đổi chủ".
- `people` không nhập `items`. Tab của trang người thuộc nghiệp vụ khác đi qua `PersonTabSlots`.

## Review Focus

1. Mọi món có sẵn đều có snapshot cũ không có `ownerPersonId`. Lần sửa đầu tiên sau đợt này không được hiện "Chủ" trong danh sách trường đã đổi. Test: `aFieldAddedAsNullIsNotAChange` (Task 1), `firstEditAfterTheColumnWasAddedDoesNotReportTheOwner` (Task 2).
2. Xoá một người đúng lúc một món đang được gán cho họ: hoặc lệnh xoá thấy món đó và bị từ chối, hoặc lệnh gán thấy người đã xoá và bị từ chối. Test: `personDeleteSeesAnItemAssignedConcurrently`, `itemAssignedDuringAPersonDeleteIsRefused` (Task 2).
3. `ownerPersonId` là người của không gian khác: 404 như người không tồn tại, không phải lỗi 500 từ khoá ngoại. Test: `ownerMustBeALivePersonOfThisWorkspace` (Task 2).
4. Món đã gỡ có chủ, chủ bị xoá, rồi món được đưa lại: bị chặn với lý do rõ, và đưa lại được sau khi khôi phục người. Test: `putBackIsRefusedWhileTheOwnerIsDeleted` (Task 2).
5. Trên form thêm món, chọn một người rồi đổi lại "Của địa điểm này": thân gửi lên không có chủ. Test: `switchingBackToThePlaceSendsNoOwner` (Task 3).

---

### Task 1: Trường mới thêm vào snapshot không tính là đã đổi

**Files:** sửa `platform-common/src/main/java/com/home_platform/common/history/EntityHistory.java` (hàm `changedFields`); test `platform-common/src/test/java/com/home_platform/common/history/EntityHistoryIT.java`.

  - Hiện nay một trường có ở snapshot mới với giá trị `null` mà không có ở snapshot cũ bị coi là đã đổi. Sửa: trường vắng mặt và trường `null` là như nhau, theo cả hai chiều. Test có sẵn `fieldsAddedOrRemovedCountAsChanged` (thêm, bớt trường có giá trị) vẫn phải xanh.

- [ ] Test (ghi hai version liên tiếp rồi đọc `changedFields` của version sau): `aFieldAddedAsNullIsNotAChange` (`{"a":1}` rồi `{"a":1,"b":null}`: rỗng), `aFieldAddedWithAValueIsAChange` (`{"a":1}` rồi `{"a":1,"b":2}`: `["b"]`), `aFieldDroppedWhileNullIsNotAChange` (`{"a":1,"b":null}` rồi `{"a":1}`: rỗng), `aFieldSetBackToNullIsAChange` (`{"a":1,"b":2}` rồi `{"a":1,"b":null}`: `["b"]`).
- [ ] Hỏng → sửa → PASS; `./mvnw -pl platform-common verify -Dit.test=EntityHistoryIT -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false`. Commit `Treat a missing snapshot field and a null one as equal`.

### Task 2: Backend, chủ của món đồ

**Files:** tạo `registry-service/src/main/resources/db/migration/V4__item_owner.sql`, `registry-service/src/main/java/com/home_platform/registry/item/ItemOwnerRequest.java`; sửa `item/{Item,ItemRequest,ItemSnapshot,ItemView,ItemResponse,ItemDetailResponse,ItemService,ItemRepository,ItemController}.java`, `person/{PersonService,PersonRepository}.java`; test tạo `item/ItemOwnerIT.java`; sửa `item/{ItemTestData,ItemListIT}.java`, `person/PersonIT.java`, `ConcurrentWritesIT.java`, `schema/RegistrySchemaIT.java`.

**Interfaces:**
- Produces:

```java
// Item: thêm Long ownerPersonId ngay sau replacedByItemId.  ItemSnapshot: thêm Long ownerPersonId cùng chỗ.
// ItemRequest: thêm Long ownerPersonId ngay sau placeId.
//   ItemRequest inheriting(Item old)   // thay cho atPlace: placeId trống lấy của old, ownerPersonId trống lấy của old
public record ItemOwnerRequest(@NotNull Integer version, Long ownerPersonId, @Size(max = 255) String note)
public record ItemView(Item item, String placeName, String categoryName, String ownerName)
// ItemResponse, ItemDetailResponse: thêm Long ownerPersonId, String ownerName ngay sau categoryName
// ItemService
@Transactional public Item changeOwner(WorkspaceContext ctx, long id, ItemOwnerRequest request)
public CursorPage<ItemView> ownedBy(long workspaceId, long ownerPersonId, CursorRequest page)   // 404 nếu người không có
// ItemRepository
CursorPage<Item> ownedBy(long workspaceId, long ownerPersonId, CursorRequest page)
	// owner_person_id = :ownerId AND removed_at IS NULL, theo tên; index idx_reg_item_owner
// PersonRepository
int countOwnedItems(long workspaceId, long personId)     // reg_item: owner_person_id, deleted_at IS NULL, removed_at IS NULL
// API
POST /items/{id}/owner  (ItemOwnerRequest) -> ItemDetailResponse
GET  /items?ownerId=
```

- Migration đúng như mục 6 của spec ("Chủ đồ đạc").
- `ItemService` nhận thêm `PersonService`. `create`: sau `places.getForShare`, nếu có `ownerPersonId` thì `people.getForUpdate(ws, ownerPersonId)` (404 khi không có, đã xoá, hay thuộc không gian khác).
- `changeOwner`: `requireNotRemoved`; so `version` trước (lệch thì `versionConflict`); chủ mới bằng chủ cũ thì trả bản hiện tại, không ghi gì; có chủ mới thì `people.getForUpdate`; `update` với `withOwnerPersonId`, ghi version `UPDATE` kèm `request.note()`.
- `replace`: món mới dựng bằng `replacement.inheriting(old)`.
- `putBack` và `restore`: món có chủ mà `people.findForUpdate` rỗng thì `ConflictException("Restore person " + ownerId + " first")`; kiểm sau bước kiểm địa điểm đang có.
- `views`: tên chủ lấy theo lô bằng `people.names` (một truy vấn cho cả trang; trang không có món nào có chủ thì không chạy).
- `PersonService.delete`: sau khi giữ riêng người, đếm thêm `countOwnedItems`; tổng lớn hơn 0 thì 409 với thông báo ở Global Constraints (luôn in cả hai số).
- `ItemController.list`: `ownerId` là lựa chọn thứ tư; đúng một trong bốn.

- [ ] Test `ItemOwnerIT extends ItemTestData` (thêm vào `ItemTestData`: `@Autowired PersonService people` và `long person(String fullName)`):
  - `createsAnItemOwnedByAPerson`: thân có `ownerPersonId`; response có `ownerPersonId` và `ownerName` = tên người.
  - `itemWithoutOwnerHasNullOwner`.
  - `ownerMustBeALivePersonOfThisWorkspace`: `id` không có, người đã xoá, người của `WS_B`: cả ba 404, không món nào được tạo.
  - `changeOwnerRecordsAVersion`: `POST /items/{id}/owner` `{version: 1, ownerPersonId}` trả `version` 2; version 2 là `UPDATE`, `changedFields` = `["ownerPersonId"]`.
  - `clearingTheOwnerGivesTheItemBackToThePlace`: `{version, ownerPersonId: null}`.
  - `changingToTheSameOwnerWritesNothing`: `version` vẫn như cũ, không có version mới.
  - `changeOwnerChecksTheVersion` (409, `code` `VERSION_CONFLICT`), `removedItemCannotChangeOwner` (409 `Item <id> is removed`).
  - `updateIgnoresASentOwner`, `restoringAnOldVersionKeepsTheOwner` (tạo không chủ, gán chủ, khôi phục version 1: vẫn có chủ), `movingKeepsTheOwner`.
  - `replacementInheritsTheOwner`, `replacementCanNameAnotherOwner`.
  - `putBackIsRefusedWhileTheOwnerIsDeleted`: món của An, gỡ món, xoá An, đưa lại: 409 `Restore person <id> first`; khôi phục An rồi đưa lại: 200.
  - `restoreIsRefusedWhileTheOwnerIsDeleted`: món của An, xoá món, xoá An, khôi phục món: 409.
  - `firstEditAfterTheColumnWasAddedDoesNotReportTheOwner`: chèn bằng SQL một món và version 1 có snapshot không có khoá `ownerPersonId`; `PUT` đổi tên: `changedFields` của version 2 là `["name"]`.
- [ ] Test `ItemListIT`: `listsCurrentItemsOfAnOwnerByName` (không gồm món đã gỡ, đã xoá, của người khác), `ownerListOfAnUnknownPersonIs404`, `everyListCarriesOwnerNames` (danh sách theo `placeId` có `ownerName`), `ownedByUsesAnIndex` (`EXPLAIN` có `key = idx_reg_item_owner`); sửa `exactlyOneFilter` theo thông báo mới và thêm trường hợp `placeId` cùng `ownerId`.
- [ ] Test `PersonIT`: `personOwningCurrentItemsCannotBeDeleted` (409 `Person <id> still has 0 current or upcoming occupancies and 2 items`), `personOwningOnlyRemovedOrDeletedItemsCanBeDeleted`; sửa test xoá người đang ở theo thông báo mới (`… 1 current or upcoming occupancies and 0 items`).
- [ ] Test `ConcurrentWritesIT`: `personDeleteSeesAnItemAssignedConcurrently` (`holdOpen` lệnh tạo món có chủ, xoá người ở nền: 409 chứa `and 1 items`), `itemAssignedDuringAPersonDeleteIsRefused` (`holdOpen` lệnh xoá, tạo món có chủ ở nền: `NotFoundException`, `rows("reg_item")` = 0). Sửa hàm `item(...)` của lớp theo `ItemRequest` mới.
- [ ] Test `RegistrySchemaIT`: thêm `reg_item.idx_reg_item_owner` = `workspace_id, owner_person_id, removed_at, name` vào `indexesMatchDesign`; `itemOwnerFromAnotherWorkspaceIsRejected` (chèn bằng SQL: `DataIntegrityViolationException`).
- [ ] Hỏng → viết → PASS; `./mvnw -pl registry-service test` xanh; chạy `-Dit.test='Item*IT,Person*IT,ConcurrentWritesIT,RegistrySchemaIT'`. Commit `Record who owns an item`.

### Task 3: Frontend, chủ trên form, trang và bảng đồ đạc

**Files:** `frontend/src/api/schema/*` (sinh lại); sửa `frontend/src/features/people/{index.ts,components/PersonPicker.tsx}`, `frontend/src/features/items/{types.ts,api.ts,queries.ts,pages/ItemPage.tsx,components/ItemFormDialog.tsx,components/ItemList.tsx,components/ItemHistoryTab.tsx,components/ReplaceItemDialog.tsx}`, `frontend/src/features/activity/labels.ts`, `frontend/src/test/{fixtures.ts,itemBackend.ts}`; tạo `frontend/src/features/items/components/{OwnerField.tsx,ChangeOwnerDialog.tsx}`; test `features/items/ItemForm.test.tsx`, `features/items/ItemLifecycle.test.tsx`, `features/items/PlaceItems.test.tsx`, `features/items/ItemHistory.test.tsx`.

**Interfaces:**
- Consumes: Task 2 (backend đang chạy bản mới để `npm run api:sync`).
- Produces:

```ts
// people/index.ts: thêm export { PersonPicker }
// PersonPicker: onCreateNew thành tuỳ chọn; không truyền thì không hiện nút "Thêm người mới"
// items/types.ts: ItemSummary và ItemDetail có thêm ownerPersonId: string | null, ownerName: string | null (qua Present)
// items/api.ts
changeItemOwner(ws: string, id: string, version: number, ownerPersonId: string | null): Promise<ItemDetail>
listItemsOwnedBy(ws: string, personId: string, cursor?: string): Promise<CursorPage<ItemSummary>>
// items/queries.ts
itemKeys.ownedBy(ws, personId)          // ['items', ws, 'list', 'owner', personId], nằm dưới itemKeys.lists
useItemsOwnedBy(ws, personId): CursorList<ItemSummary>
useChangeItemOwner(ws): UseMutationResult<ItemDetail, ApiError, { item: ItemDetail; ownerPersonId: string | null }>
// items/components
export function OwnerField(props: { ws: string; value: PersonSummary | null; onChange: (owner: PersonSummary | null) => void }): JSX.Element
export function ChangeOwnerDialog(props: { ws: string; item: ItemDetail; open: boolean; onOpenChange: (open: boolean) => void }): JSX.Element
```

  - `OwnerField`: nhóm radio "Của ai" với "Của địa điểm này" (mặc định) và "Của một người"; chọn vế sau thì hiện `PersonPicker` (không có nút tạo người mới). Giá trị nằm ngoài React Hook Form (state của hộp thoại), như `place` trong form cư trú.
  - Form thêm món (`ItemFormDialog` chế độ `create`): có `OwnerField` dưới ô nhóm; thân gửi `ownerPersonId` khi đã chọn người. Chọn "Của một người" mà chưa chọn ai: chặn gửi, báo "Chọn một người". Form sửa không có ô này.
  - `ChangeOwnerDialog`: mở ra với chủ hiện tại; cùng `OwnerField`; nút "Lưu" tắt khi lựa chọn chưa khác chủ hiện tại. Xong: thông báo "Đã đổi chủ". 409 "đã có người sửa": `VersionConflictAlert` với "Tải bản mới".
  - Trang món đồ: dòng "Của <liên kết tới /w/:ws/people/:id>" trong phần đầu trang khi có chủ; menu `⋯` có "Đổi chủ" (biểu tượng `UserRoundIcon`), chỉ với món chưa gỡ.
  - `ItemList`: khi `ownerName` có, hiện "của <tên>" chữ nhỏ, mờ, dưới tên món.
  - `ReplaceItemDialog`: món cũ có chủ thì hiện dòng "Món mới cũng là của <tên chủ>."; không gửi `ownerPersonId` (backend tự thừa hưởng).
  - Nhãn: `ItemHistoryTab.FIELD_LABELS` và `activity/labels.ts` thêm `ownerPersonId: 'Chủ'`.
  - `itemBackend`: nhận `options.ownerNames`; lưu `ownerPersonId` khi tạo; `POST /items/:id/owner`; `GET /items?ownerId=`; thay thì món mới lấy chủ của món cũ.

- [ ] Test `ItemForm.test.tsx`: `newItemBelongsToThePlaceByDefault` (thân không có `ownerPersonId`), `createsAnItemOwnedByAPerson`, `switchingBackToThePlaceSendsNoOwner`, `choosingAPersonIsRequiredOnceAsked`, `editFormHasNoOwnerField`.
- [ ] Test `ItemLifecycle.test.tsx`: `showsTheOwnerWithALinkToThePerson`, `changesTheOwner` (thân `{version, ownerPersonId}`; đầu trang hiện chủ mới), `givesTheItemBackToThePlace` (thân có `ownerPersonId: null`), `saveIsDisabledUntilTheOwnerChanges`, `removedItemHasNoChangeOwnerAction`, `replaceDialogSaysTheNewItemKeepsTheOwner`.
- [ ] Test `PlaceItems.test.tsx`: `ownedItemsShowTheirOwner`; `ItemHistory.test.tsx`: `ownerChangeIsLabelled` ("Đã đổi: Chủ").
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Show and change item owners in the frontend`.

### Task 4: Frontend, tab "Đồ đạc" trên trang một người

**Files:** sửa `frontend/src/features/people/{index.ts,pages/PersonPage.tsx}`, `frontend/src/features/items/index.ts`, `frontend/src/app/router.tsx`; tạo `frontend/src/features/items/components/PersonItemsTab.tsx`; test `features/people/PersonPage.test.tsx`, `features/items/PersonItems.test.tsx`.

**Interfaces:**
- Produces:

```ts
// people/pages/PersonPage.tsx
export type PersonTabSlots = Partial<Record<'items', (ws: string, person: PersonDetail) => ReactNode>>
export function PersonPage(props: { tabs?: PersonTabSlots }): JSX.Element
// items
export function PersonItemsTab(props: { ws: string; person: PersonDetail }): JSX.Element
// app/router.tsx
const personTabs: PersonTabSlots = { items: (ws, person) => <PersonItemsTab ws={ws} person={person} /> }
```

  - Thứ tự tab: Thông tin, Nơi ở, Đồ đạc, Lịch sử. Tab "Đồ đạc" chỉ có khi `tabs.items` được truyền; `?tab=items` trên URL.
  - `PersonItemsTab`: `ItemList` với `showPlace`, dữ liệu từ `useItemsOwnedBy`; trống thì "Không có món đồ nào là của riêng người này."
  - `DeletePersonDialog` đã hiện nguyên văn lý do 409, không phải sửa.

- [ ] Test `PersonPage.test.tsx`: `pageWithoutSlotsHasNoItemsTab` (render `PersonPage` không truyền `tabs`), `itemsTabIsOnTheUrl`. Test `PersonItems.test.tsx` (qua `renderApp`): `listsTheItemsThisPersonOwnsWithTheirPlaces`, `emptyOwnerTabSaysSo`, `changingAnOwnerRefreshesThePersonsList`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the items tab to the person page`.

### Task 5: Hoàn tất

- [ ] `docs/superpowers/specs/2026-11-13-business-rules-design.md`: mục 3 thêm `owner_person_id` vào danh sách trường do hệ thống quản lý; mục 5.6 thêm điều kiện xoá người; mục 5.9 thêm một đoạn "Chủ" trỏ sang `2026-11-16-ownership-design.md` mục 5.5.
- [ ] `README.md` gốc: dòng `/items` thêm `POST /{id}/owner`, `GET ?ownerId=`; dòng `/people` thêm điều kiện xoá. `frontend/README.md`: luật phụ thuộc (`people` không nhập `items`; `PersonTabSlots`), dòng "Đổi chủ" trong đoạn về đồ đạc của mục "Sau một lệnh ghi".
- [ ] Chạy thử trên backend thật: thêm một món của một người thuê, mở trang người thấy món ở tab Đồ đạc, đổi chủ về địa điểm, thử xoá người khi còn món (thấy lý do) rồi khi hết (xoá được); chụp màn hình. Xoá dữ liệu vừa tạo theo đúng `id`.
- [ ] `./mvnw verify` (chạy nền) xanh; `npm test`, `npm run build`, `npm run lint` sạch. Commit `Document item ownership` và push `main`.

## Để sau, có lý do

- **Lọc "đồ của địa điểm" và "đồ của người ở" trong tab Đồ đạc của một địa điểm**: cần thêm một cách đọc ở backend; chờ xem người dùng có cần không, vì tên chủ đã hiện ngay trên từng dòng.
- **Đổi chủ hàng loạt** (người thuê chuyển đi, để lại hết đồ): thuộc nhóm thao tác hàng loạt (H), làm cùng chọn nhiều dòng.
- **Tạo người mới ngay trong ô "Của ai"**: form cư trú có vì thêm người ở thường đi cùng thêm hồ sơ; chủ của món đồ gần như luôn là người đã có.
- **Nhiều chủ cho một món**: spec chọn một chủ (mục 4.2).
