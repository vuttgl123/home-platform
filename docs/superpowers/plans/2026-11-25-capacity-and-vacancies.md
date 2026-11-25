# Capacity and Vacancies Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Địa điểm có sức chứa (tuỳ chọn); trang Chỗ trống để xếp người; cảnh báo vượt sức chứa trong form cư trú; dòng thời gian cư trú; "Sắp chuyển đi" trên trang Tổng quan.

**Architecture:** Backend thêm cột `reg_place.capacity` và một package `capacity` (kiểm tra sức chứa, danh sách chỗ trống) chỉ đọc. Định nghĩa "người đang ở" (L2) là một mảnh SQL duy nhất trong `PlaceRepository`, dùng chung cho chi tiết địa điểm và chỗ trống; trang Tổng quan giữ câu `CURRENT` của nó, cùng nghĩa. Frontend thêm `features/vacancies`, component `OccupancyTimeline` trong `features/people`, và sửa form địa điểm, form cư trú, trang Tổng quan.

**Tech Stack:** như `2026-11-20-ui-refresh.md`. Backend Spring Boot 4, JdbcClient, Flyway, MapStruct, IT trên MySQL thật; frontend React 19, TanStack Query, react-hook-form + zod, shadcn (`toggle-group`, `progress`, `scroll-area`, `alert` đã có), msw trong test.

**Spec:** `docs/superpowers/specs/2026-11-25-capacity-and-vacancies-design.md`.

## Global Constraints

- Lệnh Maven: `cmd.exe /c "cd /d D:\java\home-platform && set JAVA_HOME=C:\Users\vu.phamtuan\.jdks\ms-21.0.12.1&& call mvn""w.cmd -B ..." < /dev/null`. Một IT: `-pl registry-service -am verify -Dit.test=<Tên>IT -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false`. Frontend: `cmd.exe /c "npm test"`, `npm run build`, `npm run lint` trong `frontend/` (0 lỗi; 19 cảnh báo cũ trong `components/ui`, `hooks` được phép).
- Mỗi task một commit bằng git của WSL; không đưa `platform-common/pom.xml`, `pom.xml` vào commit. Push chỉ khi bạn đồng ý.
- Sức chứa: số nguyên 0 tới 9999, bỏ trống được (spec L1). Mọi quan hệ đều chiếm chỗ (L2). Ngày kết thúc vẫn là ngày còn ở.
- "Sắp chuyển đi" = 30 ngày, tính cả hôm nay và hôm nay + 30; tối đa 10 dòng (L5). Dòng thời gian tối đa 200 dòng.
- Chữ mới (đúng nguyên văn): "Sức chứa", "Sức chứa (người)", "Bỏ trống nếu không giới hạn", "Chưa đặt", "đã đầy", "vượt", "còn N chỗ", "Chỗ trống", "Xếp người", "Mọi loại", "Mọi toà", "Không còn chỗ trống nào khớp bộ lọc", "Trống, chưa đặt sức chứa", "Dòng thời gian", "Hôm nay", "Chỉ hiện 200 lần ở gần nhất", "Sắp chuyển đi", "Không ai chuyển đi trong 30 ngày tới", "còn N ngày", "hôm nay", "Xem chỗ trống", "Vẫn lưu được.".
- Màu: sức chứa còn chỗ `success`, đã đầy `warning`, vượt `danger`; thanh dòng thời gian theo `CURRENT` success, `UPCOMING` warning, `PAST` neutral (như Đợt 5).
- `components/ui` không sửa tay. Kiểu API lấy từ `src/api/schema/registry.d.ts`, sinh lại bằng `api:sync` (Task 6).

## Review Focus

