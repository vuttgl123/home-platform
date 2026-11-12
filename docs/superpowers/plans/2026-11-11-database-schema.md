# Database Schema Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tạo 14 bảng của ba nhóm identity, registry, media trong database `home_platform` bằng migration có phiên bản, kèm test chứng minh cấu trúc đúng với thiết kế.

**Architecture:** Flyway chạy khi ứng dụng khởi động và áp dụng ba file SQL, mỗi nhóm bảng một file. Test tích hợp (`*IT`, chạy ở `./mvnw verify`) kết nối database thật, đọc `information_schema` để so cấu trúc, và thử các ràng buộc bằng dữ liệu nằm trong transaction được rollback. `./mvnw test` và `./mvnw package` vẫn không cần database.

**Tech Stack:** MySQL 8.0.36, Flyway 12.4.0 (`spring-boot-starter-flyway` + `flyway-mysql`, phiên bản do Spring Boot 4.1.2 quản lý), JUnit + AssertJ, `JdbcTemplate`, maven-failsafe-plugin.

**Spec:** `docs/superpowers/specs/2026-11-11-database-design.md`. Mục 6 của spec là nguồn cho tên bảng, tên cột, kiểu dữ liệu và cột nào cho phép NULL.

## Môi trường chạy test

Máy phát triển không có Docker, nên không dựng được MySQL riêng cho test. Test tích hợp và migration chạy thẳng trên database `home_platform` của server UAT (profile `uat`, đang trống). Hệ quả:

- Lần chạy test tích hợp đầu tiên của mỗi task chính là lúc bảng được tạo thật.
- Dữ liệu test dùng `workspace_id` âm (`-1`, `-2`) và luôn được rollback, nên không để lại gì.
- Lệnh DDL của MySQL không rollback được. Nếu một file migration lỗi giữa chừng: xoá các bảng mang tiền tố của file đó (bảng con trước, bảng cha sau), xoá dòng của version đó trong `flyway_schema_history`, sửa file rồi chạy lại. Chỉ làm vậy với file chưa từng chạy thành công.

## Global Constraints

- MySQL 8.0.36, database `home_platform`. Mọi bảng dùng `ENGINE=InnoDB`, bảng mã `utf8mb4`, không ghi collation (dùng mặc định của server là `utf8mb4_0900_ai_ci`).
- Ba cột `path`, `geohash`, `checksum_sha256` khai báo `CHARACTER SET ascii COLLATE ascii_bin`.
- Khoá chính của mọi bảng `reg_` và `med_` bắt đầu bằng `workspace_id`. `id` là `BIGINT`, không `AUTO_INCREMENT`.
- Thời gian là `DATETIME(3)`. Các cột `created_at`, `updated_at`, `joined_at`, `added_at` là `NOT NULL DEFAULT (UTC_TIMESTAMP(3))`; không dùng `ON UPDATE`. `changed_at` là `NOT NULL`, không default.
- Không dùng `ENUM`.
- Cột là `NOT NULL` trừ khi spec ghi `NULL`.
- Default: `version` 1; `quantity` 1; `sort_order`, `depth`, `is_required`, `type_id` 0; `country_code` 'VN'; `status` 'ACTIVE', riêng `reg_item` là 'IN_USE' và `med_file` là 'PENDING'.
- `created_by`, `updated_by`, `changed_by`, `uploaded_by`, `added_by` là `BIGINT NOT NULL`, không default.
- Khoá ngoại chỉ nối các bảng cùng tiền tố, luôn gồm `workspace_id`, hành vi xoá mặc định (RESTRICT). Không có khoá ngoại giữa `idn_`, `reg_`, `med_`.
- `reg_entity_version` không có khoá ngoại và khai báo `PARTITION BY KEY (workspace_id) PARTITIONS 64`.
- `geohash` là cột thường, do ứng dụng tính (spec để ngỏ; quyết định ở đây).
- File migration đã chạy thành công thì không sửa; thay đổi tiếp theo là một file version mới.
- Không commit trong lúc thực hiện; chỉ commit khi người dùng yêu cầu.

## Review Focus

