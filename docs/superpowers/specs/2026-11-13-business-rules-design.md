# Logic nghiệp vụ của home-platform

Ngày: 2026-11-13 · Trạng thái: **chờ duyệt**

Ký hiệu: **(bạn)** là điều bạn đã quyết (phần lớn từ thiết kế database ngày 2026-11-11); **(tôi chọn)** là quyết định của tôi, bạn có thể bác. Bạn đã giao cho tôi tự viết theo những gì đã chốt, nên mọi chỗ không ghi **(bạn)** đều là đề xuất.

Tài liệu liên quan: thiết kế database `2026-11-11-database-design.md`, phần nền `2026-11-12-platform-common-base-design.md`.

## 1. Hệ thống làm gì

Một kho hồ sơ về các địa điểm, luôn được cập nhật. Mỗi người có một không gian riêng; trong đó họ ghi lại địa điểm (lồng nhiều cấp: toà nhà, tầng, phòng), địa chỉ, ảnh, người sống ở đó và đồ đạc bên trong. Mỗi lần sửa để lại một version xem lại và khôi phục được. **(bạn)**

Ba câu hỏi hệ thống phải trả lời nhanh, vì đó là lý do nó tồn tại:

1. Địa điểm này là gì, nằm trong đâu, chứa những gì?
2. Ai đang ở đây, ai từng ở đây; người này đã ở những đâu?
3. Ở đây có đồ đạc gì, món nào đã gỡ hay bị thay?

## 2. Mục tiêu, phần không làm, điều phải tránh

**Mục tiêu**

- Ghi đúng và đủ ba loại hồ sơ (địa điểm, người, đồ đạc) cho mọi loại địa điểm, không giới hạn ở ví dụ. **(bạn)**
- Không mất quá khứ: chuyển đi, gỡ đồ, sửa, xoá đều để lại dấu vết. **(bạn)**
- Hai người sửa cùng lúc không ghi đè lên nhau mà không biết. **(bạn)**
- Mỗi người dùng tự định nghĩa loại địa điểm và các mục thông tin riêng. **(bạn)**

**Không làm ở đợt này**

- Tiền: hợp đồng, đặt phòng, hoá đơn. **(bạn)**
- Đăng nhập và phân quyền theo vai trò. **(bạn)** Vai trò OWNER, EDITOR, VIEWER đã có trong dữ liệu nhưng chưa được kiểm tra.
- Tìm theo từ khoá bất kỳ trong nội dung. **(bạn)**
- Mời thêm thành viên vào không gian, xoá không gian, khoá không gian. **(tôi chọn)** Bảng đã sẵn, làm khi có đăng nhập.
- Luật "loại nào được chứa loại nào" (ví dụ phòng chỉ nằm trong tầng). Mọi loại chứa được mọi loại. **(tôi chọn)**
- Nhập, xuất dữ liệu hàng loạt; thông báo; lọc theo mục tuỳ biến. **(tôi chọn)**

**Điều phải tránh**

- Thấy hoặc sửa được dữ liệu của không gian khác. **(bạn)**
- Một thay đổi trên địa điểm, người hay đồ đạc mà không có version. **(bạn)**
- Dữ liệu bị đổi ngầm: một thao tác chỉ đổi đúng thứ nó nói sẽ đổi.

## 3. Quy tắc chung cho mọi nghiệp vụ

**Không gian.** Mọi thao tác trong registry và media nhận `WorkspaceContext` (header `X-Workspace-Id`, `X-User-Id`). Mọi đọc, ghi chỉ chạm dữ liệu của không gian đó. Bản ghi của không gian khác được coi là không tồn tại (404), không phải "không có quyền". **(tôi chọn)**

**Chặn sửa đè.** Bản ghi có cột `version` (địa điểm, người, cư trú, đồ đạc) chỉ sửa, xoá, khôi phục được khi client gửi kèm `version` đã đọc. `version` đã đổi thì trả 409 và client phải đọc lại. **(bạn)**

