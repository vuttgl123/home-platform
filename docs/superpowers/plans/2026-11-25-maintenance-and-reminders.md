# Maintenance and Reminders Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Nhật ký bảo trì cho món đồ; ngày hết bảo hành và chu kỳ bảo trì; nhắc hạn cho mục tuỳ biến kiểu ngày; trang Việc cần làm có số trên thanh bên; chuông báo việc mới.

**Architecture:** Backend thêm hai gói trong registry. Gói `maintenance` gồm bảng `reg_maintenance`, có version `MAINTENANCE`. Gói `todo` chỉ đọc: tính danh sách việc khi đọc từ năm nguồn, và có thêm bảng `reg_todo_seen` giữ mốc `seenAt` của mỗi người. "Lúc một giá trị được đặt" lấy từ bảng version, bằng một hàm mới `EntityHistory.valueSetAt`. Hạn bảo trì kế tiếp là một hàm thuần `ServiceSchedule.nextServiceOn` trong gói `item`, dùng chung cho trang món đồ và cho việc cần làm. Frontend thêm `features/maintenance` (tab, gắn vào trang món đồ qua slot như trang địa điểm) và `features/todos` (trang, chuông, số trên thanh bên); tóm tắt việc dùng `meta.refreshAfterWrites` như trang Tổng quan.

**Tech Stack:** như `2026-11-25-capacity-and-vacancies.md`. Spring Boot 4, JdbcClient, Flyway, MapStruct, IT trên MySQL thật; React 19, TanStack Query, react-hook-form + zod, shadcn (`toggle-group`, `popover`, `switch`, `sidebar` có `SidebarMenuBadge`), msw.

**Spec:** `docs/superpowers/specs/2026-11-25-maintenance-and-reminders-design.md`.

## Global Constraints

- Lệnh Maven: `cmd.exe /c "cd /d D:\java\home-platform && set JAVA_HOME=C:\Users\vu.phamtuan\.jdks\ms-21.0.12.1&& call mvn""w.cmd -B ..." < /dev/null`. Một IT: `-pl registry-service -am verify -Dit.test=<Tên>IT -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false` (nhiều IT cách nhau bằng dấu phẩy). Frontend: `cmd.exe /c "npm test"`, `npm run build`, `npm run lint` trong `frontend/` (0 lỗi; các cảnh báo cũ trong `components/ui`, `hooks` được phép). Không chạy `npx prettier`.
- Mỗi task một commit bằng git của WSL, trên nhánh `batch-8-maintenance`. Không đưa `platform-common/pom.xml`, `pom.xml` vào commit. Push chỉ khi bạn đồng ý. Dừng registry preview trước khi chạy `verify`.
- Giới hạn trường (L1, L3, L4):
  - `work` tối đa 500 ký tự, bắt buộc; `doneBy` tối đa 150; `note` tối đa 1000; `doneOn` không sau hôm nay;
  - `serviceEveryMonths` từ 1 tới 120; `remindDays` từ 1 tới 365, chỉ cho kiểu `DATE`; ô nhắc trên giao diện mặc định 30.
- Ghi chú version khi sửa xong: `"Đã sửa: " + work`, cắt còn 255 ký tự (L2).
- Khung (L5): `WARRANTY` 30 ngày trước hạn; `SERVICE` 7 ngày trước hạn, quá hạn vẫn hiện; `MOVE_OUT` như L5 Đợt 6 (30 ngày); `CUSTOM_DATE` từ `ngày − remindDays` tới `ngày + 30`. Thứ tự `kind`: `BROKEN`, `WARRANTY`, `SERVICE`, `MOVE_OUT`, `CUSTOM_DATE`.
- Chuông: tối đa 10 việc trong `latest`; chấm số "9+" khi nhiều hơn 9; tóm tắt tải lại mỗi 5 phút, khi quay lại tab trình duyệt, và sau mọi lệnh ghi. Hoàn tác sau khi xoá dòng nhật ký: 8 giây (`deletedToast`).
- Chữ mới (đúng nguyên văn):
  - tab, hộp thoại: "Bảo trì", "Ghi bảo trì", "Sửa dòng bảo trì", "Sửa chữa", "Ngày làm", "Việc đã làm", "Người làm", "Ghi chú", "Đã sửa xong, chuyển về Đang dùng", "Chưa có lần sửa hay bảo trì nào";
  - form món đồ: "Hết bảo hành", "Bảo trì mỗi (tháng)", "Bảo hành tới {ngày} · còn N ngày", "Bảo hành tới {ngày} · hết hôm nay", "Bảo hành tới {ngày} · đã hết", "Bảo trì mỗi N tháng · lần tới {ngày}";
  - mục tuỳ biến: "Nhắc trong Việc cần làm", "Nhắc trước (ngày)";
  - trang việc: "Việc cần làm", "Tất cả", "Hỏng", "Bảo hành", "Bảo trì", "Chuyển đi", "Hạn khác", "quá hạn N ngày", "hôm nay", "còn N ngày", "hỏng từ {dd/MM}", "Không có việc gì cần làm";
  - chuông: "Thông báo", "Chưa có gì mới", "Xem tất cả việc cần làm".
- Màu (`ToneBadge`): quá hạn `danger`; trong 7 ngày `warning`; còn lại `neutral`. Bảo hành đã hết `neutral`. Loại Sửa chữa `warning`, loại Bảo trì `info`. Thẻ Thông tin của món: bảo hành hết hôm nay hay còn ≤ 30 ngày `warning`, xa hơn `neutral`; hạn bảo trì kế tiếp ≤ hôm nay `warning`.
- `components/ui` không sửa tay. Kiểu API lấy từ `src/api/schema/registry.d.ts`, sinh lại bằng `api:sync` (Task 7).