1. Tên có dấu tiếng Việt và emoji ghi vào rồi đọc ra phải y nguyên. Test: `vietnameseAndEmojiSurviveRoundTrip` (Task 2).
2. Địa điểm cấp cao nhất (`parent_id` NULL) phải tạo được dù khoá ngoại tới cha là khoá ghép. Test: `rootPlaceWithNullParentIsAccepted` (Task 2).
3. Hai không gian dùng cùng một mã loại địa điểm không được đụng nhau. Test: `sameCodeInTwoWorkspacesIsAcceptedButNotTwiceInOne` (Task 2).
4. Xoá cứng một địa điểm còn địa điểm con, còn người ở hoặc còn đồ phải bị chặn. Test: `hardDeleteOfPlaceInUseIsRejected` (Task 2).
5. Khởi động lại ứng dụng không chạy lại migration và không báo lệch checksum. Kiểm tra ở Task 3, bước 5 và 6.

## Ngoài phạm vi

Thư viện sinh `id`, dữ liệu tạo sẵn cho không gian mới, và mọi code ứng dụng đọc ghi các bảng này. Chúng thuộc về các tính năng đầu tiên dùng tới bảng.

## Cấu trúc file

| File | Trách nhiệm |
|---|---|
| `pom.xml` (sửa) | Thêm Flyway và bật failsafe |
| `src/main/resources/db/migration/V1__identity_schema.sql` | 3 bảng `idn_` |
| `src/main/resources/db/migration/V2__registry_schema.sql` | 9 bảng `reg_` |
| `src/main/resources/db/migration/V3__media_schema.sql` | 2 bảng `med_` |
| `src/test/java/com/home_platform/HomePlatformApplicationTests.java` (sửa) | Tắt Flyway để test này không cần database |
| `src/test/java/com/home_platform/schema/AbstractSchemaIT.java` | Khung chung và hàm đọc `information_schema` |
| `src/test/java/com/home_platform/schema/IdentitySchemaIT.java` | Test nhóm identity |
| `src/test/java/com/home_platform/schema/RegistrySchemaIT.java` | Test nhóm registry |
| `src/test/java/com/home_platform/schema/MediaSchemaIT.java` | Test nhóm media |

Khi tách thành nhiều service, mỗi service mang file của mình đi làm version đầu tiên, với bảng lịch sử Flyway riêng.

## Tên index và khoá ngoại

Mọi task dùng đúng các tên dưới đây.

| Bảng | Index (cột theo thứ tự) | Khoá ngoại |
|---|---|---|
| `idn_user` | `uk_idn_user_email` UNIQUE (email) | |
| `idn_workspace` | `idx_idn_workspace_owner` (owner_user_id) | `fk_idn_workspace_owner` → `idn_user (id)` |
| `idn_workspace_member` | `idx_idn_member_user` (user_id, workspace_id) | `fk_idn_member_workspace` → `idn_workspace (id)`; `fk_idn_member_user` → `idn_user (id)` |
| `reg_place_type` | `uk_reg_place_type_code` UNIQUE (workspace_id, code) | |
| `reg_field_def` | `uk_reg_field_def_key` UNIQUE (workspace_id, target, type_id, field_key) | |
| `reg_place` | `idx_reg_place_parent` (workspace_id, parent_id, name); `idx_reg_place_path` (workspace_id, path); `idx_reg_place_type` (workspace_id, place_type_id, name); `idx_reg_place_name` (workspace_id, name); `idx_reg_place_code` (workspace_id, code) | `fk_reg_place_parent` (workspace_id, parent_id) → `reg_place`; `fk_reg_place_type` (workspace_id, place_type_id) → `reg_place_type` |
| `reg_place_address` | `idx_reg_address_area` (workspace_id, province_code, ward_code); `idx_reg_address_geohash` (workspace_id, geohash) | `fk_reg_address_place` (workspace_id, place_id) → `reg_place` |
| `reg_person` | `idx_reg_person_name` (workspace_id, full_name); `idx_reg_person_phone` (workspace_id, phone); `idx_reg_person_id_number` (workspace_id, id_number) | |
| `reg_occupancy` | `idx_reg_occupancy_place` (workspace_id, place_id, end_date, start_date); `idx_reg_occupancy_person` (workspace_id, person_id, start_date) | `fk_reg_occupancy_place` → `reg_place`; `fk_reg_occupancy_person` → `reg_person` |
| `reg_item_category` | `uk_reg_item_category_name` UNIQUE (workspace_id, name) | |
| `reg_item` | `idx_reg_item_place` (workspace_id, place_id, removed_at, name); `idx_reg_item_category` (workspace_id, category_id, name); `idx_reg_item_name` (workspace_id, name); `idx_reg_item_code` (workspace_id, code); `idx_reg_item_replaced_by` (workspace_id, replaced_by_item_id) | `fk_reg_item_place` → `reg_place`; `fk_reg_item_category` → `reg_item_category`; `fk_reg_item_replaced_by` → `reg_item` |
| `reg_entity_version` | `idx_reg_version_changed_at` (workspace_id, changed_at) | |
| `med_file` | `idx_med_file_checksum` (workspace_id, checksum_sha256) | |
| `med_attachment` | `idx_med_attachment_owner` (workspace_id, owner_type, owner_id, removed_at, sort_order); `idx_med_attachment_file` (workspace_id, file_id) | `fk_med_attachment_file` (workspace_id, file_id) → `med_file` |

