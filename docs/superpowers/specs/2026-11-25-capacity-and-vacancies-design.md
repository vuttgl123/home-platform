# Chỗ ở và sức chứa (Đợt 6, D3)

Ngày: 2026-11-25 · Trạng thái: **đã duyệt theo uỷ quyền** (bạn: "Được hãy làm cho tôi"; "Còn các quy tắc bạn tự đề xuất tự làm nhé")

Ký hiệu: **(bạn)** là điều bạn đã nói; **(tôi chọn)** là quyết định của tôi, bạn có thể bác.

Liên quan: lộ trình `2026-11-20-roadmap-design.md` (mục Đợt 6), luật nghiệp vụ `2026-11-13-business-rules-design.md`, trang tổng quan `2026-11-17-overview-and-export-design.md`, giao diện `2026-11-20-ui-refresh-design.md`.

## 1. Mục đích

Trang Tổng quan đã đếm được chỗ "có người" và chỗ "trống", nhưng chưa biết một phòng chứa được mấy người, và chưa có chỗ nào để tìm phòng còn trống rồi xếp người vào. Đợt này trả lời ba câu hỏi hằng ngày của người quản lý:

| Câu hỏi | Hiện nay | Sau đợt này |
|---|---|---|
| Còn chỗ nào xếp được người? | Mở từng phòng trên cây | Trang **Chỗ trống**: lọc theo loại, theo toà; mỗi dòng ghi còn mấy chỗ; bấm **Xếp người** là thêm luôn |
| Phòng này đầy chưa? | Không biết, vì chưa có sức chứa | Địa điểm có **sức chứa** (tuỳ chọn); trang địa điểm hiện "đang ở 3/4"; form thêm người cảnh báo khi vượt |
| Ai sắp đi, ai đã ở đây trước? | Lật danh sách cư trú theo trạng thái | **Dòng thời gian** cư trú của một địa điểm và của một người; trang Tổng quan có **Sắp chuyển đi** (30 ngày tới) |

## 2. Mục tiêu, phần không làm, điều phải tránh

**Mục tiêu**

- Đặt sức chứa cho một địa điểm trong form sửa, có trong lịch sử version, so sánh version, xuất Excel và trang in.
- Xếp người vào một chỗ trống trong hai lần bấm, từ trang Chỗ trống.
- Thấy cảnh báo vượt sức chứa trước khi lưu, nhưng vẫn lưu được.
- Nhìn một dải thời gian là biết ai ở từ khi nào tới khi nào, ai sắp đến, ai sắp đi.

**Không làm ở đợt này** (tôi chọn)

- Sức chứa mặc định theo loại địa điểm, hay sức chứa của toà là tổng các phòng. Sức chứa chỉ tính người ở **trực tiếp** tại địa điểm đó.
- Chặn khi vượt sức chứa. Lộ trình đã chốt chỉ cảnh báo, vì thực tế vẫn có lúc phải ở ghép tạm.
- Xếp tới từng giường, đặt chỗ trước, tiền phòng (tiền đã ở ngoài lộ trình **(bạn, Đợt 1)**).
- Nhắc việc, thông báo khi ai đó sắp chuyển đi: thuộc Đợt 8. Ở đợt này chỉ có danh sách trên trang Tổng quan.

**Điều phải tránh**

- Con số lệch nhau giữa trang Chỗ trống, trang địa điểm và trang Tổng quan.
- Cảnh báo cản đường: hộp thoại xác nhận chen ngang, hoặc nút Lưu bị khoá.
- Dòng thời gian không đọc được trên điện thoại.
- Trang Chỗ trống chậm dần khi dữ liệu nhiều lên, hoặc gửi một request cho mỗi dòng.

## 3. Luật nghiệp vụ (tôi chọn)

**L1. Sức chứa.** Cột `capacity` của địa điểm, kiểu số nguyên, **bỏ trống được** (nghĩa là "chưa đặt"), từ 0 tới 9999. Giá trị 0 nghĩa là "không xếp người ở", ví dụ một phòng kho. Sửa sức chứa là một lần sửa địa điểm bình thường: tăng `version`, ghi lịch sử.

**L2. Ai chiếm chỗ.** Mỗi **người** có dòng cư trú còn hiệu lực **trong ngày đang xét** tại đúng địa điểm đó chiếm một chỗ, **dù quan hệ là gì** (ở, khách, nhân viên, khác, hay "chủ" cũ). Chỉ tính dòng chưa xoá của người chưa xoá. Đây cũng là định nghĩa "có người" mà trang Tổng quan đang dùng, nên hai nơi không lệch nhau. "Còn hiệu lực" giữ nguyên như hiện nay: `start_date ≤ ngày ≤ end_date`, trong đó ngày kết thúc vẫn tính là còn ở.

**L3. Vượt sức chứa.** Khi thêm hoặc sửa một dòng cư trú, gọi **đỉnh** là số người nhiều nhất có mặt cùng một ngày tại địa điểm đó trong khoảng ngày của dòng, **không tính dòng đang sửa**. Khi `đỉnh + 1 > sức chứa`, form hiện cảnh báo nêu ngày đông nhất. Nút Lưu vẫn bấm được. Địa điểm chưa đặt sức chứa thì không bao giờ cảnh báo. Khoảng ngày không có ngày kết thúc thì tính tới mọi dòng bắt đầu về sau.