## Review Focus

1. Đổi tên một món đang trong khung bảo hành sau khi đã mở chuông: việc đó không thành "mới" lại. Test: `renamingDoesNotMakeAWarrantyNew` (Task 6).
2. Món tạo thẳng với trạng thái Hỏng, rồi sửa tên: ngày hỏng là ngày tạo, không phải ngày sửa tên. Test: `brokenSinceIsWhenStatusBecameBroken` (Task 5).
3. Hai người cùng sửa: người A mở món (version 3), người B ghi bảo trì "đã sửa xong" (món lên version 4); A lưu form thì nhận 409, không mất lần sửa của B. Và B gửi `itemVersion` cũ thì không có dòng bảo trì nào được ghi. Test: `staleItemVersionWritesNothing` (Task 2).
4. Bảo trì mỗi tháng, ghi ngày 31/01: hạn kế tiếp là 28/02 (năm thường), không tràn sang tháng 3. Test: `endOfMonthClampsToTheLastDay` (Task 1).
5. Một người có hai mục ngày có nhắc (CCCD, hộ chiếu) cùng trong khung: thành hai việc riêng, cùng mở trang người đó, `key` khác nhau, không trùng hay mất khi phân trang. Test: `twoDateFieldsOnOnePersonAreTwoTodos` (Task 5).

---

### Task 1: Bảo hành và chu kỳ bảo trì của đồ (backend, L3)

**Files:**
- Create: `registry-service/src/main/resources/db/migration/V8__maintenance_and_reminders.sql`; `item/ServiceSchedule.java`; test `item/ServiceScheduleTests.java`, `item/ItemWarrantyIT.java`
- Modify: `item/Item.java`, `ItemSnapshot.java`, `ItemRequest.java`, `ItemUpdateRequest.java`, `ItemDetailResponse.java`, `ItemView.java`, `ItemService.java` (`create`, `applyContent`, `restoreVersion`, `views`), `ItemRepository.java`; `export/ItemSheet.java`, `ExportItemRow.java`, `ExportRepository.java`; các chỗ gọi `new ItemRequest(` / `new ItemUpdateRequest(` / `new ItemView(` (main và test); `ExportWorkspaceIT.java`; `README.md` (API đồ đạc)

**Interfaces:**
- Produces:
  - `Item.warrantyUntil()` kiểu `LocalDate`, `Item.serviceEveryMonths()` kiểu `Integer`, đặt sau `placedAt`. Cùng hai trường đó, cùng vị trí, trong `ItemSnapshot`, `ItemRequest`, `ItemUpdateRequest` (`@Min(1) @Max(120) Integer serviceEveryMonths`) và `ItemDetailResponse`. `ItemDetailResponse` có thêm `LocalDate nextServiceOn` ngay sau hai trường đó.
  - `ItemView(Item item, String placeName, String categoryName, String ownerName, LocalDate nextServiceOn)`.
  - `ServiceSchedule.nextServiceOn(LocalDate lastServiceOn, LocalDate placedAt, LocalDate createdOn, Integer everyMonths): LocalDate` (`public static`; `everyMonths` null thì trả null).
  - `ItemRepository.lastServiceOn(long workspaceId, Collection<Long> itemIds): Map<Long, LocalDate>` đọc `MAX(done_on)` của các dòng `reg_maintenance` chưa xoá, `kind = 'SERVICE'`.
- Migration tạo đủ cho cả đợt: `reg_maintenance`, hai cột của `reg_item` cùng index `(workspace_id, warranty_until)`, `reg_field_def.remind_days`, `reg_todo_seen`. Định nghĩa cột theo spec mục 4.2. `kind` là `VARCHAR(10)`, `work` `VARCHAR(500)`, `done_by` `VARCHAR(150)`, `note` `VARCHAR(1000)`.

- [ ] **Step 1: Viết test**
  - `ServiceScheduleTests`:
    - `noIntervalHasNoDate`: `everyMonths` null → null;
    - `countsFromTheLastService`: last 2026-03-10, 6 tháng → 2026-09-10;
    - `fallsBackToPlacedAtThenCreation`: không có last → từ `placedAt`; không có cả hai → từ `createdOn`;
    - `endOfMonthClampsToTheLastDay`: last 2026-01-31, 1 tháng → 2026-02-28.
  - `ItemWarrantyIT extends ItemTestData`:
    - `warrantyAndIntervalAreSavedAndVersioned`: POST `/items` có `"warrantyUntil":"2027-03-12","serviceEveryMonths":6` → 201, hai trường có trong response, `$.nextServiceOn` = `placedAt` + 6 tháng (khi gửi `placedAt`) hoặc hôm nay + 6 tháng; PUT đổi thành 12 → GET `/items/{id}/versions/2` có `$.snapshot.serviceEveryMonths` 12.
    - `intervalOutOfRangeIs400`: 0 và 121 → 400.
    - `restoringAVersionBringsThemBack`: tạo có bảo hành, PUT bỏ trống, khôi phục version 1 → `warrantyUntil` trở lại.
  - `ExportWorkspaceIT`: sheet đồ đạc có tiêu đề "Hết bảo hành" và "Bảo trì (tháng)", cùng giá trị của một món có đặt hai trường.
