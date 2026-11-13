# home-platform

Kho hồ sơ về các địa điểm, tổ chức theo microservice. Thiết kế database nằm ở
`docs/superpowers/specs/2026-11-11-database-design.md`.

## Các module

| Module | Cổng | Nội dung |
|---|---|---|
| `platform-common` | | Phần nền dùng chung: người gọi, quy ước JSON, lỗi API, phân trang, repository tổng quát, lịch sử version, Swagger, cấu hình MapStruct, sinh `id`, log, cache Caffeine. Test-jar có khung test repository và bộ luật kiến trúc |
| `identity-service` | 8081 | Người dùng, không gian, thành viên (bảng `idn_`) |
| `registry-service` | 8082 | Địa điểm, địa chỉ, người, đồ đạc, version (bảng `reg_`) |
| `media-service` | 8083 | Thông tin file và việc gắn file vào bản ghi (bảng `med_`) |

Ba service dùng chung một database MySQL. Giữa các service không có join và không có khoá
ngoại; chúng chỉ lưu `id` của nhau.

## Chuẩn bị

Cần Java 21 và MySQL 8.0. Trong **mỗi** service, tạo hai file
`src/main/resources/application-uat.yaml` và `src/main/resources/application-prod.yaml`.
Hai file này không được đưa lên git.

```yaml
spring:
  datasource:
    url: jdbc:mysql://<host>:3306/<database>
    username: <user>
    password: <password>
```

Profile mặc định là `uat`.

Muốn chạy `./mvnw verify` cho `platform-common`, tạo thêm
`platform-common/src/test/resources/application-uat.yaml` với cùng phần `spring.datasource`
(chép từ file của một service). File này cũng không được đưa lên git. Test của module này
chỉ dùng bảng tạm (`CREATE TEMPORARY TABLE`), nên database không bị thêm bảng nào; tài khoản
database cần quyền `CREATE TEMPORARY TABLES`.

## Chạy và kiểm tra

```bash
./mvnw package -DskipTests                                         # đóng gói mọi module
java -jar registry-service/target/registry-service-0.0.1-SNAPSHOT.jar   # chạy một service

./mvnw test      # test thường, không cần database
./mvnw verify    # thêm test cấu trúc database, cần kết nối được database
```

Khi khởi động, mỗi service tự chạy các file migration của mình (Flyway) rồi mới nhận request.
Kiểm tra nhanh: `curl localhost:8082/actuator/health`. Tài liệu API của mỗi service nằm ở
`/swagger-ui.html` (dạng máy đọc: `/v3/api-docs`).

## Quy ước

- **Package:** gốc của mỗi service là `com.home_platform.<service>`. Code chia theo nghiệp vụ,
  ví dụ `com.home_platform.registry.place` chứa `PlaceController`, `PlaceService`,
  `PlaceRepository`.
- **Ba tầng:** controller gọi service, service gọi repository. Controller không gọi thẳng
  repository; request và response là kiểu riêng, không trả thẳng record của bảng.
- **Người gọi:** controller khai báo một tham số `WorkspaceContext` (API trong một không gian)
  hoặc `UserContext` (API không gắn không gian). Giá trị lấy từ header `X-Workspace-Id` và
  `X-User-Id`; thiếu hoặc sai trả 400. Context được truyền xuống service và repository như
  tham số thường. Khi có đăng nhập, thay bằng một bean `ContextResolver` khác; controller không
  phải đổi.
- **JSON:** mọi giá trị `long` ra API dưới dạng chuỗi (`"id": "114540094935228416"`), `int`
  vẫn là số. Dùng `long` cho `id` và số rất lớn, `int` cho số đếm. Thời điểm ghi dạng ISO-8601
  giờ UTC. JSON lưu trong database (cột `@JsonColumn`, `snapshot`) giữ số là số.
- **Repository:** mỗi bảng có khoá `(workspace_id, id)` là một record cộng một lớp kế thừa
  `WorkspaceRepository<Record>`:

  ```java
  @Repository
  class PlaceRepository extends WorkspaceRepository<Place> {
      PlaceRepository(RepositorySupport support) { super(support, "reg_place", Place.class); }

      CursorPage<Place> listChildren(long workspaceId, long parentId, CursorRequest page) {
          return keyset().where("parent_id = :parentId", Map.of("parentId", parentId))
              .orderBy("name")
              .fetch(workspaceId, page);
      }
  }
  ```

  Có sẵn `insert`, `findById`, `getById`, `findByIdIncludingDeleted`, `update`, `softDelete`,
  `restore`, `deletePermanently`. Các cột chuẩn (`createdAt`, `createdBy`, `updatedAt`,
  `updatedBy`, `version`, `deletedAt`) được tự điền nếu record có. `update` ghi đè cả bản ghi
  và từ chối (409) nếu `version` đã khác. Truy vấn riêng viết tay bằng `jdbc()`, `columns()`,
  `rowMapper()`. Mọi hàm public của repository nhận `workspaceId` hoặc `WorkspaceContext` làm
  tham số đầu tiên. Repository không tự mở transaction: service đặt `@Transactional`.
- **Lỗi:** ném `NotFoundException.of("Place", id)`, `ConflictException`, `BadRequestException`;
  mọi lỗi được trả về ở dạng Problem Details (`application/problem+json`). Lỗi `@Valid` trả 400
  kèm danh sách từng trường; trùng khoá duy nhất trả 409; vi phạm ràng buộc khác của database
  (chuỗi dài quá cột, khoá ngoại sai) trả 400.
- **Thời gian:** mọi cột `DATETIME(3)` lưu giờ UTC. Kết nối database được đặt sẵn ở UTC, nên SQL
  viết tay truyền thẳng `Instant` hay dùng `NOW()` đều đúng, bất kể múi giờ của máy chạy.
- **Danh sách dài:** controller nhận một tham số `CursorRequest` (tham số URL `cursor` và
  `limit`, mặc định 20, tối đa 100), repository dựng truy vấn bằng `keyset()`, trả về
  `CursorPage`. Cột sắp xếp là hằng số trong code, `NOT NULL`, nên có index phục vụ.
- **Lịch sử version:** service đặt `platform.history.table` (ví dụ `reg_entity_version`) thì có
  bean `EntityHistory`. Service của nghiệp vụ gọi `record(...)` ngay sau lần sửa, trong cùng
  transaction; `changed_fields` được tự tính. Sau mỗi lần ghi có sự kiện `EntityChangedEvent`.
- **Mapper:** `@Mapper(config = PlatformMapperConfig.class)`. Build báo lỗi nếu một trường của
  kiểu đích không được gán.
- **Cache:** dùng `@Cacheable`; khoá cache luôn có `workspace_id`.
- **Luật kiến trúc:** `ArchitectureTests` của mỗi service chạy `PlatformArchitectureRules`
  trong `./mvnw test`: không JPA, không dùng lớp của service khác, chỉ repository chạy SQL,
  controller không gọi thẳng repository, hàm public của repository nhận `workspaceId` trước.
- **Test repository:** kế thừa `AbstractRepositoryIT` (test-jar của `platform-common`): chạy
  trên database thật, `workspace_id` âm (`WS_A`, `WS_B`), mọi dữ liệu được rollback.
- **Thay đổi cấu trúc database:** thêm file `V<số>__<mô_tả>.sql` vào `db/migration` của service
  sở hữu bảng. Không sửa file đã chạy.
- **Chạy nhiều instance của một service:** đặt `platform.id.node-id` (0 đến 1023) khác nhau cho
  từng instance, nếu không chúng có thể sinh trùng `id`.