**L4. Chỗ trống.** Một địa điểm chưa xoá được coi là **còn chỗ hôm nay** khi:
- (a) có sức chứa và số người đang ở nhỏ hơn sức chứa. Số chỗ còn lại = sức chứa − số người đang ở; hoặc
- (b) chưa đặt sức chứa, **không có địa điểm con** chưa xoá, và không có ai đang ở. Ghi là "Trống, chưa đặt sức chứa".

Nhờ điều kiện "không có con" ở (b), toà nhà và tầng không lẫn vào danh sách, trừ khi bạn đặt sức chứa cho chúng. Địa điểm "Đang dùng" hay "Ngừng dùng" đều được tính theo cùng một cách; cột trạng thái của dòng ghi rõ trạng thái đó.

**L5. Sắp chuyển đi.** Dòng cư trú còn hiệu lực hôm nay, có `end_date` từ hôm nay tới hôm nay + 30 ngày (tính cả hai đầu), của người và địa điểm chưa xoá. Sắp theo ngày kết thúc, gần nhất trước.

## 4. Cách làm

### 4.1 Hướng đã cân nhắc

| Việc | Hướng | Được | Mất |
|---|---|---|---|
| Danh sách chỗ trống | **a. API `GET /places/vacancies` đếm bằng SQL** (đã chọn) | Một request mỗi trang; luật L4 nằm ở một nơi; phân trang như các danh sách khác | Thêm một câu truy vấn có subquery đếm |
| | b. Frontend tự ghép từ cây địa điểm và danh sách cư trú | Không đụng backend | Phải tải cả cây và mọi dòng cư trú; chậm dần theo dữ liệu |
| Cảnh báo vượt sức chứa | **a. API kiểm tra `GET /places/{id}/capacity-check`, form gọi khi chọn chỗ và ngày** (đã chọn) | Cảnh báo **trước** khi lưu; không đổi API ghi | Thêm một request nhẹ mỗi khi đổi ngày |
| | b. API ghi trả về `warnings` sau khi lưu | Không thêm API đọc | Cảnh báo tới khi đã lưu xong, quá muộn để đổi ý |
| | c. API ghi trả 409 khi vượt | Không bao giờ vượt | Trái với lộ trình (chỉ cảnh báo, không chặn) |
| Dòng thời gian | **a. Thanh bằng `div` đặt theo %, trong `scroll-area`** (đã chọn) | Một người có nhiều đợt ở vẫn nằm trên một hàng; test được bằng DOM; trên điện thoại cuộn ngang được | Tự viết trục tháng |
| | b. `recharts` với BarChart nằm ngang có khoảng | Có sẵn trục, tooltip | Khó vẽ nhiều thanh trên một hàng; trục ngày gượng; test cần shim |
| | c. Thư viện Gantt | Đủ tính năng | Thêm phụ thuộc cho một việc nhỏ |

### 4.2 Backend (registry-service)

**Migration `V7__place_capacity.sql`:** `ALTER TABLE reg_place ADD COLUMN capacity SMALLINT UNSIGNED NULL AFTER status`.

**Địa điểm.** `capacity` đi qua `Place`, `PlaceSnapshot`, `PlaceCreateRequest`, `PlaceUpdateRequest` (`@Min(0) @Max(9999)`, bỏ trống được), `PlaceDetailResponse`, và câu insert/update của `PlaceRepository`. `PlaceDetailResponse` có thêm `occupantCount`, là số người đang ở hôm nay theo L2. Sheet "Địa điểm" của file Excel có thêm cột **Sức chứa**. Các version cũ không có trường này thì đọc ra là `null`.

**`GET /places/{id}/capacity-check?startDate=&endDate=&excludeOccupancyId=`** trả `{ capacity, peak, peakDate }` theo L3. `endDate` bỏ trống nghĩa là không có ngày kết thúc. `peakDate` là ngày đầu tiên đạt `peak`, hoặc `null` khi `peak` = 0. Địa điểm không có hay đã xoá thì trả 404. Cách đếm: lấy các dòng giao với khoảng đó (dùng index `idx_reg_occupancy_place`) rồi quét theo ngày trong Java.

**`GET /places/vacancies?placeTypeId=&rootId=&cursor=&limit=`** trả trang `{ placeId, code, name, placeTypeId, placeTypeName, status, ancestors[], capacity, occupantCount, free }` theo L4, sắp theo tên rồi id, phân trang bằng con trỏ như các danh sách khác. `free` là `null` ở trường hợp (b). `rootId` lọc theo cây con bằng `path` (không lấy chính gốc). `ancestors` lấy theo lô như trang địa điểm.

**`GET /occupancies/timeline?placeId=` hoặc `?personId=`** (đúng một trong hai) trả `{ rows: OccupancyResponse[], truncated }`: mọi dòng chưa xoá, mọi trạng thái, tối đa 200 dòng có `start_date` mới nhất. `truncated` = còn dòng cũ hơn chưa trả về. API danh sách hiện có lọc theo một trạng thái và phân trang, nên không vẽ được cả dải thời gian.

