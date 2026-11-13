# Registry Items Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bước 4 của logic nghiệp vụ: đồ đạc đặt trong địa điểm, với tạo, sửa, chuyển chỗ, gỡ, thay, đưa lại, xoá, khôi phục, các danh sách và lịch sử version.

**Architecture:** Package `com.home_platform.registry.item`, ba tầng như bước 2 và 3. Version với `entityType` `ITEM`. `ItemService` dùng `PlaceService` (địa điểm chưa xoá, tên địa điểm) và `ItemCategoryService` (nhóm dùng được, tên nhóm).

**Tech Stack:** như bước 3.

**Spec:** `docs/superpowers/specs/2026-11-13-business-rules-design.md` mục 3, 5.9, 5.10.

## Global Constraints

- Mọi ràng buộc chung của kế hoạch bước 3 (`docs/superpowers/plans/2026-11-13-registry-people.md`) vẫn áp dụng, kể cả cách chạy test mỗi task (test thường cộng `Item*IT`; bộ đầy đủ một lần ở task cuối).
- Request ghi lên đồ đạc dùng `note` cho ghi chú của version, như địa điểm (đồ đạc không có trường `note` riêng; mô tả là `description`).
- Lệnh sửa không đổi `placeId`, `removedAt`, `replacedByItemId`; mỗi thứ có thao tác riêng. `status` sửa được chỉ là IN_USE, BROKEN, STORED; REMOVED chỉ đến từ gỡ hoặc thay.
- Món đã gỡ chỉ đọc được, trừ "đưa lại" và xoá: sửa, chuyển, gỡ, thay, khôi phục version trên món đã gỡ trả `ConflictException("Item <id> is removed")`.
- Nhóm đồ: chọn nhóm mới thì phải `ACTIVE`; giữ nguyên nhóm cũ đã lưu trữ thì được (như loại địa điểm). `attributes` kiểm tra với `FieldTarget.ITEM` và `typeId` là `categoryId`, hoặc 0 khi không có nhóm.

## Review Focus

1. Thay một món: món cũ và món mới ghi trong một transaction; món mới sai (thiếu tên) thì món cũ không bị gỡ. Test: `invalidReplacementLeavesTheOldItemAlone` (Task 2).
2. Đưa lại một món đã bị thay: 409, không có hai món cùng "đang dùng" cho một chỗ. Test: `replacedItemCannotBeReturned` (Task 2).
3. Gỡ với ngày trước ngày đưa vào: 400. Test: `removalDateCannotPrecedePlacement` (Task 2).
4. Đồ trong cả toà nhà không gồm đồ đã gỡ và đồ ở phòng đã xoá. Test: `subtreeSkipsRemovedItemsAndDeletedPlaces` (Task 3).
5. Khôi phục version cũ không đưa món về chỗ cũ, không bỏ gỡ. Test: `restoresContentButNotPositionOrRemoval` (Task 4).

---

### Task 1: Tạo, xem, sửa

**Files:** `registry-service/src/main/java/com/home_platform/registry/item/{ItemStatus,Item,ItemRepository,ItemSnapshot,ItemService,ItemController,ItemMapper,ItemRequest,ItemUpdateRequest,ItemView,ItemResponse,ItemDetailResponse}.java`; test `.../item/{ItemTestData,ItemIT}.java`.

```java
public enum ItemStatus { IN_USE, BROKEN, STORED, REMOVED }

@With
public record Item(long workspaceId, long id, long placeId, Long categoryId, String code, String name, int quantity,
		String unit, ItemStatus status, LocalDate placedAt, LocalDate removedAt, Long replacedByItemId,
		String description, @JsonSerialize(contentUsing = PlainNumbers.class) @JsonColumn Map<String, Object> attributes,
		Long coverFileId, Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version,
		Instant deletedAt) {}

public record ItemSnapshot(long id, long placeId, Long categoryId, String code, String name, int quantity, String unit,
		ItemStatus status, LocalDate placedAt, LocalDate removedAt, Long replacedByItemId, String description,
		Map<String, Object> attributes, Long coverFileId, Instant deletedAt) {      // attributes có PlainNumbers
	public static ItemSnapshot of(Item item)
}

public record ItemView(Item item, String placeName, String categoryName) {}

// ItemService
public static final String ENTITY_TYPE = "ITEM";
public Item get(long workspaceId, long id)
public ItemView view(long workspaceId, long id)
public Item create(WorkspaceContext ctx, ItemRequest request)
public Item update(WorkspaceContext ctx, long id, ItemUpdateRequest request)
```

  - `ItemRequest(Long placeId, Long categoryId, String code, String name, Integer quantity, String unit, ItemStatus status, LocalDate placedAt, String description, Map<String, Object> attributes, Long coverFileId, String note)`: `placeId` `@NotNull`; `name` `@NotBlank @Size(max = 255)`; `code` `@Size(max = 50)`; `quantity` `@Min(1)`, null thành 1; `unit` `@Size(max = 20)`; `status` null thành IN_USE, REMOVED thì `BadRequestException("status REMOVED is set by removing or replacing an item")`; `description` `@Size(max = 10000)`; `note` `@Size(max = 255)`.
  - `ItemUpdateRequest`: `Integer version` (`@NotNull`) rồi các trường của `ItemRequest` trừ `placeId`; `status` `@NotNull`.
  - HTTP: `POST /items` 201, `GET /items/{id}`, `PUT /items/{id}`; `ItemDetailResponse` là mọi trường trừ `workspaceId`, thêm `placeName`, `categoryName`.