1. Một người ở hai đợt liền nhau tại cùng một phòng (đợt 1 kết thúc 10/12, đợt 2 bắt đầu 11/12): đỉnh không đếm người đó hai lần, dòng thời gian vẽ hai thanh trên cùng một hàng. Test: `backToBackStaysCountOnce` (Task 2), `sameRowHoldsEveryStayOfOnePerson` (Task 9).
2. Hạ sức chứa xuống dưới số người đang ở (4 người, đổi sức chứa thành 2): lưu được, trang địa điểm ghi "4/2 · vượt 2", phòng không có trong Chỗ trống. Test: `lowerCapacityThanOccupantsIsAllowed` (Task 1), `fullOrOverfullPlacesAreNotVacant` (Task 3), `overCapacityIsShownInDanger` (Task 6).
3. Sửa một dòng cư trú đã có ở phòng đầy: không tự cảnh báo chính mình (bỏ dòng đang sửa). Test: `editedRowIsNotCountedAgainstItself` (Task 2), `editDoesNotWarnAboutItself` (Task 7).
4. Phòng chưa đặt sức chứa có người ở "khác" hay "nhân viên": không hiện là trống. Test: `anyRelationTakesTheLeafOutOfVacancies` (Task 3).
5. Cư trú bắt đầu trong tương lai và không có ngày kết thúc trên dòng thời gian: trục kéo tới ngày bắt đầu đó, thanh chạm mép phải. Test: `openFutureStayReachesTheRightEdge` (Task 9).

---

### Task 1: Sức chứa của địa điểm (backend)

**Files:**
- Create: `registry-service/src/main/resources/db/migration/V7__place_capacity.sql`, `registry-service/src/test/java/com/home_platform/registry/place/PlaceCapacityIT.java`
- Modify: `place/Place.java`, `PlaceSnapshot.java`, `PlaceCreateRequest.java`, `PlaceUpdateRequest.java`, `PlaceDetailResponse.java`, `PlaceView.java`, `PlaceMapper.java`, `PlaceService.java` (create, `applyContent`, `restoreVersion`, `view`), `PlaceRepository.java`; `export/PlaceSheet.java`; 11 chỗ gọi `new PlaceCreateRequest(` / `new PlaceUpdateRequest(` (main và test); `README.md` (mục API địa điểm); `ExportWorkspaceIT.java` (cột mới)

**Interfaces:**
- Produces: `Place.capacity()` kiểu `Integer`; `PlaceCreateRequest`/`PlaceUpdateRequest` có `@Min(0) @Max(9999) Integer capacity` ngay sau `status`; `PlaceDetailResponse` có `Integer capacity, int occupantCount` (sau `status`); `PlaceRepository.PEOPLE_LIVING_AT`: mảnh SQL `public static final String` đếm `COUNT(DISTINCT o.person_id)` của dòng cư trú còn hiệu lực ngày `:today` tại địa điểm `p.id` (người chưa xoá), dùng được trong subquery với alias `p`; `PlaceRepository.countPeopleLiving(long workspaceId, long placeId, LocalDate today): int`.

- [ ] **Step 1: Viết `PlaceCapacityIT extends OccupancyTestData`**
  - `capacityIsSavedAndVersioned`: POST `/places` có `"capacity":4` → 201, `$.capacity` 4, `$.occupantCount` 0; PUT với `"capacity":2` → `$.capacity` 2; GET `/places/{id}/versions/2` → `$.snapshot.capacity` 2; PUT `"capacity":null` → `$.capacity` không có giá trị.
  - `capacityOutOfRangeIs400`: POST với `-1` và với `10000` → 400.
  - `restoringAVersionBringsCapacityBack`: tạo với 3, sửa thành 5, POST khôi phục version 1 → `$.capacity` 3.
  - `occupantCountCountsEveryRelationLivingToday`: cùng một phòng có các dòng sau: RESIDENT đang ở; GUEST đang ở; STAFF đang ở; một dòng kết thúc hôm nay; một dòng kết thúc hôm qua; một dòng bắt đầu ngày mai; một dòng của người đã xoá → `$.occupantCount` 4. Dòng có quan hệ khác RESIDENT thì POST `/occupancies` bằng JSON có `relation`.
  - `lowerCapacityThanOccupantsIsAllowed`: 2 người đang ở, PUT `"capacity":1` → 200, `$.capacity` 1, `$.occupantCount` 2.
  - Trong `ExportWorkspaceIT`: sheet "Địa điểm" có tiêu đề "Sức chứa" ngay sau "Loại", và giá trị là số của địa điểm có sức chứa.