---

### Task 1: Flyway, khung test và nhóm identity

**Files:**
- Modify: `pom.xml`
- Modify: `src/test/java/com/home_platform/HomePlatformApplicationTests.java`
- Create: `src/test/java/com/home_platform/schema/AbstractSchemaIT.java`
- Create: `src/test/java/com/home_platform/schema/IdentitySchemaIT.java`
- Create: `src/main/resources/db/migration/V1__identity_schema.sql`

**Interfaces:**
- Produces: lớp `AbstractSchemaIT` (gói `com.home_platform.schema`) mà Task 2 và 3 kế thừa:

```java
@SpringBootTest
@Transactional
abstract class AbstractSchemaIT {
    protected static final long WS_A = -1L;
    protected static final long WS_B = -2L;
    @Autowired protected JdbcTemplate jdbc;

    /** Tên các bảng của database hiện tại bắt đầu bằng prefix, xếp theo tên. */
    protected List<String> tables(String prefix)
    /** Các cột khoá chính theo thứ tự trong khoá. */
    protected List<String> primaryKey(String table)
    /** Các cột của một index theo thứ tự; danh sách rỗng nếu index không tồn tại. */
    protected List<String> index(String table, String indexName)
}
```

- [x] **Step 1: Sửa `pom.xml` và test hiện có**

Thêm hai dependency `org.springframework.boot:spring-boot-starter-flyway` và `org.flywaydb:flyway-mysql` (không ghi version). Thêm plugin `org.apache.maven.plugins:maven-failsafe-plugin` vào `<build><plugins>` (không cấu hình gì thêm; Spring Boot parent đã khai báo sẵn hai goal). Đổi annotation của `HomePlatformApplicationTests` thành `@SpringBootTest(properties = "spring.flyway.enabled=false")`.

- [x] **Step 2: Viết `AbstractSchemaIT` và test identity**

Ba hàm của `AbstractSchemaIT` đọc `information_schema.tables` và `information_schema.statistics` với `table_schema = DATABASE()`. `tables` so tiền tố bằng `LEFT(table_name, ?) = ?` vì `_` là ký tự đại diện của `LIKE`. `primaryKey(t)` là `index(t, "PRIMARY")`.

```java
class IdentitySchemaIT extends AbstractSchemaIT {

    @Test
    void identityTablesExist() {
        assertThat(tables("idn_")).containsExactly("idn_user", "idn_workspace", "idn_workspace_member");
    }

    @Test
    void keysAndIndexesMatchDesign() {
        assertThat(primaryKey("idn_user")).containsExactly("id");
        assertThat(primaryKey("idn_workspace")).containsExactly("id");
        assertThat(primaryKey("idn_workspace_member")).containsExactly("workspace_id", "user_id");
        assertThat(index("idn_user", "uk_idn_user_email")).containsExactly("email");
        assertThat(index("idn_workspace", "idx_idn_workspace_owner")).containsExactly("owner_user_id");
        assertThat(index("idn_workspace_member", "idx_idn_member_user")).containsExactly("user_id", "workspace_id");
    }

    @Test
    void emailIsUnique() {
        jdbc.update("INSERT INTO idn_user (id, email, display_name) VALUES (-1, 'a@example.test', 'A')");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO idn_user (id, email, display_name) VALUES (-2, 'a@example.test', 'B')"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void workspaceOwnerMustExist() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO idn_workspace (id, owner_user_id, name) VALUES (-1, -999, 'W')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void defaultsFollowDesign() {
        jdbc.update("INSERT INTO idn_user (id, email, display_name) VALUES (-1, 'a@example.test', 'A')");
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, ABS(TIMESTAMPDIFF(SECOND, created_at, UTC_TIMESTAMP(3))) AS age FROM idn_user WHERE id = -1");
        assertThat(row.get("status")).isEqualTo("ACTIVE");
        assertThat(((Number) row.get("age")).longValue()).isLessThanOrEqualTo(5L);
    }
}
```

