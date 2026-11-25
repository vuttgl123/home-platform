# home-platform

Kho hồ sơ về các địa điểm, tổ chức theo microservice. Thiết kế database nằm ở
`docs/superpowers/specs/2026-11-11-database-design.md`.

## Các module

| Module | Cổng | Nội dung |
|---|---|---|
| `platform-common` | | Phần nền dùng chung: người gọi, quy ước JSON, lỗi API, phân trang, repository tổng quát, lịch sử version, Swagger, cấu hình MapStruct, sinh `id`, log, cache Caffeine. Test-jar có khung test repository và bộ luật kiến trúc |
| `identity-service` | 8081 | Người dùng, không gian, thành viên (bảng `idn_`) |
| `registry-service` | 8082 | Địa điểm, địa chỉ, người, đồ đạc, version (bảng `reg_`) |
| `media-service` | 8083 | Ảnh: tải lên, ảnh thu nhỏ, gắn vào địa điểm, người, món đồ (bảng `med_`, kho file trên đĩa) |
| `frontend` | 5173 | Giao diện web (React, Vite); cách chạy và cấu trúc ở `frontend/README.md` |

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
| `POST /setup` | Tạo loại địa điểm, nhóm đồ và mục tuỳ biến mặc định cho không gian mới. Client gọi ngay sau khi tạo không gian; gọi lại, hoặc gọi khi không gian đã có bất kỳ danh mục nào, thì không làm gì |
| `/place-types` | Loại địa điểm: `GET`, `GET /{id}`, `POST`, `PUT /{id}`, `POST /{id}/archive`, `POST /{id}/unarchive`, `DELETE /{id}` (chỉ khi chưa địa điểm nào dùng) |
| `/item-categories` | Nhóm đồ, cùng bộ thao tác như loại địa điểm |
| `/field-defs` | Mục thông tin tuỳ biến: `GET ?target=PLACE&typeId=`, `GET /{id}`, `POST`, `PUT /{id}`, `POST /{id}/archive`, `POST /{id}/unarchive`. Không xoá |
| `/code-rules` | Mẫu mã tự cấp: `GET` (luôn có mẫu chung của địa điểm và đồ đạc, `builtIn` khi không gian chưa đặt), `PUT /{PLACE\|ITEM}/{typeId}` (`typeId` 0 là mẫu chung; tiền tố, số chữ số, theo năm), `DELETE /{target}/{typeId}` |
| `/codes` | `GET /status` (số địa điểm, món đồ chưa có mã), `POST /backfill` (cấp mã cho tối đa 100 bản ghi chưa có mã, cũ trước) |
| `/places` | Địa điểm lồng tới 20 cấp, mã do hệ thống cấp khi tạo và không sửa được: `POST` (kèm địa chỉ tuỳ chọn), `POST /{id}/reissue-code` (cấp mã mới), `GET /{id}` (kèm tổ tiên, địa chỉ, địa chỉ hiệu lực, `occupantCount` là số người đang ở hôm nay), `PUT /{id}`, `POST /{id}/move`, `PUT /{id}/address`, `DELETE /{id}/address`, `DELETE /{id}`, `POST /{id}/restore`. `capacity` là sức chứa tuỳ chọn, 0 tới 9999 |
| `/places` (đọc) | `GET` (cấp cao nhất), `GET /{id}/children`, `GET /{id}/subtree`, `GET /by-type/{typeId}`, `GET /by-area?province=&ward=`, `GET /search?name=` hoặc `?code=`; phân trang bằng `cursor`, `limit` |
| `/places/vacancies` | `GET ?placeTypeId=&rootId=`: địa điểm còn chỗ hôm nay, theo tên. Còn chỗ là có sức chứa và số người đang ở nhỏ hơn sức chứa, hoặc chưa đặt sức chứa, không có con và không ai ở. Mỗi dòng có `capacity`, `occupantCount`, `free`, tổ tiên; phân trang bằng `cursor`, `limit` |
| `/places/{id}/capacity-check` | `GET ?startDate=&endDate=&excludeOccupancyId=`: sức chứa và đỉnh (số người nhiều nhất có mặt cùng một ngày trong khoảng, không tính dòng đang sửa), để form cư trú cảnh báo khi vượt; không chặn ghi |
| `/places/{id}/versions` | Lịch sử version; `GET /{no}` xem nội dung; `POST /{no}/restore` khôi phục nội dung (không đổi chỗ đứng) |
| `GET /trash?entityType=PLACE\|PERSON\|ITEM` | Thùng rác: bản ghi đang bị xoá của một loại, mới xoá trước, kèm `version` để khôi phục; không có xoá hẳn |
| `GET /activity` | Hoạt động gần đây của không gian, mới nhất trước; lọc `entityType`, `action` (`CREATE`, `UPDATE`, `DELETE`, `RESTORE`), `changedBy` (id người dùng), `from`, `to` (mốc ISO-8601; `from` tính cả mốc, `to` không tính; `from` phải trước `to`, không thì 400); các lọc dùng cùng lúc được |
| `GET /overview` | Số liệu của trang tổng quan, một request: số địa điểm và số theo loại (kèm số chỗ có người ở hôm nay), số hồ sơ người và số người đang ở, số đồ theo từng trạng thái, số địa điểm và đồ chưa có mã, số thay đổi theo ngày và hành động của 14 ngày gần nhất (`activityByDay`, ngày theo giờ nghiệp vụ), người sắp chuyển đi (`endingSoon`: đang ở, kết thúc từ hôm nay tới hôm nay + 30; `total` và 10 dòng gần nhất). Chỉ tính bản ghi chưa xoá |
| `/exports` | File Excel (`.xlsx`): `GET /items.xlsx?status=&categoryId=` (sheet Đồ đạc, lọc như `GET /items` không phạm vi), `GET /people.xlsx` (sheet Người), `GET /workspace.xlsx` (năm sheet: Địa điểm, Người, Cư trú, Sở hữu, Đồ đạc). Nhãn tiếng Việt như giao diện, ô ngày và ô số đúng kiểu, đường dẫn tên ngăn bằng ` / `; không có bản ghi đã xoá. Đọc và ghi theo lô 500 dòng trong một transaction chỉ đọc, ghi thẳng ra response |
| `/people` | Hồ sơ người: `POST`, `GET /{id}` (kèm nơi đang ở), `PUT /{id}`, `DELETE /{id}` (khi không còn cư trú, sở hữu hiện tại hay sắp tới, và không còn là chủ món đồ chưa gỡ), `POST /{id}/restore`, `GET` (theo tên), `GET /search?name=` \| `?phone=` \| `?idNumber=`, `/{id}/versions` như địa điểm. Hai người chưa xoá không trùng cặp loại và số giấy tờ (database giữ luật này bằng khoá duy nhất `uk_reg_person_live_document`) |
| `/occupancies` | Cư trú: `POST`, `GET /{id}`, `PUT /{id}` (sửa nhập nhầm; địa điểm và người của dòng phải chưa xoá), `POST /{id}/end` (chuyển đi), `DELETE /{id}`; `GET ?placeId=&status=CURRENT\|UPCOMING\|PAST`, `?subtreeOf=` (đang ở trong cả toà nhà), `?personId=`; `GET /timeline?placeId=` hoặc `?personId=` trả mọi lần ở (mọi trạng thái, tối đa 200, mới nhất trước, `truncated` khi còn lần cũ hơn) cho dòng thời gian. Một người không có hai dòng giao thời gian ở cùng một chỗ. Quan hệ `OWNER` không còn nhận cho dòng mới (chủ ghi ở `/ownerships`) |
| `/ownerships` | Sở hữu địa điểm: `POST`, `GET /{id}`, `PUT /{id}` (sửa nhập nhầm), `POST /{id}/end` (thôi sở hữu), `POST /{id}/transfer` (chuyển nhượng cả dòng cho người khác), `DELETE /{id}`; `GET ?placeId=&status=CURRENT\|UPCOMING\|PAST`, `?subtreeOf=`, `?personId=`. Một người không có hai dòng giao thời gian ở cùng chỗ; tổng tỷ lệ của một địa điểm không vượt 100% trong bất kỳ ngày nào |
| `/items` | Đồ đạc, mã do hệ thống cấp khi tạo và không sửa được: `POST`, `GET /{id}`, `PUT /{id}`, `POST /{id}/reissue-code`, `POST /{id}/owner` (đổi chủ; trống là của địa điểm), `POST /{id}/move`, `/remove` (gỡ, vẫn nằm trong danh sách đã gỡ), `/replace` (gỡ món cũ và tạo món mới trong một bước), `/put-back` (đưa lại; món đã bị thay chỉ đưa lại được khi món thay nó đã bị xoá), `/restore`, `DELETE /{id}`; `GET ?placeId=&removed=`, `?subtreeOf=`, `?categoryId=`, `?ownerId=` (đồ của riêng một người), không tham số (cả không gian; lọc `status`, có thể cùng `categoryId`); `GET /search?name=` \| `?code=`; `/{id}/versions` như địa điểm. Món đã gỡ chỉ đọc được, trừ đưa lại và xoá. `warrantyUntil` là ngày cuối còn bảo hành, `serviceEveryMonths` là chu kỳ bảo trì (1 tới 120 tháng), cả hai tuỳ chọn; `GET /{id}` có `nextServiceOn` = lần bảo trì định kỳ gần nhất (không có thì ngày đặt, rồi ngày tạo) cộng chu kỳ |