**Sửa là gửi lại cả phần sửa được.** Lệnh sửa nhận toàn bộ các trường người dùng được sửa, không phải từng trường. Service đọc dòng hiện tại, đặt các trường đó lên, rồi ghi. Các trường do hệ thống quản lý (`parent_id`, `path`, `depth`, `place_id` của đồ đạc, `removed_at`, `replaced_by_item_id`) không bao giờ lấy từ lệnh sửa; mỗi trường có thao tác riêng. **(tôi chọn)** Lý do: một client giữ bản cũ không thể vô tình kéo một địa điểm về chỗ cũ.

**Version.** Mỗi lần tạo, sửa, xoá, khôi phục một địa điểm, một người hay một món đồ ghi thêm một version (`EntityHistory.record`) trong cùng transaction. Client có thể gửi kèm ghi chú (`note`, tối đa 255 ký tự). **(bạn)** Cư trú không có version riêng: lịch sử của nó chính là các dòng cư trú với ngày bắt đầu, ngày kết thúc. **(tôi chọn)**

**Xoá là xoá mềm** với địa điểm, người, cư trú, đồ đạc. Bản ghi đã xoá trả 404 ở mọi thao tác thường, chỉ thấy được qua "khôi phục" và lịch sử. **(bạn)**

**Danh mục** (loại địa điểm, mục tuỳ biến, nhóm đồ) không xoá mềm; ngừng dùng bằng trạng thái `ARCHIVED`. Thứ đã lưu trữ vẫn hiển thị trên bản ghi cũ nhưng không chọn được cho bản ghi mới. **(tôi chọn)**

**Chữ.** Mọi chuỗi được cắt khoảng trắng hai đầu. Trường bắt buộc mà rỗng sau khi cắt thì trả 400. Độ dài tối đa đúng như cột trong database. So sánh tên, mã không phân biệt hoa thường và dấu, theo collation của MySQL. **(tôi chọn)**

**Ngày "hôm nay"** dùng để biết ai đang ở, món nào còn: tính theo múi giờ `Asia/Ho_Chi_Minh`, đặt bằng thuộc tính `registry.business-zone`. **(tôi chọn)**

**Lỗi.**

| Mã | Khi nào |
|---|---|
| 400 | Dữ liệu vào sai: thiếu trường, sai định dạng, giá trị mục tuỳ biến sai kiểu, cursor hỏng |
| 404 | Không có, đã xoá mềm, hoặc thuộc không gian khác |
| 409 | `version` đã đổi; vi phạm luật nghiệp vụ (còn địa điểm con, trùng mã, trùng thời gian cư trú...) |

Thông báo lỗi viết tiếng Anh, nói rõ luật nào bị vi phạm, ví dụ `Place 42 still has 3 child places`.

## 4. Identity: người dùng và không gian

Chưa có đăng nhập nên phần này nhỏ: đủ để biết ai sở hữu không gian nào. **(bạn)**

**Người dùng (`idn_user`)**

- Tạo: `email` bắt buộc, đúng dạng email, lưu chữ thường; trùng email trả 409. `display_name` bắt buộc, tối đa 150 ký tự. Trạng thái ban đầu `ACTIVE`.
- Sửa `display_name`. Đổi email chưa làm (gắn với đăng nhập).
- `DISABLED`: không tạo được không gian mới. Đặt trạng thái này chưa có API (làm cùng phần quản trị). **(tôi chọn)**

**Không gian (`idn_workspace`, `idn_workspace_member`)**

- Tạo (nhận `UserContext`): `name` bắt buộc, tối đa 150. Người gọi thành chủ (`owner_user_id`) và có một dòng thành viên vai trò `OWNER`. Cả hai ghi trong một transaction. Trạng thái `ACTIVE`.
- "Các không gian của tôi": các không gian người gọi là thành viên, xếp theo tên, phân trang.
- Đổi tên.
- Sau khi tạo, client gọi thao tác "khởi tạo không gian" của registry (mục 5.1) để có dữ liệu mặc định. **(tôi chọn)** Hai service chưa gọi nhau, nên bước này do client làm; khi có cơ chế gọi giữa service hoặc sự kiện, identity sẽ tự kích hoạt.

## 5. Registry

### 5.1 Khởi tạo không gian

Một thao tác tạo dữ liệu mặc định cho không gian mới. Chỉ chạy khi không gian chưa có loại địa điểm nào; nếu đã có thì không làm gì và trả thành công. Nhờ vậy gọi lại nhiều lần vẫn an toàn, và không tạo lại thứ người dùng đã cố ý xoá. **(tôi chọn)**