- [ ] **Step 2: Chạy `PlaceCapacityIT`, thấy FAIL** (không có trường `capacity`).
- [ ] **Step 3: Cài đặt**
  - Migration: `ALTER TABLE reg_place ADD COLUMN capacity SMALLINT UNSIGNED NULL AFTER status;`.
  - Thêm `Integer capacity` vào `Place` (sau `status`) và `PlaceSnapshot` (sau `status`). `applyContent` ghi `.withCapacity(request.capacity())`. `restoreVersion` truyền `old.capacity()`. `create` ghi capacity.
  - `PlaceView` có thêm `int occupantCount`. `PlaceService.view` lấy số này bằng `countPeopleLiving(..., businessDate.today())`, và `PlaceMapper.toDetail` chép sang response.
  - `PlaceSheet`: cột "Sức chứa" sau "Loại"; ô trống khi `null`.
  - Sửa 11 chỗ gọi constructor (truyền `null`).
  - README: thêm `capacity`, `occupantCount` vào mô tả API địa điểm.
- [ ] **Step 4: Chạy `PlaceCapacityIT`, `PlaceVersionIT`, `ExportWorkspaceIT`, `RegistrySchemaIT` → PASS**; sau đó `mvnw -B test` → BUILD SUCCESS.
- [ ] **Step 5: Commit** "Give places an optional capacity".

### Task 2: Kiểm tra sức chứa (backend, L3)

**Files:**
- Create: `registry/capacity/CapacityController.java`, `CapacityService.java`, `CapacityRepository.java`, `CapacityCheckResponse.java`; `src/test/.../capacity/CapacityCheckIT.java`, `src/test/.../capacity/PeakTests.java`
- Modify: `README.md`

**Interfaces:**
- Consumes: `Place.capacity()`, `PlaceService.get(long, long)` (404 khi không có hay đã xoá).
- Produces: `GET /places/{id}/capacity-check?startDate=YYYY-MM-DD&endDate=&excludeOccupancyId=` → `CapacityCheckResponse(Integer capacity, int peak, LocalDate peakDate)`. `CapacityService.Peak peak(List<Stay> stays, LocalDate from, LocalDate to)` (thuần Java, `to` null = mở); `record Stay(long personId, LocalDate start, LocalDate end)`.

- [ ] **Step 1: Viết test**
  - `PeakTests` (unit):
    - `emptyRangeHasNoPeak`: không có dòng nào → peak 0, ngày `null`;
    - `overlappingStaysPeakOnTheirCommonDay`: [1–10], [5–20], [8–9] trong khoảng [1–31] → peak 3, ngày 8;
    - `backToBackStaysCountOnce`: cùng người [1–10], [11–20] → peak 1;
    - `peakOutsideTheRangeIsIgnored`: [1–3] ×2, khoảng [5–9] có một dòng [6–7] → peak 1, ngày 6;
    - `openRangeSeesLaterStays`: khoảng [1–∞), dòng [100–∞) và [150–200] → peak 2, ngày 150.
  - `CapacityCheckIT extends OccupancyTestData`:
    - `reportsCapacityAndPeak`: phòng sức chứa 2, hai người đang ở → `capacity` 2, `peak` 2, `peakDate` hôm nay (khoảng từ hôm nay);
    - `editedRowIsNotCountedAgainstItself`: gửi `excludeOccupancyId` của một trong hai → `peak` 1;
    - `noCapacityStillReportsPeak`: `capacity` null;
    - `deletedPlaceIs404`;
    - `endBeforeStartIs400`.
- [ ] **Step 2: Chạy, thấy FAIL.**
- [ ] **Step 3: Cài đặt.** Repository lấy các dòng `(person_id, start_date, end_date)` của địa điểm: chưa xoá, người chưa xoá, giao với khoảng, bỏ `excludeOccupancyId`. Đi theo index `idx_reg_occupancy_place`. `peak` quét các ngày bắt đầu của dòng (và ngày `from`) nằm trong khoảng; tại mỗi ngày đó đếm số người khác nhau có dòng phủ ngày ấy; lấy ngày sớm nhất có số lớn nhất. Đầu vào nhỏ nên không cần sweep tối ưu.
- [ ] **Step 4: Chạy `PeakTests`, `CapacityCheckIT` → PASS.**
- [ ] **Step 5: Commit** "Check a place's capacity over a range of dates".

