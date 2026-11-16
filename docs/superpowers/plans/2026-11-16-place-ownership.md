# Place Ownership Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ghi được ai sở hữu một địa điểm, từ ngày nào tới ngày nào, bao nhiêu phần trăm, theo giấy tờ nào, tách khỏi việc ai đang ở; chuyển nhượng là một thao tác; xem được một người sở hữu những chỗ nào.

**Architecture:** Bảng mới `reg_ownership` và package `com.home_platform.registry.ownership`, dựng theo đúng mẫu của cư trú (dòng có ngày bắt đầu, ngày kết thúc, trạng thái suy ra từ ngày, không có version riêng). Khác cư trú ở ba chỗ: có tỷ lệ với luật tổng không quá 100% mỗi ngày, có thao tác chuyển nhượng, và mọi lệnh ghi giữ riêng dòng địa điểm để hai lệnh cùng lúc không cùng qua bước kiểm tra tổng. Frontend có feature mới `features/ownership`, ghép vào trang địa điểm và trang người qua các slot tab.

**Tech Stack:** như `2026-11-16-auto-codes.md`. Không thêm thư viện.

**Spec:** `docs/superpowers/specs/2026-11-16-ownership-design.md` (mục 4.1, 5.1 đến 5.4, 6, 7, 8). Các số mục trong kế hoạch này trỏ vào spec đó.

**Phụ thuộc:** chạy sau `2026-11-16-item-ownership.md`. Từ kế hoạch đó dùng: `PersonTabSlots`, `PersonPicker` đã export, thông báo xoá người có số món đồ, số migration V4. Nếu bỏ phần đồ đạc: tạo `PersonTabSlots` ở Task 7 theo đúng mô tả trong kế hoạch kia (Task 4), và thông báo xoá người ở Task 3 chỉ còn hai số (`… occupancies and <w> ownerships`).

## Global Constraints

- Như Global Constraints của `2026-11-16-auto-codes.md` (quy ước backend, lệnh chạy IT, npm qua Node của Windows, mỗi task một commit).
- `share_percent`: trống, hoặc lớn hơn 0 và không quá 100, tối đa hai chữ số thập phân. Dòng không ghi tỷ lệ không tham gia luật tổng.
- `end_date` là ngày cuối cùng còn sở hữu. Trạng thái: `CURRENT` khi `start_date` ≤ hôm nay và (`end_date` trống hoặc ≥ hôm nay); `UPCOMING` khi `start_date` > hôm nay; `PAST` khi `end_date` < hôm nay. "Hôm nay" theo `BusinessDate`.
- Thứ tự giữ dòng trong mọi lệnh ghi sở hữu: dòng sở hữu (nếu lệnh làm trên một dòng có sẵn), rồi địa điểm (`FOR UPDATE`), rồi người (`FOR UPDATE`). Không đảo.
- Mọi kiểm tra của một lệnh chạy xong trước lần ghi đầu tiên của nó.
- Thông báo lỗi cố định:
  - 409 `Person <personId> already has an overlapping ownership at this place`
  - 409 `Ownership shares of place <placeId> would total <tổng>% on <yyyy-MM-dd>` (`<tổng>` không có số 0 thừa: `110`, `100.5`)
  - 409 `Ownership <id> has already ended`
  - 409 `Restore place <id> first`, `Restore person <id> first`
  - 400 `endDate must not be before startDate`, `transferDate must be after startDate`, `toPersonId must be a different person`
  - 400 `relation OWNER is no longer accepted; record an ownership instead`
  - 409 `Place <id> still has <c> child places, <o> occupancies, <w> ownerships and <i> items`
  - 409 `Person <id> still has <n> current or upcoming occupancies, <w> ownerships and <k> items`
- Nhãn giao diện: tab "Sở hữu"; trạng thái "Đang sở hữu", "Sắp tới", "Đã thôi"; thao tác "Thêm chủ", "Chuyển nhượng", "Thôi sở hữu", "Sửa", "Xoá dòng nhập nhầm".
- `places` và `people` không nhập `ownership`.

## Review Focus

1. Hai lệnh thêm chủ 60% cho cùng một địa điểm chạy cùng lúc: chỉ một lệnh thành công. Test: `twoOwnersAddedTogetherCannotExceedOneHundredPercent` (Task 3).
2. Tỷ lệ hợp lệ theo từng ngày nhưng cộng dồn mọi dòng giao với dòng mới thì quá 100 (hai dòng 60% nối tiếp nhau, dòng thứ ba 40% phủ cả hai): phải được nhận. Test: `sharesThatNeverMeetAreAccepted` (Task 1).
3. Chuyển nhượng hỏng ở bước tạo dòng mới (người nhận đã là đồng sở hữu): dòng cũ không bị kết thúc. Test: `receiverAlreadyOwningHereIs409AndNothingChanges` (Task 2).
4. Dòng cư trú "Chủ" có từ trước vẫn sửa được ngày và ghi chú, ở cả backend lẫn form. Test: `existingOwnerRowKeepsItsRelationWhenEdited` (Task 3), `editingAnOwnerRowKeepsTheOwnerOption` (Task 4).
5. Người dùng gõ tỷ lệ kiểu Việt Nam, `33,33`: được hiểu là 33.33, không bị báo sai dạng. Test: `shareMustBeBetweenZeroAndOneHundred` (Task 5).