- [ ] **Step 2: Chạy, thấy FAIL** (không có trường).
- [ ] **Step 3: Cài đặt.** `views` gọi `lastServiceOn` một lần cho cả lô. `createdOn` = ngày của `createdAt` theo `BusinessDate` zone. `restoreVersion` truyền hai trường cũ qua `ItemUpdateRequest` (vì thế quên truyền thì không biên dịch được). Sheet Excel để ô trống khi `null`. Các chỗ gọi constructor trong test truyền `null`.
- [ ] **Step 4: Chạy `ServiceScheduleTests`, `ItemWarrantyIT`, `ItemVersionIT`, `ExportWorkspaceIT`, `RegistrySchemaIT` → PASS**, rồi `mvnw -B test` → BUILD SUCCESS.
- [ ] **Step 5: Commit** "Give items a warranty end and a service interval".

### Task 2: Nhật ký bảo trì (backend, L1, L2)

**Files:**
- Create: `maintenance/Maintenance.java`, `MaintenanceKind.java`, `MaintenanceSnapshot.java`, `MaintenanceRepository.java`, `MaintenanceService.java`, `MaintenanceController.java`, `MaintenanceMapper.java`, `MaintenanceCreateRequest.java`, `MaintenanceUpdateRequest.java`, `MaintenanceVersionRequest.java`, `MaintenanceResponse.java`; test `maintenance/MaintenanceIT.java`
- Modify: `item/ItemService.java` (`markFixed`), `README.md`

**Interfaces:**
- Consumes: `ItemService.get`, `ItemRepository.findByIdForUpdate`, `EntityHistory.record`, `BusinessDate.today()`, `ItemRepository.lastServiceOn` (Task 1).
- Produces:
  - `Maintenance(long workspaceId, long id, long itemId, MaintenanceKind kind, LocalDate doneOn, String work, String doneBy, String note, Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version, Instant deletedAt)`, với `enum MaintenanceKind { REPAIR, SERVICE }` và `MaintenanceService.ENTITY_TYPE = "MAINTENANCE"`.
  - `MaintenanceResponse(long id, long itemId, MaintenanceKind kind, LocalDate doneOn, String work, String doneBy, String note, Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version)`.
  - `MaintenanceCreateRequest(@NotNull Long itemId, @NotNull MaintenanceKind kind, @NotNull LocalDate doneOn, @NotBlank @Size(max = 500) String work, @Size(max = 150) String doneBy, @Size(max = 1000) String note, Boolean markFixed, Integer itemVersion)`, và `MaintenanceUpdateRequest(@NotNull Integer version, …)` với các trường nội dung y như vậy (trừ `itemId`, `markFixed`, `itemVersion`). Chữ được cắt khoảng trắng bằng `Text.trimToNull`.
  - Các đường: `GET /maintenance?itemId=`, `POST /maintenance` (201), `PUT /maintenance/{id}`, `DELETE /maintenance/{id}?version=` (204), `POST /maintenance/{id}/restore` (thân `{version}`).
  - `ItemService.markFixed(WorkspaceContext ctx, long itemId, int version, String work): Item`. Hàm đòi món chưa gỡ và đang `BROKEN` (sai thì 400), lệch `version` thì 409. Nó đổi trạng thái sang `IN_USE` và ghi version `UPDATE` của món.
  - `MaintenanceService.names(long workspaceId, List<Long> ids): Map<Long, String>` ("<tên món>: <work>", kể cả dòng đã xoá) và `itemIds(long workspaceId, List<Long> ids): Map<Long, Long>`, dùng cho Task 4.

- [ ] **Step 1: Viết `MaintenanceIT extends ItemTestData`**
  - `logsAndListsNewestFirst`: ba dòng có `doneOn` khác nhau → GET trả theo `doneOn` giảm dần; mỗi lần tạo có một version `MAINTENANCE` `CREATE`.
  - `validatesFields`: thiếu `work` → 400; `work` 501 ký tự → 400; `doneOn` ngày mai → 400; `itemId` của món đã xoá → 404.
  - `removedItemStillTakesEntries`: món đã gỡ → 201.
  - `markFixedPutsABrokenItemBackInUse`: món `BROKEN` (version v), POST `REPAIR` có `markFixed:true,itemVersion:v` → GET món có `status` `IN_USE`, `version` v+1; version mới nhất của món có note bắt đầu bằng "Đã sửa: ".
  - `staleItemVersionWritesNothing`: `itemVersion` v−1 → 409; GET `/maintenance?itemId=` rỗng; món vẫn `BROKEN`.
  - `markFixedOnlyForBrokenRepairs`: món `IN_USE` → 400; kind `SERVICE` → 400; `markFixed` không có `itemVersion` → 400.
  - `editDeleteRestore`: PUT với `version` đúng → 200, version `UPDATE`; `version` cũ → 409; DELETE → 204, không còn trong danh sách; POST restore với `version` sau xoá → 200, trở lại danh sách.
  - `deletedItemHidesAndLocksItsLog`: xoá món → GET theo `itemId` → 404; PUT một dòng của nó → 409; khôi phục món → dòng trở lại.
  - `onlyServiceEntriesMoveTheNextServiceDate`: món có `serviceEveryMonths` 3 và `placedAt` 2026-01-10. Ghi `REPAIR` ngày 2026-05-01 → `nextServiceOn` vẫn 2026-04-10. Ghi `SERVICE` ngày 2026-05-01 → 2026-08-01. Xoá dòng `SERVICE` → lại 2026-04-10.
  - `otherWorkspaceIs404`: gọi bằng `asB` → 404.