### Task 3: Danh sách chỗ trống (backend, L4)

**Files:**
- Create: `capacity/VacancyResponse.java`, `src/test/.../capacity/VacancyIT.java`
- Modify: `CapacityController.java`, `CapacityService.java`, `CapacityRepository.java`, `README.md`

**Interfaces:**
- Consumes: `PlaceRepository.PEOPLE_LIVING_AT`, `PlaceRepository.refs`, keyset builder của `WorkspaceRepository` (như `PlaceRepository.byType`).
- Produces: `GET /places/vacancies?placeTypeId=&rootId=&cursor=&limit=` → `CursorPage<VacancyResponse>`; `VacancyResponse(long placeId, String code, String name, long placeTypeId, String placeTypeName, PlaceStatus status, List<PlaceRef> ancestors, Integer capacity, int occupantCount, Integer free)`. Mapping `/places/vacancies` phải được chọn trước `/places/{id}`.

- [ ] **Step 1: Viết `VacancyIT extends OccupancyTestData`**
  - `placesWithRoomAreListed`: phòng sức chứa 3 có 1 người → một dòng: `free` 2, `occupantCount` 1, `ancestors` đúng thứ tự từ gốc.
  - `fullOrOverfullPlacesAreNotVacant`: sức chứa 1 có 1 người; sức chứa 1 có 2 người; sức chứa 0 → không có dòng nào.
  - `emptyLeafWithoutCapacityIsVacant`: phòng lá không sức chứa, không người → có dòng, `capacity` null, `free` null; tầng chứa nó (có con) → không có dòng.
  - `anyRelationTakesTheLeafOutOfVacancies`: phòng lá không sức chứa, có một STAFF đang ở → không có dòng; người đã chuyển đi hôm qua → có dòng.
  - `filtersByTypeAndBuilding`: hai toà, mỗi toà một phòng trống; lọc `rootId` toà A → chỉ phòng của toà A; lọc `placeTypeId` → chỉ loại đó.
  - `pagesByName`: 3 phòng trống, `limit=2` → 2 dòng theo tên, có `nextCursor`; trang sau → dòng thứ 3.
  - `deletedPlacesAreHidden`.
- [ ] **Step 2: Chạy, thấy FAIL.**
- [ ] **Step 3: Cài đặt** bằng keyset `orderBy("name")` với điều kiện:
  `(capacity IS NOT NULL AND (<PEOPLE_LIVING_AT>) < capacity) OR (capacity IS NULL AND NOT EXISTS (con chưa xoá) AND (<PEOPLE_LIVING_AT>) = 0)`, thêm `place_type_id = :placeTypeId` và `path LIKE :prefix AND path <> :rootPath` khi có. Số người đang ở của cả trang lấy bằng một truy vấn theo lô (`IN (:ids)`). Tên loại và `ancestors` cũng lấy theo lô.
- [ ] **Step 4: Chạy `VacancyIT` → PASS.**
- [ ] **Step 5: Commit** "List places with room for more people".

### Task 4: Dòng thời gian cư trú (backend)

**Files:**
- Create: `occupancy/OccupancyTimelineResponse.java`, `src/test/.../occupancy/OccupancyTimelineIT.java`
- Modify: `OccupancyController.java`, `OccupancyService.java`, `OccupancyRepository.java`, `README.md`

**Interfaces:**
- Produces: `GET /occupancies/timeline?placeId=` hoặc `?personId=` → `OccupancyTimelineResponse(List<OccupancyResponse> rows, boolean truncated)`; `OccupancyService.timelineAtPlace(long ws, long placeId)`, `timelineOfPerson(long ws, long personId)`, cả hai trả `Timeline(List<OccupancyView> rows, boolean truncated)`. `TIMELINE_LIMIT = 200`.