## File Structure

```
registry-service/src/main/resources/db/migration/V5__ownership.sql
registry-service/src/main/java/com/home_platform/registry/ownership/
  Ownership.java, OwnershipStatus.java       dòng của bảng; trạng thái suy ra từ ngày
  ShareLimit.java                             ngày đầu tiên tổng tỷ lệ vượt 100. Thuần, không Spring
  OwnershipRepository.java                    đọc theo địa điểm, cây con, người; tìm dòng giao nhau
  OwnershipService.java                       luật ở mục 5.1, 5.2
  OwnershipController.java                    /ownerships
  OwnershipCreateRequest, OwnershipUpdateRequest, OwnershipEndRequest, OwnershipTransferRequest
  OwnershipResponse, OwnershipTransferResponse, OwnershipView, OwnershipTransfer, OwnershipMapper
frontend/src/features/people/components/PersonChoiceField.tsx   chọn một người hoặc tạo người mới (tách từ form cư trú)
frontend/src/features/ownership/
  types.ts, api.ts, queries.ts, labels.ts, index.ts
  components/ownershipForm.ts                 schema của form, đọc và in tỷ lệ
  components/OwnershipList.tsx, OwnershipActions.tsx
  components/OwnershipFormDialog.tsx, EndOwnershipDialog.tsx, TransferOwnershipDialog.tsx, DeleteOwnershipDialog.tsx
  components/PlaceOwnersTab.tsx, PersonOwnershipsTab.tsx
frontend/src/test/ownershipBackend.ts         backend giả có trạng thái
```

---

### Task 1: Backend, bảng sở hữu và các thao tác cơ bản

**Files:** tạo `registry-service/src/main/resources/db/migration/V5__ownership.sql` và, trong `registry-service/src/main/java/com/home_platform/registry/ownership/`: `Ownership.java`, `OwnershipStatus.java`, `ShareLimit.java`, `OwnershipRepository.java`, `OwnershipService.java`, `OwnershipController.java`, `OwnershipCreateRequest.java`, `OwnershipUpdateRequest.java`, `OwnershipEndRequest.java`, `OwnershipResponse.java`, `OwnershipView.java`, `OwnershipMapper.java`; sửa `place/PlaceService.java`, `registry-service/src/test/java/com/home_platform/registry/CommittedDataIT.java` (`WIPE`), `schema/RegistrySchemaIT.java`; test tạo `ownership/{ShareLimitTests,OwnershipTestData,OwnershipIT,OwnershipListIT}.java`.

**Interfaces:**
- Produces:

```java
@With public record Ownership(long workspaceId, long id, long placeId, long personId, BigDecimal sharePercent,
		LocalDate startDate, LocalDate endDate, String documentNo, String note, Instant createdAt, long createdBy,
		Instant updatedAt, long updatedBy, int version, Instant deletedAt)
public enum OwnershipStatus { CURRENT, UPCOMING, PAST;
	public static OwnershipStatus of(LocalDate startDate, LocalDate endDate, LocalDate today) }

/** Thuần. */
final class ShareLimit {
	record Overflow(LocalDate date, BigDecimal total) { }
	/** Ngày đầu tiên mà tổng tỷ lệ của `others` đang có hiệu lực cộng `share` vượt 100, trong [start, end]; end null là vô hạn. */
	static Optional<Overflow> firstOverflow(List<Ownership> others, BigDecimal share, LocalDate start, LocalDate end)
}

// PlaceService: thêm
public Place getForUpdate(long workspaceId, long id)             // 404 nếu không có hoặc đã xoá
public Optional<Place> findForUpdate(long workspaceId, long id)

// OwnershipRepository extends WorkspaceRepository<Ownership> ("reg_ownership")
CursorPage<Ownership> atPlace(long workspaceId, long placeId, OwnershipStatus status, LocalDate today, CursorRequest page)
CursorPage<Ownership> currentInSubtree(long workspaceId, String rootPath, LocalDate today, CursorRequest page)
CursorPage<Ownership> ofPerson(long workspaceId, long personId, CursorRequest page)
boolean overlaps(long workspaceId, long personId, long placeId, LocalDate start, LocalDate end, long excludeId)
List<Ownership> sharedAt(long workspaceId, long placeId, LocalDate start, LocalDate end, long excludeId)
	// dòng chưa xoá của địa điểm, có share_percent, giao với [start, end], không kể excludeId

// OwnershipService
Ownership get(long workspaceId, long id);  OwnershipView view(long workspaceId, long id)
CursorPage<OwnershipView> atPlace(...), currentInSubtree(...), ofPerson(...)     // như OccupancyService
@Transactional Ownership create(WorkspaceContext ctx, OwnershipCreateRequest request)
@Transactional Ownership update(WorkspaceContext ctx, long id, OwnershipUpdateRequest request)
@Transactional Ownership end(WorkspaceContext ctx, long id, OwnershipEndRequest request)
@Transactional void delete(WorkspaceContext ctx, long id, int version)
record OwnershipView(Ownership ownership, String placeName, String personFullName, OwnershipStatus status)

// Request, response
record OwnershipCreateRequest(@NotNull Long placeId, @NotNull Long personId,
		@DecimalMin(value = "0", inclusive = false) @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal sharePercent,
		@NotNull LocalDate startDate, LocalDate endDate, @Size(max = 100) String documentNo, @Size(max = 500) String note)
record OwnershipUpdateRequest(@NotNull Integer version, <cùng ràng buộc> BigDecimal sharePercent,
		@NotNull LocalDate startDate, LocalDate endDate, @Size(max = 100) String documentNo, @Size(max = 500) String note)
record OwnershipEndRequest(@NotNull Integer version, LocalDate endDate)
record OwnershipResponse(long id, long placeId, String placeName, long personId, String personFullName,
		BigDecimal sharePercent, LocalDate startDate, LocalDate endDate, OwnershipStatus status, String documentNo,
		String note, Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version)
// GET /ownerships?placeId=&status= | ?subtreeOf= | ?personId=   POST /ownerships   GET|PUT /ownerships/{id}
// POST /ownerships/{id}/end   DELETE /ownerships/{id}?version=
```

- Migration: bảng đúng như mục 6 của spec, rồi chép cư trú OWNER:

  ```sql
  INSERT INTO reg_ownership (workspace_id, id, place_id, person_id, start_date, end_date, note,
                             created_at, created_by, updated_at, updated_by, version)
  SELECT workspace_id, id, place_id, person_id, start_date, end_date, note,
         created_at, created_by, updated_at, updated_by, 1
  FROM reg_occupancy WHERE relation = 'OWNER' AND deleted_at IS NULL;
  ```

- `ShareLimit.firstOverflow`: `share` null thì rỗng. Tổng chỉ có thể tăng ở một ngày bắt đầu, nên chỉ xét các ngày: `start`, và `startDate` của từng dòng trong `others` nằm trong `(start, end]`; xét tăng dần, trả ngày đầu tiên có tổng lớn hơn 100.
- `create`: `places.getForUpdate`, `people.getForUpdate`, rồi kiểm tra: thứ tự ngày, `overlaps` (409), `ShareLimit.firstOverflow(repository.sharedAt(...), ...)` (409), rồi `insert`.
- `update`: `repository.findByIdForUpdate` (404); `places.findForUpdate` rỗng thì 409 `Restore place … first`; `people.findForUpdate` rỗng thì 409 `Restore person … first`; cùng ba kiểm tra với `excludeId = id`; `update` với `version` của request.
- `end`: như `OccupancyService.end`, thêm giữ riêng dòng trước; `endDate` mặc định hôm nay.
- Chuỗi rỗng thành `null` (`Text.trimToNull`) cho `documentNo`, `note`. Controller: đúng một trong `placeId`, `subtreeOf`, `personId`, nếu không 400 `Give exactly one of placeId, subtreeOf or personId`; `status` mặc định `CURRENT`.
- `CommittedDataIT.WIPE`: thêm `DELETE FROM reg_ownership WHERE workspace_id = :ws` ngay sau dòng xoá `reg_occupancy`.

