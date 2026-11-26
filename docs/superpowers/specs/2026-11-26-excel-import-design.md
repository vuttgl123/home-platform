# Nhập từ Excel (Đợt 9)

Ngày: 2026-11-26 · Trạng thái: **đã duyệt** (bạn: "Tôi đồng ý" với đề xuất 7 luật và cách A; "Tôi đồng ý" với bản thiết kế)

Ký hiệu: **(bạn)** là điều bạn đã nói; **(tôi chọn)** là quyết định của tôi, bạn có thể bác.

Liên quan: lộ trình `2026-11-20-roadmap-design.md` (mục Đợt 9), xuất Excel `2026-11-17-overview-and-export-design.md` (các sheet và cột), mã tự cấp `2026-11-16-auto-codes-design.md` (mục 2: "gõ tay mã có sẵn từ sổ sách thuộc về nhập hàng loạt"), luật nghiệp vụ `2026-11-13-business-rules-design.md`, sở hữu `2026-11-16-ownership-design.md` (quan hệ cư trú "Chủ" đã bỏ).

## 1. Mục đích

Một nơi đã có danh sách phòng, người ở, đồ đạc trên Excel thì việc gõ lại từng dòng là rào cản lớn nhất để bắt đầu dùng **(bạn, lộ trình)**. Đợt này cho đưa cả danh sách vào một lần, nhìn thấy trước dòng nào sai và vì sao.

| Câu hỏi | Hiện nay | Sau đợt này |
|---|---|---|
| Có sẵn 200 phòng và 300 người trên Excel | Gõ tay từng hồ sơ | Điền vào file mẫu, tải lên, nhập một lần |
| File có chỗ sai (thiếu loại, ngày sai, trùng mã) | Không biết trước | Xem trước từng dòng, lỗi kèm lý do và tên cột; sửa file rồi tải lại |
| Muốn giữ mã phòng, mã tài sản đã in nhãn | Mã chỉ do hệ thống cấp | Mã ghi trong file được giữ; ô mã trống thì hệ thống cấp như thường |
| Chuyển dữ liệu sang một không gian mới | Không làm được | File xuất cả không gian nhập được vào không gian trống |

## 2. Mục tiêu, phần không làm, điều phải tránh

**Mục tiêu**

- Tải file mẫu đã có sẵn cột mục tuỳ biến của không gian và một sheet hướng dẫn.
- Tải file lên là thấy kết quả từng dòng của cả năm sheet, trước khi có gì được ghi.
- Dòng sai được báo bằng tiếng Việt, chỉ đúng cột, nói cách sửa khi được.
- Nhập chạy đúng những luật mà form đang chạy; hồ sơ nhập vào giống hệt hồ sơ tạo bằng form (có mã, có version, có trong Hoạt động).
- File xuất cả không gian (Đợt 4) là một file nhập hợp lệ.

**Không làm ở đợt này** (tôi chọn)

- Sửa hồ sơ đã có qua Excel. Chỉ thêm mới.
- Hoàn tác cả một lần nhập. Lưới an toàn là bước xem trước và luật "tất cả hoặc không".
- Sửa ô ngay trên trình duyệt; bản nháp lưu trên server.
- Nhập địa chỉ, ảnh, nhật ký bảo trì (file xuất không có các cột này).
- File `.xls`, `.csv`, Google Sheets.
- Tự tạo loại địa điểm, nhóm đồ, mục tuỳ biến còn thiếu.

**Điều phải tránh**

- Nhập một phần rồi người dùng tải lại file đã sửa, sinh ra người trùng. Người không có mã nên không chặn trùng được: vì vậy luật N7.
- Xem trước báo ổn mà nhập thật lại hỏng vì một luật chỉ có ở lệnh tạo: vì vậy xem trước chạy chính lệnh tạo (mục 4.1).
- Xem trước để lại dữ liệu, số đếm mã, hay version.
- Lỗi chỉ ghi bằng tiếng Anh hoặc chỉ ghi "dòng không hợp lệ".

