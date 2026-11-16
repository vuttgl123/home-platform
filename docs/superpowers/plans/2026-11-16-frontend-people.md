# Frontend People and Occupancy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Trả lời câu hỏi thứ hai của hệ thống trên giao diện: ai đang ở đây, ai từng ở đây, người này đã ở những đâu. Gồm tab "Người ở" của một địa điểm, trang danh sách người và trang một người.

**Architecture:** Một feature mới `features/people` chứa cả hồ sơ người và cư trú (một người ở một địa điểm từ ngày A tới ngày B), đúng như spec mục 7 ("people/ danh sách, hồ sơ, cư trú"); tách hai feature thì chúng nhập lẫn nhau. `people` dùng `places` qua `index.ts`; `places` không nhập `people`: trang địa điểm nhận nội dung tab "Người ở" từ `app/` (prop `tabs` của `PlacePage`). Các đường dẫn `features/occupancies/...` bên dưới hiểu là `features/people/...`. Mọi thứ dựng trên nền đã có: `useCursorList`, `Present`, `AttributeFields`, `applyServerErrors`, `VersionConflictAlert`, `PlacePicker`.

**Tech Stack:** như `2026-11-13-frontend-foundation-places.md`; không thêm thư viện.

**Spec:** `docs/superpowers/specs/2026-11-13-frontend-structure-design.md` (mục 6: tab Người ở, `/people`, `/people/:personId`; mục 8 bước 3), `docs/superpowers/specs/2026-11-13-business-rules-design.md` (mục 5.6, 5.7).

**Phụ thuộc:** chạy sau `2026-11-16-place-tree-and-search-gaps.md` (dùng `PlacePicker`, `isVersionConflict` theo `code`).

## Global Constraints

- Như Global Constraints của `2026-11-13-frontend-foundation-places.md`.
- Ngày (`startDate`, `endDate`, `dateOfBirth`) là chuỗi `YYYY-MM-DD`, không qua `Date` khi gửi. "Hôm nay" tính theo `Asia/Ho_Chi_Minh` như backend (`registry.business-zone`), không theo múi giờ của máy.
- Trạng thái cư trú (đang ở, sắp đến, đã rời) lấy từ `status` của backend, không tự tính lại.
- Lệnh sửa người gửi lại cả `avatarFileId` (PUT ghi đè cả bản ghi, form không có ô ảnh).
- Nhãn tiếng Việt: quan hệ OWNER "Chủ", RESIDENT "Người ở", GUEST "Khách", STAFF "Nhân viên", OTHER "Khác"; giới tính MALE "Nam", FEMALE "Nữ", OTHER "Khác"; trạng thái CURRENT "Đang ở", UPCOMING "Sắp đến", PAST "Đã rời".

## Review Focus

1. Ghi một cư trú xong thì cả ba nơi hiện nó đều đổi: tab Người ở của địa điểm, "cả toà nhà" của tổ tiên, trang của người đó. Test: `addingAnOccupantRefreshesEveryView` (Task 4).
2. Ngày "hôm nay" mặc định đúng theo giờ Việt Nam cả khi máy đặt múi giờ khác (23:30 UTC đã là hôm sau ở Việt Nam). Test: `todayFollowsTheBusinessZone` (Task 1).
3. Hai người trùng giấy tờ: thông báo cho mở được hồ sơ người đã có. Test: `duplicateDocumentLinksToTheOtherPerson` (Task 2).
4. Sửa tên một người thì các dòng cư trú đang hiện tên cũ cũng đổi. Test: `renamingAPersonRenamesTheirRows` (Task 3).
5. Thêm người ở với một người chưa có hồ sơ: hồ sơ được tạo rồi mới tạo cư trú; nếu bước sau hỏng, hồ sơ đã tạo không bị tạo lần hai khi bấm lại. Test: `retryAfterOccupancyFailureReusesTheNewPerson` (Task 4).

---

### Task 1: Lớp API của người và cư trú