- [ ] **Đếm trước khi chép.** Trước khi tạo file migration, thêm tạm vào `RegistrySchemaIT` một test in ra số dòng `reg_occupancy` có `relation = 'OWNER'` và `deleted_at IS NULL`; chạy, ghi con số, xoá test tạm. Sau khi migration chạy, số dòng `reg_ownership` phải bằng đúng con số đó (kiểm bằng một test tạm nữa, rồi xoá).
- [ ] Test `ShareLimitTests`: `noShareMeansNoLimit`, `overlappingSharesOverOneHundredOverflowOnTheFirstSharedDay` (đang có 60% từ 2026-01-01; thêm 50% từ 2026-06-01: `Overflow(2026-06-01, 110)`), `sharesThatNeverMeetDoNotAddUp` (60% tháng 1 đến 6, 60% tháng 7 đến 12, thêm 40% cả năm: rỗng), `overflowCanStartLaterThanTheNewRow` (40% tháng 1 đến 6, 60% từ tháng 7; thêm 50% cả năm: `Overflow(2026-07-01, 110)`), `exactlyOneHundredIsAllowed`, `openEndedRowsRunForever`.
- [ ] Test `OwnershipIT extends OwnershipTestData` (`OwnershipTestData`: như `OccupancyTestData`, có `place`, `person`, `postOwnership(placeId, personId, share, start, end)`, `ownership(...)`):
  - `createsAnOwnershipWithShareAndDocument`: thân `{placeId, personId, sharePercent: 50, startDate: "2026-01-01", documentNo: "CS 123456"}` trả 201 có `placeName`, `personFullName`, `sharePercent` 50, `status`, `version` 1.
  - `shareIsOptional`; `invalidInputIs400` (`sharePercent` 0, 100.01, 33.333; `endDate` trước `startDate`; thiếu `startDate`).
  - `placeAndPersonMustBeAlive` (không có, đã xoá, của `WS_B`: 404).
  - `samePersonCannotOverlapAtOnePlace` (409 đúng thông báo), `samePersonMayOwnTwoPlaces`, `backToBackPeriodsDoNotOverlap` (kết thúc 2026-06-30, bắt đầu 2026-07-01).
  - `sharesOverOneHundredAreRefusedWithTheDay` (409 `Ownership shares of place <id> would total 110% on 2026-06-01`), `sharesThatNeverMeetAreAccepted`, `rowsWithoutShareAreNotCounted`, `deletedRowsAreNotCounted`.
  - `updateChangesShareDatesDocumentAndNote`, `updateRechecksOverlapAndShares`, `updateDoesNotCountTheRowAgainstItself` (dòng 100% sửa ghi chú vẫn được), `updateOfARowWhosePlaceOrPersonIsDeletedIs409`, `updateChecksTheVersion`.
  - `endSetsTheLastDayAndDefaultsToToday`, `endingTwiceIs409`, `deleteIsSoftAndChecksTheVersion`.
  - `otherWorkspaceSeesNothing`.
- [ ] Test `OwnershipListIT`: `currentUpcomingAndPastAtAPlace`, `currentOwnersInAWholeBuilding` (không gồm địa điểm đã xoá), `everythingAPersonOwnsNewestFirst`, `exactlyOneFilter`, `listsUseTheirIndexes` (`EXPLAIN`: theo địa điểm dùng `idx_reg_ownership_place`, theo người dùng `idx_reg_ownership_person`).
- [ ] Test `RegistrySchemaIT`: thêm `reg_ownership` vào `registryTablesExist`; hai index vào `indexesMatchDesign`; `ownershipOfAPlaceInAnotherWorkspaceIsRejected`; `hardDeleteOfPlaceInUseIsRejected` thêm một địa điểm chỉ có dòng sở hữu.
- [ ] Hỏng → viết → PASS; `./mvnw -pl registry-service test` xanh. Commit `Add place ownership`.

### Task 2: Backend, chuyển nhượng

**Files:** tạo `ownership/{OwnershipTransferRequest,OwnershipTransfer,OwnershipTransferResponse}.java`; sửa `ownership/{OwnershipService,OwnershipController}.java`; test `ownership/OwnershipTransferIT.java`.

**Interfaces:**
- Produces:

```java
record OwnershipTransferRequest(@NotNull Integer version, @NotNull Long toPersonId, LocalDate transferDate,
		@Size(max = 100) String documentNo, @Size(max = 500) String note)
record OwnershipTransfer(Ownership ended, Ownership started)
record OwnershipTransferResponse(OwnershipResponse ended, OwnershipResponse started)
// OwnershipService
@Transactional OwnershipTransfer transfer(WorkspaceContext ctx, long id, OwnershipTransferRequest request)
// POST /ownerships/{id}/transfer -> OwnershipTransferResponse
```

- Thứ tự: giữ riêng dòng (404); `version` lệch thì `versionConflict("Ownership", id)`; đã có `endDate` thì 409; `toPersonId` bằng chủ hiện tại thì 400; ngày = `transferDate` hoặc hôm nay, không sau `startDate` thì 400; `places.findForUpdate` rỗng thì 409 `Restore place … first`; `people.getForUpdate(toPersonId)` (404); `overlaps` cho người nhận trên `[ngày, vô hạn]` (409); `ShareLimit` cho dòng mới với `excludeId = id` (dòng cũ sẽ kết thúc hôm trước, không được tính vào). Rồi mới ghi: dòng cũ `endDate = ngày − 1`, dòng mới cùng `placeId`, cùng `sharePercent`, `startDate = ngày`, `documentNo` và `note` của request.