## 3. Luật nghiệp vụ (tôi chọn)

**N1. File.** Một file `.xlsx`, tối đa **5 MB** và **5.000 dòng dữ liệu** cộng mọi sheet (hằng số `MAX_ROWS`). Quá giới hạn, không mở được, không phải `.xlsx`, hay không có dòng nào để nhập thì là **lỗi cả file**: không đọc tiếp, không nhập gì.

- Sheet được nhận theo tên: **Địa điểm, Người, Cư trú, Sở hữu, Đồ đạc** (bỏ khoảng trắng hai đầu, không phân biệt hoa thường). Thiếu sheet nào thì bỏ qua sheet đó. Sheet khác tên bị bỏ qua kèm **ghi chú** (sheet "Hướng dẫn" bỏ qua không cần ghi chú).
- Dòng 1 là tiêu đề. Cột được nhận theo chữ tiêu đề (cùng cách so như tên sheet), thứ tự cột không quan trọng. Cột lạ bị bỏ qua kèm ghi chú "Cột 'X' ở sheet Y không có trong mẫu, bị bỏ qua". Cột "Đang ở" của sheet Người bỏ qua không cần ghi chú (cột chỉ để xem, cư trú khai ở sheet Cư trú). Sheet có dòng mà thiếu cột bắt buộc (N2) là lỗi cả file.
- Dòng mà mọi ô đều trống thì bỏ qua. Số dòng báo lại là số dòng trong Excel (dòng dữ liệu đầu tiên là dòng 2).
- Ô chữ: bỏ khoảng trắng hai đầu; rỗng là trống. Ô số nguyên nhận số (`12`, `12.0`) hoặc chữ chỉ có chữ số; số lẻ ở cột nguyên là lỗi. Ô ngày nhận ô ngày của Excel, hoặc chữ dạng `dd/mm/yyyy`, `d/m/yyyy`, `yyyy-mm-dd`. Ô công thức dùng giá trị Excel đã lưu; không có giá trị thì là lỗi "Ô có công thức chưa tính".

**N2. Cột của từng sheet.** Cùng tiêu đề với file xuất cả không gian. Giá trị liệt kê nhận nhãn tiếng Việt như trong file xuất (không phân biệt hoa thường) hoặc mã tiếng Anh (`IN_USE`).

| Sheet | Cột | Cách đọc |
|---|---|---|
| Địa điểm | Mã | Tuỳ chọn, N3 |
| | **Tên** | Bắt buộc |
| | **Loại** | Bắt buộc; tên hoặc mã của loại địa điểm đang dùng (N4) |
| | Sức chứa | Số nguyên 0 tới 9999 |
| | Nằm trong | Địa điểm cha (N4); trống là cấp cao nhất |
| | Trạng thái | Đang dùng, Tạm ngưng; trống là Đang dùng |
| | Mô tả, mục tuỳ biến của địa điểm | N6 |
| Người | **Họ tên** | Bắt buộc |
| | Điện thoại, Email, Loại giấy tờ, Số giấy tờ, Ngày sinh, Ghi chú | Như form |
| | Giới tính | Nam, Nữ, Khác; trống là không ghi |
| | Đang ở | Bỏ qua (N1) |
| | Mục tuỳ biến của người | N6 |
| Cư trú | Địa điểm, Mã địa điểm | Cần ít nhất một (N4) |
| | **Người** | Bắt buộc (N4) |
| | Quan hệ | Người ở, Khách, Nhân viên, Khác; trống là Người ở. "Chủ" là lỗi: "Quan hệ Chủ đã bỏ; ghi người này ở sheet Sở hữu" |
| | **Từ ngày** | Bắt buộc |
| | Đến ngày, Ghi chú | Như form |
| Sở hữu | Địa điểm, Mã địa điểm | Cần ít nhất một (N4) |
| | **Người** | Bắt buộc (N4) |
| | Tỷ lệ (%) | Lớn hơn 0, không quá 100, tối đa hai chữ số thập phân; trống là không ghi |
| | **Từ ngày** | Bắt buộc |
| | Đến ngày, Số giấy tờ, Ghi chú | Như form |
| Đồ đạc | Mã | Tuỳ chọn, N3 |
| | **Tên** | Bắt buộc |
| | Nhóm | Tên của nhóm đồ đang dùng; trống là không nhóm |
| | **Ở đâu** | Bắt buộc; địa điểm (N4) |
| | Số lượng | Số nguyên từ 1; trống là 1 |
| | Đơn vị, Ngày đặt, Hết bảo hành, Mô tả | Như form |
| | Trạng thái | Đang dùng, Hỏng, Cất kho, Đã gỡ; trống là Đang dùng |
| | Ngày gỡ | Bắt buộc khi Đã gỡ; phải trống với trạng thái khác (N8) |
| | Bảo trì (tháng) | Số nguyên 1 tới 120 |
| | Chủ | Người (N4); trống là đồ của địa điểm |
| | Mục tuỳ biến của đồ đạc | N6 |

