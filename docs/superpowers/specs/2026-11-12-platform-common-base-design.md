# Thiết kế phần nền dùng chung (`platform-common`)

Ngày: 2026-11-12 · Trạng thái: **chờ duyệt**

Ký hiệu: **(bạn)** là điều bạn đã nói; **(tôi chọn)** là quyết định của tôi, bạn có thể bác.

## 1. Phần này là gì

`platform-common` hiện có bộ sinh `id`, xử lý lỗi, `CursorPage` và cache. Giữa request HTTP và câu SQL chưa có gì. Đợt này bổ sung phần còn thiếu, viết ở dạng tổng quát, để một service bất kỳ (ba service hiện có và các service thêm sau này) chỉ cần thêm dependency rồi khai báo phần riêng của nó.

Đợt này **chỉ làm `platform-common`**. **(bạn)** Nghiệp vụ mẫu trong `registry-service` là đợt kế tiếp.

## 2. Mục tiêu, phần không làm, điều phải tránh

**Mục tiêu**

- Viết "động": cái gì lặp lại giữa các bảng, các service thì viết một lần ở đây. **(bạn)**
- Thêm một bảng mới ở service nào cũng chỉ cần: khai báo một record, kế thừa một repository, rồi viết tay các truy vấn riêng. **(bạn đã xác nhận hình dung này)**
- Chỗ nào có thư viện hợp lý thì dùng thư viện thay vì tự viết. **(bạn)**
- Tổ chức code theo design pattern. **(bạn)**
- Mọi phần mặc định đều thay được: service khai báo bean của riêng nó thì bản mặc định tự lùi. **(tôi chọn)**

**Không làm**

- Controller và service tổng quát. Mỗi nghiệp vụ vẫn viết controller, service của nó. **(bạn đã xác nhận)**
- Một API chung chạy theo cấu hình kiểu `/entities/{loại}`. **(bạn đã xác nhận)**
- Dựng SQL động từ bộ lọc do client gửi lên.
- Đăng nhập, phân quyền, Redis, Elasticsearch, gateway: vẫn để sau như đã thống nhất.

**Điều phải tránh**

- Một truy vấn chạy mà không kèm `workspace_id`.
- Phần nền che mất câu SQL: người viết nghiệp vụ không biết lệnh nào đang chạy.
- Phần nền cứng tới mức gặp ca đặc biệt thì phải lách. Ở mọi repository luôn viết tay được bằng `JdbcClient`.
- Dùng pattern cho có. Pattern chỉ xuất hiện ở chỗ nó bỏ được code lặp hoặc đón một thay đổi đã biết trước.

## 3. Ràng buộc

- Spring Boot 4.1.2, Java 21, Jackson 3, MySQL 8.0.36. Máy dev không có Docker.
- Truy vấn bằng `JdbcClient`, không JPA. **(đã thống nhất từ trước)**
- `platform-common` không được biết tên service nào, bảng nào.
- Giữ quy ước code đang có: chú thích tiếng Việt, tên và thông báo lỗi tiếng Anh, kiểu trình bày như các lớp hiện tại.
- Viết theo TDD như các phần trước.

## 4. Thư viện thêm vào

| Thư viện | Phiên bản | Đặt ở đâu | Dùng để |
|---|---|---|---|
| springdoc-openapi (`springdoc-openapi-starter-webmvc-ui`) | 3.2.0 | `platform-common` | Swagger UI và file OpenAPI cho mọi service |
| MapStruct (`mapstruct`, `mapstruct-processor`) | 1.6.3 | `platform-common` và pom gốc | Chuyển giữa request, record, response |
| `lombok-mapstruct-binding` | 0.2.0 | pom gốc | Để Lombok và MapStruct chạy chung |
| ArchUnit (`archunit`) | 1.5.1 | test của từng service | Test kiểm tra quy ước |

- springdoc 3.2.0 được dựng trên đúng Spring Boot 4.1.2 (đã kiểm tra trên Maven Central ngày 2026-11-12).
- Dùng `archunit` lõi và gọi từ test JUnit thường, không dùng `archunit-junit5`, để không phụ thuộc vào việc engine đó chạy được trên JUnit 6. **(tôi chọn)**
- Phiên bản khai báo một lần ở pom gốc.
- Thư viện geohash và AWS SDK S3 thuộc về `registry-service` và `media-service`, thêm khi làm hai service đó.

## 5. Các thành phần

Cấu trúc package sau đợt này:

```
com.home_platform.common
├── autoconfigure   tự bật mọi thứ bên dưới
├── context         ai đang gọi
├── id              (đã có)
├── json            quy ước JSON
├── web             lỗi, phân trang
├── jdbc            repository tổng quát
├── history         ghi và đọc version
├── openapi         cấu hình Swagger
└── mapping         cấu hình chung cho MapStruct
```

Và trong test-jar (`com.home_platform.common.test`): khung test repository, bộ luật ArchUnit.

### 5.1 Ai đang gọi (`context`)

Hai record:

- `WorkspaceContext(long workspaceId, long userId)`: dùng cho API làm việc trong một không gian.
- `UserContext(long userId)`: dùng cho API không gắn với không gian nào (ví dụ "các không gian của tôi" bên identity).

Controller chỉ cần khai báo một tham số kiểu này:

```java
@GetMapping("/{id}")
PlaceResponse get(WorkspaceContext ctx, @PathVariable long id) { ... }
```

- Giá trị lấy qua interface `ContextResolver`. Bản mặc định đọc header `X-Workspace-Id` và `X-User-Id`. Khi có đăng nhập, chỉ cần một bean `ContextResolver` khác; controller không đổi.
- Thiếu header hoặc header không phải số: trả 400 Problem Details, nêu tên header.
- Không dùng `ThreadLocal`. Context đi xuống service và repository như một tham số bình thường, nên quên truyền thì không biên dịch được. **(tôi chọn)**
- Tên: trước đó tôi gọi là `RequestContext`. Tôi đổi vì Spring đã có lớp trùng tên, dễ import nhầm. **(tôi chọn)**

### 5.2 Quy ước JSON (`json`)

- Mọi giá trị kiểu `long` được ghi ra JSON dưới dạng **chuỗi**; kiểu `int` vẫn là số. Chiều nhận vào chấp nhận cả chuỗi lẫn số. **(tôi chọn)**
  - Lý do chọn cách "tất cả `long`" thay vì đánh dấu từng trường: quên đánh dấu một trường `id` thì trình duyệt làm tròn sai `id` mà không báo lỗi gì.
  - Hệ quả: người viết dùng `long` cho `id` và số rất lớn, dùng `int` cho số đếm. Trường như `sizeBytes` sẽ ra dạng chuỗi.
- Thời điểm (`Instant`) ghi dạng ISO-8601 giờ UTC, ví dụ `2026-11-12T06:40:00.123Z`. Ngày (`LocalDate`) ghi `2026-11-12`.
- Các thiết lập JSON khác giữ mặc định của Spring Boot.

### 5.3 Lỗi (`web`)

Đã có: `NotFoundException` (404), `ConflictException` (409), lỗi không lường trước (500). Thêm:

- `BadRequestException` (400) cho dữ liệu vào sai mà code tự phát hiện (cursor hỏng, thiếu header).
- Lỗi kiểm tra dữ liệu (`@Valid` trên body và trên tham số) trả 400 kèm danh sách từng trường:

  ```json
  {
    "status": 400,
    "title": "Bad Request",
    "detail": "Invalid request content.",
    "errors": [ { "field": "name", "message": "must not be blank" } ]
  }
  ```

- Vi phạm khoá duy nhất trong database (`DuplicateKeyException`) trả 409 với thông báo chung, không lộ câu SQL. Nghiệp vụ nào muốn thông báo rõ hơn thì tự bắt và ném `ConflictException`.
- Hàm tạo nhanh cho thông báo quen thuộc, ví dụ `NotFoundException.of("Place", 42)` cho ra "Place 42 not found".

### 5.4 Phân trang (`web`)

- `CursorRequest(String cursor, int limit)`: lấy từ tham số `cursor` và `limit` trên URL. Mặc định 20, tối đa 100, đổi được bằng `platform.paging.default-limit` và `platform.paging.max-limit`. `limit` ngoài khoảng 1 đến tối đa: trả 400.
- `CursorPage<T>(items, nextCursor)` đã có; thêm hàm `map` để đổi record sang response.
- `cursor` là một chuỗi client không cần hiểu: Base64URL của JSON `[giá trị sắp xếp, id]` của phần tử cuối trang. Cursor hỏng: trả 400.

### 5.5 Repository tổng quát (`jdbc`)

Đây là phần chính của đợt này.

**Khai báo một bảng.** Một record Java, mỗi thành phần ứng với một cột, tên đổi từ `camelCase` sang `snake_case`:

```java
@With
public record Place(long workspaceId, long id, Long parentId, long placeTypeId, String path,
        int depth, String code, String name, String description, String status,
        @JsonColumn Map<String, Object> attributes, Long coverFileId,
        Instant createdAt, long createdBy, Instant updatedAt, long updatedBy,
        int version, Instant deletedAt) {
}

@Repository
class PlaceRepository extends WorkspaceRepository<Place> {

    PlaceRepository(RepositorySupport support) {
        super(support, "reg_place", Place.class);
    }

    // Truy vấn riêng của nghiệp vụ, dùng index (workspace_id, parent_id, name)
    CursorPage<Place> listChildren(long workspaceId, long parentId, CursorRequest page) {
        return keyset().where("parent_id = :parentId", Map.of("parentId", parentId))
            .orderBy("name")
            .fetch(workspaceId, page);
    }
}
```

`RepositorySupport` là một bean gom sẵn `JdbcClient`, `IdGenerator`, `Clock` và bộ đọc ghi JSON dùng cho việc lưu, để hàm khởi tạo của repository chỉ nhận một thứ.

**JSON khi lưu khác JSON khi trả ra API.** Cột `@JsonColumn` (và `snapshot` ở mục 5.6) được lưu với số vẫn là số, không áp dụng quy tắc "`long` thành chuỗi" của mục 5.2. Nếu áp dụng, một giá trị số lớn trong `attributes` sẽ bị lưu thành chuỗi và đọc lên thành kiểu khác. Quy tắc của mục 5.2 chỉ áp dụng ở lớp ngoài cùng, khi response được ghi ra cho client. Vì vậy API không trả thẳng JSON đã lưu ở dạng cây (`JsonNode`); nghiệp vụ đổi nó về kiểu Java rồi mới trả.

**Transaction.** Repository không tự mở transaction. Service của nghiệp vụ đặt `@Transactional` quanh các thao tác phải đi cùng nhau.

**Điều kiện.** Record phải có `workspaceId` và `id`; bảng có khoá chính `(workspace_id, id)`. Chín trong mười bốn bảng hiện tại thuộc dạng này.

**Kiểu dữ liệu hỗ trợ.** `long`, `int`, `boolean` (và dạng cho phép null của chúng), `String`, `BigDecimal`, `Instant` (cột `DATETIME(3)` giờ UTC), `LocalDate`, enum (lưu theo tên), và thành phần đánh dấu `@JsonColumn` (cột `JSON`, kiểu Java bất kỳ mà Jackson đọc ghi được). Gặp kiểu khác: báo lỗi ngay khi khởi động, nêu tên record và thành phần. Việc đọc ghi `Instant` không được phụ thuộc múi giờ của máy chạy.

**Cột chuẩn.** Repository tự lo các cột sau nếu record có thành phần tương ứng; record không có thì bỏ qua phần đó. Cột có tên khác (ví dụ `removed_at`, `added_by`) được coi là cột nghiệp vụ.

| Thành phần | Khi thêm | Khi sửa | Khi xoá mềm, khôi phục |
|---|---|---|---|
| `workspaceId` | lấy từ context | là khoá, không đổi | là khoá |
| `id` | nếu bằng 0 thì sinh mới, khác 0 thì giữ | là khoá | là khoá |
| `createdAt`, `createdBy` | thời điểm hiện tại, người gọi | không đổi | không đổi |
| `updatedAt`, `updatedBy` | thời điểm hiện tại, người gọi | cập nhật | cập nhật |
| `version` | 1 | phải khớp, rồi tăng 1 | phải khớp, rồi tăng 1 |
| `deletedAt` | `null` | không đổi | thời điểm hiện tại, hoặc `null` khi khôi phục |

`id` được giữ khi khác 0 vì có nghiệp vụ cần biết `id` trước khi ghi (cột `path` của địa điểm chứa chính `id` của nó). Thời điểm hiện tại lấy từ bean `Clock`, cắt tới mili giây.

**Thao tác có sẵn.**

| Thao tác | Hành vi |
|---|---|
| `insert(ctx, record)` | Ghi một dòng, trả về record đã điền đủ cột chuẩn |
| `findById(workspaceId, id)` | Trả về nếu có và chưa bị xoá mềm |
| `getById(workspaceId, id)` | Như trên, không có thì ném `NotFoundException` |
| `findByIdIncludingDeleted(workspaceId, id)` | Thấy cả dòng đã xoá mềm (dùng cho khôi phục) |
| `update(ctx, record)` | Ghi đè toàn bộ cột nghiệp vụ. Record mang `version` mà người gọi đã đọc. Trả về record sau khi ghi |
| `softDelete(ctx, id, expectedVersion)` | Đánh dấu `deleted_at`. Chỉ dùng được với bảng có cột này. Trả về record sau khi ghi |
| `restore(ctx, id, expectedVersion)` | Bỏ đánh dấu `deleted_at`. Trả về record sau khi ghi |
| `deletePermanently(workspaceId, id)` | Xoá thật. Dành cho bảng không có `deleted_at` |