Mọi lệnh ghi lên địa điểm cần `version` đã đọc (trong body, hoặc tham số URL với `DELETE`); `version` đã
đổi thì nhận 409 và phải đọc lại. Có thể gửi kèm `note` (tối đa 255 ký tự) để ghi vào version.

**Media** (cổng 8083): ảnh của địa điểm, người, món đồ. Thiết kế: `docs/superpowers/specs/2026-11-25-photos-design.md`.

| Đường dẫn | Việc |
|---|---|
| `/attachments` | `POST` multipart (`file`, `ownerType` PLACE\|PERSON\|ITEM, `ownerId`, `caption`): tải lên và gắn, ảnh mới đứng cuối. Chỉ JPEG, PNG, WebP, tối đa 10 MB (quá thì 413), nội dung được giải mã thật (sai thì 400). Cùng nội dung đã có trong không gian thì dùng lại file cũ. Tối đa 100 ảnh mỗi bản ghi (409). `GET ?ownerType=&ownerId=`: ảnh đang hiện theo thứ tự, kèm `file` (`width`, `height` theo chiều hiển thị sau EXIF) |
| `/attachments/{id}` | `PUT` `{caption}` (rỗng là bỏ); `DELETE` gỡ ảnh: ghi `removed_by`, `removed_at`, dòng và file còn |
| `/attachments/order` | `PUT` `{ownerType, ownerId, attachmentIds}`: đủ mọi ảnh đang hiện theo thứ tự mới; danh sách cũ thì 409 |
| `/workspaces/{ws}/files/{id}` | Nội dung ảnh gốc; `/thumbnail` là ảnh thu nhỏ JPEG cạnh dài tối đa 480 px, đã xoay đúng chiều. Không gian nằm trên đường dẫn vì `<img>` không gửi được header; `Cache-Control: private, max-age=31536000, immutable`. File khác không gian hay chưa xong là 404 |