**N3. Mã.** Ô mã có chữ thì hồ sơ **giữ đúng mã đó**: tối đa 50 ký tự, không trùng với mã nào của bảng đó trong không gian (kể cả hồ sơ đã xoá, vì khoá duy nhất bao cả dòng đã xoá), và không lặp giữa các dòng của file. Mã địa điểm và mã đồ đạc là hai không gian mã riêng, như database. So mã không phân biệt hoa thường, như collation của database. Ô mã trống thì cấp theo mẫu mã của loại (nhóm), như khi tạo bằng form. Bộ đếm tự nhảy qua mã đã có, nên mã giữ từ file không làm hỏng lần cấp sau.

**N4. Nhận ra hồ sơ được nhắc tới.** Một dòng có thể nhắc tới hồ sơ đã có (chưa xoá) hoặc một dòng của chính file. Mọi phép so chữ ở N1 tới N6 (tên sheet, tiêu đề, nhãn, tên, mã) làm sau khi **chuẩn hoá Unicode NFC** cả hai bên: chữ Việt gõ bằng bảng mã tổ hợp (dấu tách rời) phải khớp với chữ dựng sẵn.

- **Địa điểm** (Nằm trong, Ở đâu, Địa điểm, Mã địa điểm): giá trị được thử là **mã** trước; không có mã đó thì là **đường dẫn tên** từ cấp cao nhất, các phần ngăn bằng `/` (khoảng trắng quanh `/` tuỳ ý), từng phần so không phân biệt hoa thường. Địa điểm có dấu `/` trong tên thì chỉ nhắc được bằng mã. Ở Cư trú và Sở hữu: Mã địa điểm có chữ thì dùng nó; cả hai cột có chữ mà chỉ tới hai địa điểm khác nhau là lỗi.
- **Người** (Người, Chủ): khớp khi giá trị bằng **họ tên** (không phân biệt hoa thường, gộp khoảng trắng liền nhau), hoặc bằng **số điện thoại** (chuẩn hoá như form), hoặc bằng **số giấy tờ**.
- **Loại địa điểm**: tên hoặc mã; **nhóm đồ**: tên. Phải có trong Danh mục và đang dùng. Không có thì "Chưa có loại 'X' trong Danh mục"; ngừng dùng thì "Loại 'X' đang ngừng dùng".
- Không khớp hồ sơ nào: "Không tìm thấy địa điểm 'X'" / "Không tìm thấy người 'X'". Khớp nhiều hơn một: "Có 2 địa điểm 'X'; ghi mã thay cho đường dẫn" / "Có 2 người khớp 'X'; ghi số điện thoại hoặc số giấy tờ thay cho tên".
- Nhắc tới một dòng của file mà dòng đó lỗi: dòng này cũng lỗi, "Địa điểm ở dòng 5 (sheet Địa điểm) bị lỗi".

