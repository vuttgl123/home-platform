# Tìm và lọc sâu hơn (Đợt 11)

Ngày: 2026-11-26 · Trạng thái: **đã xây** (2026-11-27), **đã duyệt** (bạn: "Được làm cho tôi nhé cứ theo thứ tự bạn đề xuất" với nhóm 2 "Tìm và lọc sâu hơn"; "làm tiếp wave tiếp theo")

Ký hiệu: **(bạn)** là điều bạn đã nói; **(tôi chọn)** là quyết định của tôi, bạn có thể bác.

Liên quan: lộ trình `2026-11-20-roadmap-design.md` (Đợt 11; mục 4 "Để dành": lọc theo mục tuỳ biến, tìm theo từ khoá trong nội dung), Đợt 6 `2026-11-25-capacity-and-vacancies-design.md` (sức chứa, người đang ở), Đợt 2 `2026-11-16-find-and-recover-design.md` (ô tìm chung Ctrl+K).

## 1. Mục đích

Bạn muốn những gì đã có "chi tiết hơn, cụ thể hơn" **(bạn)**. Dữ liệu đã nhiều (nhập Excel, mục tuỳ biến), nhưng tìm và lọc còn mỏng:

| Câu hỏi | Hiện nay | Sau đợt này |
|---|---|---|
| Phòng nào ở Toà A còn chỗ, loại Phòng, diện tích từ 20 m² trở lên? | Không có trang danh sách địa điểm; trang Chỗ trống không lọc theo mục tuỳ biến | Trang **Địa điểm** dạng bảng, lọc theo loại, trạng thái, toà, còn chỗ, mục tuỳ biến |
| Ai quê Huế? CCCD của ai hết hạn trước 2027? | Chỉ tìm theo tên (khớp đầu), số điện thoại, số giấy tờ (khớp đúng) | Trang Người lọc theo mục tuỳ biến, theo đang ở / không ở đâu, theo toà |
| Món nào có ghi "bảo hành" trong mô tả? Đồ ở Toà B? | Trang Đồ đạc chỉ lọc trạng thái, nhóm | Ô tìm chữ (chứa, trong tên, mã, mô tả), lọc theo toà và mục tuỳ biến |
| Tìm một chữ nằm trong ghi chú | Ctrl+K chỉ tìm tên và mã | Ctrl+K có dòng "Tìm '…' trong Người / Đồ đạc / Địa điểm" mở trang danh sách đã lọc |

## 2. Mục tiêu, không làm, phải tránh

**Mục tiêu:** lọc kết hợp được (mọi điều kiện cùng đúng); bộ lọc nằm trên URL để chép đường dẫn gửi người khác; tìm chữ không phân biệt hoa thường và dấu (như cách database so chữ); nhanh với vài nghìn bản ghi mỗi không gian.

**Không làm** (tôi chọn): điều kiện "hoặc"; lưu bộ lọc có tên; sắp theo mục tuỳ biến; xuất Excel theo bộ lọc của trang Địa điểm; chỉ mục toàn văn (FULLTEXT); tìm trong giá trị mục tuỳ biến bằng ô chữ (đã có bộ lọc mục tuỳ biến).

**Phải tránh:** câu SQL ghép từ chữ người dùng (mọi giá trị đi qua tham số; khoá mục tuỳ biến chỉ nhận khoá của mục đang dùng); bộ lọc lạ trên URL làm hỏng trang (bị bỏ qua).

## 3. Luật (tôi chọn)