**Files:** tạo `frontend/src/features/people/{types.ts,api.ts,queries.ts,index.ts}`, `frontend/src/features/occupancies/{types.ts,api.ts,queries.ts,index.ts,labels.ts}`, `frontend/src/lib/dates.ts`; thêm vào `src/test/fixtures.ts` (`person`, `personDetail`, `occupancy`); test `lib/dates.test.ts`, `features/occupancies/queries.test.tsx`.

**Interfaces:**
- Produces:

```ts
// lib/dates.ts
export function todayInBusinessZone(now?: Date): string                // 'YYYY-MM-DD' ở Asia/Ho_Chi_Minh
// people/types.ts
export type PersonSummary = Present<S['PersonResponse'], 'id' | 'fullName' | 'version'>
export type PersonDetail = Omit<Present<S['PersonDetailResponse'], 'id' | 'fullName' | 'version' | 'createdAt' | 'updatedAt'>, 'currentOccupancies'> & { currentOccupancies: Occupancy[] }
// people/api.ts
listPeople(ws, cursor?), searchPeople(ws, by: 'name' | 'phone' | 'idNumber', term: string, cursor?): Promise<CursorPage<PersonSummary>>
getPerson(ws, id): Promise<PersonDetail>; createPerson(ws, body); updatePerson(ws, id, body); deletePerson(ws, id, version): Promise<void>
// people/queries.ts
export const personKeys = { all, lists, list(ws), search(ws, by, term), details, detail(ws, id) }
usePeople(ws), usePeopleSearch(ws, by, term), usePerson(ws, id), useCreatePerson(ws), useUpdatePerson(ws), useDeletePerson(ws)
// occupancies/types.ts
export type Occupancy = Present<S['OccupancyResponse'], 'id' | 'placeId' | 'placeName' | 'personId' | 'personFullName' | 'relation' | 'startDate' | 'status' | 'version'>
// occupancies/api.ts
listAtPlace(ws, placeId, status, cursor?), listInSubtree(ws, placeId, cursor?), listOfPerson(ws, personId, cursor?)
createOccupancy(ws, body), updateOccupancy(ws, id, body), endOccupancy(ws, id, version, endDate), deleteOccupancy(ws, id, version)
// occupancies/queries.ts
export const occupancyKeys = { all(ws), atPlace(ws, placeId, status), inSubtree(ws, placeId), ofPerson(ws, personId) }
useOccupanciesAtPlace(ws, placeId, status), useOccupanciesInSubtree(ws, placeId, enabled), useOccupanciesOfPerson(ws, personId)
useCreateOccupancy(ws), useUpdateOccupancy(ws), useEndOccupancy(ws), useDeleteOccupancy(ws)
export function refreshOccupancies(queryClient, ws, personId: string): void
	// làm cũ occupancyKeys.all(ws) và personKeys.detail(ws, personId) (currentOccupancies)
// occupancies/labels.ts
export const RELATION_LABEL, STATUS_LABEL
```

  - Mọi lệnh ghi cư trú gọi `refreshOccupancies`. Ghi người (`useUpdatePerson`) đặt `personKeys.detail` bằng response, làm cũ `personKeys.lists`, và làm cũ `occupancyKeys.all(ws)` (các dòng mang `personFullName`).