- [x] **Step 3: Chạy để thấy test hỏng**

Run: `./mvnw -B verify -Dit.test=IdentitySchemaIT`
Expected: FAIL. `identityTablesExist` báo danh sách bảng rỗng, hoặc context không khởi động vì chưa có migration nào.

- [x] **Step 4: Viết `V1__identity_schema.sql`**

Ba lệnh `CREATE TABLE` cho `idn_user`, `idn_workspace`, `idn_workspace_member` theo mục 6 của spec, Global Constraints và bảng tên index ở trên.

- [x] **Step 5: Chạy để thấy test đạt**

Run: `./mvnw -B verify -Dit.test=IdentitySchemaIT`
Expected: `Tests run: 5, Failures: 0, Errors: 0` ở failsafe, `BUILD SUCCESS`; log có dòng Flyway báo đã áp dụng version 1.

---

### Task 2: Nhóm registry

**Files:**
- Create: `src/test/java/com/home_platform/schema/RegistrySchemaIT.java`
- Create: `src/main/resources/db/migration/V2__registry_schema.sql`

**Interfaces:**
- Consumes: `AbstractSchemaIT` của Task 1 (`WS_A`, `WS_B`, `jdbc`, `tables`, `primaryKey`, `index`).
- Produces: không có gì cho task khác.

- [x] **Step 1: Viết test**

Các hàm chèn dữ liệu dưới đây là hàm private của lớp test; mỗi hàm chèn một dòng chỉ gồm các cột bắt buộc không có default, với `created_by` và `updated_by` (hoặc `changed_by`) bằng `-1`:

```java
private void type(long ws, long id, String code)                       // reg_place_type, name = code
private void place(long ws, long id, Long parentId, long typeId, String path, String name)
private void person(long ws, long id, String fullName)
private void occupancy(long ws, long id, long placeId, long personId)  // relation 'RESIDENT', start_date 2026-01-01
private void item(long ws, long id, long placeId, String name)
private void version(long ws, String entityType, long entityId, int versionNo)  // action 'UPDATE', snapshot '{}', changed_at UTC_TIMESTAMP(3)
```

