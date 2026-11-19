# Nhìn tổng thể và lấy dữ liệu ra: tổng quan, xuất Excel, in hồ sơ, lọc hoạt động, so sánh version

Ngày: 2026-11-17 · Trạng thái: **đã duyệt, đã xây** (2026-11-19)

Ký hiệu: **(bạn)** là điều bạn đã nói; **(tôi chọn)** là quyết định của tôi, bạn có thể bác.

Liên quan: logic nghiệp vụ `2026-11-13-business-rules-design.md`, cấu trúc frontend `2026-11-13-frontend-structure-design.md`, đợt trước `2026-11-16-find-and-recover-design.md`.

## 1. Mục đích

Bạn giao cho tôi tự đề xuất các ý còn lại. **(bạn: "cứ theo ý kiến của bạn đề xuất đi")** Tôi xếp năm ý này vào Đợt 4 vì chúng chỉ đọc dữ liệu đang có: không thêm bảng, không đổi luật nghiệp vụ. Làm xong đợt này, những gì bạn đã nhập trở nên dễ xem, dễ mang ra ngoài. **(tôi chọn)**

| Việc | Hiện nay | Sau đợt này |
|---|---|---|
| F1. Vào một không gian | Trang trống "Chọn một địa điểm trên cây" | Trang tổng quan: con số chính, chỗ có người và chỗ trống theo loại, đồ theo trạng thái, việc cần làm, hoạt động gần đây |
| F4. Mang dữ liệu ra Excel | Không có | Tải một file Excel đủ năm sheet; tải riêng danh sách đồ đạc (theo lọc đang chọn) và danh sách người |
| F6. In hồ sơ một địa điểm | Không có | Trang in: thông tin, người đang ở, chủ sở hữu, đồ đạc |
| G6. Tìm một thay đổi cũ | Trang Hoạt động chỉ lọc được theo loại bản ghi | Lọc thêm theo hành động, theo khoảng ngày, "chỉ việc tôi làm"; bộ lọc nằm trên URL |
| G2. Xem hai lần sửa khác gì | Mở từng version một | Chọn hai version, xem cạnh nhau, tô đậm trường khác |

## 2. Mục tiêu, phần không làm, điều phải tránh

**Mục tiêu**

- Mở không gian ra là biết ngay: bao nhiêu chỗ, bao nhiêu chỗ còn trống, bao nhiêu món đang hỏng, còn gì phải xử lý.
- File Excel mở được ngay trên Excel tiếng Việt, đúng dấu, ngày là ngày, số là số.
- Trả lời "ai đã đổi gì trong tuần trước" và "lần sửa thứ 3 với thứ 7 khác gì" mà không phải lật từng dòng.

**Không làm ở đợt này**

- Biểu đồ. Ở quy mô một gia đình hay một toà nhà, con số và danh sách ngắn rõ hơn biểu đồ. **(tôi chọn)**
- Danh sách chỗ trống chi tiết, sức chứa: thuộc D3 ở Đợt 6.
- Lọc hoạt động theo tên từng thành viên: registry chưa biết tên thành viên, identity chưa có API danh sách thành viên. Đợt này chỉ có "chỉ việc tôi làm".
- Xuất PDF, hẹn giờ xuất, gửi file qua email. In ra PDF dùng chức năng "Lưu thành PDF" của trình duyệt.
- Xuất bản ghi đã xoá. File chỉ chứa bản ghi đang có; đồ đã gỡ vẫn có, cột trạng thái ghi rõ.
- Nhập ngược từ Excel: F5 ở Đợt 9.

**Điều phải tránh**

- File xuất mở trên Excel tiếng Việt bị dồn cả dòng vào một ô, hoặc lỗi dấu.
- Trang tổng quan gửi hàng chục request, hoặc chậm dần khi dữ liệu nhiều lên.
- Số trên trang tổng quan lệch với danh sách khi bấm vào xem.
- Xuất một không gian lớn làm service hết bộ nhớ.

## 3. Cách làm

### 3.1 Trang tổng quan (F1)