- [ ] Test: `todayFollowsTheBusinessZone` (`new Date('2026-11-15T23:30:00Z')` → `'2026-11-16'`), `occupancyWritesRefreshPlacePersonAndSubtree` (ba danh sách đang mở đều tải lại sau `useEndOccupancy`), `searchSendsTheChosenField` (`by: 'phone'` → `?phone=`).
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the people and occupancy API layers`.

### Task 2: Danh sách người và form hồ sơ

**Files:** tạo `frontend/src/features/people/{pages/PeoplePage.tsx,components/PersonFormDialog.tsx,components/personForm.ts,components/PersonSearchBox.tsx}`; sửa `app/router.tsx` (`people` → `PeoplePage`), `features/catalog` nếu `useFieldDefs` cần nhận `'0'` (mục chung); test `features/people/PeoplePage.test.tsx`, `PersonForm.test.tsx`.

**Interfaces:**
- Produces: `PersonFormDialog(props: { ws: string; open: boolean; onOpenChange; onSaved?: (person: PersonDetail) => void } & ({ mode: 'create'; initialName?: string } | { mode: 'edit'; person: PersonDetail }))`.

  - Trang `/w/:ws/people`: ô tìm có chọn kiểu (Tên, Số điện thoại, Số giấy tờ; chờ 300 ms), danh sách (tên, điện thoại, giấy tờ) có "Tải thêm", nút "Thêm người". Trống: "Chưa có ai" + nút thêm.
  - Form: họ tên (bắt buộc, ≤150), điện thoại (≤20), email (đúng dạng nếu có), giới tính, ngày sinh (không ở tương lai, so với `todayInBusinessZone()`), loại giấy tờ (gợi ý CCCD, CMND, Hộ chiếu qua `<datalist>`, vẫn gõ tự do ≤20), số giấy tờ (≤30), ghi chú, mục tuỳ biến `PERSON` (`useFieldDefs(ws, 'PERSON', '0')`). Thêm xong thì tới trang của người đó.
  - 409 "Person <id> already has this document": hiện thông báo kèm liên kết "Mở hồ sơ đã có" tới `/w/:ws/people/<id>` (lấy `id` từ `detail` bằng biểu thức chính quy; không lấy được thì chỉ hiện thông báo).
  - Version lệch: như form địa điểm (giữ chữ đã nhập, "Tải bản mới" giữ ô đã sửa).

- [ ] Test: `listsPeopleWithLoadMore`, `searchesByPhone`, `createsAPersonAndOpensTheirPage`, `birthDateCannotBeInTheFuture`, `duplicateDocumentLinksToTheOtherPerson`, `editSendsAvatarBack`, `personAttributesAreSent`, `versionConflictKeepsTypedValues`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the people list and person form`.

### Task 3: Trang một người

**Files:** tạo `frontend/src/features/people/{pages/PersonPage.tsx,components/PersonHeader.tsx,components/PersonInfoTab.tsx,components/PersonPlacesTab.tsx,components/DeletePersonDialog.tsx}`; tạo `frontend/src/features/occupancies/components/{OccupancyList.tsx,OccupancyRow.tsx}`; sửa `app/router.tsx` (`people/:personId`); test `PersonPage.test.tsx`.

**Interfaces:**
- Produces: `OccupancyList(props: { ws: string; list: CursorList<Occupancy>; show: 'place' | 'person'; empty: string; actions?: (row: Occupancy) => ReactNode })` — bảng dùng chung cho trang người (cột là địa điểm) và tab Người ở (cột là người); cột: tên (liên kết), quan hệ, từ ngày, tới ngày ("—" khi đang ở), trạng thái.

  - Đầu trang: họ tên, giới tính, ngày sinh (dd/MM/yyyy), `v<version>`; nút "Sửa", menu `⋯` có "Xoá". Tab trên URL: "Thông tin" (điện thoại, email, giấy tờ, ghi chú, mục tuỳ biến qua `AttributeView`), "Nơi ở" (`useOccupanciesOfPerson`, mới nhất trước), "Lịch sử" (`ComingSoonState` cho tới kế hoạch lịch sử).
  - "Nơi ở" có nút "Thêm nơi ở" mở form cư trú của Task 4 với người cố định, địa điểm chọn bằng `PlacePicker`.
  - Xoá: hỏi lại; 409 (còn cư trú hiện tại hay sắp tới) hiện nguyên văn; xong thì về `/w/:ws/people`. Cùng thứ tự như xoá địa điểm: rời trang rồi mới bỏ khỏi cache.
  - 404 hoặc `id` sai dạng: `NotFoundState`.