- [ ] Test `OwnershipTransferIT extends OwnershipTestData`:
  - `transferEndsTheOldRowTheDayBeforeAndStartsTheNewOne`: An 50% từ 2024-03-01; chuyển cho Cường ngày 2026-11-16 với `documentNo: "CS 999"`: `ended.endDate` = 2026-11-15, `ended.version` = 2; `started.personId` = Cường, `started.sharePercent` = 50, `started.startDate` = 2026-11-16, `started.documentNo` = `CS 999`, `started.endDate` trống.
  - `transferDefaultsToToday`, `transferKeepsACoOwnersRowUntouched` (Bình 50% không đổi).
  - `transferToTheSamePersonIs400`, `transferDateMustBeAfterTheStart` (ngày bằng `startDate`: 400), `endedOwnershipCannotBeTransferred`.
  - `receiverAlreadyOwningHereIs409AndNothingChanges`: An 50%, Bình 50%; chuyển phần của An cho Bình: 409 đúng thông báo giao nhau; đọc lại dòng của An: `endDate` vẫn trống, `version` vẫn 1.
  - `unknownReceiverIs404AndNothingChanges`, `transferChecksTheVersion`, `transferOfARowInADeletedPlaceIs409`.
- [ ] Hỏng → viết → PASS. Commit `Add transferring an ownership`.

### Task 3: Backend, cư trú thôi nhận "Chủ"; xoá địa điểm và xoá người biết về sở hữu

**Files:** sửa `occupancy/OccupancyService.java`, `place/{PlaceService,PlaceRepository}.java`, `person/{PersonService,PersonRepository}.java`; test sửa `occupancy/OccupancyIT.java`, `place/PlaceDeleteIT.java`, `person/PersonIT.java`, `ConcurrentWritesIT.java`.

**Interfaces:**
- Produces:

```java
// PlaceRepository
int countCurrentOwnerships(long workspaceId, long placeId, LocalDate today)    // chưa xoá, end_date trống hoặc >= today
// PersonRepository
int countCurrentOwnerships(long workspaceId, long personId, LocalDate today)
```

- `OccupancyService.create`: `relation` là OWNER thì 400. `update`: request là OWNER mà dòng hiện tại không phải OWNER thì 400; dòng đang là OWNER gửi lại OWNER thì được.
- `PlaceService.delete` và `PersonService.delete`: đếm thêm sở hữu, thông báo theo Global Constraints.

- [ ] Test `OccupancyIT`: `newOccupancyAsOwnerIsRejected`, `changingARelationToOwnerIsRejected`, `existingOwnerRowKeepsItsRelationWhenEdited` (dòng OWNER chèn bằng SQL; `PUT` với `relation: OWNER` và ghi chú mới trả 200); sửa test đang đổi quan hệ sang OWNER để dùng `STAFF`.
- [ ] Test `PlaceDeleteIT`: `placeWithACurrentOrUpcomingOwnerCannotBeDeleted`, `placeWhoseOwnershipsAllEndedCanBeDeleted`; sửa câu kiểm thông báo có sẵn thành `… 1 child places, 1 occupancies, 0 ownerships and 1 items`. `PersonIT`: `personWhoStillOwnsAPlaceCannotBeDeleted`, `personWhoOnlyOwnedInThePastCanBeDeleted`; sửa các câu kiểm thông báo theo dạng ba số.
- [ ] Test `ConcurrentWritesIT`: sửa câu `contains` của `deleteSeesAnItemAddedConcurrently` theo thông báo mới; thêm `twoOwnersAddedTogetherCannotExceedOneHundredPercent` (`holdOpen` lệnh thêm 60%, lệnh thêm 60% thứ hai ở nền, commit: lệnh sau ném `ConflictException` chứa `would total 120%`, `rows("reg_ownership")` = 1), `placeDeleteSeesAnOwnershipAddedConcurrently`, `ownershipAddedDuringAPersonDeleteIsRefused`.
- [ ] Hỏng → viết → PASS; `./mvnw test` xanh; chạy `-Dit.test='Ownership*IT,Occupancy*IT,Place*IT,Person*IT,ConcurrentWritesIT,RegistrySchemaIT'`. Cập nhật `README.md` gốc (dòng `/ownerships`; dòng `/occupancies`, `/places`, `/people`). Commit `Stop recording owners as occupancies; block deletes while owned`.

### Task 4: Frontend, tách ô "chọn hoặc tạo người"; form cư trú bỏ "Chủ"

**Files:** `frontend/src/api/schema/*` (sinh lại); tạo `frontend/src/features/people/components/PersonChoiceField.tsx`; sửa `frontend/src/features/people/{index.ts,components/OccupancyFormDialog.tsx}`; test `features/people/PlaceOccupants.test.tsx`, `features/people/PersonChoiceField.test.tsx`.

**Interfaces:**
- Produces (export qua `people/index.ts`):