- Loại địa điểm: `BUILDING` Toà nhà, `FLOOR` Tầng, `ROOM` Phòng, `APARTMENT` Căn hộ, `HOUSE` Nhà ở, `HOTEL` Khách sạn, `OTHER` Khác.
- Nhóm đồ: Nội thất, Điện tử, Điện lạnh, Thiết bị vệ sinh, Đồ gia dụng, Khác.
- Hai mục tuỳ biến mẫu: `HOTEL` có `star_rating` (Số sao, NUMBER); `APARTMENT` có `area_m2` (Diện tích m², NUMBER).

**(tôi chọn)** cho cả ba danh sách.

### 5.2 Loại địa điểm (`reg_place_type`)

- Tạo: `code` bắt buộc, chỉ gồm `A-Z`, `0-9`, `_`, tối đa 50, tự viết hoa; trùng trong không gian trả 409. `name` bắt buộc, tối đa 100. `sort_order` mặc định 0.
- `code` không đổi được sau khi tạo; `name`, `description`, `sort_order` sửa được. **(tôi chọn)** Mã là khoá ổn định cho việc tích hợp sau này.
- Lưu trữ (`ARCHIVED`) và bỏ lưu trữ. Loại đã lưu trữ không chọn được khi tạo địa điểm hay đổi loại.
- Xoá hẳn chỉ khi chưa có địa điểm nào (kể cả đã xoá mềm) dùng loại này; ngược lại trả 409 và gợi ý lưu trữ. **(tôi chọn)**
- Danh sách theo `sort_order`, rồi `name`; không phân trang (một không gian có vài chục loại).
- Bảng này không có `version` nên hai người sửa cùng lúc thì người sau thắng. **(bạn, do thiết kế bảng)**
- Đọc qua cache Caffeine theo `workspace_id`; mọi lệnh ghi xoá cache của không gian đó. **(bạn: dữ liệu ít đổi vào cache)**

### 5.3 Mục thông tin tuỳ biến (`reg_field_def`)

Một mục áp dụng cho một đích (`target`) và một phạm vi (`type_id`):

| `target` | `type_id` | Áp dụng cho |
|---|---|---|
| PLACE | 0 | Mọi địa điểm |
| PLACE | `id` loại địa điểm | Địa điểm thuộc loại đó |
| PERSON | luôn 0 | Mọi người |
| ITEM | 0 | Mọi đồ đạc |
| ITEM | `id` nhóm đồ | Đồ thuộc nhóm đó |

- Tạo: `field_key` bắt buộc, dạng `[a-z][a-z0-9_]*`, tối đa 50. `label` bắt buộc, tối đa 100. `data_type` là TEXT, NUMBER, BOOLEAN, DATE hoặc SELECT. `type_id` khác 0 phải là loại hoặc nhóm đang `ACTIVE` của không gian.
- Một khoá không được trùng trong các phạm vi chồng nhau: đã có `area_m2` cho mọi địa điểm thì không tạo `area_m2` riêng cho căn hộ, và ngược lại. Vi phạm trả 409. **(tôi chọn)** Nếu không, một địa điểm sẽ có hai định nghĩa cho cùng một khoá.
- SELECT phải có `options`: danh sách 1 đến 100 chuỗi, mỗi chuỗi tối đa 100 ký tự, không trùng. Kiểu khác thì `options` phải trống.
- Sau khi tạo, `field_key`, `target`, `type_id`, `data_type` không đổi được. **(tôi chọn)** Đổi chúng sẽ làm sai các giá trị đã lưu. Muốn đổi thì lưu trữ mục cũ và tạo mục mới.
- Sửa được `label`, `options`, `is_required`, `sort_order`. Bớt một lựa chọn không đổi dữ liệu cũ; bản ghi đang mang lựa chọn đó phải đổi sang lựa chọn khác ở lần sửa tiếp theo.
- Không xoá hẳn, chỉ lưu trữ. **(tôi chọn)** Giá trị của nó vẫn nằm trong `attributes` của các bản ghi cũ.
- Đọc qua cache như loại địa điểm.