**N5. Thứ tự.** Nhập theo thứ tự sheet: Địa điểm, Người, Cư trú, Sở hữu, Đồ đạc **(bạn, lộ trình)**. Trong sheet Địa điểm, cha luôn được tạo trước con, dù dòng con đứng trước trong file. Các dòng chỉ tới nhau thành vòng thì mọi dòng trong vòng lỗi. Các sheet khác đi theo thứ tự dòng, nên hai dòng cư trú chồng ngày của cùng người ở cùng nơi được bắt như hai lần tạo liên tiếp.

**N6. Mục tuỳ biến.** Mỗi mục đang dùng của đích là một cột, tiêu đề như file xuất (nhãn; nhãn trùng thì thêm "(tên loại)"). Giá trị đọc theo kiểu: chữ; số; ngày (N1); Có/Không cho kiểu có-không; lựa chọn phải đúng một lựa chọn của mục (không phân biệt hoa thường, lưu đúng chữ của lựa chọn). Ô có chữ ở cột của một mục không áp cho loại (nhóm) của dòng là lỗi "Mục 'X' không áp cho loại Y". Mục bắt buộc trống là lỗi, như form.

**N7. Tất cả hoặc không.** Còn **một lỗi** (lỗi dòng hay lỗi cả file) thì **không nhập gì**; nút Nhập bị khoá. Sửa file rồi chọn lại file. **Cảnh báo** và **ghi chú** không chặn.

Cảnh báo "có vẻ đã có" (không chặn, vì trùng tên vẫn có thể là hai hồ sơ thật), so với hồ sơ đã có và với các dòng khác của file:
- người có họ tên, hoặc số điện thoại, trùng với một người khác;
- địa điểm có cùng đường dẫn với một địa điểm khác;
- đồ đạc cùng tên, ở cùng địa điểm với một món khác (chưa gỡ).

**N8. Đồ đã gỡ.** Dòng Đã gỡ được tạo như món Đang dùng, rồi gỡ với ngày gỡ trong file: hai version (tạo, gỡ), như khi làm bằng tay. Luật của lệnh gỡ (ngày gỡ không trước ngày đặt) áp như thường.

**N9. Lịch sử.** Mỗi hồ sơ nhập vào có version tạo với ghi chú **"Nhập từ Excel: <tên file>"** (cắt còn 255 ký tự); món đã gỡ có thêm version gỡ cùng ghi chú. Trang Hoạt động hiện chúng như mọi lần tạo.

## 4. Cách làm

### 4.1 Hướng đã cân nhắc

| Hướng | Được | Mất |
|---|---|---|
| **A. Chạy thử thật rồi huỷ** (bạn chọn) | Xem trước và nhập là cùng một đoạn code, gọi đúng lệnh tạo của form, nên mọi luật (trùng giấy tờ, cư trú chồng ngày, tổng sở hữu quá 100%, độ sâu cây, mục tuỳ biến) được kiểm như nhau; không viết luật lần hai | Xem trước giữ một transaction ghi trong lúc chạy (N1 giới hạn độ dài); lỗi của lệnh tạo bằng tiếng Anh phải dịch |
| B. Kiểm riêng trong bộ nhớ, rồi ghi | Xem trước chỉ đọc | Viết lại mọi luật; lâu dần lệch nhau, xem trước báo ổn mà nhập hỏng |
| C. Bản nháp trên server, sửa từng ô trên trình duyệt | Không phải tải lại file | Lớn gấp mấy lần (bảng nháp, dọn nháp, giao diện sửa từng sheet); lộ trình đã chọn "sửa rồi nhập lại" |
| (bỏ) Đọc file trên trình duyệt | Xem trước nhanh | Thêm thư viện đọc Excel cho frontend; luật vẫn phải chạy ở server |

### 4.2 Backend (registry-service)