| Hướng | Được | Mất |
|---|---|---|
| **a. Một API `GET /overview` trả mọi con số** (đã chọn) | Một request; mỗi con số là một câu `COUNT` dùng index sẵn có; số đếm đúng một cách ở một nơi | Thêm một package nhỏ ở backend |
| b. Frontend tự đếm từ các API danh sách | Không đụng backend | Phải tải hết mọi danh sách chỉ để đếm; chậm dần theo dữ liệu |

`GET /overview` trả về:

| Phần | Nội dung |
|---|---|
| `places` | Tổng số địa điểm đang có |
| `placeTypes[]` | Với mỗi loại địa điểm đang dùng: `typeId`, `name`, `total`, `occupied` (số chỗ có ít nhất một dòng cư trú còn hiệu lực hôm nay). Chỗ trống = `total − occupied`. Loại không có chỗ nào thì bỏ |
| `peopleLiving` | Số người khác nhau có ít nhất một dòng cư trú còn hiệu lực hôm nay |
| `people` | Tổng số hồ sơ người |
| `itemsByStatus` | Số món theo `IN_USE`, `BROKEN`, `STORED`, `REMOVED` |
| `missingCodes` | Số địa điểm và số đồ chưa có mã (đúng con số của `GET /codes/status`) |

"Còn hiệu lực hôm nay" giữ nguyên định nghĩa đang dùng ở tab Người ở: `start_date <= hôm nay` và (`end_date` trống hoặc `>= hôm nay`), bản ghi chưa xoá. "Hôm nay" lấy theo cách service đang tính ngày cho cư trú. **(tôi chọn)** Không cache: mỗi câu đếm chạy trên index theo `workspace_id`, và số phải khớp ngay sau khi sửa.

Bố cục (thay trang trống khi không gian đã có ít nhất một địa điểm; không gian chưa có danh mục hay chưa có địa điểm giữ nguyên trang hiện nay):

```
Tổng quan                                                [Tải Excel]
┌────────────┬──────────────┬──────────────┬──────────────┐
│ 42 địa điểm│ 18 người đang│ 230 món đồ   │ 3 món hỏng   │
│            │ ở / 25 hồ sơ │ đang dùng    │              │
└────────────┴──────────────┴──────────────┴──────────────┘
Chỗ ở theo loại               Đồ đạc theo trạng thái
Phòng   12 có người · 8 trống  Đang dùng 230 · Hỏng 3 · Cất kho 12 · Đã gỡ 40
Căn hộ   5 có người · 1 trống
Cần xử lý                      Hoạt động gần đây            Xem tất cả →
• 4 địa điểm chưa có mã [Cấp mã] • Sửa "P301" · v4 · Bạn · 08:12
                                 • …(5 dòng)
```

- Mỗi số trạng thái đồ, kể cả ô "món đồ đang dùng" và "món hỏng" ở hàng trên, là liên kết tới `/w/:ws/items?status=…` (trang Đồ đạc đã đọc `?status=` từ URL), để số bấm vào luôn khớp với danh sách mở ra.
- "Cần xử lý" chỉ hiện khi có việc. Đợt này có một việc: bản ghi chưa có mã. Nút "Cấp mã" mở trang Danh mục ở phần "Mã tự cấp". Các đợt sau thêm việc vào đây (bảo hành sắp hết, bảo trì tới hạn, người sắp chuyển đi).
- "Hoạt động gần đây" là 5 dòng đầu của `GET /activity`, dùng lại dòng của trang Hoạt động.
- Liên kết "Tổng quan" đứng đầu thanh điều hướng, trỏ tới `/w/:ws`.

### 3.2 Xuất Excel (F4)

| Hướng | Được | Mất |
|---|---|---|
| **a. Backend tạo file `.xlsx` thật bằng thư viện ghi dạng luồng** (đã chọn) | Excel tiếng Việt mở đúng ngay; ngày, số đúng kiểu; một file nhiều sheet; ghi từng lô nên không giữ cả không gian trong bộ nhớ | Thêm một thư viện vào registry-service |
| b. CSV | Không thêm thư viện | Windows tiếng Việt dùng dấu `;` làm dấu phân cách danh sách, nên CSV dấu phẩy bị dồn vào một cột; một file chỉ một bảng |
| c. Frontend tự tạo file | Không đụng backend | Phải tải hết dữ liệu về trình duyệt; thư viện xlsx phía trình duyệt nặng |