Test (`ItemIT`): `createsAnItemInAPlace` (201, quantity 1, status IN_USE, placeName, version 1 CREATE); `placeMustBeAlive` (đã xoá hoặc của WS_B -> 404); `archivedCategoryIsRejectedForNewItems` (409); `keepingAnArchivedCategoryIsAllowed`; `statusRemovedCannotBeSetDirectly` (400); `quantityMustBePositive` (0 -> 400); `attributesFollowTheCategory` (mục ITEM của nhóm Điện tử `warranty_until` DATE: món thuộc nhóm đó nhận được, món không nhóm gửi khoá đó -> 400); `updateWritesFieldsAndChecksVersion`; `updateNeverMovesTheItem` (body có `placeId` -> bỏ qua); `otherWorkspaceSeesNothing`.

Commit `Add items: create, read, update with versions`.

### Task 2: Chuyển, gỡ, thay, đưa lại, xoá, khôi phục

**Files:** `item/{ItemMoveRequest,ItemRemoveRequest,ItemReplaceRequest,ItemReplaceResponse,ItemVersionRequest}.java`; sửa `ItemService`, `ItemController`; test `.../item/ItemLifecycleIT.java`.

```java
public record ItemMoveRequest(@NotNull Integer version, @NotNull Long placeId, @Size(max = 255) String note) {}
public record ItemRemoveRequest(@NotNull Integer version, LocalDate removedAt, @Size(max = 255) String note) {}   // removedAt null: hôm nay
public record ItemReplaceRequest(@NotNull Integer version, LocalDate removedAt, @NotNull @Valid ItemRequest replacement) {}
	// replacement.placeId null: cùng chỗ với món cũ (ràng buộc @NotNull của placeId bỏ qua ở đây, xem dưới)
public record ItemReplaceResponse(ItemDetailResponse removed, ItemDetailResponse replacement) {}
public record ItemVersionRequest(@NotNull Integer version, @Size(max = 255) String note) {}   // đưa lại, khôi phục

// ItemService
public Item move(WorkspaceContext ctx, long id, ItemMoveRequest request)
public Item remove(WorkspaceContext ctx, long id, ItemRemoveRequest request)
public Item[] replace(WorkspaceContext ctx, long id, ItemReplaceRequest request)   // [cũ, mới]
public Item putBack(WorkspaceContext ctx, long id, ItemVersionRequest request)
public void delete(WorkspaceContext ctx, long id, int version, String note)
public Item restore(WorkspaceContext ctx, long id, ItemVersionRequest request)
```

  - Vì `replacement.placeId` được để trống, `ItemRequest.placeId` bỏ `@NotNull`; `create` tự kiểm tra và ném `BadRequestException("placeId is required")`.
  - Gỡ: `removedAt` trước `placedAt`: `BadRequestException("removedAt must not be before placedAt")`. Đặt `removedAt`, `status` REMOVED.
  - Thay: món mới tạo trước (nhận `id`), rồi gỡ món cũ với `replacedByItemId`; version CREATE cho món mới, UPDATE cho món cũ; tất cả trong một transaction.
  - Đưa lại: món chưa gỡ: `ConflictException("Item <id> is not removed")`; đã bị thay: `ConflictException("Item <id> was replaced by item <newId>")`. Bỏ `removedAt`, `status` IN_USE.
  - Khôi phục (bỏ xoá): địa điểm của món đang bị xoá: `ConflictException("Restore place <placeId> first")`.
  - HTTP: `POST /items/{id}/move`, `/remove`, `/replace` (trả `ItemReplaceResponse`), `/put-back`, `/restore`; `DELETE /items/{id}?version=&note=` 204.