**Kiểm tra `attributes` khi tạo hoặc sửa một bản ghi** (địa điểm, người, đồ đạc). Các mục áp dụng là mục cùng `target` có `type_id` bằng 0 hoặc bằng loại (nhóm) hiện tại của bản ghi.

1. Khoá không thuộc mục áp dụng nào (kể cả mục đã lưu trữ) thì trả 400. Đổi loại địa điểm thì client phải bỏ các khoá của loại cũ.
2. Mục đang `ACTIVE` thì kiểm tra giá trị:

   | Kiểu | Giá trị hợp lệ |
   |---|---|
   | TEXT | Chuỗi, tối đa 1000 ký tự |
   | NUMBER | Số JSON (nguyên hoặc thập phân); lưu nguyên dạng số |
   | BOOLEAN | `true` hoặc `false` |
   | DATE | Chuỗi `YYYY-MM-DD` là ngày có thật |
   | SELECT | Một chuỗi thuộc `options` |

3. Mục `is_required` mà thiếu, `null` hoặc chuỗi rỗng thì trả 400.
4. Mục đã lưu trữ: giữ nguyên giá trị, không kiểm tra.
5. Giá trị `null` nghĩa là bỏ trống; khoá đó không được lưu.

Mọi lỗi liệt kê theo từng khoá trong danh sách `errors` (field là `attributes.<khoá>`). **(tôi chọn)** cho cả năm quy tắc.

### 5.4 Địa điểm (`reg_place`)

**Tạo**

- `name` bắt buộc, tối đa 255. `place_type_id` là loại `ACTIVE`. `code` tuỳ chọn, tối đa 50, **không bắt buộc duy nhất** (mỗi toà nhà đều có thể có phòng P101). **(tôi chọn)**
- `status` mặc định `ACTIVE`; `INACTIVE` chỉ là nhãn "tạm ngưng sử dụng", không chặn thao tác nào. **(tôi chọn)**
- Có `parent_id` thì cha phải tồn tại và chưa xoá. `depth` bằng `depth` của cha cộng 1, tối đa 19 (20 cấp). `path` bằng `path` của cha nối thêm `id` của chính nó và dấu `/`. Không có cha thì `depth` là 0, `path` là `/<id>/`. **(bạn: 20 cấp, path)**
- `id` được sinh trước khi ghi để tính `path`. **(bạn)**
- `attributes` kiểm tra theo mục 5.3. Có thể gửi kèm địa chỉ (5.5) trong cùng lệnh tạo.
- `cover_file_id` là `id` bên media; registry không kiểm tra được vì hai service không gọi nhau. **(tôi chọn)**
- Ghi version `CREATE`.

**Sửa** `name`, `code`, `description`, `status`, `place_type_id`, `attributes`, `cover_file_id`. Đổi sang loại khác thì loại mới phải `ACTIVE`; giữ nguyên loại cũ đã lưu trữ thì vẫn được. Ghi version `UPDATE`.

**Di chuyển** sang cha khác hoặc lên cấp cao nhất.

- Cha mới phải tồn tại, chưa xoá, không phải chính nó, không nằm trong cây con của nó (`path` của cha mới bắt đầu bằng `path` của nó thì trả 409).
- Cấp sâu nhất trong cây con sau khi chuyển không vượt quá 19; vượt thì trả 409.
- Trong một transaction: đổi `parent_id`, `path`, `depth` của chính nó; đổi `path`, `depth` của mọi địa điểm trong cây con (kể cả đã xoá mềm, để khôi phục sau này vẫn đúng chỗ) bằng **một** câu `UPDATE` theo tiền tố `path`.
- Địa điểm được chuyển ghi version `UPDATE`. Các địa điểm con được tăng `version` và cập nhật `updated_at`, `updated_by` nhưng **không** ghi version. **(tôi chọn)** Chuyển một toà nhà có hàng nghìn phòng không nên ghi hàng nghìn snapshot. Tăng `version` để client nào đang giữ bản cũ của một phòng sẽ nhận 409 thay vì ghi đè. Hệ quả: lịch sử của phòng có một khoảng nhảy số version; version kế tiếp của phòng sẽ cho thấy `path` đã đổi.