- [ ] **Step 2: Chạy, thấy FAIL.**
- [ ] **Step 3: Cài đặt.**
  - Tạo: khoá món (`findByIdForUpdate`, không có thì 404). Nếu `markFixed`, kiểm tra trước lần ghi đầu tiên: đúng loại `REPAIR`, có `itemVersion`, món `BROKEN`, khớp `version`. Rồi insert dòng, ghi version, và gọi `markFixed` nếu có.
  - Sửa, xoá, khôi phục: khoá dòng, rồi khoá món; món đã xoá thì 409 "Restore item first".
  - Danh sách chỉ lấy dòng chưa xoá của món chưa xoá, sắp `done_on DESC, id DESC`, phân trang con trỏ.
  - README: mục "Bảo trì".
- [ ] **Step 4: Chạy `MaintenanceIT`, `ItemLifecycleIT` → PASS.**
- [ ] **Step 5: Commit** "Keep a maintenance log for each item".

### Task 3: Nhắc hạn cho mục tuỳ biến (backend, L4)

**Files:**
- Modify: `fielddef/FieldDef.java`, `FieldDefCreateRequest.java`, `FieldDefUpdateRequest.java`, `FieldDefResponse.java`, `FieldDefService.java`, các chỗ gọi constructor; test `fielddef/FieldDefIT.java` (hoặc IT mục tuỳ biến đang có); `README.md`

**Interfaces:**
- Produces: `FieldDef.remindDays()` kiểu `Integer`, đặt sau `isRequired`; `@Min(1) @Max(365) Integer remindDays` trong cả hai request (sau `required`) và trong response.

- [ ] **Step 1: Viết test**
  - `dateFieldCanRemind`: tạo mục `DATE` có `remindDays` 30 → 201, `$.remindDays` 30; PUT đổi thành 60 → 60; PUT bỏ trống → không có giá trị.
  - `remindDaysOnlyForDates`: mục `TEXT` có `remindDays` → 400.
  - `remindDaysRange`: 0 và 366 → 400.
- [ ] **Step 2: FAIL; Step 3: cài đặt** (kiểm tra trong `FieldDefService`, vì kiểu dữ liệu không có trong request sửa); **Step 4: chạy test → PASS.**
- [ ] **Step 5: Commit** "Let date fields ask for a reminder".

### Task 4: Bảo trì trong Hoạt động (backend)

**Files:**
- Modify: `history/ActivityService.java`, `ActivityEntry.java`, `ActivityResponse.java`, `ActivityMapper.java`; test `history/ActivityIT.java` (IT hoạt động đang có); `README.md`

**Interfaces:**
- Consumes: `MaintenanceService.names`, `MaintenanceService.itemIds` (Task 2).
- Produces: `ActivityResponse` có thêm `Long parentId` ở cuối: id món đồ với `MAINTENANCE`, `null` với loại khác. Bộ lọc `entityType=MAINTENANCE` dùng được.

- [ ] **Step 1: Viết test** `maintenanceShowsUpWithItsItem`: ghi một dòng bảo trì → GET `/activity` có dòng `entityType` `MAINTENANCE`, `entityName` "Tủ lạnh: Thay ron cửa", `parentId` là id món; lọc `entityType=MAINTENANCE` chỉ còn dòng đó; dòng `ITEM` có `parentId` null.
- [ ] **Step 2: FAIL; Step 3: cài đặt** (tra `itemIds` theo lô như `names`); **Step 4: PASS.**
- [ ] **Step 5: Commit** "Show maintenance entries in the activity feed".

### Task 5: Danh sách việc cần làm (backend, L5)

**Files:**
- Create: `todo/TodoKind.java`, `Todo.java`, `TodoRepository.java`, `TodoService.java`, `TodoController.java`, `TodoResponse.java`; test `todo/TodoIT.java`, `todo/TodoOrderTests.java`
- Modify: `platform-common/.../history/EntityHistory.java` (`valueSetAt`), `platform-common/src/test/.../history/EntityHistoryIT.java`; `README.md`

**Interfaces:**
- Consumes: `ServiceSchedule.nextServiceOn`, `ItemRepository.lastServiceOn` (Task 1); `FieldDef.remindDays` (Task 3); `BusinessDate`.
- Produces:
  - `EntityHistory.valueSetAt(long workspaceId, String entityType, Collection<Long> entityIds, String jsonPath): Map<Long, Instant>`: với mỗi bản ghi, `changed_at` của version mới nhất mà giá trị ở `jsonPath` của `snapshot` khác version liền trước (version 1 luôn tính). Một truy vấn, dùng `LAG(...) OVER (PARTITION BY entity_id ORDER BY version_no)`; `jsonPath` truyền bằng tham số. Ids rỗng thì trả map rỗng.
  - `enum TodoKind { BROKEN, WARRANTY, SERVICE, MOVE_OUT, CUSTOM_DATE }` (thứ tự này là thứ tự sắp).
  - `Todo(String key, TodoKind kind, LocalDate dueOn, Instant since, String subjectType, long subjectId, String subjectName, String subjectCode, String context)`. `key` có dạng `"BROKEN:123"`, `"MOVE_OUT:<occupancyId>"`, `"CUSTOM_DATE:ITEM:123:<fieldKey>"`.
  - `TodoService.all(long workspaceId): List<Todo>`: đã sắp theo `dueOn`, thứ tự `kind`, `key`.
  - `TodoResponse(String key, TodoKind kind, LocalDate dueOn, Instant since, boolean isNew, String subjectType, long subjectId, String subjectName, String subjectCode, String context)`.
  - `GET /todos?kind=&cursor=&limit=` → `CursorPage<TodoResponse>`. Ở task này `isNew` luôn là `true`; Task 6 thay bằng phép so với mốc.