```ts
/** Một người đã có, hoặc tên của người sẽ được tạo khi gửi form. */
export type PersonChoice = { kind: 'existing'; person: PersonSummary } | { kind: 'new'; fullName: string }
export type PersonChoiceState = {
  choice: PersonChoice | null
  setChoice: (choice: PersonChoice | null) => void
  /** `id` và tên của người đã chọn. Với người mới: tạo hồ sơ, đúng một lần kể cả khi gửi lại sau lỗi. `null` khi chưa chọn. */
  resolve: () => Promise<{ id: string; fullName: string } | null>
  isCreating: boolean
}
export function usePersonChoice(ws: string): PersonChoiceState
export function PersonChoiceField(props: { ws: string; state: PersonChoiceState; label?: string }): JSX.Element
```

  - Hành vi giữ nguyên như đoạn đang nằm trong `OccupancyFormDialog`: ô tìm người; "Thêm người mới “tên”" chuyển sang khung "Người mới: tên · Hồ sơ được tạo khi bấm Thêm; bổ sung thông tin sau." với nút "Đổi" (ẩn sau khi hồ sơ đã tạo).
  - `OccupancyFormDialog` dùng `usePersonChoice` và `PersonChoiceField`; các test có sẵn của form cư trú phải xanh mà không sửa câu kiểm nào.
  - Ô "Quan hệ": form thêm không có lựa chọn "Chủ"; form sửa chỉ có "Chủ" khi dòng đang sửa là OWNER.

- [ ] Test `PersonChoiceField.test.tsx`: `picksAnExistingPerson`, `newPersonIsCreatedOnResolve`, `resolveTwiceCreatesOnePerson`, `choiceCanBeChangedBeforeCreating`.
- [ ] Test `PlaceOccupants.test.tsx`: `newOccupancyCannotBeOwner`, `editingAnOwnerRowKeepsTheOwnerOption`; test đang chọn "Chủ" khi thêm đổi sang "Khách" (`GUEST`).
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Extract the person choice field; drop owner from new occupancies`.

### Task 5: Frontend, tab "Sở hữu" của một địa điểm

**Files:** tạo `frontend/src/features/ownership/{types.ts,api.ts,queries.ts,labels.ts,index.ts}`, `frontend/src/features/ownership/components/{ownershipForm.ts,OwnershipList.tsx,OwnershipActions.tsx,OwnershipFormDialog.tsx,EndOwnershipDialog.tsx,DeleteOwnershipDialog.tsx,PlaceOwnersTab.tsx}`, `frontend/src/test/ownershipBackend.ts`; sửa `frontend/src/features/places/pages/PlacePage.tsx`, `frontend/src/app/router.tsx`, `frontend/src/test/fixtures.ts` (`ownership`); test `features/ownership/{ownershipForm.test.ts,PlaceOwners.test.tsx}`, `features/places/PlacePage.test.tsx`.

**Interfaces:**
- Consumes: `PersonChoiceField`, `usePersonChoice` (Task 4); `PlacePicker`, `useChildren`, `PlaceDetail` từ `places`.
- Produces:

```ts
// types.ts
export type Ownership = Present<S['OwnershipResponse'],
  'id' | 'placeId' | 'placeName' | 'personId' | 'personFullName' | 'startDate' | 'status' | 'version'>
