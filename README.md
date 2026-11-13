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

## Nghiệp vụ đã có

Logic nghiệp vụ đầy đủ: `docs/superpowers/specs/2026-11-13-business-rules-design.md`.

**Identity** (cổng 8081; người gọi lấy từ header `X-User-Id`):

| Đường dẫn | Việc |
|---|---|
| `POST /users` | Tạo người dùng (email lưu chữ thường, không trùng); chưa cần đăng nhập |
| `GET /users/me`, `PUT /users/me` | Xem và đổi tên hiển thị của người gọi |
| `/workspaces` | `POST` (người gọi thành chủ, vai trò OWNER), `GET` (các không gian của tôi, theo tên), `GET /{id}`, `PUT /{id}` (đổi tên). Không gian mà người gọi không phải thành viên trả 404 |

Luồng tạo không gian: `POST /workspaces` ở identity, rồi `POST /setup` ở registry với `X-Workspace-Id`
là `id` vừa nhận.

**Registry** (cổng 8082; mọi API cần header `X-Workspace-Id`, `X-User-Id`):

| Đường dẫn | Việc |
|---|---|
| `POST /setup` | Tạo loại địa điểm, nhóm đồ và mục tuỳ biến mặc định cho không gian mới. Client gọi ngay sau khi tạo không gian; gọi lại không làm gì |
| `/place-types` | Loại địa điểm: `GET`, `GET /{id}`, `POST`, `PUT /{id}`, `POST /{id}/archive`, `POST /{id}/unarchive`, `DELETE /{id}` (chỉ khi chưa địa điểm nào dùng) |
| `/item-categories` | Nhóm đồ, cùng bộ thao tác như loại địa điểm |
| `/field-defs` | Mục thông tin tuỳ biến: `GET ?target=PLACE&typeId=`, `GET /{id}`, `POST`, `PUT /{id}`, `POST /{id}/archive`, `POST /{id}/unarchive`. Không xoá |
| `/places` | Địa điểm lồng tới 20 cấp: `POST` (kèm địa chỉ tuỳ chọn), `GET /{id}` (kèm tổ tiên, địa chỉ, địa chỉ hiệu lực), `PUT /{id}`, `POST /{id}/move`, `PUT /{id}/address`, `DELETE /{id}/address`, `DELETE /{id}`, `POST /{id}/restore` |
| `/places` (đọc) | `GET` (cấp cao nhất), `GET /{id}/children`, `GET /{id}/subtree`, `GET /by-type/{typeId}`, `GET /by-area?province=&ward=`, `GET /search?name=` hoặc `?code=`; phân trang bằng `cursor`, `limit` |
| `/places/{id}/versions` | Lịch sử version; `GET /{no}` xem nội dung; `POST /{no}/restore` khôi phục nội dung (không đổi chỗ đứng) |
| `GET /activity` | Hoạt động gần đây của không gian, mới nhất trước; lọc `entityType` |
| `/people` | Hồ sơ người: `POST`, `GET /{id}` (kèm nơi đang ở), `PUT /{id}`, `DELETE /{id}` (khi không còn cư trú hiện tại hay sắp tới), `POST /{id}/restore`, `GET` (theo tên), `GET /search?name=` \| `?phone=` \| `?idNumber=`, `/{id}/versions` như địa điểm. Hai người chưa xoá không trùng cặp loại và số giấy tờ |
| `/occupancies` | Cư trú: `POST`, `GET /{id}`, `PUT /{id}` (sửa nhập nhầm), `POST /{id}/end` (chuyển đi), `DELETE /{id}`; `GET ?placeId=&status=CURRENT\|UPCOMING\|PAST`, `?subtreeOf=` (đang ở trong cả toà nhà), `?personId=`. Một người không có hai dòng giao thời gian ở cùng một chỗ |
| `/items` | Đồ đạc: `POST`, `GET /{id}`, `PUT /{id}`, `POST /{id}/move`, `/remove` (gỡ, vẫn nằm trong danh sách đã gỡ), `/replace` (gỡ món cũ và tạo món mới trong một bước), `/put-back` (đưa lại), `/restore`, `DELETE /{id}`; `GET ?placeId=&removed=`, `?subtreeOf=`, `?categoryId=`; `GET /search?name=` \| `?code=`; `/{id}/versions` như địa điểm. Món đã gỡ chỉ đọc được, trừ đưa lại và xoá |

Mọi lệnh ghi lên địa điểm cần `version` đã đọc (trong body, hoặc tham số URL với `DELETE`); `version` đã
đổi thì nhận 409 và phải đọc lại. Có thể gửi kèm `note` (tối đa 255 ký tự) để ghi vào version.

**Media** (cổng 8083): chưa làm. Đã có sẵn record của hai bảng, luật tải lên và interface kho file;
việc còn lại và các quyết định cần có nằm ở `docs/superpowers/todo/media.md`.

Service của địa điểm, người, đồ đạc kiểm tra cột `attributes` bằng
`AttributeValidation.validate(workspaceId, target, typeId, attributes)` trước khi ghi, rồi lưu
giá trị nó trả về.

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
  giờ UTC. JSON lưu trong database (cột `@JsonColumn`, `snapshot`) giữ số là số; số trong
  `attributes` cũng giữ dạng số khi trả ra API (thành phần đánh dấu `@JsonSerialize(contentUsing = PlainNumbers.class)`).
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
