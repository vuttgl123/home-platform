# home-platform

Kho hồ sơ về các địa điểm, tổ chức theo microservice. Thiết kế database nằm ở
`docs/superpowers/specs/2026-11-11-database-design.md`.

## Các module

| Module | Cổng | Nội dung |
|---|---|---|
| `platform-common` | | Thư viện dùng chung: cấu hình log, sinh `id`, xử lý lỗi API, kiểu trang kết quả, cache Caffeine |
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

## Chạy và kiểm tra

```bash
./mvnw package -DskipTests                                         # đóng gói mọi module
java -jar registry-service/target/registry-service-0.0.1-SNAPSHOT.jar   # chạy một service

./mvnw test      # test thường, không cần database
./mvnw verify    # thêm test cấu trúc database, cần kết nối được database
```

Khi khởi động, mỗi service tự chạy các file migration của mình (Flyway) rồi mới nhận request.
Kiểm tra nhanh: `curl localhost:8082/actuator/health`.

## Quy ước

- **Package:** gốc của mỗi service là `com.home_platform.<service>`. Code chia theo nghiệp vụ,
  ví dụ `com.home_platform.registry.place` chứa `PlaceController`, `PlaceService`,
  `PlaceRepository`.
- **Truy vấn:** SQL viết tay qua `JdbcClient`. Mọi truy vấn lọc theo `workspace_id` trước.
- **Id:** lấy từ `IdGenerator`. API trả `id` dưới dạng chuỗi.
- **Lỗi:** ném `NotFoundException`, `ConflictException`; mọi lỗi được trả về ở dạng Problem
  Details (`application/problem+json`).
- **Danh sách dài:** phân trang theo vị trí, trả về `CursorPage`.
- **Cache:** dùng `@Cacheable`; khoá cache luôn có `workspace_id`.
- **Thay đổi cấu trúc database:** thêm file `V<số>__<mô_tả>.sql` vào `db/migration` của service
  sở hữu bảng. Không sửa file đã chạy.
- **Chạy nhiều instance của một service:** đặt `platform.id.node-id` (0 đến 1023) khác nhau cho
  từng instance, nếu không chúng có thể sinh trùng `id`.