- Sửa, xoá mềm, khôi phục mà không trúng dòng nào: nếu dòng không tồn tại thì ném `NotFoundException`; nếu tồn tại nhưng `version` đã khác thì ném `ConflictException`. Bảng không có cột `version` thì không kiểm tra.
- Dòng đã xoá mềm được coi là không tồn tại đối với `update` và `softDelete`. `restore` trên một dòng chưa bị xoá ném `ConflictException`.
- `softDelete` và `restore` trả về record vì service cần số `version` mới để ghi lịch sử.
- `update` là ghi đè cả bản ghi, không phải sửa từng trường. Nghiệp vụ muốn sửa một phần thì đọc lên, tạo bản sao đã đổi (ví dụ bằng `@With` của Lombok), rồi gọi `update`.

**Phân trang.** Repository con dựng danh sách bằng `keyset()` như ví dụ trên:

- Luôn tự thêm điều kiện `workspace_id`, và `deleted_at IS NULL` nếu bảng có cột đó.
- `where(...)` thêm điều kiện riêng. `orderBy(cột)` hoặc `orderByDesc(cột)` chọn cột sắp xếp; `id` luôn được thêm làm cột phụ để thứ tự ổn định. Không gọi `orderBy` thì sắp theo `id`.
- Cột sắp xếp là hằng số trong code, phải là cột `NOT NULL` và nên có index phục vụ. Không bao giờ nhận tên cột từ client.
- Trả về `CursorPage<T>`.

**Viết tay.** Repository con dùng được `JdbcClient`, bộ ánh xạ dòng sang record và danh sách cột của bảng để viết bất kỳ truy vấn nào. Ba thứ này cũng dùng được ngoài `WorkspaceRepository`, cho các bảng có khoá khác (ví dụ `reg_place_address`, `idn_workspace_member`).

**Minh bạch.** Câu SQL của các thao tác có sẵn được dựng một lần lúc khởi động và ghi ra log ở mức DEBUG. Record khai báo sai (thiếu `workspaceId`, kiểu không hỗ trợ) làm ứng dụng dừng ngay khi khởi động với thông báo rõ ràng. Record lệch với bảng thật (sai tên cột) sẽ lộ ra ở test repository của service đó.

**Vì sao tự viết mà không dùng Spring Data JDBC.** **(tôi chọn)** Spring Data JDBC cho sẵn CRUD và không phải JPA. Nhưng ba thứ dự án này cần nhất lại là chỗ nó không có sẵn hoặc phải tuỳ biến nhiều: bắt buộc `workspace_id` ở mọi truy vấn, xoá mềm, và phân trang theo vị trí. Nó cũng đi ngược quyết định "SQL viết tay" trước đó. Phần tự viết ước chừng vài trăm dòng và mọi câu SQL đều nhìn thấy được.

### 5.6 Lịch sử version (`history`)

- Bật khi service đặt `platform.history.table` (ví dụ `reg_entity_version`). Bảng phải có đúng các cột như `reg_entity_version` trong thiết kế database. Service không đặt thì phần này không tồn tại.
- Bean `EntityHistory` có ba việc:

| Thao tác | Hành vi |
|---|---|
| `record(ctx, entityType, entityId, versionNo, action, snapshot, note)` | Ghi một dòng version |
| `list(workspaceId, entityType, entityId, page)` | Các version của một bản ghi, mới nhất trước, không kèm `snapshot` |
| `get(workspaceId, entityType, entityId, versionNo)` | Một version kèm `snapshot`; không có thì ném `NotFoundException` |