**Xoá** (mềm): chỉ khi không còn địa điểm con chưa xoá, không còn người đang ở hoặc sắp đến ở (cư trú chưa xoá có `end_date` trống hoặc từ hôm nay trở đi), và không còn đồ đạc chưa gỡ, chưa xoá. Vi phạm trả 409 kèm số lượng từng loại. **(bạn: điều kiện xoá)** Ghi version `DELETE`.

**Khôi phục** (bỏ xoá): cha (nếu có) phải chưa bị xoá, nếu không trả 409 "khôi phục cha trước". Ghi version `RESTORE`.

**Đọc**

| Thao tác | Ghi chú |
|---|---|
| Xem một địa điểm | Kèm địa chỉ của nó, địa chỉ hiệu lực (5.5), và chuỗi tổ tiên (tên, `id`) lấy từ `path` để hiện "Toà A › Tầng 3 › P301" |
| Địa điểm cấp cao nhất | Xếp theo tên, phân trang |
| Con trực tiếp | Xếp theo tên, phân trang |
| Cả cây con | Xếp theo `path` (tức theo thứ tự tạo trong từng nhánh), phân trang |
| Theo loại | Xếp theo tên, phân trang |
| Tìm theo tên bắt đầu bằng | Ký tự `%` và `_` trong từ khoá được thoát; tối thiểu 1 ký tự |
| Tìm theo mã | Khớp đúng mã, không phân biệt hoa thường |
| Theo khu vực | Theo `province_code`, tuỳ chọn thêm `ward_code` |

Đọc chỉ thấy địa điểm chưa xoá.

### 5.5 Địa chỉ (`reg_place_address`)

- Mỗi địa điểm có tối đa một địa chỉ. Đặt (tạo hoặc thay) và gỡ là thao tác **trên địa điểm**: cần `version` của địa điểm, tăng `version` của địa điểm, và ghi version `UPDATE` của địa điểm. **(bạn: version của địa điểm gồm cả địa chỉ)**
- `full_address` bắt buộc, tối đa 500. `country_code` hai chữ cái viết hoa, mặc định `VN`. Mã và tên tỉnh, quận, phường là chữ tự do, chưa đối chiếu danh mục hành chính. **(tôi chọn)**
- `latitude`, `longitude` phải có cả hai hoặc không có cả hai; vĩ độ từ -90 đến 90, kinh độ từ -180 đến 180. Có toạ độ thì `geohash` (độ chính xác 9 ký tự, khoảng 5 m) do service tính; không có thì trống. **(tôi chọn: độ chính xác)**
- **Địa chỉ hiệu lực**: địa chỉ của chính nó, nếu không có thì của tổ tiên gần nhất có địa chỉ (tìm theo các `id` trong `path`, một truy vấn). Khi trả về, ghi rõ địa chỉ lấy từ địa điểm nào. **(bạn: dùng địa chỉ của cha gần nhất)**
- Snapshot version của địa điểm có dạng `{ ...các trường của địa điểm, "address": {...} | null }`.

### 5.6 Người (`reg_person`)

- Tạo: `full_name` bắt buộc, tối đa 150. `phone` tối đa 20, bỏ khoảng trắng và dấu chấm. `email` đúng dạng nếu có. `gender` là MALE, FEMALE hoặc OTHER nếu có. `date_of_birth` không ở tương lai. `id_type` (ví dụ CCCD, PASSPORT) và `id_number` đi cùng nhau: có một thì phải có cả hai. **(tôi chọn)**
- Hai người chưa xoá trong cùng không gian không được trùng cặp (`id_type`, `id_number`); trùng thì trả 409 kèm `id` người đã có. **(tôi chọn)** Kiểm tra ở ứng dụng, chưa có khoá duy nhất trong database.
- `attributes` kiểm tra theo mục 5.3 (`target` PERSON).
- Sửa mọi trường trên. Version `CREATE`, `UPDATE`, `DELETE`, `RESTORE` như địa điểm.
- Xoá: chỉ khi người đó không còn cư trú hiện tại hay sắp tới; ngược lại 409. **(tôi chọn)**
- Đọc: xem một người kèm các nơi đang ở; tìm theo tên bắt đầu bằng, theo số điện thoại, theo số giấy tờ; danh sách theo tên.

### 5.7 Cư trú (`reg_occupancy`)