- [ ] **Step 1: Viết `OccupancyTimelineIT extends OccupancyTestData`**
  - `placeTimelineHasEveryStatus`: một dòng đã đi, một đang ở, một sắp đến → 3 dòng, `status` lần lượt PAST/CURRENT/UPCOMING, mới nhất (theo ngày bắt đầu) trước, `truncated` false.
  - `personTimelineSpansPlaces`: hai địa điểm → 2 dòng.
  - `deletedRowsAreLeftOut`.
  - `exactlyOneFilter`: không có tham số, hoặc có cả hai → 400.
  - `truncatesAfterTheLimit`: gọi service với limit nhỏ qua một hàm package-private `timelineAtPlace(ws, placeId, limit)`. 3 dòng, limit 2 → 2 dòng mới nhất, `truncated` true.
- [ ] **Step 2: Chạy, thấy FAIL.**
- [ ] **Step 3: Cài đặt.** Lấy `limit + 1` dòng sắp theo `start_date DESC, id DESC`; có dòng thứ `limit + 1` thì `truncated`. Gắn tên địa điểm, tên người và trạng thái bằng `views(...)` có sẵn. Mapping `/occupancies/timeline` phải được chọn trước `/{id}`.
- [ ] **Step 4: Chạy → PASS.**
- [ ] **Step 5: Commit** "Return a whole occupancy timeline for a place or a person".

### Task 5: Sắp chuyển đi trên tổng quan (backend, L5)

**Files:**
- Create: `overview/EndingSoon.java`, `overview/EndingSoonRow.java`
- Modify: `OverviewRepository.java`, `OverviewService.java`, `OverviewResponse.java`, `OverviewIT.java`, `README.md`

**Interfaces:**
- Produces: `OverviewResponse.endingSoon` kiểu `EndingSoon(int total, List<EndingSoonRow> rows)`; `EndingSoonRow(long occupancyId, long personId, String personName, long placeId, String placeName, LocalDate endDate)`.

- [ ] **Step 1: Thêm vào `OverviewIT`**
  - `endingSoonCoversTheNext30Days`: dòng kết thúc hôm nay, hôm nay + 30, hôm nay + 31, hôm qua, không có ngày kết thúc, và một dòng sắp đến kết thúc trong 10 ngày → `total` 2, `rows` sắp theo `endDate` (hôm nay trước), có tên người và địa điểm.
  - `endingSoonKeepsTenRows`: 12 dòng → `total` 12, 10 dòng.
  - `endingSoonSkipsDeletedPeopleAndPlaces`.
- [ ] **Step 2: Chạy `OverviewIT`, thấy FAIL.**
- [ ] **Step 3: Cài đặt.** Một câu `COUNT` và một câu `LIMIT 10` có join tên, dùng điều kiện `CURRENT` sẵn có và thêm `o.end_date BETWEEN :today AND :last`.
- [ ] **Step 4: Chạy `OverviewIT` → PASS**; `mvnw -B test` → BUILD SUCCESS.
- [ ] **Step 5: Commit** "List who moves out in the next 30 days on the overview".

### Task 6: Đồng bộ kiểu API, sức chứa trên frontend

**Files:**
- Modify: `frontend/src/api/schema/registry.json`, `registry.d.ts` (sinh lại); `features/places/types.ts`, `components/placeForm.ts`, `PlaceFormDialog.tsx`, `PlaceOverviewTab.tsx`; `features/activity/labels.ts` (nhãn trường `capacity: 'Sức chứa'`); `features/print/pages/PlacePrintPage.tsx`; `test/fixtures.ts`
- Create: `features/places/components/CapacityLine.tsx`; test trong `features/places/PlaceForm.test.tsx`, `PlacePage.test.tsx`

**Interfaces:**
- Produces: `PlaceDetail` có `capacity?: number`, `occupantCount: number`; `<CapacityLine capacity={number | undefined} occupants={number} />` hiện chữ và `ToneBadge` theo bảng dưới (Task 8 dùng lại).