Test (`ItemLifecycleIT`): `movesToAnotherPlace` (version 2, changedFields chứa placeId); `cannotMoveIntoADeletedPlace` (404); `removesAndKeepsTheItemInRemovedList`; `removalDateCannotPrecedePlacement`; `removedItemIsReadOnly` (PUT, move, remove -> 409 "Item <id> is removed"); `replacesInOneStep` (cũ REMOVED, replacedByItemId = mới; mới ở cùng chỗ; cả hai có version); `replacementCanGoElsewhere`; `invalidReplacementLeavesTheOldItemAlone` (replacement thiếu name -> 400, cũ vẫn IN_USE version 1); `putsBackARemovedItem`; `replacedItemCannotBeReturned`; `deletesAndRestores`; `restoreNeedsThePlaceAlive`.

Commit `Add moving, removing, replacing and restoring items`.

### Task 3: Danh sách và tìm kiếm

**Files:** sửa `ItemRepository`, `ItemService`, `ItemController`; test `.../item/ItemListIT.java`.

```java
// ItemService, mỗi hàm trả CursorPage<ItemView>, chỉ món chưa xoá
public CursorPage<ItemView> atPlace(long workspaceId, long placeId, boolean removed, CursorRequest page)
	// removed false: removed_at IS NULL, theo tên; true: removed_at IS NOT NULL, gỡ gần nhất trước
public CursorPage<ItemView> inSubtree(long workspaceId, long rootPlaceId, CursorRequest page)   // chưa gỡ, địa điểm chưa xoá, theo tên
public CursorPage<ItemView> byCategory(long workspaceId, long categoryId, CursorRequest page)   // chưa gỡ, theo tên
public CursorPage<ItemView> searchByName(long workspaceId, String prefix, CursorRequest page)  // cả món đã gỡ, theo tên
public CursorPage<ItemView> findByCode(long workspaceId, String code, CursorRequest page)
```

  - HTTP: `GET /items?placeId=&removed=` | `?subtreeOf=` | `?categoryId=` (đúng một: `BadRequestException("Give exactly one of placeId, subtreeOf or categoryId")`); `GET /items/search?name=` | `?code=` (`"Give exactly one of name or code"`). Phản hồi là `ItemResponse` (`id, placeId, placeName, categoryId, categoryName, code, name, quantity, unit, status, removedAt, version`).
  - Tên địa điểm và tên nhóm lấy theo lô (`PlaceService.names`, `ItemCategoryService.list`).

Test (`ItemListIT`): `currentAndRemovedAtAPlace`; `subtreeSkipsRemovedItemsAndDeletedPlaces`; `byCategoryListsCurrentItems`; `searchByNameIncludesRemovedItems`; `searchByCodeMatchesExactly`; `exactlyOneFilter`.

Commit `Add item listings and search`.

### Task 4: Lịch sử

**Files:** `item/{ItemVersionResponse,ItemVersionSummaryResponse}.java`; sửa `ItemService`, `ItemController`, `ItemMapper`; test `.../item/ItemVersionIT.java`.

  - `GET /items/{id}/versions`, `GET /items/{id}/versions/{no}`, `POST /items/{id}/versions/{no}/restore` (body `ItemVersionRequest`), như địa điểm. Khôi phục chỉ đổi `code`, `name`, `categoryId`, `quantity`, `unit`, `status` (giữ status hiện tại nếu version cũ là REMOVED), `placedAt`, `description`, `attributes`, `coverFileId`; kiểm tra như một lần sửa; note "Restored from version <n>". Món đã xoá: `ConflictException("Restore item <id> before restoring an old version")`; món đã gỡ: "Item <id> is removed".

Test (`ItemVersionIT`): `listsAndShowsVersions`; `restoresContentButNotPositionOrRemoval`; `restoringARemovedItemIsAConflict`.

Commit `Add item version history`.

### Task 5: Tài liệu, kiểm tra toàn bộ, chạy thử

README thêm `/items...`; `./mvnw verify` toàn dự án; chạy thử với `workspace_id = -9` (phòng, món, thay món, xem danh sách đã gỡ); dọn mọi dòng của `-9` (đồ đạc theo thứ tự món thay trước, rồi các bảng như bước 3); commit `Document registry items` và push `main`.