- [ ] **Step 1: Viết test**
  - `EntityHistoryIT.valueSetAtFindsTheLastChangeOfThatValue`: các version có `status` A, A, B, B, (đổi `name`), B → trả `changed_at` của version 3; bản ghi chỉ có version 1 → `changed_at` của version 1.
  - `TodoOrderTests` (unit, sắp và cắt trang trên `List<Todo>`): cùng `dueOn` thì theo thứ tự `kind`; con trỏ sau `key` X bắt đầu đúng ở việc kế tiếp.
  - `TodoIT extends OccupancyTestData`:
    - `brokenItemsAreListed`: món `BROKEN` → `kind` `BROKEN`, `subjectType` `ITEM`, `context` là tên địa điểm; món đã gỡ hoặc đã xoá → không có.
    - `brokenSinceIsWhenStatusBecameBroken`: tạo món `BROKEN`, sửa tên → `since` bằng `changed_at` của version 1, `dueOn` là ngày tạo.
    - `warrantyWindowEdges`: `warrantyUntil` = hôm nay, = hôm nay + 30 → có; = hôm qua, = hôm nay + 31 → không.
    - `serviceDueWithinSevenDaysOrOverdue`: hạn = hôm nay + 7 → có; = hôm nay + 8 → không; quá hạn 40 ngày → có, `dueOn` là ngày hạn.
    - `moveOutMatchesTheOverview`: cùng dữ liệu, số việc `MOVE_OUT` bằng `endingSoon.total` của `GET /overview`; `subjectType` `PERSON`, `context` là tên địa điểm.
    - `customDateWindowEdges`: mục `DATE` của người, `remindDays` 10 → ngày = hôm nay + 10 có; + 11 không; ngày = hôm nay − 30 có; − 31 không. Mục `ARCHIVED` → không có. Mục không có `remindDays` → không có.
    - `twoDateFieldsOnOnePersonAreTwoTodos`: hai mục `DATE` có nhắc trên cùng một người, cả hai trong khung → hai việc `CUSTOM_DATE`, `subjectId` như nhau, `key` khác nhau, `context` là hai nhãn; `limit=1` qua hai trang lấy đủ cả hai.
    - `malformedCustomDateIsSkipped`: ghi thẳng `attributes` sai định dạng bằng `JdbcClient` → GET `/todos` vẫn 200, không có việc của bản ghi đó.
    - `deletedPlacesTakeTheirItemsOut`: món hỏng ở địa điểm đã xoá → không có.
    - `filtersByKindAndPages`: `kind=WARRANTY` chỉ trả loại đó; `limit=2` có `nextCursor`, trang sau nối tiếp không trùng.
- [ ] **Step 2: Chạy, thấy FAIL.**
- [ ] **Step 3: Cài đặt.** Mỗi nguồn một truy vấn trong `TodoRepository`, chỉ lấy dòng trong khung (spec bảng L5). `BROKEN` lấy `since` bằng `valueSetAt(ITEM, ids, "$.status")`, không có thì dùng `updated_at`. `CUSTOM_DATE` chạy một truy vấn cho mỗi mục `ACTIVE` có `remindDays`, đọc `JSON_UNQUOTE(JSON_EXTRACT(attributes, :path))`. Chuỗi không đọc được thành `LocalDate` thì bỏ qua trong Java, nên không truy vấn nào lỗi vì một ngày gõ sai. `since` ở task này là mốc đầu ngày theo bảng L5; phần `max(...)` của L6 thuộc Task 6. README: mục "Việc cần làm".
- [ ] **Step 4: Chạy `EntityHistoryIT` (trong `platform-common`), `TodoOrderTests`, `TodoIT` → PASS.**
- [ ] **Step 5: Commit** "List what needs doing: broken items, warranties, services, move-outs, dates".

### Task 6: Tóm tắt, mốc đã xem, việc mới (backend, L6)

**Files:**
- Create: `todo/TodoSeenRepository.java`, `TodoSummaryResponse.java`; test `todo/TodoSeenIT.java`
- Modify: `TodoService.java`, `TodoController.java`, `TodoRepository.java`, `README.md`

**Interfaces:**
- Produces:
  - `GET /todos/summary` → `TodoSummaryResponse(int total, Map<TodoKind, Integer> byKind, int newCount, List<TodoResponse> latest)`. `byKind` có đủ năm loại, loại không có việc thì 0. `latest` gồm tối đa 10 việc, `since` mới nhất trước.
  - `POST /todos/seen` → 204, lưu `seen_at` = `Instant.now(clock)` cho `(workspaceId, userId)` (upsert).
  - `isNew` = chưa có mốc, hoặc `since > seenAt`.
  - `since` cuối cùng = `min(bây giờ, max(mốc của Task 5, lúc giá trị được đặt))`, với "lúc được đặt" theo spec L6: `$.warrantyUntil`, `$.serviceEveryMonths` cùng `updated_at` muộn nhất của các dòng `SERVICE` (kể cả đã xoá), `$.attributes.<key>` cùng `updated_at` của mục, `updated_at` của dòng cư trú.