**`GET /overview`** có thêm `endingSoon: { total, rows[] }`, trong đó `rows` gồm tối đa 10 dòng `{ occupancyId, personId, personName, placeId, placeName, endDate }` theo L5.

### 4.3 Frontend

- **Form địa điểm:** ô số "Sức chứa (người)", bỏ trống được, kèm gợi ý "Bỏ trống nếu không giới hạn". Trong tab Tổng quan của địa điểm, thẻ Thông tin có dòng "Sức chứa": "3/4 người · còn 1 chỗ", "4/4 · đã đầy", "5/4 · vượt 1", nhãn có màu theo `ToneBadge`. Chưa đặt sức chứa thì ghi "Chưa đặt · đang ở 3". Trang in và phần so sánh version có thêm nhãn "Sức chứa".
- **Form cư trú:** sau khi đã có địa điểm và ngày bắt đầu, gọi `capacity-check` (chờ 300 ms sau lần gõ cuối). Khi vượt, hiện một `Alert` màu cảnh báo, ví dụ "P101 chứa 2 người; ngày 01/12/2026 đã có 2 người. Vẫn lưu được." Nút Lưu không bị khoá.
- **Trang Chỗ trống** `/w/:ws/vacancies`, nằm trên thanh bên ngay dưới "Người", biểu tượng giường. Lọc nhanh theo loại bằng `toggle-group` ("Mọi loại" và các loại đang dùng), lọc theo toà bằng một ô chọn các địa điểm cấp cao nhất. Bộ lọc nằm trên URL như trang Hoạt động. Mỗi dòng có tên (bấm để mở), đường dẫn "Toà A › Tầng 1", loại, một thanh `progress` nhỏ "đang ở 1/4 · còn 3", và nút **Xếp người**. Nút này mở `OccupancyFormDialog` với địa điểm đã chọn sẵn; lưu xong thì danh sách tải lại. Có "Tải thêm" và trạng thái rỗng "Không còn chỗ trống nào khớp bộ lọc". Trên điện thoại, mỗi dòng thành một thẻ, như danh sách đồ đạc.
- **Dòng thời gian:** component `OccupancyTimeline`, nằm trên danh sách trong tab "Người ở" của địa điểm (mỗi hàng một người) và trong tab thông tin cư trú của người (mỗi hàng một địa điểm). Trục tháng chạy từ ngày bắt đầu sớm nhất tới `max(hôm nay, ngày kết thúc muộn nhất)`, đệm thêm vài ngày hai đầu. Có vạch "Hôm nay". Thanh không có ngày kết thúc kéo tới mép phải và mờ dần. Màu thanh theo trạng thái: đang ở, sắp đến, đã đi. Rê chuột lên thanh thì hiện quan hệ và hai ngày; trên điện thoại thì bấm. Không có dòng nào thì ẩn cả component. Khi `truncated` thì ghi chú "Chỉ hiện 200 lần ở gần nhất".
- **Trang Tổng quan:** thẻ "Sắp chuyển đi" ở cột phải, dưới "Đồ đạc theo trạng thái". Mỗi dòng gồm tên người, địa điểm, và "còn N ngày" hoặc "hôm nay". Có số tổng khi nhiều hơn 10 dòng. Không có ai thì ghi "Không ai chuyển đi trong 30 ngày tới". Phần "Chỗ ở theo loại" có thêm liên kết "Xem chỗ trống". Registry bản cũ không có `endingSoon` thì ẩn thẻ này, như cách đã làm với `activityByDay`.

## 5. Kiểm tra

- **Backend (IT trên MySQL thật, trong transaction rollback như hiện nay):**
  - L2 đếm đúng với nhiều quan hệ, với người đã xoá, với ngày kết thúc là hôm nay;
  - L3 tính đúng đỉnh và ngày đông nhất, bỏ dòng đang sửa, xử lý khoảng mở;
  - L4 xét đủ (a), (b), địa điểm có con, sức chứa 0, lọc theo loại và theo toà, phân trang;
  - L5 lấy đúng hai đầu khoảng 30 ngày;
  - `capacity` có trong lịch sử, khôi phục version, xuất Excel;
  - sửa sức chứa sai khoảng thì trả 400.
- **Frontend (Vitest):** form địa điểm và cư trú, trang Chỗ trống (lọc, Xếp người, rỗng), `OccupancyTimeline` (vị trí thanh, khoảng mở, vạch hôm nay, rỗng), thẻ Sắp chuyển đi (có, không có, registry cũ).
- **Xem bằng mắt** ở khổ 1440, 1920 và 390 (điện thoại), cả nền sáng và nền tối, bằng bộ preview riêng.

## 6. Để lại cho người làm

Tên class, cách chia task, tên query key và vị trí file theo cấu trúc sẵn có: `features/vacancies` mới; `OccupancyTimeline` đặt trong `features/people`, gần các component cư trú. Độ dài debounce, cỡ chữ và khoảng đệm hai đầu trục có thể chỉnh khi xem bằng mắt.
