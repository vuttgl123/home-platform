# Frontend History, Catalog and Activity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hoàn tất bước 5 của spec frontend: tab "Lịch sử" (xem, so và khôi phục version) cho địa điểm, người, đồ đạc; trang "Danh mục" để người dùng tự định nghĩa loại địa điểm, nhóm đồ và mục tuỳ biến; trang "Hoạt động" của cả không gian, cũng là nơi khôi phục thứ đã xoá.

**Architecture:** `features/history` là một bộ component chung, nhận một "nguồn version" (đường dẫn API của địa điểm, người hay đồ đạc) chứ không biết nghiệp vụ; mỗi feature cung cấp nguồn của mình và nhãn tiếng Việt cho các trường. `features/catalog` có thêm trang quản lý. `features/activity` là trang mới đọc `GET /activity`.

**Tech Stack:** như `2026-11-13-frontend-foundation-places.md`; không thêm thư viện.

**Spec:** `docs/superpowers/specs/2026-11-13-frontend-structure-design.md` (mục 6: tab Lịch sử, `/catalog`, `/activity`; mục 8 bước 5), `docs/superpowers/specs/2026-11-13-business-rules-design.md` (mục 5.2, 5.3, 5.8, 5.10).

**Phụ thuộc:** chạy sau `2026-11-16-frontend-items.md`, và sau Task 4 của `2026-11-16-place-tree-and-search-gaps.md` (`ActivityResponse.entityName`).

## Global Constraints

- Như Global Constraints của `2026-11-13-frontend-foundation-places.md`.
- Khôi phục về một version cũ là một lần sửa: gửi `version` hiện tại của bản ghi (không phải số của version cũ), backend ghi version mới "Restored from version N". Khôi phục không đổi chỗ đứng (địa điểm không di chuyển, đồ đạc không đổi chỗ hay trạng thái gỡ); giao diện nói rõ điều này trước khi xác nhận.
- Khôi phục thứ đã xoá gửi `version` của dòng đã xoá; số đó chính là `versionNo` của version DELETE mới nhất của nó.
- `changedBy`: "Bạn" khi trùng người dùng của phiên, không thì "Người dùng khác" (chưa có API đọc tên thành viên).
- Danh mục không có `version`: hai người sửa cùng lúc thì người sau thắng; giao diện không hiện "Tải bản mới" cho danh mục.
- Nhãn hành động: CREATE "Tạo", UPDATE "Sửa", DELETE "Xoá", RESTORE "Khôi phục".

## Review Focus

1. Khôi phục một version cũ khi bản ghi đã được sửa tiếp ở tab khác: 409 version lệch có "Tải bản mới", không ghi đè im lặng. Test: `restoreWithStaleVersionOffersReload` (Task 2).
2. Khôi phục thứ đã xoá từ trang Hoạt động, rồi mở được nó và nó hiện lại trên cây đúng chỗ. Test: `restoringADeletedPlacePutsItBackInTheTree` (Task 4).
3. Khôi phục một địa điểm mà cha của nó đang bị xoá: lý do "Restore the parent place … first" hiện nguyên văn kèm liên kết tới hoạt động của cha. Test: `restoreNeedsTheParentFirst` (Task 4).
4. Lưu trữ một loại địa điểm thì form thêm địa điểm không còn gợi ý nó, nhưng địa điểm đang dùng nó vẫn hiện đúng tên. Test: `archivedTypeLeavesNewFormsButStaysOnOldPlaces` (Task 3).
5. Bản ghi có hàng trăm version: danh sách tải dần, mở một version không tải lại cả danh sách. Test: `versionsLoadPageByPage` (Task 1).

---

### Task 1: Bộ component lịch sử dùng chung

**Files:** tạo `frontend/src/features/history/{types.ts,api.ts,queries.ts,index.ts,labels.ts,components/VersionList.tsx,components/VersionDialog.tsx,components/HistoryTab.tsx}`; test `features/history/HistoryTab.test.tsx`.

**Interfaces:**
- Produces:

```ts
export type EntityKind = 'places' | 'people' | 'items'
export type VersionSummary = Present<S['PlaceVersionSummaryResponse'], 'versionNo' | 'action' | 'changedFields' | 'changedBy' | 'changedAt'>
export type Version<Snapshot> = VersionSummary & { snapshot: Snapshot }
listVersions(ws, kind, id, cursor?), getVersion(ws, kind, id, versionNo), restoreVersion(ws, kind, id, versionNo, currentVersion, note?)
export const historyKeys = { all(ws), list(ws, kind, id), version(ws, kind, id, versionNo) }
export function HistoryTab<Snapshot>(props: {
	ws: string; kind: EntityKind; id: string; currentVersion: number
	fieldLabels: Record<string, string>                        // 'name' → 'Tên', 'attributes.area_m2' do nơi gọi thêm
	renderSnapshot: (snapshot: Snapshot) => ReactNode           // mỗi feature tự hiện nội dung của mình
	onRestored: (restored: unknown) => void                     // feature cập nhật cache của nó (refreshAfterWrite...)
}): JSX.Element
```

  - Danh sách: mỗi dòng "v<số> · <hành động> · <thời điểm> · <Bạn | Người dùng khác>", các trường đã đổi (theo `fieldLabels`, trường lạ hiện nguyên khoá), ghi chú. Mới nhất trước, "Tải thêm".
  - Bấm một dòng mở `VersionDialog`: nội dung version (`renderSnapshot`) và nút "Khôi phục bản này" (ẩn ở version mới nhất). Xác nhận có câu "Chỉ nội dung được khôi phục; chỗ đứng giữ nguyên." và ô ghi chú tuỳ chọn.
  - Khôi phục xong: làm cũ `historyKeys.list`, gọi `onRestored`, đóng hộp thoại, thông báo "Đã khôi phục về v<số>".

- [ ] Test: `listsVersionsNewestFirst`, `versionsLoadPageByPage`, `changedFieldsUseLabels`, `opensAVersionWithItsSnapshot`, `latestVersionCannotBeRestored`, `restoreSendsTheCurrentVersion`, `changedByIsYouForTheSignedInUser`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the shared version history components`.

### Task 2: Tab Lịch sử của địa điểm, người, đồ đạc

**Files:** tạo `frontend/src/features/places/components/{PlaceHistoryTab.tsx,placeFieldLabels.ts}`, tương tự cho `people` và `items`; sửa ba trang chi tiết (tab `history`); test `PlaceHistory.test.tsx`, `PersonHistory.test.tsx`, `ItemHistory.test.tsx`.

  - Địa điểm: nội dung version gồm tên, mã, loại (tên từ `usePlaceTypes`, kể cả loại đã lưu trữ), trạng thái, mô tả, địa chỉ của version đó, mục tuỳ biến qua `AttributeView`. Khôi phục xong gọi `refreshAfterWrite(..., 'descendants')` (tên và địa chỉ có thể đổi).
  - Người, đồ đạc: tương tự với trường của mình; khôi phục xong gọi hàm làm mới cache của feature đó.
  - 400 khi nội dung cũ không còn hợp lệ (loại đã lưu trữ, mục tuỳ biến đổi kiểu): hiện nguyên văn kèm câu "Sửa tay bản hiện tại thay vì khôi phục."

- [ ] Test: `placeHistoryShowsAddressOfThatVersion`, `restoringAPlaceVersionRenamesItInTheTree`, `restoreWithStaleVersionOffersReload`, `invalidOldContentExplainsWhy`, `personHistoryRestores`, `itemHistoryRestoresContentOnly` (món đã gỡ vẫn ở trạng thái gỡ sau khi khôi phục nội dung).
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add history tabs to places, people and items`.

### Task 3: Trang Danh mục

**Files:** tạo `frontend/src/features/catalog/{pages/CatalogPage.tsx,components/PlaceTypeSection.tsx,components/ItemCategorySection.tsx,components/FieldDefSection.tsx,components/PlaceTypeDialog.tsx,components/ItemCategoryDialog.tsx,components/FieldDefDialog.tsx}`; thêm hàm ghi vào `catalog/api.ts`, `catalog/queries.ts`; sửa `app/router.tsx` (`catalog`); test `CatalogPage.test.tsx`, `FieldDefDialog.test.tsx`.

**Interfaces:**
- Produces: hook ghi cho từng thao tác: `useCreatePlaceType`, `useUpdatePlaceType`, `useArchivePlaceType`, `useUnarchivePlaceType`, `useDeletePlaceType`, cùng bộ cho nhóm đồ, và `useCreateFieldDef`, `useUpdateFieldDef`, `useArchiveFieldDef`, `useUnarchiveFieldDef`. Mọi lệnh ghi làm cũ `catalogKeys.all(ws)`.

  - Ba phần trên cùng một trang, mỗi phần một bảng: tên (và mã với loại địa điểm), thứ tự, trạng thái; mục đã lưu trữ ở cuối, chữ mờ, có "Bỏ lưu trữ".
  - Loại địa điểm: thêm (mã chỉ `A-Z0-9_`, tự viết hoa khi gõ, ≤50; tên ≤100; mô tả ≤500; thứ tự), sửa (mã không sửa được), lưu trữ, xoá (409 "đang được dùng" hiện nguyên văn kèm gợi ý lưu trữ). Nhóm đồ: tên ≤100, thứ tự.
  - Mục tuỳ biến: lọc theo đích (Địa điểm, Người, Đồ đạc) và phạm vi (Mọi … hoặc một loại/nhóm). Thêm: khoá (`[a-z][a-z0-9_]*`, ≤50; gợi ý từ nhãn bằng cách bỏ dấu, viết thường, thay khoảng trắng bằng `_`), nhãn, kiểu, lựa chọn (chỉ với SELECT: 1–100 dòng, không trùng, mỗi dòng ≤100), bắt buộc, thứ tự. Sửa chỉ nhãn, lựa chọn, bắt buộc, thứ tự (khoá, đích, phạm vi, kiểu hiện ở dạng chỉ đọc kèm lý do). 409 trùng khoá trong phạm vi chồng nhau hiện nguyên văn.