- [ ] **Step 1: Viết `TodoSeenIT extends OccupancyTestData`**
  - `everythingIsNewBeforeTheFirstLook`: hai việc → `newCount` 2, `total` 2, `byKind.BROKEN` 1, `byKind.CUSTOM_DATE` 0.
  - `seenClearsNewAndStaysCleared`: POST `/todos/seen` → `newCount` 0; GET lại lần nữa vẫn 0.
  - `aFreshlySetWarrantyIsNew`: đặt mốc vào một phút trước (ghi thẳng `reg_todo_seen` bằng `JdbcClient` để không phụ thuộc mili giây). Rồi PUT món với `warrantyUntil` = hôm nay + 20 → việc `WARRANTY` có `isNew` true, dù đầu ngày của hôm nay − 10 đã qua.
  - `renamingDoesNotMakeAWarrantyNew`: món có bảo hành trong khung, đặt mốc vào sau đó, PUT đổi tên → `isNew` false.
  - `seenIsPerUser`: người dùng A xem; gọi với `X-User-Id` khác → `newCount` vẫn đủ.
  - `latestHoldsTenNewestBySince`: 12 việc → `latest` có 10 việc, `since` giảm dần.
- [ ] **Step 2: FAIL; Step 3: cài đặt**; **Step 4: chạy `TodoSeenIT`, `TodoIT` → PASS**, rồi `mvnw -B test` cả repo → BUILD SUCCESS.
- [ ] **Step 5: Commit** "Count new to-dos since the bell was last opened".

### Task 7: Kiểu API, form và thông tin món đồ (frontend)

**Files:**
- Modify: `src/api/schema/registry.{json,d.ts}` (sinh lại); `features/items/types.ts`, `components/itemForm.ts`, `ItemFields.tsx`, `ItemFormDialog.tsx` (`FIXED_FIELDS`), `pages/ItemPage.tsx` (`ItemInfo`), `components/ItemHistoryTab.tsx` (nhãn), `features/activity/labels.ts` (`FIELD_LABEL`); `test/fixtures.ts`; test `ItemForm.test.tsx`, `ItemLifecycle.test.tsx` (hoặc test trang món)

**Interfaces:**
- Produces: `ItemFormValues` có thêm `warrantyUntil: string`, `serviceEveryMonths: string`; `itemPayload` gửi `warrantyUntil`, `serviceEveryMonths` (chuỗi rỗng thì không gửi); `warrantyLine(warrantyUntil: string, today: string): { text: string; tone: Tone }` trong `features/items/labels.ts`.

- [ ] **Step 1: Sinh lại kiểu.** Chạy registry mới trên 18082, rồi `cmd.exe /c "set HP_REGISTRY_PORT=18082&& node scripts/api-sync.mjs --only=registry"`. Diff phải có `warrantyUntil`, `serviceEveryMonths`, `nextServiceOn`, `MaintenanceResponse`, `remindDays`, `parentId`, `TodoResponse`, `TodoSummaryResponse`.
- [ ] **Step 2: Viết test**
  - `sendsWarrantyAndInterval`: điền "Hết bảo hành" (qua `DateInput`) và "Bảo trì mỗi (tháng)" = 6 → body có `warrantyUntil`, `serviceEveryMonths: 6`; bỏ trống thì body không có hai khoá.
  - `intervalMustBeOneToHundredTwenty`: 0 và 121 → lỗi "Số nguyên từ 1 tới 120", không gửi.
  - `infoShowsWarrantyAndNextService`: thẻ Thông tin có "Bảo hành tới 12/03/2027 · còn N ngày" và "Bảo trì mỗi 6 tháng · lần tới 01/02/2027"; hạn bảo hành đã qua thì ghi "· đã hết".
  - `warrantyLine` (unit, trong `ItemForm.test.tsx` hoặc file test riêng): còn 31 ngày → "· còn 31 ngày" `neutral`; còn 30 → `warning`; hôm nay → "· hết hôm nay" `warning`; hôm qua → "· đã hết" `neutral`.
  - `overdueServiceIsHighlighted`: `nextServiceOn` hôm qua → chữ "lần tới" nằm trong `ToneBadge` `warning`.
- [ ] **Step 3: FAIL; cài đặt.** Hai ô nằm cạnh nhau, dưới "Ngày đặt". Nhãn version và so sánh version: `warrantyUntil` là "Hết bảo hành", `serviceEveryMonths` là "Bảo trì (tháng)".
- [ ] **Step 4: `npm test`, `build`, `lint` → xanh.**
- [ ] **Step 5: Commit** "Edit and show an item's warranty and service interval".

### Task 8: Tab Bảo trì (frontend)

**Files:**
- Create: `features/maintenance/{api.ts,queries.ts,types.ts,labels.ts,index.ts}`, `components/MaintenanceTab.tsx`, `components/MaintenanceDialog.tsx`; test `features/maintenance/MaintenanceTab.test.tsx`; `test/maintenanceBackend.ts` (msw, cùng kiểu với `itemBackend.ts`)
- Modify: `features/items/pages/ItemPage.tsx` (nhận `tabs?: ItemTabSlots`, tab `maintenance` giữa "Ảnh" và "Lịch sử", `?tab=maintenance`), `features/items/index.ts` (xuất `ItemTabSlots`), `app/router.tsx`