| Trường hợp | Chữ | Tone |
|---|---|---|
| `capacity` undefined | "Chưa đặt · đang ở {n}" | không có nhãn |
| `n < capacity` | "{n}/{capacity} người · còn {capacity−n} chỗ" | success |
| `n == capacity` | "{n}/{capacity} · đã đầy" | warning |
| `n > capacity` | "{n}/{capacity} · vượt {n−capacity}" | danger |

- [ ] **Step 1: Sinh lại kiểu.** Chạy registry mới trên 18082 (bộ preview của `.superpowers/preview`), rồi `cmd.exe /c "set HP_REGISTRY_PORT=18082&& node scripts/api-sync.mjs --only=registry"`. Diff phải có `capacity`, `occupantCount`, `CapacityCheckResponse`, `VacancyResponse`, `OccupancyTimelineResponse`, `EndingSoon`.
- [ ] **Step 2: Viết test**
  - `capacityIsSentAndEmptyMeansNone` (PlaceForm): nhập 4 → body PUT có `capacity: 4`; xoá trống → `capacity: null`; nhập `-1` hay `1.5` → lỗi "Nhập số nguyên từ 0 đến 9999", không gửi.
  - `capacityLineShowsEachCase` (PlacePage): đủ bốn dòng của bảng, kèm `data-tone`.
  - `overCapacityIsShownInDanger` (PlacePage): `occupantCount` 4, `capacity` 2 → "4/2 · vượt 2", `data-tone="danger"`.
- [ ] **Step 3: Chạy test, thấy FAIL; cài đặt.** Ô "Sức chứa (người)" là `TextField` kiểu `number` (`inputMode="numeric"`), có gợi ý "Bỏ trống nếu không giới hạn". Zod nhận chuỗi rỗng hoặc số nguyên 0..9999. Thẻ Thông tin của tab Tổng quan có dòng "Sức chứa" dùng `CapacityLine`. Trang in có `Field label="Sức chứa"` với chữ thường, không `ToneBadge`.
- [ ] **Step 4: `npm test`, `build`, `lint` → xanh.**
- [ ] **Step 5: Commit** "Edit and show a place's capacity".

### Task 7: Cảnh báo vượt sức chứa trong form cư trú

**Files:**
- Modify: `features/people/api.ts` (`checkCapacity`), `queries.ts` (`useCapacityCheck`), `components/OccupancyFormDialog.tsx`
- Create: `features/people/components/CapacityWarning.tsx`; test `features/people/OccupancyCapacity.test.tsx`

**Interfaces:**
- Produces: `checkCapacity(ws, placeId, { startDate, endDate?, excludeOccupancyId? }): Promise<CapacityCheck>`; `useCapacityCheck(ws, placeId | null, range | null)` với query key `['places', ws, 'capacity-check', placeId, startDate, endDate, excludeId]`, chờ 300 ms sau lần đổi cuối và chỉ chạy khi có `placeId` và `startDate`.

- [ ] **Step 1: Viết test** (msw cho `/api/registry/places/:id/capacity-check`):
  - `warnsWhenThePeakIsFull`: `{capacity: 2, peak: 2, peakDate: '2026-12-01'}` → `role="alert"` chứa "P101 chứa 2 người; ngày 01/12/2026 đã có 2 người. Vẫn lưu được.", nút Lưu vẫn bật.
  - `staysQuietBelowCapacityOrWithoutOne`: `peak` 1 trên `capacity` 2 → không có alert; `capacity` null → không có alert.
  - `editDoesNotWarnAboutItself`: chế độ sửa gửi `excludeOccupancyId` bằng id của dòng đang sửa (kiểm tra trên URL request).
  - `savesDespiteTheWarning`: có cảnh báo, bấm Lưu → POST `/occupancies` vẫn được gửi.
- [ ] **Step 2: FAIL; cài đặt.** Địa điểm lấy từ `fixedPlace`, từ `place` đã chọn, hoặc từ `editing.placeId`. Ngày lấy từ `form.watch`. `CapacityWarning` là `Alert` tone warning, đặt trên nút Lưu.
- [ ] **Step 3: `npm test`, `build`, `lint` → xanh.**
- [ ] **Step 4: Commit** "Warn before going over a place's capacity".

