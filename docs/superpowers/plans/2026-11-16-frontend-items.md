# Frontend Items Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Trả lời câu hỏi thứ ba của hệ thống trên giao diện: ở đây có đồ đạc gì, món nào đã gỡ hay bị thay. Gồm tab "Đồ đạc" của một địa điểm và trang một món đồ với đủ vòng đời: thêm, sửa, chuyển chỗ, gỡ, thay, đưa lại, xoá.

**Architecture:** Feature mới `features/items`; nhóm đồ (`/item-categories`) thêm vào `features/catalog`. Món đồ có một chỗ đứng (`placeId`) và một trạng thái gỡ; mọi lệnh đổi chỗ đứng hay trạng thái gỡ là một thao tác riêng, đúng như backend. Dùng lại `PlacePicker`, `AttributeFields`, `VersionConflictAlert`, `applyServerErrors`.

**Tech Stack:** như `2026-11-13-frontend-foundation-places.md`; không thêm thư viện.

**Spec:** `docs/superpowers/specs/2026-11-13-frontend-structure-design.md` (mục 6: tab Đồ đạc, `/items/:itemId`; mục 8 bước 4), `docs/superpowers/specs/2026-11-13-business-rules-design.md` (mục 5.8, 5.9).

**Phụ thuộc:** chạy sau `2026-11-16-frontend-people.md` (dùng `todayInBusinessZone`, mẫu tab có công tắc "Cả bên trong").

## Global Constraints

- Như Global Constraints của `2026-11-13-frontend-foundation-places.md` và của kế hoạch người ở (ngày là chuỗi, "hôm nay" theo `Asia/Ho_Chi_Minh`).
- Món đã gỡ chỉ đọc: chỉ còn "Đưa lại" và "Xoá". Giao diện không hiện thao tác nào khác cho nó.
- Lệnh sửa gửi lại cả `coverFileId` và `placedAt`.
- Mục tuỳ biến của món đồ: mục chung (`typeId` `'0'`) cộng mục của nhóm đang chọn; không có nhóm thì chỉ mục chung.
- Nhãn: IN_USE "Đang dùng", BROKEN "Hỏng", STORED "Cất kho", REMOVED "Đã gỡ".

## Review Focus

1. Thay một món: món cũ sang "Đã gỡ" có liên kết tới món mới, món mới hiện trong "Đang có" (cùng chỗ hoặc chỗ mới), cả hai danh sách tự cập nhật. Test: `replaceMovesTheOldOneToRemovedAndLinksTheNewOne` (Task 4).
2. Đưa lại một món đã bị thay bằng một món còn đó: nút bị chặn với lý do rõ, không gửi request. Test: `putBackIsBlockedWhileTheReplacementExists` (Task 4).
3. Đổi nhóm của một món: giá trị mục tuỳ biến của nhóm cũ không bị gửi lên (backend sẽ từ chối). Test: `changingCategoryDropsOldCategoryFields` (Task 2).
4. Chuyển một món sang toà khác: "Cả bên trong" của toà cũ không còn nó, của toà mới có nó. Test: `moveRefreshesBothSubtrees` (Task 4).
5. Số lượng: chỉ số nguyên từ 1; "1.5", "0", "-2" bị chặn trước khi gửi. Test: `quantityMustBeAPositiveWholeNumber` (Task 2).

---

### Task 1: Nhóm đồ và lớp API của đồ đạc

**Files:** thêm vào `frontend/src/features/catalog/{types.ts,api.ts,queries.ts,index.ts}` (`ItemCategory`, `listItemCategories`, `useItemCategories`); tạo `frontend/src/features/items/{types.ts,api.ts,queries.ts,index.ts,labels.ts}`; thêm `item`, `itemDetail`, `itemCategory` vào `src/test/fixtures.ts`; test `features/items/queries.test.tsx`.

**Interfaces:**
- Produces:

```ts
// catalog
export type ItemCategory = Present<S['ItemCategoryResponse'], 'id' | 'name' | 'sortOrder' | 'status'>
export function useItemCategories(ws: string): UseQueryResult<ItemCategory[], ApiError>
// items/types.ts
export type ItemSummary = Present<S['ItemResponse'], 'id' | 'placeId' | 'placeName' | 'name' | 'quantity' | 'status' | 'version'>
export type ItemDetail = Present<S['ItemDetailResponse'], 'id' | 'placeId' | 'placeName' | 'name' | 'quantity' | 'status' | 'version' | 'createdAt' | 'updatedAt'>
// items/api.ts
listAtPlace(ws, placeId, removed: boolean, cursor?), listInSubtree(ws, placeId, cursor?): Promise<CursorPage<ItemSummary>>
getItem, createItem, updateItem, moveItem(ws, id, version, placeId), removeItem(ws, id, version, removedAt), replaceItem(ws, id, body),
putBackItem(ws, id, version), deleteItem(ws, id, version)
// items/queries.ts
export const itemKeys = { all(ws), atPlace(ws, placeId, removed), inSubtree(ws, placeId), details(ws), detail(ws, id) }
useItemsAtPlace, useItemsInSubtree, useItem, và một hook ghi cho mỗi lệnh trên
export function refreshItems(queryClient, ws, ...changed: ItemDetail[]): void   // đặt detail của từng món; làm cũ mọi danh sách đồ của không gian
```

  - Danh sách đồ là của từng địa điểm và cây con; một lệnh ghi có thể chạm hai địa điểm (chuyển, thay sang chỗ khác), nên làm cũ cả `itemKeys.all(ws)` trừ chi tiết vừa đặt, thay vì đoán từng khoá.

- [ ] Test: `itemWritesRefreshEveryItemList`, `removedListAsksForRemoved` (`?placeId=&removed=true`), `categoriesAreSortedForPickers`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the item API layer and item categories`.

### Task 2: Form món đồ

**Files:** tạo `frontend/src/features/items/components/{ItemFormDialog.tsx,itemForm.ts}`; test `ItemForm.test.tsx`.

**Interfaces:**
- Produces: `ItemFormDialog(props: { ws; open; onOpenChange } & ({ mode: 'create'; place: PlaceDetail } | { mode: 'edit'; item: ItemDetail }))`; `itemFormSchema(defs: FieldDef[])`; `itemPayload(values, defs, base: ItemDetail | null)`.

  - Ô: nhóm (`NativeSelect`, có lựa chọn "Không nhóm"; chỉ nhóm ACTIVE, khi sửa giữ nhóm cũ đã lưu trữ), tên (bắt buộc, ≤255), mã (≤50), số lượng (số nguyên ≥ 1, mặc định 1), đơn vị (≤20, gợi ý "cái", "bộ", "chiếc"), trạng thái (Đang dùng, Hỏng, Cất kho), ngày đặt (tuỳ chọn), mô tả, mục tuỳ biến theo nhóm (`useFieldDefs(ws, 'ITEM', categoryId ?? '0')`).
  - Cùng cách xử lý lỗi và 409 như form địa điểm; nút gửi khoá khi mục tuỳ biến chưa tải xong.

- [ ] Test: `createsAnItemInThePlace` (body có `placeId`, `quantity: 1`), `quantityMustBeAPositiveWholeNumber`, `changingCategoryDropsOldCategoryFields`, `editSendsCoverAndPlacedAtBack`, `versionConflictKeepsTypedValues`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the item form`.

### Task 3: Tab "Đồ đạc" của một địa điểm

**Files:** tạo `frontend/src/features/items/components/{PlaceItemsTab.tsx,ItemList.tsx}`; sửa `features/places/pages/PlacePage.tsx` (tab `items`); test `PlaceItems.test.tsx`.

  - Hai nhóm "Đang có" (theo tên) và "Đã gỡ" (mới gỡ trước, chỉ tải khi mở nhóm). Công tắc "Cả bên trong" (chỉ hiện khi địa điểm có chỗ con: đọc `useChildren(ws, place.id)`, cùng cache với cây và tab Bên trong; `PlaceDetail` không có `hasChildren`): đồ đang có trong cả cây con, thêm cột địa điểm.
  - Mỗi dòng: tên (liên kết tới trang món), nhóm, số lượng + đơn vị, trạng thái; dòng đã gỡ có ngày gỡ và, nếu bị thay, "thay bằng <tên món mới>".
  - Nút "Thêm đồ" mở form của Task 2 với địa điểm đang xem.