**Interfaces:**
- Consumes: `itemKeys` (từ `@/features/items`), `deletedToast`, `DateInput`, `ToneBadge`.
- Produces:
  - `type ItemTabSlots = { maintenance?: (ws: string, item: ItemDetail) => ReactNode }`;
  - `MaintenanceTab({ ws, item }: { ws: string; item: ItemDetail })`;
  - `maintenanceKeys.list(ws, itemId)` = `['maintenance', ws, itemId]`;
  - hook `useMaintenance`, `useCreateMaintenance`, `useUpdateMaintenance`, `useDeleteMaintenance`, `useRestoreMaintenance`. Mỗi lệnh ghi làm cũ `maintenanceKeys.list` và `itemKeys` của món đó (vì `nextServiceOn`, `status`, `version`).

- [ ] **Step 1: Viết test**
  - `listsEntriesNewestFirst`: hai dòng, có nhãn "Sửa chữa" / "Bảo trì", ngày, việc, người làm.
  - `emptyState`: "Chưa có lần sửa hay bảo trì nào".
  - `brokenItemDefaultsToRepairAndFixed`: món `BROKEN`, bấm "Ghi bảo trì" → loại "Sửa chữa" đang chọn, ô "Đã sửa xong, chuyển về Đang dùng" đã đánh. Lưu → body có `markFixed: true`, `itemVersion` = version món; sau đó món được tải lại.
  - `workingItemDefaultsToService`: món `IN_USE` → loại "Bảo trì", không có ô "Đã sửa xong".
  - `requiresWorkAndNoFutureDate`: thiếu "Việc đã làm" → "Nhập việc đã làm"; ngày mai → "Không được sau hôm nay".
  - `editSendsVersion`: Sửa một dòng → PUT có `version`.
  - `deleteOffersUndo`: Xoá → thông báo có "Hoàn tác"; bấm → POST restore với `version + 1`.
  - `staleItemVersionShowsConflict`: POST trả 409 → `VersionConflictAlert`.
  - `removedItemIsReadOnlyExceptLogging`: món đã gỡ vẫn có nút "Ghi bảo trì", không có ô "Đã sửa xong".
- [ ] **Step 2: FAIL; cài đặt.** Loại chọn bằng `toggle-group`. Danh sách dùng `InfoCard` hoặc bảng như tab Ảnh, trên điện thoại thì thành thẻ. Menu mỗi dòng gồm "Sửa", "Xoá".
- [ ] **Step 3: `npm test`, `build`, `lint` → xanh.**
- [ ] **Step 4: Commit** "Add a maintenance tab to the item page".

### Task 9: Ô nhắc của mục tuỳ biến (frontend)

**Files:**
- Modify: `features/catalog/types.ts` (`remindDays`), `components/FieldDefSection.tsx`, `api.ts`; test `CatalogPage.test.tsx`

- [ ] **Step 1: Viết test**
  - `dateFieldCanRemind`: chọn kiểu "Ngày" → có công tắc "Nhắc trong Việc cần làm"; bật lên thì hiện "Nhắc trước (ngày)" = 30; lưu → body có `remindDays: 30`. Tắt → không gửi khoá đó.
  - `otherTypesHaveNoReminder`: kiểu "Chữ" → không có công tắc.
  - `remindRange`: 0 hay 366 → lỗi "Số nguyên từ 1 tới 365".
  - Bảng mục tuỳ biến có cột "Nhắc", ghi "trước 30 ngày" hoặc "—".
- [ ] **Step 2: FAIL; cài đặt** (cùng kiểu `Switch` với "Bắt buộc"); **Step 3: test, build, lint → xanh.**
- [ ] **Step 4: Commit** "Turn on reminders for date fields in the catalog".

### Task 10: Bảo trì trong Hoạt động (frontend)

**Files:**
- Modify: `features/activity/types.ts` (`EntityType` thêm `'MAINTENANCE'`), `labels.ts` (`ENTITY_LABEL.MAINTENANCE` = "Bảo trì"), `components/ActivityRow.tsx`, `components/RestoreButton.tsx`, `filters.ts`; test `ActivityPage.test.tsx`

- [ ] **Step 1: Viết test** `maintenanceRowLinksToTheItemTab`: dòng `MAINTENANCE` có nhãn "Bảo trì", liên kết tới `/w/:ws/items/<parentId>?tab=maintenance`, không có nút khôi phục; bộ lọc loại có "Bảo trì" và gửi `entityType=MAINTENANCE`.
- [ ] **Step 2: FAIL; cài đặt; Step 3: test, build, lint → xanh.**
- [ ] **Step 4: Commit** "Link maintenance activity to the item's maintenance tab".

### Task 11: Trang Việc cần làm và số trên thanh bên (frontend)

**Files:**
- Create: `features/todos/{api.ts,queries.ts,types.ts,labels.ts,dueLabel.ts,index.ts}`, `pages/TodosPage.tsx`, `components/TodoRow.tsx`, `components/TodoNavBadge.tsx`; test `features/todos/dueLabel.test.ts`, `TodosPage.test.tsx`
- Modify: `app/router.tsx` (`todos`), `app/layout/AppLayout.tsx` (mục "Việc cần làm" sau "Tổng quan", `ListChecksIcon`, số bằng `SidebarMenuBadge`), `test/handlers.ts` (`mockShell` có thêm `GET /api/registry/todos/summary`, mặc định tóm tắt rỗng), `test/fixtures.ts` (`todo()`, `emptyTodoSummary()`)