Thư viện: `org.dhatim:fastexcel`. Nó ghi xlsx dạng luồng, không kéo theo Apache POI. **(tôi chọn)**

**API**

| Lệnh | File |
|---|---|
| `GET /exports/workspace.xlsx` | Năm sheet: Địa điểm, Người, Cư trú, Sở hữu, Đồ đạc |
| `GET /exports/items.xlsx?status=&categoryId=` | Một sheet Đồ đạc, cùng luật lọc như `GET /items` không phạm vi |
| `GET /exports/people.xlsx` | Một sheet Người |

Tên file: `<tên không gian bỏ dấu>-<loại>-<yyyy-MM-dd>.xlsx`, gửi qua `Content-Disposition` có cả `filename*` UTF-8. Registry không biết tên không gian, nên frontend tự đặt tên khi lưu file. Header chỉ là tên dự phòng: `home-platform-<loại>-<ngày>.xlsx`. **(tôi chọn)**

**Cột của từng sheet** (dòng đầu là tiêu đề tiếng Việt, cố định dòng đầu, bật lọc tự động):

| Sheet | Cột |
|---|---|
| Địa điểm | Mã, Tên, Loại, Nằm trong (đường dẫn tên từ gốc, ví dụ "Toà A / Tầng 3"), Trạng thái, Mô tả, rồi mỗi mục tuỳ biến của địa điểm một cột |
| Người | Họ tên, Điện thoại, Email, Loại giấy tờ, Số giấy tờ, Ngày sinh, Giới tính, Đang ở (các địa điểm còn hiệu lực hôm nay, ngăn bằng "; "), Ghi chú, rồi các mục tuỳ biến của người |
| Cư trú | Địa điểm (đường dẫn đầy đủ), Mã địa điểm, Người, Quan hệ, Từ ngày, Đến ngày, Ghi chú |
| Sở hữu | Địa điểm (đường dẫn đầy đủ), Mã địa điểm, Người, Tỷ lệ (%), Từ ngày, Đến ngày, Số giấy tờ, Ghi chú |
| Đồ đạc | Mã, Tên, Nhóm, Ở đâu (đường dẫn đầy đủ), Số lượng, Đơn vị, Trạng thái, Ngày đặt, Ngày gỡ, Chủ, Mô tả, rồi các mục tuỳ biến của đồ |

- Nhãn của trạng thái, quan hệ, loại giấy tờ, giới tính là chữ tiếng Việt như trên giao diện, không phải mã `IN_USE`.
- Ngày ghi thành ô kiểu ngày (`dd/MM/yyyy`); số lượng, tỷ lệ ghi thành ô số.
- Cột mục tuỳ biến: mọi mục đang `ACTIVE` của đích đó, theo `sort_order`. Mục chỉ áp cho một loại thì để trống ở dòng loại khác. Tiêu đề là nhãn của mục. Hai mục trùng nhãn thì thêm tên loại vào sau. Giá trị kiểu lựa chọn ghi nhãn của lựa chọn.
- Dòng sắp theo: địa điểm theo đường dẫn cây; người theo tên; cư trú và sở hữu theo địa điểm rồi ngày bắt đầu; đồ theo nơi rồi tên.
- Dữ liệu đọc theo lô 500 dòng bằng keyset và ghi thẳng ra luồng trả về. Đường dẫn tên của địa điểm dựng từ một lần đọc toàn bộ id, tên và cha của địa điểm (chỉ ba cột, đủ nhẹ cho hàng chục nghìn chỗ).
- Không có giới hạn số dòng; nếu sau này cần thì thêm. **(tôi chọn)**

**Frontend**: request phải mang header người dùng như mọi API khác, nên không dùng được thẻ `<a href>` thường. Hàm `downloadFile(path, fileName)` gọi `fetch`, nhận blob, rồi bấm một liên kết tạm. Nút đặt ở ba chỗ:

- trang Tổng quan: "Tải Excel" (cả không gian);
- trang Đồ đạc: "Xuất Excel", gửi đúng lọc đang chọn;
- trang Người: "Xuất Excel".

Nút hiện vòng xoay khi đang tải. Nếu lỗi thì hiện thông báo lỗi như các thao tác khác.