- [ ] Test: `listsCurrentItemsByName`, `removedGroupLoadsOnlyWhenOpened`, `wholeBuildingShowsWhereEachItemIs`, `addsAnItemFromTheTab`, `emptyTabInvitesToAdd`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the items tab to the place page`.

### Task 4: Trang một món đồ và vòng đời của nó

**Files:** tạo `frontend/src/features/items/{pages/ItemPage.tsx,components/ItemHeader.tsx,components/ItemActionsMenu.tsx,components/MoveItemDialog.tsx,components/RemoveItemDialog.tsx,components/ReplaceItemDialog.tsx,components/PutBackItemDialog.tsx,components/DeleteItemDialog.tsx}`; sửa `app/router.tsx` (`items/:itemId`); test `ItemPage.test.tsx`, `ItemLifecycle.test.tsx`.

  - Đầu trang: tên, nhóm, trạng thái, "ở <liên kết địa điểm>", số lượng, `v<version>`; tab "Thông tin" (mã, ngày đặt, ngày gỡ, mô tả, mục tuỳ biến) và "Lịch sử" (`ComingSoonState` tới kế hoạch lịch sử).
  - Món chưa gỡ: "Sửa"; menu: Chuyển chỗ (`PlacePicker`, không có "lên cấp cao nhất", loại trừ chỗ hiện tại), Gỡ (ngày gỡ, mặc định hôm nay, không trước ngày đặt), Thay (form của Task 2 cho món mới, ngày gỡ món cũ, tuỳ chọn "đặt ở chỗ khác" bằng `PlacePicker`; xong thì tới trang món mới), Xoá (nhập nhầm).
  - Món đã gỡ: dải thông báo "Đã gỡ ngày …" (và "Đã thay bằng <liên kết>" nếu có); chỉ còn Đưa lại và Xoá. Đưa lại bị chặn (nút tắt, lý do bên cạnh) khi `replacedByItemId` trỏ tới món còn đó: trang đọc món thay bằng `useItem`; món thay đã xoá (404) thì cho đưa lại.
  - Xoá xong về trang của địa điểm chứa nó. 409 hiện nguyên văn; version lệch có "Tải bản mới".

- [ ] Test: `showsHeaderWithPlaceLink`, `movesToAnotherPlace` (`{version, placeId}`), `moveRefreshesBothSubtrees`, `removesWithADate` (`{version, removedAt}`), `removedItemIsReadOnly`, `replaceMovesTheOldOneToRemovedAndLinksTheNewOne`, `replaceAtAnotherPlace`, `putBackIsBlockedWhileTheReplacementExists`, `putBackWorksOnceTheReplacementIsDeleted`, `deleteReturnsToThePlace`, `unknownItemShowsNotFound`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the item page and its lifecycle`.

### Task 5: Hoàn tất

- [ ] `frontend/README.md`: feature `items`, dòng đồ đạc trong bảng "Sau một lệnh ghi".
- [ ] Chạy thử trên backend thật: thêm một món vào một phòng, thay nó bằng món mới ở phòng khác, mở món cũ thấy "Đã thay bằng …", xoá món mới rồi đưa món cũ lại; chụp màn hình. Xoá dữ liệu vừa tạo theo đúng `id`, có chốt an toàn.
- [ ] `npm test`, `npm run build`, `npm run lint` sạch. Commit `Document items` và push `main`.

## Để sau, có lý do

- Tìm đồ đạc trên toàn không gian (`GET /items/search`): spec chưa có màn hình cho nó; thêm khi có trang tìm kiếm chung.
- Danh sách đồ theo nhóm (`?categoryId=`): dùng ở trang danh mục của kế hoạch sau nếu cần.