export type OwnershipStatus = Ownership['status']
// labels.ts
export const OWNERSHIP_STATUS_LABEL: Record<OwnershipStatus, string>   // Đang sở hữu, Sắp tới, Đã thôi
// api.ts
listOwnershipsAtPlace(ws, placeId, status, cursor?), listOwnershipsInSubtree(ws, placeId, cursor?), listOwnershipsOfPerson(ws, personId, cursor?)
createOwnership(ws, body), updateOwnership(ws, id, body), endOwnership(ws, id, version, endDate), deleteOwnership(ws, id, version)
// queries.ts
export const ownershipKeys = { all(ws), atPlace(ws, placeId, status), inSubtree(ws, placeId), ofPerson(ws, personId) }
useOwnershipsAtPlace(ws, placeId, status, { enabled? }), useOwnershipsInSubtree(ws, placeId, enabled), useOwnershipsOfPerson(ws, personId)
useCreateOwnership, useUpdateOwnership, useEndOwnership, useDeleteOwnership      // onSuccess: làm cũ ownershipKeys.all(ws)
// components/ownershipForm.ts
export function parseShare(text: string): number | null | 'invalid'   // '' -> null; '33,33' -> 33.33; '0', '101', '33,333', 'abc' -> 'invalid'
export function formatShare(share: number | null | undefined): string // 50 -> '50%'; 33.33 -> '33,33%'; null -> '—'
export const ownershipFormSchema                                        // sharePercent, startDate, endDate, documentNo (<=100), note (<=500)
// places/pages/PlacePage.tsx
export type PlaceTabSlots = Partial<Record<'owners' | 'people' | 'items', (ws: string, place: PlaceDetail) => ReactNode>>
// components
export function PlaceOwnersTab(props: { ws: string; place: PlaceDetail }): JSX.Element
```

  - Thứ tự tab của trang địa điểm: Tổng quan, Bên trong, Sở hữu, Người ở, Đồ đạc, Lịch sử; `?tab=owners`.
  - `PlaceOwnersTab` như mục 8 của spec, cùng bố cục với `PlaceOccupantsTab`: công tắc "Cả bên trong" (khi có chỗ con), nhóm "Đang sở hữu", "Sắp tới", "Đã thôi" (chỉ tải khi mở), nút "Thêm chủ".
  - `OwnershipList`: cột Người (hoặc Địa điểm khi `show="place"`), Tỷ lệ, Từ ngày, Tới ngày, Giấy tờ, thao tác. Dòng tổng dưới nhóm "Đang sở hữu" khi có ít nhất một dòng ghi tỷ lệ và danh sách đã tải hết.
  - `OwnershipActions`: Chuyển nhượng và Thôi sở hữu (chỉ khi dòng chưa có `endDate`), Sửa, Xoá dòng nhập nhầm. Mục "Chuyển nhượng" nối vào ở Task 6; ở task này chưa hiện.
  - `OwnershipFormDialog`: hai chế độ như form cư trú (`create` với `place` hoặc `person` cố định; `edit`). Ô: người (`PersonChoiceField`) hoặc địa điểm (`PlacePicker`), tỷ lệ (`inputMode="decimal"`, hậu tố `%`), từ ngày (mặc định hôm nay), tới ngày ("Để trống nếu vẫn đang sở hữu"), số giấy tờ, ghi chú. Lỗi 409 của backend hiện nguyên văn; `version` lệch thì dùng thông báo `STALE_ROW` như form cư trú.
  - `ownershipBackend`: giữ trạng thái, áp đúng ba luật (giao nhau, tổng tỷ lệ theo ngày bắt đầu của dòng mới, `version`), có `end`, `transfer`, lọc theo `placeId` + `status`, `subtreeOf`, `personId`.

- [ ] Test `ownershipForm.test.ts`: `parsesSharesWithCommaOrDot`, `rejectsZeroOverOneHundredAndThreeDecimals`, `formatsSharesTheVietnameseWay`, `endDateCannotBeBeforeTheStart`.
- [ ] Test `PlaceOwners.test.tsx`: `listsCurrentOwnersWithSharesAndTotal` (hai dòng 50% và dòng tổng "100%"), `totalIsHiddenWhenNoShareIsRecorded`, `pastOwnersLoadOnlyWhenOpened`, `wholeBuildingShowsWhoOwnsEachPlace`, `addsAnOwnerWithAShare` (thân `{placeId, personId, sharePercent: 50, startDate}`), `addsAnOwnerWhoIsANewPerson` (`POST /people` rồi `POST /ownerships`), `shareMustBeBetweenZeroAndOneHundred` (`0`, `101`, `33,333` bị chặn trước khi gửi; `33,33` gửi `33.33`), `refusalAboutTheTotalIsShownAsIs`, `endsAnOwnership` (thân `{version, endDate}`), `editsShareAndDocument`, `deletesAMistakenRow`, `endedRowCannotBeEndedAgain`, `emptyTabInvitesToAddAnOwner`.
- [ ] Test `PlacePage.test.tsx`: `ownersTabSitsBeforeOccupants`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the owners tab to the place page`.

### Task 6: Frontend, chuyển nhượng

**Files:** tạo `frontend/src/features/ownership/components/TransferOwnershipDialog.tsx`; sửa `frontend/src/features/ownership/{api.ts,queries.ts,components/OwnershipActions.tsx}`; test `features/ownership/TransferOwnership.test.tsx`.

**Interfaces:**
- Produces:

```ts
transferOwnership(ws, id, body: S['OwnershipTransferRequest']): Promise<{ ended: Ownership; started: Ownership }>
useTransferOwnership(ws): UseMutationResult<{ ended: Ownership; started: Ownership }, ApiError, { ownership: Ownership; body: Omit<S['OwnershipTransferRequest'], 'version'> }>
export function TransferOwnershipDialog(props: { ws: string; ownership: Ownership; open: boolean; onOpenChange: (open: boolean) => void }): JSX.Element
```

  - Ô: người nhận (`PersonChoiceField`, nhãn "Chuyển cho"), ngày chuyển nhượng (mặc định hôm nay), số giấy tờ mới, ghi chú.
  - Dòng tóm tắt đổi theo lựa chọn: "<chủ cũ> thôi sở hữu từ hết ngày <ngày − 1>; <người nhận> sở hữu <tỷ lệ> từ <ngày>." (bỏ phần tỷ lệ khi dòng không ghi tỷ lệ).
  - Chặn trước khi gửi: chưa chọn người nhận; người nhận là chủ hiện tại ("Người nhận phải khác chủ hiện tại"); ngày không sau ngày bắt đầu của dòng.
  - Xong: thông báo "Đã chuyển nhượng cho <tên>".