Một dòng nghĩa là: người X có quan hệ R với địa điểm Y từ ngày A tới ngày B.

- Tạo: địa điểm và người phải tồn tại, chưa xoá. `relation` là OWNER, RESIDENT, GUEST, STAFF hoặc OTHER. `start_date` bắt buộc. `end_date` trống (đang ở) hoặc không trước `start_date`.
- Một người không có hai dòng chưa xoá **ở cùng một địa điểm** có khoảng thời gian giao nhau (ngày cuối trống coi như vô hạn); trùng thì trả 409. Ở nhiều nơi cùng lúc thì được. **(tôi chọn)**
- **Chuyển đi**: đặt `end_date`. Đây là cách kết thúc cư trú bình thường; dòng không bị xoá. **(bạn)**
- **Sửa** quan hệ, ngày, ghi chú khi nhập nhầm; vẫn kiểm tra luật giao nhau.
- **Xoá** (mềm) chỉ dành cho dòng nhập nhầm.
- Trạng thái suy ra từ ngày, không lưu: **đang ở** khi `start_date` ≤ hôm nay và (`end_date` trống hoặc ≥ hôm nay); **sắp đến** khi `start_date` > hôm nay; **đã rời** khi `end_date` < hôm nay. **(tôi chọn)** `end_date` là ngày cuối cùng còn ở.
- Đọc: ai đang ở một địa điểm; ai từng ở (đã rời, mới nhất trước); ai đang ở trong cả cây con (ví dụ cả toà nhà, join với `reg_place` theo tiền tố `path`, cùng nhóm bảng nên được join); một người đã và đang ở những đâu.

### 5.8 Nhóm đồ (`reg_item_category`)

- Tạo: `name` bắt buộc, tối đa 100, trùng trong không gian (không phân biệt hoa thường, dấu) trả 409.
- Sửa `name`, `sort_order`. Lưu trữ, bỏ lưu trữ. Xoá hẳn chỉ khi chưa món đồ nào dùng. Cache như loại địa điểm. **(tôi chọn, giống 5.2)**

### 5.9 Đồ đạc (`reg_item`)

**Tạo** trong một địa điểm chưa xoá. `name` bắt buộc, tối đa 255. `category_id` tuỳ chọn, phải là nhóm `ACTIVE`. `quantity` là số nguyên ≥ 1, mặc định 1. `status` là IN_USE (mặc định), BROKEN hoặc STORED. `placed_at` tuỳ chọn. `attributes` kiểm tra theo mục 5.3 (`target` ITEM). Version `CREATE`.

**Sửa** `name`, `code`, `category_id`, `quantity`, `unit`, `status` (IN_USE, BROKEN, STORED), `placed_at`, `description`, `attributes`, `cover_file_id`. Version `UPDATE`.

**Chuyển** sang địa điểm khác chưa xoá. Version `UPDATE`; vị trí cũ xem được trong version trước. **(bạn)**

**Gỡ**: đặt `removed_at` (mặc định hôm nay, không trước `placed_at`) và `status` REMOVED. Món đồ vẫn hiện trong danh sách "đã gỡ" của địa điểm. **(bạn)**

**Thay**: trong một transaction, tạo món mới (ở cùng địa điểm, hoặc địa điểm khác nếu client chỉ định), gỡ món cũ như trên và đặt `replaced_by_item_id` của món cũ là `id` món mới. Cả hai đều có version. **(bạn)**

**Đưa lại** món đã gỡ: bỏ `removed_at`, `status` về IN_USE; không làm được nếu món đó đã bị thay. **(tôi chọn)**

Món đã gỡ chỉ đọc được, ngoại trừ "đưa lại" và xoá. **(tôi chọn)**

**Xoá** (mềm) dành cho món nhập nhầm. **Khôi phục** khi địa điểm của nó chưa bị xoá.

**Đọc**: đồ đang có ở một địa điểm (theo tên); đồ đã gỡ; đồ trong cả cây con; theo nhóm; tìm theo tên bắt đầu bằng hoặc theo mã.

### 5.10 Lịch sử và khôi phục version