- [ ] Test: `listsTheThreeCatalogs`, `placeTypeCodeIsUppercased`, `deletingAUsedTypeSuggestsArchiving`, `archivedTypeLeavesNewFormsButStaysOnOldPlaces`, `fieldKeyIsSuggestedFromTheLabel` ("Diện tích sàn" → `dien_tich_san`), `selectNeedsOptions`, `fieldKeyAndTypeAreReadOnlyWhenEditing`, `overlappingKeyConflictIsShown`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the catalog page`.

### Task 4: Trang Hoạt động và khôi phục thứ đã xoá

**Files:** tạo `frontend/src/features/activity/{types.ts,api.ts,queries.ts,index.ts,pages/ActivityPage.tsx,components/ActivityRow.tsx,components/RestoreDeletedDialog.tsx}`; thêm `restorePlace`, `restorePerson`, `restoreItem` vào api/queries của từng feature; sửa `app/router.tsx` (`activity`); test `ActivityPage.test.tsx`.

**Interfaces:**
- Produces: `ActivityEntry = Present<S['ActivityResponse'], 'entityType' | 'entityId' | 'versionNo' | 'action' | 'changedFields' | 'changedBy' | 'changedAt'>`; `useActivity(ws, entityType: 'PLACE' | 'PERSON' | 'ITEM' | null)`.

  - Danh sách mới nhất trước, "Tải thêm"; lọc Tất cả / Địa điểm / Người / Đồ đạc (trên URL `?type=`). Mỗi dòng: tên bản ghi (`entityName`; liên kết tới trang của nó trừ khi dòng là DELETE), hành động, trường đã đổi, thời điểm, người sửa, ghi chú.
  - Dòng DELETE có nút "Khôi phục": gửi `version = versionNo` của dòng đó. 409 version lệch nghĩa là đã có thay đổi sau lần xoá (đã khôi phục rồi): hiện "Đã được khôi phục hoặc thay đổi sau lần xoá này" và làm mới danh sách. 409 "Restore the parent place … first" hiện nguyên văn.
  - Khôi phục xong: làm mới cache của feature tương ứng (địa điểm: `refreshAfterWrite(..., 'self')` để nó hiện lại trên cây), làm mới danh sách hoạt động, thông báo kèm liên kết "Mở".

- [ ] Test: `listsActivityNewestFirst`, `filterFollowsTheUrl`, `rowsLinkToTheirRecords`, `deletedRowsOfferRestore`, `restoringADeletedPlacePutsItBackInTheTree`, `restoreNeedsTheParentFirst`, `alreadyRestoredRowExplainsItself`, `restoresAPersonAndAnItem`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add recent activity and restoring deleted records`.

### Task 5: Hoàn tất

- [ ] Thanh bên: ba liên kết Người, Danh mục, Hoạt động đều trỏ tới trang thật; bỏ `ComingSoonPage` và `ComingSoonState` nếu không còn ai dùng. `frontend/README.md` cập nhật cấu trúc và bảng cache.
- [ ] Chạy thử trên backend thật: đổi tên một phòng hai lần, khôi phục về bản đầu; tạo một mục tuỳ biến cho loại Phòng rồi điền nó; xoá một phòng rồi khôi phục từ trang Hoạt động; chụp màn hình. Xoá dữ liệu vừa tạo theo đúng `id`, có chốt an toàn.
- [ ] `npm test`, `npm run build`, `npm run lint` sạch. Commit `Document history, catalog and activity` và push `main`.

## Để sau, có lý do

- Ảnh (bìa của địa điểm, ảnh đại diện của người, ảnh đồ đạc): media chưa có backend, còn chờ chọn kho lưu trữ file (`docs/superpowers/todo/media.md`).
- Đăng nhập thật, mời thành viên, tên người sửa trong lịch sử: chờ phần bảo mật.
- Trang "thùng rác" riêng: trang Hoạt động lọc theo hành động Xoá đã đủ cho nhu cầu hiện tại (spec business rules, câu hỏi mở số 3).