- [ ] Test: `showsHeaderAndInfo`, `placesTabListsEveryStayWithStatus`, `renamingAPersonRenamesTheirRows` (sửa tên → dòng cư trú ở tab Người ở của địa điểm cũng đổi khi quay lại), `deleteIsBlockedWhileTheyLiveSomewhere`, `deleteReturnsToThePeopleList`, `unknownPersonShowsNotFound`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the person page`.

### Task 4: Tab "Người ở" của một địa điểm và các thao tác cư trú

**Files:** tạo `frontend/src/features/occupancies/components/{PlaceOccupantsTab.tsx,OccupancyFormDialog.tsx,EndOccupancyDialog.tsx,DeleteOccupancyDialog.tsx,occupancyForm.ts}`, `frontend/src/features/people/components/PersonPicker.tsx`; sửa `features/places/pages/PlacePage.tsx` (tab `people` dùng `PlaceOccupantsTab`); test `PlaceOccupants.test.tsx`.

**Interfaces:**
- Produces:

```ts
export function PersonPicker(props: { ws: string; value: PersonSummary | null; onChange: (person: PersonSummary | null) => void; onCreateNew: (name: string) => void }): JSX.Element
	// ô tìm theo tên; danh sách kết quả; dòng cuối "Thêm người mới “<chữ đang gõ>”"
export function OccupancyFormDialog(props: { ws: string; open; onOpenChange } & ({ mode: 'create'; place?: PlaceDetail; person?: PersonDetail } | { mode: 'edit'; occupancy: Occupancy })): JSX.Element
```

  - Tab Người ở: ba nhóm "Đang ở", "Sắp đến", "Đã rời" (mỗi nhóm một `useOccupanciesAtPlace` với `status` tương ứng, "Đã rời" chỉ tải khi mở nhóm đó). Công tắc "Cả bên trong" (chỉ hiện khi địa điểm có chỗ con: đọc `useChildren(ws, place.id)`, cùng cache với cây và tab Bên trong; `PlaceDetail` không có `hasChildren`): thay bằng `useOccupanciesInSubtree` (chỉ người đang ở, cột thêm tên địa điểm).
  - Form thêm: người (PersonPicker, hoặc nhập họ tên để tạo hồ sơ mới ngay trong form), quan hệ (mặc định Người ở), từ ngày (mặc định hôm nay theo `todayInBusinessZone`), tới ngày (tuỳ chọn, không trước từ ngày), ghi chú (≤500). Người mới: `createPerson` rồi `createOccupancy`; hồ sơ vừa tạo được giữ trong state của form, bấm lại sau lỗi không tạo lần hai.
  - Mỗi dòng: "Chuyển đi" (hộp thoại một ô ngày, mặc định hôm nay, không trước ngày bắt đầu; ẩn với dòng đã rời), "Sửa" (quan hệ, ngày, ghi chú), "Xoá dòng nhập nhầm". 409 trùng thời gian, "has already ended", "Restore place first" hiện nguyên văn; version lệch có "Tải bản mới".

- [ ] Test: `groupsOccupantsByStatus`, `pastGroupLoadsOnlyWhenOpened`, `wholeBuildingShowsWhereEachPersonLives`, `addsAnExistingPerson` (body `{placeId, personId, relation, startDate}`), `addsANewPersonInOneGo`, `retryAfterOccupancyFailureReusesTheNewPerson`, `addingAnOccupantRefreshesEveryView`, `endsAnOccupancy` (body `{version, endDate}`; dòng sang "Đã rời"), `endDateCannotPrecedeStart`, `overlapConflictIsShown`, `deletesAMistakenRow` (`DELETE ?version=`).
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add occupants to the place page`.

### Task 5: Hoàn tất

- [ ] Liên kết "Người" ở thanh bên sáng khi đang ở `/people/...`. `frontend/README.md`: thêm hai feature vào phần cấu trúc, bảng "Sau một lệnh ghi" thêm dòng cư trú và người.
- [ ] Chạy thử trên backend thật như Task 8 của kế hoạch trước (bản build phục vụ từ WSL, Chromium của Playwright): tạo một toà nhà và một phòng, thêm một người mới vào phòng, xem "Cả bên trong" ở toà nhà, chuyển người đó đi, mở trang người đó thấy "Đã rời"; chụp màn hình. Xoá dữ liệu vừa tạo theo đúng `id`, có chốt an toàn như lần trước.
- [ ] `npm test`, `npm run build`, `npm run lint` sạch. Commit `Document people and occupancy` và push `main`.