**Interfaces:**
- Produces:
  - `todoKeys.all(ws)` = `['todos', ws]`, `todoKeys.list(ws, kind)`, `todoKeys.summary(ws)`;
  - `useTodoSummary(ws)`: `staleTime: 0`, `meta: { refreshAfterWrites: true }`, `refetchInterval: 300_000`, `refetchOnWindowFocus: true`, `retry: false`; 404 thì `data` là `null` để ẩn mục trên thanh bên và chuông;
  - `useMarkTodosSeen(ws)`;
  - `dueLabel(todo: { kind: TodoKind; dueOn: string }, today: string): { text: string; tone: Tone }`;
  - `todoHref(ws, todo)`: mở `/items/:id`, `/people/:id` hoặc `/places/:id`. Với `SERVICE` thì thêm `?tab=maintenance`.

- [ ] **Step 1: Viết test**
  - `dueLabel.test.ts`: quá hạn 3 ngày → "quá hạn 3 ngày" `danger`; hôm nay → "hôm nay" `warning`; còn 5 ngày → "còn 5 ngày" `warning`; còn 12 ngày → "còn 12 ngày" `neutral`; `BROKEN` ngày 2026-11-20 → "hỏng từ 20/11" `danger`.
  - `TodosPage.test.tsx`:
    - `listsTodosWithLinksAndDueLabels`: ba việc ba loại, tên là liên kết đúng `todoHref`, chữ phụ hiện ra.
    - `kindToggleFiltersAndLandsOnTheUrl`: bấm "Bảo hành" (kèm số) → request có `kind=WARRANTY`, URL có `?kind=WARRANTY`; "Tất cả" bỏ cả hai.
    - `emptyState`: "Không có việc gì cần làm".
    - `loadsMore`: có `nextCursor` thì có "Tải thêm".
  - Trong test của `AppLayout` (hoặc `app.test.tsx`): `sidebarShowsTheTodoCount`: `total` 4 → mục "Việc cần làm" có số 4; `total` 0 → không có số; summary 404 → không có mục.
- [ ] **Step 2: FAIL; cài đặt.** Bố cục giống `VacanciesPage`: tiêu đề "Việc cần làm", bộ lọc `toggle-group`, danh sách (trên điện thoại thì thành thẻ). Mỗi dòng có biểu tượng loại: Hỏng `WrenchIcon`, Bảo hành `ShieldCheckIcon`, Bảo trì `CalendarClockIcon`, Chuyển đi `LogOutIcon`, Hạn khác `CalendarIcon`.
- [ ] **Step 3: `npm test`, `build`, `lint` → xanh.**
- [ ] **Step 4: Commit** "Add a to-do page with a count in the sidebar".

### Task 12: Chuông thông báo (frontend, L6)

**Files:**
- Create: `features/todos/components/TodoBell.tsx`; test `features/todos/TodoBell.test.tsx`
- Modify: `features/todos/index.ts`, `app/layout/AppLayout.tsx` (chuông ở mép phải thanh đầu trang)

**Interfaces:**
- Consumes: `useTodoSummary`, `useMarkTodosSeen`, `dueLabel`, `todoHref` (Task 11).
- Produces: `TodoBell({ ws }: { ws: string })`.

- [ ] **Step 1: Viết test**
  - `showsNewCountAndCapsAtNine`: `newCount` 3 → nút "Thông báo" có "3"; 12 → "9+"; 0 → không có chấm.
  - `openingListsLatestAndMarksSeen`: bấm chuông → popover có các việc trong `latest`, việc `isNew` có chấm; có POST `/api/registry/todos/seen`; số trên chuông về 0 ngay, không chờ tải lại.
  - `emptyPopover`: `latest` rỗng → "Chưa có gì mới".
  - `seeAllGoesToTheTodoPage`: "Xem tất cả việc cần làm" trỏ tới `/w/:ws/todos`; bấm một việc thì popover đóng.
  - `olderRegistryHidesTheBell`: summary 404 → không có nút "Thông báo".
- [ ] **Step 2: FAIL; cài đặt.** Cập nhật số tại chỗ bằng `setQueryData` trên `todoKeys.summary`, rồi để lần tải lại sau lệnh ghi cập nhật `isNew`.
- [ ] **Step 3: `npm test`, `build`, `lint` → xanh.**
- [ ] **Step 4: Commit** "Add a notification bell for new to-dos".

### Task 13: Xem bằng mắt, tài liệu, kiểm tra cuối

**Files:**
- Modify: `README.md` (mục tính năng); spec: đổi trạng thái thành "đã xây"

- [ ] **Step 1:** Dựng bộ preview (18081/18082/15173/9222), dùng registry mới. Trong không gian -9001, tạo qua API:
  - một món Hỏng;
  - một món có bảo hành hết sau 10 ngày;
  - một món bảo trì 1 tháng, lần cuối 35 ngày trước;
  - một mục tuỳ biến "CCCD hết hạn" của người, có nhắc, với một người hết hạn sau 5 ngày.

  Rồi chụp ở khổ 1920 và 390 trên nền sáng, và ở khổ 1440 trên nền tối, các màn hình: Việc cần làm (có lọc), chuông đang mở, tab Bảo trì, hộp thoại Ghi bảo trì, thẻ Thông tin của món, mục tuỳ biến có nhắc. Sửa những chỗ sai.
- [ ] **Step 2:** `mvnw -B verify` cho `registry-service` và `platform-common` (đủ IT), rồi `mvnw -B test` cả repo, `npm test`, `build`, `lint`. Tất cả phải xanh. Chạy lại `seed.mjs` nếu `CommittedDataIT` đã dọn không gian preview.
- [ ] **Step 3:** Tắt bộ preview, xoá `chrome-profile`. Commit "Document maintenance, to-dos and the bell".