```java
class RegistrySchemaIT extends AbstractSchemaIT {

    @Test
    void registryTablesExist() {
        assertThat(tables("reg_")).containsExactly(
                "reg_entity_version", "reg_field_def", "reg_item", "reg_item_category", "reg_occupancy",
                "reg_person", "reg_place", "reg_place_address", "reg_place_type");
    }

    @Test
    void primaryKeysStartWithWorkspaceId() {
        assertThat(tables("reg_")).allSatisfy(t -> assertThat(primaryKey(t)).startsWith("workspace_id"));
        assertThat(primaryKey("reg_place")).containsExactly("workspace_id", "id");
        assertThat(primaryKey("reg_place_address")).containsExactly("workspace_id", "place_id");
        assertThat(primaryKey("reg_entity_version"))
                .containsExactly("workspace_id", "entity_type", "entity_id", "version_no");
    }

    @Test
    void indexesMatchDesign() {
        // Mỗi dòng của bảng "Tên index và khoá ngoại" thuộc nhóm reg_ là một phần tử:
        // khoá "bảng.index", giá trị là danh sách cột theo thứ tự.
        Map<String, List<String>> expected = /* 21 index của nhóm reg_, chép từ bảng trên */;
        expected.forEach((key, columns) -> {
            String[] parts = key.split("\\.");
            assertThat(index(parts[0], parts[1])).as(key).isEqualTo(columns);
        });
    }

    @Test
    void versionTableHas64Partitions() {
        Integer partitions = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.partitions WHERE table_schema = DATABASE() AND table_name = 'reg_entity_version'",
                Integer.class);
        assertThat(partitions).isEqualTo(64);
    }

    @Test
    void rootPlaceWithNullParentIsAccepted() {
        type(WS_A, 10, "HOTEL");
        place(WS_A, 100, null, 10, "/100/", "A");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reg_place WHERE workspace_id = ? AND id = 100", Integer.class, WS_A))
                .isEqualTo(1);
    }

    @Test
    void parentFromAnotherWorkspaceIsRejected() {
        type(WS_A, 10, "HOTEL");
        type(WS_B, 10, "HOTEL");
        place(WS_A, 100, null, 10, "/100/", "A");
        assertThatThrownBy(() -> place(WS_B, 200, 100L, 10, "/100/200/", "B"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameCodeInTwoWorkspacesIsAcceptedButNotTwiceInOne() {
        type(WS_A, 10, "HOTEL");
        type(WS_B, 10, "HOTEL");
        assertThatThrownBy(() -> type(WS_A, 11, "HOTEL")).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void hardDeleteOfPlaceInUseIsRejected() {
        type(WS_A, 10, "HOTEL");
        place(WS_A, 100, null, 10, "/100/", "A");
        place(WS_A, 101, 100L, 10, "/100/101/", "P101");
        place(WS_A, 102, 100L, 10, "/100/102/", "P102");
        person(WS_A, 500, "An");
        occupancy(WS_A, 600, 101, 500);
        item(WS_A, 700, 102, "Tủ lạnh");
        for (long placeId : List.of(100L, 101L, 102L)) {
            assertThatThrownBy(() -> jdbc.update("DELETE FROM reg_place WHERE workspace_id = ? AND id = ?", WS_A, placeId))
                    .as("place " + placeId).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Test
    void vietnameseAndEmojiSurviveRoundTrip() {
        type(WS_A, 10, "HOTEL");
        place(WS_A, 100, null, 10, "/100/", "Nhà nghỉ Đường Số 5 🏠");
        assertThat(jdbc.queryForObject("SELECT name FROM reg_place WHERE workspace_id = ? AND id = 100", String.class, WS_A))
                .isEqualTo("Nhà nghỉ Đường Số 5 🏠");
    }

    @Test
    void defaultsFollowDesign() {
        type(WS_A, 10, "HOTEL");
        place(WS_A, 100, null, 10, "/100/", "A");
        item(WS_A, 700, 100, "Ghế");
        Map<String, Object> place = jdbc.queryForMap(
                "SELECT version, status, depth, deleted_at FROM reg_place WHERE workspace_id = ? AND id = 100", WS_A);
        assertThat(place.get("version")).isEqualTo(1);
        assertThat(place.get("status")).isEqualTo("ACTIVE");
        assertThat(((Number) place.get("depth")).intValue()).isZero();
        assertThat(place.get("deleted_at")).isNull();
        Map<String, Object> item = jdbc.queryForMap(
                "SELECT quantity, status, removed_at FROM reg_item WHERE workspace_id = ? AND id = 700", WS_A);
        assertThat(item.get("quantity")).isEqualTo(1);
        assertThat(item.get("status")).isEqualTo("IN_USE");
        assertThat(item.get("removed_at")).isNull();
    }

    @Test
    void versionNumbersAreUniquePerEntity() {
        version(WS_A, "PLACE", 100, 1);
        version(WS_A, "PLACE", 100, 2);
        version(WS_A, "PERSON", 100, 2);
        assertThatThrownBy(() -> version(WS_A, "PLACE", 100, 2)).isInstanceOf(DuplicateKeyException.class);
    }
}
```

- [x] **Step 2: Chạy để thấy test hỏng**

Run: `./mvnw -B verify -Dit.test=RegistrySchemaIT`
Expected: FAIL. `registryTablesExist` báo danh sách rỗng; các test chèn dữ liệu báo bảng không tồn tại.

- [x] **Step 3: Viết `V2__registry_schema.sql`**

Chín lệnh `CREATE TABLE` theo mục 6 của spec, Global Constraints và bảng tên index. Thứ tự tạo: `reg_place_type`, `reg_field_def`, `reg_place`, `reg_place_address`, `reg_person`, `reg_occupancy`, `reg_item_category`, `reg_item`, `reg_entity_version`.

- [x] **Step 4: Chạy để thấy test đạt**

Run: `./mvnw -B verify -Dit.test=RegistrySchemaIT`
Expected: `Tests run: 11, Failures: 0, Errors: 0` ở failsafe, `BUILD SUCCESS`; log có dòng Flyway báo đã áp dụng version 2.