- `snapshot` là một đối tượng do nghiệp vụ quyết định (version của địa điểm gồm cả địa chỉ), được ghi thành JSON theo quy tắc lưu ở mục 5.5: số vẫn là số. Vì nội dung do nghiệp vụ quyết định, việc ghi version do service của nghiệp vụ gọi, repository không tự làm.
- Bảng version không có khoá `(workspace_id, id)` nên `EntityHistory` tự viết SQL của nó, không kế thừa `WorkspaceRepository`.
- `changed_fields` do `EntityHistory` tự tính: tên các trường cấp một có giá trị khác với version liền trước. Với `CREATE`, hoặc khi không tìm thấy version liền trước, cột này là `NULL`.
- `record` chỉ chạy được bên trong một transaction đang mở; gọi ngoài transaction thì ném lỗi. Nhờ vậy không thể vô tình ghi version tách rời khỏi lần sửa. **(tôi chọn)**
- `action` là enum `ChangeAction`: `CREATE`, `UPDATE`, `DELETE`, `RESTORE`.
- Sau khi ghi, `EntityHistory` phát một sự kiện `EntityChangedEvent` (không gian, loại, `id`, số version, hành động, người sửa, thời điểm). Đợt này chưa có ai nghe. Nó là chỗ để sau này cắm việc đưa dữ liệu sang công cụ tìm kiếm mà không phải sửa code nghiệp vụ. **(tôi chọn)**
- Khôi phục về một version cũ là việc của nghiệp vụ: đọc `snapshot` bằng `get`, rồi gọi `update`. Phần nền chỉ cho hàm đổi `snapshot` về kiểu Java mong muốn.

### 5.7 Tài liệu API (`openapi`)

Mỗi service tự có `/swagger-ui.html` và `/v3/api-docs`. Phần nền cấu hình sẵn:

- Tên tài liệu lấy từ `spring.application.name`.
- Tham số kiểu `WorkspaceContext`, `UserContext` không hiện ra như tham số thường; thay vào đó các API dùng chúng được ghi rõ là cần header `X-Workspace-Id`, `X-User-Id`.
- Kiểu `long` hiện là `string`, khớp với mục 5.2.
- Mọi API có sẵn mô tả lỗi dạng Problem Details.

### 5.8 Cấu hình chung cho MapStruct (`mapping`)

- `PlatformMapperConfig`: mapper là bean của Spring, và **build báo lỗi nếu một trường của kiểu đích không được gán**. Nhờ vậy thêm một cột mà quên đưa vào response thì biết ngay lúc build.
- Mỗi mapper của nghiệp vụ khai báo `@Mapper(config = PlatformMapperConfig.class)`.
- Pom gốc cấu hình bộ xử lý chú thích cho Lombok và MapStruct.

### 5.9 Kiểm tra quy ước bằng test

Bộ luật `PlatformArchitectureRules` nằm trong test-jar. Mỗi service có một lớp test ngắn chạy bộ luật trên package của nó. Đợt này gắn luôn vào cả ba service (thêm dependency test và một lớp test cho mỗi service), để luật có hiệu lực từ nghiệp vụ đầu tiên.

1. Không lớp nào dùng JPA hay Hibernate.
2. Service này không dùng lớp của service khác; chỉ được dùng `com.home_platform.common`.
3. Mọi hàm public của lớp kế thừa `WorkspaceRepository` có tham số đầu tiên kiểu `long` (là `workspaceId`) hoặc `WorkspaceContext`.
4. Chỉ repository được dùng `JdbcClient` và `JdbcTemplate`. Controller không gọi thẳng repository.

Trong bộ luật, repository là lớp có `@Repository`, controller là lớp có `@RestController`. Luật không được báo lỗi khi service chưa có lớp nào thuộc loại đó.

### 5.10 Tự bật (`autoconfigure`)

- Mọi thứ ở trên bật sẵn khi service phụ thuộc vào `platform-common`; service không khai báo gì thêm.
- Mọi bean mặc định đều nhường chỗ khi service khai báo bean cùng kiểu.
- Thêm bean `Clock` (giờ UTC) để code và test dùng chung một nguồn thời gian.
- Phần `jdbc` chỉ bật khi service có `JdbcClient`; phần `history` chỉ bật khi có `platform.history.table`.

## 6. Design pattern

Mỗi pattern gắn với một vấn đề có thật trong phần nền.