**Không có migration.** Đọc file bằng `fastexcel-reader` (đã có trong `pom.xml`, đổi từ phạm vi test sang thường; cùng phiên bản với `fastexcel`). Cấu hình multipart của registry: `max-file-size: 5MB`, `max-request-size: 6MB`.

**API, gói `imports` mới:**
- `GET /imports/template.xlsx`: sheet "Hướng dẫn" đứng đầu, rồi năm sheet chỉ có dòng tiêu đề. Tiêu đề lấy **từ cùng chỗ với file xuất** (không chép tay) trừ cột "Đang ở", kèm cột mục tuỳ biến của không gian theo quy tắc của `AttributeColumns`. Sheet Hướng dẫn gồm: thứ tự nhập, cột bắt buộc của từng sheet, cách ghi địa điểm, người, ngày, mã; các giá trị hợp lệ (loại địa điểm và nhóm đồ đang dùng, trạng thái, quan hệ, giới tính); mỗi mục tuỳ biến với kiểu, lựa chọn, bắt buộc hay không, áp cho loại nào. Nếu thư viện ghi hỗ trợ danh sách chọn trong ô thì thêm cho các cột liệt kê.
- `POST /imports/preview` (multipart `file`): chạy N1 tới N9 trong **một transaction luôn bị huỷ** ở cuối; trả báo cáo, `200`.
- `POST /imports` (multipart `file`): chạy đúng như trên; không có lỗi thì **lưu** và trả báo cáo `committed: true`; có lỗi thì huỷ và trả báo cáo `committed: false`. Cả hai trường hợp trả `200` (tôi chọn): lỗi trong file là kết quả bình thường, frontend đọc một dạng duy nhất. Lỗi tầng HTTP (file quá 5 MB là `413`, không phải multipart là `400`) giữ nguyên như các API khác.

**Báo cáo** (chữ tiếng Việt do backend viết, như nhãn của file xuất):

```
{ fileName, committed,
  fileErrors: [string], notices: [string],
  totals: { rows, errors, warnings },
  sheets: [ { sheet: PLACE|PERSON|OCCUPANCY|OWNERSHIP|ITEM, name: "Địa điểm",
              rows, errors, warnings,
              items: [ { row: 7, label: "P301 · Phòng 301", code: "P301", status: OK|WARNING|ERROR,
                         messages: [ { level: ERROR|WARNING, column: "Người" | null, text } ] } ] } ] }
```

`label` của dòng: địa điểm và đồ đạc là "mã · tên" (mã tự cấp thì là mã sẽ cấp khi xem trước, mã đã cấp khi nhập); người là họ tên; cư trú và sở hữu là "người · địa điểm". `items` có mọi dòng đã đọc, kể cả dòng ổn. Sheet không có trong file thì không có trong `sheets`.

**Chạy từng dòng.** Mỗi dòng chạy trong một **savepoint** riêng (`TransactionTemplate` với `PROPAGATION_NESTED`): dòng hỏng chỉ quay về savepoint của nó, các dòng sau vẫn được kiểm, nên một lần xem trước liệt kê được mọi lỗi. Trước khi gọi lệnh tạo, request được kiểm bằng Bean Validation (lệnh tạo gọi thẳng từ service không qua `@Valid` của controller); lỗi ràng buộc dịch theo loại ràng buộc ("Bắt buộc", "Dài quá N ký tự", "Từ a tới b", "Email không hợp lệ", "Không được sau hôm nay") và gắn vào cột theo tên trường.

**Mã cho trước.** `PlaceService` và `ItemService` có thêm lệnh tạo nhận mã cho trước, chỉ dùng cho nhập (javadoc nói rõ); mọi luật khác của lệnh tạo giữ nguyên. Mã trống thì đi lối cấp mã như cũ.