**S1. Tìm chữ (`q`).** Khớp **chứa** (`LIKE %q%`, ký tự `%`, `_`, `\` hiểu theo nghĩa đen), theo collation của database (không phân biệt hoa thường và dấu). Bỏ khoảng trắng hai đầu; dưới 2 ký tự thì bỏ qua. Cột được tìm:
- Người: họ tên, số điện thoại, email, số giấy tờ, ghi chú.
- Đồ đạc: tên, mã, mô tả.
- Địa điểm: tên, mã, mô tả.

**S2. Lọc theo mục tuỳ biến (`attr`).** Tối đa 3 điều kiện, cùng đúng (AND). Mỗi điều kiện là `khoá:phép:giá trị` (giá trị có thể chứa `:`; tách ở hai dấu `:` đầu). Khoá phải là mục **đang dùng** của đúng đích; sai thì 400. Phép theo kiểu:

| Kiểu | Phép | Nghĩa |
|---|---|---|
| Chữ | `has` chứa, `eq` bằng | so theo collation, như S1 |
| Số | `eq`, `gte` (≥), `lte` (≤) | so như số |
| Ngày | `gte` (từ ngày), `lte` (đến ngày) | so ngày `yyyy-mm-dd` |
| Có/Không | `eq` với `true` / `false` | `false` khớp cả ô trống |
| Lựa chọn | `eq` | đúng lựa chọn đó |
| Mọi kiểu | `set` có giá trị, `unset` trống | |

Giá trị sai kiểu (số không phải số, ngày sai dạng, lựa chọn không có trong mục) thì 400. Mục theo loại (nhóm) chỉ khớp bản ghi thuộc loại đó, vì bản ghi khác không có giá trị (với `unset`: bản ghi khác loại cũng được tính là trống).

**S3. Trang Địa điểm** `/w/:ws/places`, mục mới trên thanh bên ngay dưới "Việc cần làm":
- Bảng: Mã, Tên (link), Loại, Nằm trong (đường dẫn tên), Sức chứa, Đang ở, Trạng thái; theo tên rồi `id`; "Tải thêm".
- Lọc: chữ (S1), Loại, Trạng thái, Toà (chỉ hậu duệ của một địa điểm cấp cao nhất, không gồm nó), "Còn chỗ" (có sức chứa và số người đang ở hôm nay nhỏ hơn), mục tuỳ biến (S2).
- API `GET /places/list?q=&typeId=&status=&rootId=&hasRoom=&attr=&cursor=&limit=`; mỗi dòng như `VacancyResponse` của Đợt 6 (`ancestors`, `capacity`, `occupantCount`) cộng `status`, `code`.

**S4. Trang Người:** ô tìm duy nhất (S1) thay cho ô "Tìm theo" ba lựa chọn; lọc "Đang ở" / "Không ở đâu" (có hay không một dòng cư trú hôm nay, như Đợt 6), Toà (đang ở tại toà đó hay một nơi bên trong nó), mục tuỳ biến. API: `GET /people?q=&living=&rootId=&attr=`.

**S5. Trang Đồ đạc:** thêm ô tìm (S1), Toà (món nằm trong toà đó), mục tuỳ biến, cạnh Trạng thái và Nhóm đang có. "Toà" gồm chính toà đó và mọi nơi bên trong. API: `GET /items` (khi không có `placeId`, `ownerId`) nhận thêm `q`, `rootId`, `attr`.

**S6. URL của trang:** `?q=`, `?type=`, `?status=`, `?root=`, `?room=1`, `?living=yes|no`, `?f=khoá~phép~giá trị` (lặp, tối đa 3). Giá trị lạ, khoá không còn dùng thì bỏ qua (không lỗi).

**S7. Ctrl+K:** khi đã gõ từ 2 ký tự, cuối danh sách có ba dòng "Tìm '…' trong Người", "… trong Đồ đạc", "… trong Địa điểm"; chọn là mở trang đó với `?q=`.

## 4. Cách làm

### 4.1 Hướng đã cân nhắc

| Việc | Hướng | Được | Mất |
|---|---|---|---|
| Lọc mục tuỳ biến | **a. Đọc JSON `attributes` ngay trong câu SQL** (đã chọn) | Không đổi cấu trúc bảng; mục mới lọc được ngay | Quét các dòng của không gian (không có chỉ mục); đủ nhanh với vài nghìn dòng |
| | b. Cột sinh ra có chỉ mục cho từng mục | Nhanh với dữ liệu lớn | Mỗi lần thêm, đổi mục là đổi bảng |
| | c. Bảng giá trị riêng (mỗi giá trị một dòng) | Chỉ mục tốt | Viết lại cách lưu mọi bản ghi |
| Tìm chữ | **a. `LIKE %q%` theo collation** (đã chọn) | Không phân biệt hoa thường và dấu sẵn; không thêm gì | Quét các dòng; đủ nhanh với vài nghìn dòng |
| | b. Chỉ mục FULLTEXT (ngram) | Nhanh với dữ liệu lớn | Cần cấu hình ngram cho tiếng Việt, kết quả khó đoán với chữ ngắn |

### 4.2 Backend (registry)

- `common`-style helper trong registry: `AttributeConditions` (đọc `attr`, kiểm theo mục tuỳ biến đang dùng, sinh điều kiện `KeysetQuery.where` với tham số có tên riêng mỗi điều kiện) và `TextSearch` (đọc `q`, sinh `LIKE` trên các cột).
- `PlaceRepository`, `PersonRepository`, `ItemRepository` thêm một câu danh sách nhận bộ lọc; điều kiện "đang ở", "còn chỗ" dùng lại biểu thức người đang ở của Đợt 6.
- Không có migration.

### 4.3 Frontend

- `features/places/pages/PlacesPage.tsx` mới; hook danh sách theo bộ lọc.
- Một thanh lọc mục tuỳ biến dùng chung (`AttributeFilters`, trong `features/catalog`): chọn mục, phép (theo kiểu), giá trị (ô chữ, số, ngày, chọn); tối đa 3; mỗi điều kiện có nút bỏ.
- Trang Người, Đồ đạc đổi như S4, S5; bộ lọc trên URL như S6.
- Ctrl+K như S7.

## 5. Kiểm tra

- Backend: IT cho S1 (chứa, không phân biệt dấu, ký tự `%` theo nghĩa đen, dưới 2 ký tự bỏ qua), S2 (mọi kiểu và phép, `unset` gồm bản ghi khác loại, khoá lạ, giá trị sai kiểu → 400, 4 điều kiện → 400), S3–S5 (từng bộ lọc, kết hợp, phân trang).
- Frontend: trang Địa điểm (bảng, từng bộ lọc lên URL, tải thêm), Người và Đồ đạc (ô tìm, toà, đang ở, mục tuỳ biến), thanh lọc (phép theo kiểu, tối đa 3, bỏ điều kiện), Ctrl+K (ba dòng tìm trong nội dung).
- Xem bằng mắt ở 1440, 390, sáng và tối.

## 6. Để lại cho người làm

Tên class, cách chia task, query key, chữ phụ của từng phép trên giao diện ("chứa", "bằng", "từ", "đến", "có giá trị", "trống"), cách hiện điều kiện đã chọn (chip), biểu tượng mục Địa điểm trên thanh bên.