| Vấn đề | Pattern | Ở đâu |
|---|---|---|
| Thêm, sửa, xoá giống nhau ở mọi bảng, chỉ khác tên bảng và cột | Template Method | `WorkspaceRepository<T>`: khung cố định, lớp con cấp bảng, record và truy vấn riêng |
| Tách truy cập dữ liệu khỏi nghiệp vụ | Repository | Mọi lớp `...Repository` |
| Một việc có nhiều cách làm và cách làm sẽ đổi (lấy người gọi từ header, sau này từ token; cách sinh `id`; cách đọc ghi từng kiểu cột) | Strategy | `ContextResolver`, `IdGenerator`, bộ chuyển kiểu cột trong `jdbc` |
| Câu truy vấn phân trang có nhiều phần tuỳ chọn | Builder | `keyset()` |
| Tạo đối tượng cần một bước chuẩn bị (đọc cấu trúc record một lần rồi dùng lại; thông báo lỗi theo mẫu) | Factory Method | Lớp mô tả record trong `jdbc`, `NotFoundException.of(...)` |
| Che chi tiết bảng version sau một lối vào đơn giản | Facade | `EntityHistory` |
| Sau một thay đổi, nơi khác cần biết mà nghiệp vụ không cần biết họ là ai | Observer | `EntityChangedEvent` |
| Dữ liệu đi qua các tầng không bị sửa ngầm | Value Object | Các record: context, `CursorRequest`, `CursorPage` |
| Bản mặc định dùng ngay được, từng phần thay được | Plug-in qua auto-configuration | `autoconfigure` |

**Cố ý không dùng**

- Lớp cha tổng quát cho controller và service: đã thống nhất ở mục 2.
- Specification hay bộ dựng truy vấn động: mỗi truy vấn thường gặp có đúng một index phục vụ, nên mỗi truy vấn là một câu SQL cố định.
- Singleton tự viết: Spring đã quản lý vòng đời bean.

**Pattern cho code nghiệp vụ, áp dụng khi viết từng service**

- Ba tầng: controller (HTTP) gọi service (luật nghiệp vụ, transaction) gọi repository (SQL).
- DTO và Mapper: request và response là kiểu riêng, không trả thẳng record của bảng; MapStruct lo việc chuyển.
- Adapter: kho file (S3) và công cụ tìm kiếm nằm sau interface của dự án.
- Strategy: kiểm tra giá trị mục tuỳ biến theo `data_type` của `reg_field_def`.

## 7. Kiểm thử

- **Test thường (`./mvnw test`, không cần database):** đọc cấu trúc record, nội dung câu SQL được dựng, mã hoá cursor, lấy context từ header, quy ước JSON, các loại lỗi, auto-configuration, nội dung file OpenAPI.
- **Test trên MySQL thật (`*IT`, `./mvnw verify`):** repository tổng quát, phân trang và `EntityHistory`.
  - `platform-common` không có bảng riêng, nên các test này tự tạo bảng bằng `CREATE TEMPORARY TABLE`. Bảng tạm chỉ tồn tại trong kết nối của test và tự mất khi kết nối đóng; database UAT không bị thêm gì. **(tôi chọn)**
  - Cần file `platform-common/src/test/resources/application-uat.yaml` chứa thông tin kết nối, chép từ file cùng tên của một service trên máy. File này chỉ nằm trên máy; `.gitignore` được bổ sung để không đưa nó lên git. Người khác clone về phải tự tạo file này thì `./mvnw verify` của `platform-common` mới chạy được.
  - Nếu tài khoản database không có quyền tạo bảng tạm: chuyển các test này sang `registry-service`, chạy trên bảng `reg_` thật với `workspace_id` âm và rollback, như các test cấu trúc hiện có.
- Test-jar thêm một khung test repository cho các service dùng lại (database thật, `workspace_id` âm, rollback).

## 8. Để lại cho người xây

- Tên và chữ ký chính xác của lớp, hàm ở những chỗ tài liệu chỉ mô tả hành vi.
- Cách dựng SQL và ánh xạ dòng bên trong `jdbc` (dùng bộ ánh xạ có sẵn của Spring hay tự viết).
- Cách cấu hình springdoc để đạt bốn kết quả ở mục 5.7.
- Chia auto-configuration thành mấy lớp.

## 9. Không nằm trong đợt này

- **Tiện ích gọi service khác.** Trước đó tôi có kể nó trong danh sách. Tôi đề nghị để lại: chưa có lời gọi nào giữa các service, địa chỉ service đã cấu hình được bằng tính năng có sẵn của Spring Boot, và phần duy nhất phải tự viết (chuyển tiếp header người gọi) sẽ đổi hẳn khi có đăng nhập. **(tôi chọn, khác với điều tôi đã nói; bạn có thể yêu cầu làm luôn)**
- Lớp cha cho bảng không có `workspace_id` (`idn_user`, `idn_workspace`): thêm khi làm identity. Phần dựng SQL bên trong đã nhận danh sách cột khoá nên việc thêm là nhỏ.
- Ghi nhiều dòng một lần, bộ lọc động, join.
- Geohash, lưu file, mã theo dõi request trong log.