- Xem danh sách version của một địa điểm, người hoặc món đồ (mới nhất trước, có `changed_fields`); xem một version kèm snapshot. **(bạn)**
- **Hoạt động gần đây** của cả không gian: các version mới nhất trước, theo index `(workspace_id, changed_at)`, có lọc theo loại bản ghi. **(tôi chọn)** Đây cũng là chỗ người dùng tìm lại `id` của thứ đã xoá để khôi phục.
- **Khôi phục về một version cũ** là một lần sửa bình thường mang nội dung của version đó: ghi version mới với `action` UPDATE và ghi chú "Restored from version N"; không version nào bị ghi đè. **(bạn)**
  - Chỉ khôi phục phần nội dung. Không di chuyển địa điểm, không đổi vị trí, trạng thái gỡ hay thay của đồ đạc. **(tôi chọn)** Đổi chỗ đứng là thao tác riêng có luật riêng.
  - Với địa điểm: khôi phục cả địa chỉ của version đó.
  - Nội dung cũ phải qua kiểm tra như một lần sửa (loại còn dùng được, `attributes` hợp lệ theo mục tuỳ biến hiện tại). Không qua thì trả 400 nói rõ lý do, người dùng sửa tay. **(tôi chọn)**
  - Bản ghi đang bị xoá thì phải bỏ xoá trước; trả 409.

## 6. Media

**Phần này chờ chọn kho lưu trữ file** (S3, MinIO hay tương đương). Ghi lại luật để thiết kế trước. **(tôi chọn: làm sau cùng)**

- **Tải lên**: chỉ nhận ảnh JPEG, PNG, WebP; tối đa 10 MB. **(tôi chọn)** Tạo `med_file` trạng thái PENDING, đưa file vào kho, đọc kích thước ảnh, rồi chuyển sang READY. Hỏng giữa chừng thì file ở PENDING và được dọn sau.
- **Trùng**: cùng `checksum_sha256` trong cùng không gian và đang READY thì dùng lại file cũ, không lưu lần hai. **(bạn: checksum để nhận ra file trùng)**
- **Gắn** vào một địa điểm, người hoặc món đồ (`owner_type`, `owner_id`). Media không kiểm tra được bản ghi bên registry có tồn tại không. **(tôi chọn)** Kèm `caption`, `sort_order`.
- **Gỡ ảnh**: ghi `removed_by`, `removed_at`; không xoá dòng. **(bạn)**
- **Sắp xếp lại** thứ tự ảnh của một bản ghi.
- Xoá bản ghi bên registry không gỡ ảnh. Ảnh vẫn còn để khôi phục bản ghi thì có lại đủ ảnh. **(tôi chọn)**

## 7. Thứ tự xây

1. **Danh mục của registry**: khởi tạo không gian, loại địa điểm, mục tuỳ biến (gồm bộ kiểm tra `attributes`), nhóm đồ.
2. **Địa điểm**: tạo, sửa, di chuyển, xoá, khôi phục, địa chỉ, các cách đọc, lịch sử và khôi phục version, hoạt động gần đây.
3. **Người và cư trú.**
4. **Đồ đạc.**
5. **Identity**: người dùng, không gian.
6. **Media**, sau khi chọn kho lưu trữ.

**(tôi chọn)** Registry là lõi và đã có đủ phần nền để làm ngay; identity nhỏ và chưa ai phụ thuộc vì chưa có đăng nhập; media cần một quyết định về hạ tầng. Mỗi bước là một kế hoạch riêng, xong và chạy được trước khi sang bước sau.

## 8. Để lại cho người xây

- Đường dẫn API, tên request và response, tên lớp và package (theo quy ước `com.home_platform.<service>.<nghiệp vụ>` và ba tầng controller, service, repository trong README).
- `version` gửi ở body hay ở tham số với từng thao tác.
- Câu SQL cụ thể của từng truy vấn, miễn mỗi truy vấn dùng đúng index ở mục 7 của thiết kế database.
- Cách tính geohash (thư viện hay tự viết).

## 9. Câu hỏi còn mở, không chặn bước 1

- Kho lưu trữ file cho media (mục 6).
- Có cần danh mục đơn vị hành chính chuẩn (tỉnh, phường) thay cho chữ tự do không.
- Cần "thùng rác" liệt kê thứ đã xoá ngoài cách tìm qua hoạt động gần đây không.