Kho file là thư mục `media.storage.root` (mặc định `data/media`, tính từ thư mục chạy service; git bỏ qua
`data/`): ảnh gốc `workspaces/<ws>/files/<id>.bin`, ảnh thu nhỏ `…/<id>/thumb.bin`. File tải lên dở
(PENDING quá 1 giờ) được dọn mỗi giờ. Chưa kiểm tra quyền: media tin header không gian như registry, tới
khi có đăng nhập (Đợt 10).

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
  `rowMapper()`; câu có `JOIN` lấy cột kèm tiền tố bằng `metadata().selectList("p")`, và danh sách
  phân trang tự viết dựng trang bằng `CursorPage.of(rows, limit, cursorOf)`. Mọi hàm public của repository nhận `workspaceId` hoặc `WorkspaceContext` làm
  tham số đầu tiên. Repository không tự mở transaction: service đặt `@Transactional`.
- **Kiểm tra rồi ghi:** khi một lệnh ghi dựa vào việc một dòng khác còn nguyên (thêm đồ vào một
  địa điểm, đếm đồ rồi mới xoá địa điểm), service đọc dòng đó bằng `findByIdForShare` (lệnh khác
  không sửa, xoá được nó) hoặc `findByIdForUpdate` (lệnh khác cũng không giữ được nó) để không
  lệnh nào chen vào giữa. Hai hàm chỉ gọi được trong transaction, và phải gọi trước câu `SELECT`
  thường đầu tiên mà kết quả dùng để quyết định: `SELECT` thường thấy snapshot chốt từ lần đọc
  thường đầu tiên của transaction, còn câu đọc kèm khoá luôn thấy bản đã commit mới nhất.
- **Lỗi:** ném `NotFoundException.of("Place", id)`, `ConflictException`, `BadRequestException`;
  mọi lỗi được trả về ở dạng Problem Details (`application/problem+json`). Lỗi `@Valid` trả 400
  kèm danh sách từng trường; trùng khoá duy nhất trả 409; vi phạm ràng buộc khác của database
  (chuỗi dài quá cột, khoá ngoại sai) trả 400. Không lấy được khoá dòng (deadlock, hoặc lệnh
  khác giữ dòng quá lâu) trả 409: lệnh chưa ghi gì, client đọc lại rồi thử lại.