---

### Task 3: Nhóm media và kiểm tra toàn bộ

**Files:**
- Create: `src/test/java/com/home_platform/schema/MediaSchemaIT.java`
- Create: `src/main/resources/db/migration/V3__media_schema.sql`

**Interfaces:**
- Consumes: `AbstractSchemaIT` của Task 1.
- Produces: không có gì cho task khác.

- [x] **Step 1: Viết test**

Hai hàm private: `file(long ws, long id)` chèn một dòng `med_file` (`storage_key` = `"k/" + id`, `original_name` 'a.jpg', `content_type` 'image/jpeg', `size_bytes` 1, `checksum_sha256` là 64 ký tự '0', `uploaded_by` -1) và `attachment(long ws, long id, long fileId, long ownerId)` chèn một dòng `med_attachment` (`owner_type` 'PLACE', `added_by` -1).

```java
class MediaSchemaIT extends AbstractSchemaIT {

    @Test
    void mediaTablesExist() {
        assertThat(tables("med_")).containsExactly("med_attachment", "med_file");
    }

    @Test
    void keysAndIndexesMatchDesign() {
        assertThat(primaryKey("med_file")).containsExactly("workspace_id", "id");
        assertThat(primaryKey("med_attachment")).containsExactly("workspace_id", "id");
        assertThat(index("med_file", "idx_med_file_checksum")).containsExactly("workspace_id", "checksum_sha256");
        assertThat(index("med_attachment", "idx_med_attachment_owner"))
                .containsExactly("workspace_id", "owner_type", "owner_id", "removed_at", "sort_order");
        assertThat(index("med_attachment", "idx_med_attachment_file")).containsExactly("workspace_id", "file_id");
    }

    @Test
    void attachmentMustPointToFileInSameWorkspace() {
        file(WS_A, 900);
        assertThatThrownBy(() -> attachment(WS_B, 950, 900, 100)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void attachmentOwnerIsNotTiedToRegistryTables() {
        file(WS_A, 900);
        attachment(WS_A, 950, 900, 123456789L);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT sort_order, removed_at FROM med_attachment WHERE workspace_id = ? AND id = 950", WS_A);
        assertThat(((Number) row.get("sort_order")).intValue()).isZero();
        assertThat(row.get("removed_at")).isNull();
    }

    @Test
    void newFileStartsAsPending() {
        file(WS_A, 900);
        assertThat(jdbc.queryForObject("SELECT status FROM med_file WHERE workspace_id = ? AND id = 900", String.class, WS_A))
                .isEqualTo("PENDING");
    }
}
```

- [x] **Step 2: Chạy để thấy test hỏng**

Run: `./mvnw -B verify -Dit.test=MediaSchemaIT`
Expected: FAIL. `mediaTablesExist` báo danh sách rỗng.

- [x] **Step 3: Viết `V3__media_schema.sql`**

Hai lệnh `CREATE TABLE` cho `med_file` rồi `med_attachment`, theo mục 6 của spec, Global Constraints và bảng tên index.

- [x] **Step 4: Chạy để thấy test đạt**

Run: `./mvnw -B verify -Dit.test=MediaSchemaIT`
Expected: `Tests run: 5, Failures: 0, Errors: 0` ở failsafe, `BUILD SUCCESS`.

- [x] **Step 5: Chạy toàn bộ, hai lần liên tiếp**

Run: `./mvnw -B verify` rồi chạy lại đúng lệnh đó.
Expected cả hai lần: surefire `Tests run: 1`, failsafe `Tests run: 21`, `BUILD SUCCESS`. Lần thứ hai log Flyway báo schema đã ở version 3 và không áp dụng gì.

- [x] **Step 6: Kiểm tra trạng thái database và ứng dụng**

- `SELECT version, success FROM flyway_schema_history ORDER BY installed_rank` trả đúng ba dòng `1`, `2`, `3`, đều `success = 1`.
- `information_schema.tables` của `home_platform` có đúng 15 bảng: 14 bảng thiết kế và `flyway_schema_history`.
- Không còn dòng nào có `workspace_id < 0` trong `reg_place`, `reg_entity_version`, `med_file`; không còn dòng nào có `id < 0` trong `idn_user`.
- Khởi động ứng dụng bằng file jar, gọi `/actuator/health`: trả `UP`.