### 3.3 In hồ sơ địa điểm (F6)

| Hướng | Được | Mất |
|---|---|---|
| **a. Trang in riêng trong frontend, dùng API sẵn có, in bằng trình duyệt** (đã chọn) | Không đụng backend; "Lưu thành PDF" của trình duyệt cho ra PDF | Hình thức phụ thuộc trình duyệt |
| b. Backend tạo PDF | File giống nhau ở mọi máy | Thêm thư viện PDF và font tiếng Việt; tốn công cho một trang |

- Đường dẫn `/w/:ws/places/:placeId/print`, không có thanh điều hướng và cây; mở ở thẻ mới từ menu thao tác của địa điểm ("In hồ sơ").
- Nội dung: tên, mã, loại, đường dẫn, trạng thái, mô tả, các mục tuỳ biến; bảng người đang ở (tên, quan hệ, từ ngày); bảng chủ sở hữu còn hiệu lực (tên, tỷ lệ, từ ngày, số giấy tờ); bảng đồ đạc chưa gỡ (mã, tên, nhóm, số lượng, trạng thái, chủ). Cuối trang ghi "In lúc dd/MM/yyyy HH:mm".
- Công tắc "Kể cả bên trong" (mặc định tắt): bật thì bảng đồ đạc lấy cả cây con (`subtreeOf`) và thêm cột "Ở đâu"; bật/tắt ghi trên URL (`?subtree=1`).
- Nút "In" gọi `window.print()`; CSS `@media print` ẩn nút và công tắc, bảng không cắt ngang dòng giữa hai trang.
- Đồ đạc tải hết các trang của danh sách (như bảng "Cả bên trong" đang làm) trước khi cho in; đang tải thì nút "In" bị khoá.

### 3.4 Lọc hoạt động (G6)

`GET /activity` nhận thêm:

| Tham số | Ý nghĩa |
|---|---|
| `action` | `CREATE`, `UPDATE`, `DELETE`, `RESTORE`; trống là mọi hành động |
| `changedBy` | Id người dùng; trống là mọi người |
| `from`, `to` | Mốc thời gian ISO-8601 (`Instant`); `from` tính cả mốc, `to` không tính mốc |

Giá trị lạ trả 400 như các API khác. Bộ lọc đi vào `EntityHistory.listRecent` qua một record mới `ChangeFilter(entityType, action, changedBy, from, to)` ở `platform-common`. Chữ ký cũ được thay thế, không giữ song song. **(tôi chọn)** Câu SQL vẫn đi theo index `(workspace_id, changed_at)` sẵn có. `from`/`to` thu hẹp khoảng quét. `action` và `changedBy` lọc trên các dòng quét được: với dữ liệu một không gian thì đủ nhanh, không thêm index.

Frontend (trang Hoạt động):

- Giữ các tab loại bản ghi.
- Thêm ô chọn "Hành động" (Tất cả, Thêm, Sửa, Xoá, Khôi phục), công tắc "Chỉ việc tôi làm", hai ô ngày "Từ ngày", "Đến ngày".
- "Đến ngày" tính hết ngày đó theo giờ máy người dùng: gửi `to` = 00:00 ngày hôm sau.
- Mọi bộ lọc nằm trên URL (`?type=&action=&mine=1&from=yyyy-MM-dd&to=yyyy-MM-dd`), giá trị lạ bị bỏ qua (như trang Đồ đạc).
- Nút "Bỏ lọc" hiện khi có ít nhất một lọc.
- Không có dòng nào thì hiện "Không có hoạt động khớp bộ lọc".

### 3.5 So sánh hai version (G2)

Chỉ làm ở frontend: API `GET …/versions/{no}` đã trả đủ snapshot.