**Dịch lỗi.** Một lớp `ImportMessages` giữ bảng dịch các lỗi đã biết của lệnh tạo (giấy tờ trùng, cư trú chồng ngày, sở hữu chồng ngày, tổng tỷ lệ quá 100%, ngày kết thúc trước ngày bắt đầu, cây quá sâu, lỗi từng khoá của mục tuỳ biến, chủ đồ đã xoá, ngày gỡ trước ngày đặt, mã trùng). Lỗi chưa có trong bảng hiện "Không nhập được dòng này (<chi tiết gốc>)", để không lỗi nào bị nuốt.

**Giới hạn thời gian.** Mục tiêu: file 5.000 dòng xem trước xong dưới 60 giây trên máy dev. Đo trong IT; quá mục tiêu thì hạ `MAX_ROWS` và ghi lại con số. Trong lúc chạy, transaction giữ khoá của bộ đếm mã, nên người khác tạo địa điểm, đồ đạc trong cùng không gian phải chờ tới khi xong (chấp nhận: một gia đình, một toà nhà; độ dài bị N1 chặn).

### 4.3 Frontend

- **Trang "Nhập từ Excel"** `/w/:ws/import`. Vào từ thanh bên (nhóm dưới, cạnh Danh mục; biểu tượng tải lên) và từ nút **Nhập Excel** cạnh "Tải Excel" ở trang Tổng quan.
- **Ba bước** hiện trên đầu trang (① Chọn file, ② Xem trước, ③ Nhập), bước hiện tại tô đậm.
  - **① Chọn file:** nút **Tải mẫu** (lưu `<tên không gian>-mau-nhap-<ngày>.xlsx`, cùng cách đặt tên với file xuất), ô chọn file chỉ nhận `.xlsx`, kéo thả được. Chọn xong là gửi xem trước ngay, có "Đang đọc file…".
  - **② Xem trước:** dải số "Địa điểm 12 · Người 30 · Cư trú 28 (2 lỗi) · …". Lỗi cả file trong một `Alert` đỏ; ghi chú trong một `Alert` nhạt. `Tabs` theo sheet, mỗi tab kèm số lỗi (đỏ) hoặc cảnh báo (vàng). Bảng trong `ScrollArea`: Dòng, Hồ sơ (`label`), Kết quả (dấu ổn, hoặc từng lời nhắn kèm tên cột). Công tắc "Chỉ hiện dòng có lỗi hoặc cảnh báo" (bật sẵn khi có lỗi). Bảng hiện 200 dòng một lần, có "Hiện thêm". Nút **Chọn file khác** và **Nhập N dòng**; còn lỗi thì nút khoá, kèm "Sửa N lỗi trong file rồi chọn lại file".
  - **③ Nhập:** gửi lại **cùng file** đã chọn. Trong lúc chạy: `Progress` chạy liên tục và "Đang nhập, đừng đóng trang". Xong: "Đã nhập 115 hồ sơ" với số theo sheet và link tới Tổng quan, Người, Đồ đạc, Hoạt động; nút "Nhập file khác". Mọi dữ liệu đã tải của không gian được làm mới.
- **Trường hợp riêng:** lần nhập trả `committed: false` (dữ liệu đổi từ lúc xem trước) thì quay về bước ② với báo cáo mới và dòng "Dữ liệu đã thay đổi từ lúc xem trước". File trên máy bị sửa hay xoá sau khi chọn (trình duyệt không đọc lại được) thì "Không đọc lại được file; chọn lại file". `413` thì "File lớn hơn 5 MB". `404` (registry chưa có `/imports`) thì "Máy chủ chưa hỗ trợ nhập Excel; cần cập nhật registry".
- Trên điện thoại: bảng cuộn ngang trong `ScrollArea`, dải số xuống dòng, các nút xếp dọc.

## 5. Kiểm tra