### Task 8: Trang Chỗ trống

**Files:**
- Create: `features/vacancies/{api.ts,queries.ts,types.ts,index.ts}`, `features/vacancies/pages/VacanciesPage.tsx`, `features/vacancies/components/{VacancyFilters.tsx,VacancyList.tsx}`, `features/vacancies/VacanciesPage.test.tsx`
- Modify: `app/router.tsx` (route `vacancies`), `app/layout/AppLayout.tsx` (mục "Chỗ trống", icon `BedDoubleIcon`, ngay sau "Người"), `features/people/queries.ts` (`refreshOccupancies` làm mới thêm `['vacancies', ws]`)

**Interfaces:**
- Consumes: `CapacityLine` (Task 6) dùng cho phần chữ; `OccupancyFormDialog` với `mode="create"` và `place={{ id, name }}`; danh sách loại địa điểm đang dùng từ `features/catalog`; các địa điểm cấp cao nhất từ `features/places`.
- Produces: `vacancyKeys.list(ws, filters)` = `['vacancies', ws, filters]`; bộ lọc trên URL: `?type=<id>&root=<id>`.

- [ ] **Step 1: Viết test**
  - `listsPlacesWithRoom`: 2 dòng; dòng có sức chứa có `progress` và chữ "1/4 người · còn 3 chỗ"; dòng không có sức chứa ghi "Trống, chưa đặt sức chứa"; tên mở `/w/:ws/places/:id`; có đường dẫn "Toà A › Tầng 1".
  - `typeToggleFiltersAndLandsOnTheUrl`: bấm loại "Phòng" trong `toggle-group` → request có `placeTypeId`, URL có `type=`; bấm "Mọi loại" thì bỏ cả hai.
  - `buildingSelectFilters`: chọn toà → request có `rootId`.
  - `seatSomeoneOpensTheFormForThatPlace`: bấm "Xếp người" → hộp thoại "Thêm người ở" có "Tại P101"; lưu xong thì danh sách gọi lại.
  - `emptyStateWhenNothingMatches`: "Không còn chỗ trống nào khớp bộ lọc".
  - `loadsMore`: có `nextCursor` thì có nút "Tải thêm".
- [ ] **Step 2: FAIL; cài đặt.** Bố cục giống `ItemsPage`: tiêu đề "Chỗ trống" và mô tả "Những chỗ còn xếp thêm người được hôm nay.", rồi bộ lọc, rồi bảng (thành thẻ trên điện thoại như `ItemList`). Cột: Tên, Nằm trong, Loại, Sức chứa (`progress` + chữ), nút "Xếp người".
- [ ] **Step 3: `npm test`, `build`, `lint` → xanh.**
- [ ] **Step 4: Commit** "Add a vacancies page to seat people where there is room".

### Task 9: Dòng thời gian cư trú (frontend)

**Files:**
- Create: `features/people/components/OccupancyTimeline.tsx`, `features/people/timeline.ts` (hàm thuần), `features/people/timeline.test.ts`, `features/people/OccupancyTimeline.test.tsx`
- Modify: `api.ts` (`getOccupancyTimeline`), `queries.ts` (`useOccupancyTimeline`, key `['occupancies', ws, 'timeline', 'place' | 'person', id]`, nằm dưới `occupancyKeys.all` để được làm mới cùng), `PlaceOccupantsTab.tsx`, `pages/PersonPage.tsx` (trên danh sách nơi ở)

**Interfaces:**
- Produces: `timelineLayout(rows: Occupancy[], groupBy: 'person' | 'place', today: string): { start: string; end: string; months: { label: string; left: number }[]; todayLeft: number; lanes: { key: string; label: string; href: string; bars: { id: string; left: number; width: number; open: boolean; status: OccupancyStatus; title: string }[] }[] }`. Vị trí `left` và `width` tính theo %, từ 0 tới 100.