- [ ] Test: `transferSaysWhatWillHappen`, `transfersToAnotherPerson` (thân `{version, toPersonId, transferDate}`; sau đó người nhận ở nhóm "Đang sở hữu" hoặc "Sắp tới", chủ cũ không còn trong "Đang sở hữu" khi ngày chuyển là hôm nay hoặc trước đó), `transfersToANewPerson`, `receiverMustDifferFromTheOwner`, `transferDateMustBeAfterTheStart`, `refusalBecauseTheReceiverAlreadyOwnsIsShownAsIs`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add transferring an ownership in the frontend`.

### Task 7: Frontend, tab "Sở hữu" trên trang một người

**Files:** tạo `frontend/src/features/ownership/components/PersonOwnershipsTab.tsx`; sửa `frontend/src/features/people/pages/PersonPage.tsx`, `frontend/src/features/ownership/index.ts`, `frontend/src/app/router.tsx`; test `features/ownership/PersonOwnerships.test.tsx`, `features/people/PersonPage.test.tsx`.

**Interfaces:**
- Produces:

```ts
export type PersonTabSlots = Partial<Record<'ownerships' | 'items', (ws: string, person: PersonDetail) => ReactNode>>
export function PersonOwnershipsTab(props: { ws: string; person: PersonDetail }): JSX.Element
```

  - Thứ tự tab của trang người: Thông tin, Nơi ở, Sở hữu, Đồ đạc, Lịch sử; tab chỉ có khi slot của nó được truyền; `?tab=ownerships`.
  - `PersonOwnershipsTab`: `OwnershipList` với `show="place"` và cột trạng thái, mới nhất trước; nút "Thêm chỗ sở hữu" mở `OwnershipFormDialog` với `person` cố định.

- [ ] Test: `listsWhatAPersonOwnsNewestFirst`, `addsAPlaceThisPersonOwns` (chọn địa điểm bằng `PlacePicker`; thân có `placeId` đã chọn), `pageWithoutSlotsHasNoOwnershipTab`, `deletingAnOwnerShowsWhyItIsBlocked` (409 của backend hiện nguyên văn trong hộp thoại xoá người).
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the ownership tab to the person page`.

### Task 8: Hoàn tất

- [ ] `docs/superpowers/specs/2026-11-13-business-rules-design.md`: mục 5.7 ghi OWNER không còn nhận cho dòng mới; thêm mục 5.7b "Sở hữu" trỏ sang `2026-11-16-ownership-design.md`; mục 5.4 và 5.6 thêm điều kiện xoá. Đổi trạng thái spec sở hữu thành "đã xây".
- [ ] `frontend/README.md`: feature `ownership` trong cây thư mục; luật phụ thuộc (`places` và `people` không nhập `ownership`); bảng "Sau một lệnh ghi" thêm dòng sở hữu (mọi lệnh làm cũ mọi danh sách sở hữu của không gian).
- [ ] Chạy thử trên backend thật: thêm hai chủ 50% cho một căn, thử thêm chủ thứ ba 10% (thấy lý do bị từ chối), chuyển nhượng phần của một người, mở trang người nhận thấy tab Sở hữu, thử xoá địa điểm còn chủ; chụp màn hình tab Sở hữu. Kiểm một dòng cư trú "Chủ" cũ (nếu có) đã có dòng sở hữu tương ứng. Xoá dữ liệu vừa tạo theo đúng `id`.
- [ ] `./mvnw verify` (chạy nền) xanh; `npm test`, `npm run build`, `npm run lint` sạch. Commit `Document place ownership` và push `main`.

## Để sau, có lý do

- **Chuyển nhượng một phần** và **gộp phần khi đồng sở hữu mua nốt**: phải tách hoặc gộp dòng tự động; spec chọn làm bằng tay (mục 2, 5.2). Làm khi người dùng gặp thật.
- **Tên chủ trên đầu trang địa điểm**: `places` không nhập `ownership`; cần thêm một slot ở `PlaceHeader`. Tab Sở hữu đã trả lời câu hỏi.
- **Đánh dấu "là chủ" trên dòng cư trú** (người ở đồng thời là chủ): cần backend ghép hai bảng cho từng trang; chờ có nhu cầu.
- **Chủ là tổ chức**: cần một loại bản ghi mới bên cạnh Người (spec mục 2).
- **Tự chuyển các dòng cư trú "Chủ" cũ thành "Người ở"**: hệ thống không biết chủ có ở đó hay không; để người dùng quyết.