- Trong tab Lịch sử (địa điểm, người, đồ đạc), mỗi dòng version có ô chọn. Chọn đúng hai dòng thì nút "So sánh" bật; chọn dòng thứ ba thì bỏ chọn dòng cũ nhất. **(tôi chọn)**
- Hộp so sánh: bảng "Trường | vA (cũ hơn) | vB (mới hơn)". Version nhỏ luôn nằm bên trái, bất kể chọn theo thứ tự nào. Dòng có giá trị khác được tô nền và in đậm. Mặc định "Chỉ hiện trường khác" bật; tắt thì hiện mọi trường.
- Nhãn và cách hiện giá trị dùng lại `SnapshotFields` (cùng nhãn trường, cùng định dạng ngày, tên loại, tên nhóm như hộp xem một version). Mục tuỳ biến so từng mục một.
- Mỗi cột ghi ngày giờ và người sửa ("Bạn" hoặc "Người dùng khác"), như dòng lịch sử.
- Hai version giống nhau hoàn toàn (ví dụ sửa rồi sửa lại) thì hiện "Hai version này giống nhau".

## 4. Thay đổi theo lớp

| Lớp | Thay đổi |
|---|---|
| `platform-common` | `ChangeFilter`; `EntityHistory.listRecent(workspaceId, ChangeFilter, CursorRequest)` |
| registry, package mới `overview` | `OverviewService`, `OverviewController` (`GET /overview`), `OverviewResponse` |
| registry, package mới `export` | `ExportController`, một lớp ghi cho mỗi sheet, `PlacePaths` (dựng đường dẫn tên), nhãn tiếng Việt của các giá trị liệt kê; phụ thuộc `org.dhatim:fastexcel` |
| registry `history` | `ActivityController` nhận `action`, `changedBy`, `from`, `to` |
| Database | Không đổi; không có migration mới |
| frontend | feature mới `overview` (trang tổng quan thay `WorkspaceHomePage` khi đã có địa điểm), `api/download.ts` (`downloadFile`), nút xuất ở trang Đồ đạc và Người, `PlacePrintPage` + mục "In hồ sơ", bộ lọc trang Hoạt động, so sánh trong `HistoryTab` |
| Tài liệu | README (API mới), spec cấu trúc frontend, đánh dấu spec này "đã xây" |

## 5. Kiểm thử

- **Backend IT** (trên UAT, không gian âm như các đợt trước):
  - Số của `/overview` đúng với dữ liệu dựng sẵn: chỗ có người và chỗ trống theo loại; người ở có cư trú hết hạn hôm qua, bắt đầu ngày mai, hay đã bị xoá thì không tính; đồ theo trạng thái; số chưa có mã khớp `/codes/status`.
  - File xuất đọc lại được (đọc bằng `fastexcel-reader` chỉ trong test): đúng tên sheet, tiêu đề, kiểu ô ngày và số, đường dẫn tên, nhãn tiếng Việt, cột mục tuỳ biến, lọc của sheet đồ đạc, không có bản ghi đã xoá.
  - Xuất nhiều hơn một lô (hơn 500 dòng) không mất, không lặp dòng.
  - `/activity` với từng lọc và kết hợp lọc; cursor đi đúng qua nhiều trang khi có lọc; giá trị lạ trả 400.
- **Frontend (Vitest + MSW)**:
  - Trang tổng quan hiện số, liên kết đúng `?status=`, "Cần xử lý" ẩn khi không có việc; không gian chưa có địa điểm vẫn hiện trang trống cũ.
  - `downloadFile` gửi đúng đường dẫn và tham số lọc; báo lỗi khi backend trả lỗi.
  - Trang in: đủ các bảng, công tắc "Kể cả bên trong" đổi nguồn đồ đạc, nút "In" gọi `window.print`.
  - Trang Hoạt động: bộ lọc đọc và ghi URL, gửi đúng tham số, "Đến ngày" gửi mốc ngày hôm sau, "Bỏ lọc".
  - So sánh: chọn hai dòng, thứ tự cột luôn cũ trái mới phải, chỉ hiện trường khác, trường hợp giống nhau.
- **Chạy thử** trên không gian thử, có chụp màn hình trang tổng quan, trang in và hộp so sánh. Mở thử file Excel bằng cách đọc lại trong test; không có Excel trong môi trường dev.

## 6. Để lại cho người xây

- Tên lớp, tên hàm, cách chia file trong các package mới.
- Kiểu chữ, khoảng cách, màu tô dòng khác nhau, miễn theo các thành phần shadcn đang dùng.
- Cách lấy dữ liệu cho sheet Người ("Đang ở"): một truy vấn gộp hay tra theo lô, miễn không gọi một truy vấn cho mỗi người.