- **Thời gian:** mọi cột `DATETIME(3)` lưu giờ UTC. Kết nối database được đặt sẵn ở UTC, nên SQL
  viết tay truyền thẳng `Instant` hay dùng `NOW()` đều đúng, bất kể múi giờ của máy chạy.
- **Giới hạn chờ database:** mở kết nối tối đa 5 giây, đọc kết quả một câu lệnh 60 giây, chờ khoá
  dòng 5 giây (hết hạn thì API trả 409), cả một transaction 30 giây. Đặt trong
  `platform-common-defaults.properties`; service ghi đè trong `application.yaml` của mình. Migration
  chạy lâu hơn 60 giây thì đặt lại `socketTimeout` cho lần chạy đó.
- **Danh sách dài:** controller nhận một tham số `CursorRequest` (tham số URL `cursor` và
  `limit`, mặc định 20, tối đa 100), repository dựng truy vấn bằng `keyset()`, trả về
  `CursorPage`. Cột sắp xếp là hằng số trong code, `NOT NULL`, nên có index phục vụ.
- **Lịch sử version:** service đặt `platform.history.table` (ví dụ `reg_entity_version`) thì có
  bean `EntityHistory`. Service của nghiệp vụ gọi `record(...)` ngay sau lần sửa, trong cùng
  transaction; `changed_fields` được tự tính. Sau mỗi lần ghi có sự kiện `EntityChangedEvent`. `listRecent(workspaceId, filter, page)` liệt kê mọi version của
  không gian, mới nhất trước (màn hình "hoạt động gần đây"); `ChangeFilter(entityType, action, changedBy, from, to)`
  lọc, trường null là không lọc (`ChangeFilter.none()`). Bảng version cần index `(workspace_id, changed_at)` cho việc
  này; `from`, `to` thu hẹp khoảng quét.
- **Log:** mỗi request có một mã: lấy từ header `X-Request-Id` nếu client gửi (chữ, số, `.`, `_`,
  `-`, tối đa 64 ký tự), không thì tự sinh; trả lại trong cùng header. Mã có trong mọi dòng log của
  request (`[mã]` ở bản text; `requestId`, `workspaceId`, `userId` ở bản JSON) và trong thuộc tính
  `requestId` của lỗi 500, nên một lỗi client báo lại lần ngược được về log. Mỗi request để lại một
  dòng tóm tắt `GET /places/1 -> 200 in 12 ms`, trừ `/actuator`.
- **Mapper:** `@Mapper(config = PlatformMapperConfig.class)`. Build báo lỗi nếu một trường của
  kiểu đích không được gán.
- **Cache:** dùng `@Cacheable`; khoá cache luôn có `workspace_id`.
- **Xuất Excel:** registry ghi `.xlsx` bằng `org.dhatim:fastexcel` (ghi luồng, chữ ghi thẳng trong ô thay vì bảng
  chuỗi chung, nên không giữ cả file trong bộ nhớ). Test đọc lại file bằng `fastexcel-reader` (chỉ ở scope `test`).
- **Luật kiến trúc:** `ArchitectureTests` của mỗi service chạy `PlatformArchitectureRules`
  trong `./mvnw test`: không JPA, không dùng lớp của service khác, chỉ repository chạy SQL,
  controller không gọi thẳng repository, hàm public của repository nhận `workspaceId` trước.
- **Test repository:** kế thừa `AbstractRepositoryIT` (test-jar của `platform-common`): chạy
  trên database thật, `workspace_id` âm (`WS_A`, `WS_B`), mọi dữ liệu được rollback.
- **Test hai lệnh ghi cùng lúc:** kế thừa `CommittedDataIT` (registry). Khác các test trên, nó
  **commit dữ liệu thật** vì hai transaction phải thấy nhau: chỉ dùng `workspace_id = -9001` và xoá
  sạch không gian đó trước và sau mỗi test. `holdOpen(...)` giữ một lệnh ở trạng thái "đã ghi, chưa
  commit", `inBackground(...)` chạy lệnh thứ hai, rồi `commit()`; xem `ConcurrentWritesIT`.
- **Thay đổi cấu trúc database:** thêm file `V<số>__<mô_tả>.sql` vào `db/migration` của service
  sở hữu bảng. Không sửa file đã chạy.
- **Chạy nhiều instance của một service:** đặt `platform.id.node-id` (0 đến 1023) khác nhau cho
  từng instance, nếu không chúng có thể sinh trùng `id`.