- [ ] **Step 1: Viết test**
  - `timeline.test.ts`:
    - `axisSpansFirstStartToLastEndOrToday`: trục bắt đầu trước ngày bắt đầu sớm nhất 7 ngày và kết thúc sau `max(hôm nay, ngày kết thúc muộn nhất)` 7 ngày;
    - `sameRowHoldsEveryStayOfOnePerson`: 2 đợt của một người → 1 hàng, 2 thanh;
    - `openFutureStayReachesTheRightEdge`: dòng bắt đầu sau hôm nay 60 ngày, không có ngày kết thúc → trục kéo tới ngày đó + 7 ngày, `left + width` = 100, `open` true;
    - `monthTicksAreLabelledInVietnamese`: nhãn "T12/2026", "T1/2027".
  - `OccupancyTimeline.test.tsx`:
    - `drawsLanesBarsAndToday`: số hàng, `data-status` của thanh, vạch có nhãn "Hôm nay";
    - `hiddenWhenThereAreNoRows`;
    - `notesTruncation`: "Chỉ hiện 200 lần ở gần nhất".
- [ ] **Step 2: FAIL; cài đặt.** Đặt trong `Card` có tiêu đề "Dòng thời gian", thân là `ScrollArea` ngang với `min-w-[640px]`. Mỗi hàng gồm nhãn (liên kết tới người hay địa điểm) và rãnh `relative`; mỗi thanh là `absolute` với `style={{ left: %, width: % }}` và `WithTooltip` ghi "{quan hệ} · {từ} → {đến | 'nay'}". Thanh mở dùng `bg-gradient-to-r` để mờ dần ở đầu phải.
- [ ] **Step 3: `npm test`, `build`, `lint` → xanh.**
- [ ] **Step 4: Commit** "Draw an occupancy timeline for places and people".

### Task 10: Tổng quan: Sắp chuyển đi, liên kết Chỗ trống

**Files:**
- Modify: `features/overview/types.ts`, `components/OverviewPage.tsx`, `OverviewPage.test.tsx`, `test/fixtures.ts`

- [ ] **Step 1: Viết test**
  - `endingSoonListsWhoIsLeaving`: 2 dòng; ngày kết thúc là hôm nay thì ghi "hôm nay", sau 5 ngày thì ghi "còn 5 ngày"; tên người và địa điểm là liên kết.
  - `endingSoonShowsTotalAboveTen`: `total` 12 với 10 dòng → có "12".
  - `endingSoonEmpty`: "Không ai chuyển đi trong 30 ngày tới".
  - `olderRegistryHidesEndingSoon`: không có `endingSoon` thì không có thẻ.
  - `linksToVacancies`: liên kết "Xem chỗ trống" trỏ tới `/w/:ws/vacancies`.
- [ ] **Step 2: FAIL; cài đặt.** Thẻ nằm ở cột phải, dưới "Đồ đạc theo trạng thái". Liên kết "Xem chỗ trống" đặt cạnh tiêu đề "Chỗ ở theo loại", cùng kiểu với "Xem tất cả".
- [ ] **Step 3: `npm test`, `build`, `lint` → xanh.**
- [ ] **Step 4: Commit** "Show who moves out soon on the overview".

### Task 11: Xem bằng mắt, tài liệu, kiểm tra cuối

**Files:**
- Modify: `README.md` (mục tính năng), `frontend/README.md` nếu có bảng trang; spec: đổi trạng thái thành "đã xây"

- [ ] **Step 1:** Dựng bộ preview (18081/18082/15173/9222). Đặt sức chứa cho vài phòng trong không gian -9001 qua API, rồi chụp ở khổ 1920 và 390 trên nền sáng, và ở khổ 1440 trên nền tối: Tổng quan, Chỗ trống (có lọc), một phòng (tab Tổng quan và Người ở), một người, hộp thoại có cảnh báo. Không dùng `"full": true` cho trang có biểu đồ. Sửa những chỗ sai.
- [ ] **Step 2:** `mvnw -B verify` cho `registry-service` (đủ IT), rồi `mvnw -B test` cả repo, `npm test`, `build`, `lint`. Tất cả phải xanh.
- [ ] **Step 3:** Tắt bộ preview, xoá `chrome-profile`. Commit "Document capacity, vacancies and timelines".