- **Backend (IT trên MySQL thật, trong transaction rollback như hiện nay):**
  - N1: quá dòng, không phải `.xlsx`, file rỗng, sheet lạ và cột lạ thành ghi chú, thiếu cột bắt buộc, dòng trống bị bỏ, số dòng báo đúng, ô ngày Excel và ba dạng chữ, số lẻ ở cột nguyên, ô công thức;
  - N2: mỗi sheet đọc đủ cột, giá trị mặc định khi ô trống, nhãn và mã tiếng Anh, quan hệ "Chủ";
  - N3: giữ mã từ file, mã trùng với hồ sơ đã có và đã xoá, mã lặp trong file, mã trống được cấp, lần cấp sau nhảy qua mã đã giữ;
  - N4: mã thắng đường dẫn, đường dẫn nhiều cấp, đường dẫn mơ hồ, người theo tên, số điện thoại, số giấy tờ, người mơ hồ, nhắc tới dòng lỗi, Mã địa điểm và Địa điểm lệch nhau;
  - N5: con đứng trước cha trong file, vòng; N6: từng kiểu mục tuỳ biến, lựa chọn sai, mục không áp cho loại, mục bắt buộc;
  - N7: một lỗi thì nhập không ghi gì; cảnh báo không chặn; dòng hỏng không làm hỏng dòng sau (savepoint); xem trước không để lại dòng, version, số đếm mã;
  - N8, N9: đồ đã gỡ có hai version; ghi chú version; Hoạt động có các lần tạo;
  - **khứ hồi:** xuất một không gian mẫu (không có hai người trùng tên), nhập file đó vào một không gian trống có cùng Danh mục (loại địa điểm, nhóm đồ, mục tuỳ biến), xuất lại: hai file giống nhau từng ô;
  - chữ tổ hợp (NFD) trong file khớp với tên, nhãn dựng sẵn (NFC);
  - mẫu: tiêu đề bằng tiêu đề file xuất trừ "Đang ở", có cột mục tuỳ biến, có sheet Hướng dẫn;
  - thời gian của file 5.000 dòng (mục 4.2).
- **Frontend (Vitest):** tải mẫu; chọn file gửi xem trước; dải số, tab, bảng, lọc dòng lỗi, "Hiện thêm"; nút Nhập bị khoá khi có lỗi; nhập xong hiện kết quả và làm mới dữ liệu; `committed: false` quay về xem trước; `413`, `404`, file không đọc lại được; mục trên thanh bên và nút ở Tổng quan.
- **Xem bằng mắt** ở khổ 1440, 1920 và 390, nền sáng và nền tối, với một file có cả dòng ổn, cảnh báo và lỗi.

## 6. Các quyết định nhỏ tôi chốt khi đọc lại (tôi chọn)

- Trả `200` cho cả nhập thành công lẫn nhập bị huỷ vì lỗi, phân biệt bằng `committed` (4.2).
- Quan hệ cư trú trống là Người ở; trạng thái trống là Đang dùng (N2).
- Cảnh báo "có vẻ đã có" cho người, địa điểm, đồ đạc, để bắt lỗi nhập cùng một file hai lần (N7).
- Không chặn các yêu cầu chạy cùng lúc trong một không gian; khoá của database và lần kiểm lại khi nhập đủ an toàn (4.2).
- Bảng xem trước hiện 200 dòng một lần thay vì cuộn ảo (4.3).

## 7. Để lại cho người làm

Tên class, cách chia task, query key, vị trí file theo cấu trúc sẵn có: gói `imports` ở registry; `features/imports` ở frontend. Cách chia việc đọc file, đọc từng sheet, nhận ra tham chiếu, chạy từng dòng thành các class. Cách đưa tiêu đề của file xuất ra dùng chung với file mẫu và với lớp đọc (mục 4.2) mà không làm `export` lộ thêm hơn cần. Câu chữ chính xác của từng lời nhắn, nội dung sheet Hướng dẫn, biểu tượng trên thanh bên có thể chỉnh khi xem bằng mắt. Cách sinh lại kiểu API cho frontend làm như các đợt trước.
