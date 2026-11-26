# Dọn và làm chắc (Đợt 10)

Ngày: 2026-11-26 · Trạng thái: **đã duyệt** (bạn: "Được làm cho tôi nhé cứ theo thứ tự bạn đề xuất" với nhóm 1 "Dọn và làm chắc" là đợt đầu)

Ký hiệu: **(bạn)** là điều bạn đã nói; **(tôi chọn)** là quyết định của tôi, bạn có thể bác.

Liên quan: lộ trình `2026-11-20-roadmap-design.md` (Đợt 10), các thiết kế Đợt 4 (`2026-11-17-overview-and-export-design.md`), Đợt 6 (`2026-11-25-capacity-and-vacancies-design.md`), Đợt 8 (`2026-11-25-maintenance-and-reminders-design.md`), Đợt 9 (`2026-11-26-excel-import-design.md`).

## 1. Mục đích

Bạn gác đăng nhập, phân quyền và muốn "tối ưu lại toàn bộ những phần đã làm để cho chi tiết hơn, cụ thể hơn" **(bạn)**. Đợt này gom 21 điểm nhỏ còn mở sau các lần duyệt Đợt 4 tới 9: chỗ dữ liệu đọc sai hay lệch, chỗ người dùng gặp chữ tiếng Anh hay thứ tự lộn xộn, chỗ test chạy chưa chắc. Không thêm tính năng mới.

**Phải tránh:** đổi hành vi đang đúng; đổi file Excel xuất ra theo cách làm hỏng việc nhập lại (phép thử khứ hồi của Đợt 9 phải vẫn qua).

## 2. Việc làm (tôi chọn)

### Excel, xuất và nhập

- **E1. Thứ tự địa điểm trong file xuất.** Sheet Địa điểm theo cây, nhưng các địa điểm cùng cha xếp theo **tên** (so theo tiếng Việt, số trong tên so như số: "P2" trước "P10"), thay vì theo thứ tự tạo. Các sheet khác giữ nguyên.
- **E2. Tiêu đề cột mục tuỳ biến không trùng.** Hai mục cùng nhãn mà vẫn trùng sau khi thêm "(tên loại)" (ví dụ hai mục chung cùng nhãn) thì thêm khoá: "Nhãn (khoá)". File xuất và file mẫu dùng cùng quy tắc, nên việc nhập vẫn nhận ra cột.
- **E3. File không có phần định dạng** (`xl/styles.xml`, do vài công cụ tạo ra) đọc lại không cần định dạng thay vì báo "Không đọc được file". File như vậy không thể có ô ngày, nên không mất gì.
- **E4. Giới hạn trước khi đọc:**
  - tổng dung lượng giải nén tối đa 100 MB, quá thì lỗi cả file "File quá lớn khi giải nén; hãy lưu lại file chỉ gồm dữ liệu cần nhập";
  - số dòng quét (kể cả dòng trống) tối đa 50.000, quá thì "File có quá nhiều dòng (kể cả dòng trống); hãy xoá các dòng thừa".
  Chặn file "bom zip" hay file có hàng triệu dòng trống làm registry chậm hay hết bộ nhớ.
- **E5. File dùng hệ ngày 1904** (Excel trên Mac cũ) đọc ngày đúng: mốc ngày đọc từ `workbookPr date1904` của file.
- **E6. Tên file kèm đường dẫn** (trình duyệt cũ gửi "C:\…\nha.xlsx") chỉ giữ phần tên cuối cho báo cáo và ghi chú version.
- **E7. Mục tuỳ biến chung vẫn được kiểm khi loại sai.** Dòng có "Loại" hay "Nhóm" sai vẫn được kiểm các mục áp cho mọi loại (giá trị, bắt buộc), để một lần xem trước báo đủ lỗi.

### Lỗi và log

- **P1. Lỗi "đang bận".** Không lấy được khoá dòng (ví dụ trong lúc người khác đang nhập Excel) vẫn trả 409, nhưng có mã `BUSY`. Giao diện hiện "Đang có thao tác khác trên dữ liệu này (ví dụ đang nhập Excel); thử lại sau ít phút" thay cho chữ tiếng Anh, ở mọi chỗ đang hiện lỗi của lệnh ghi.
- **P2. Client ngắt kết nối** (đóng tab giữa lúc tải file Excel) không ghi log mức ERROR nữa: ghi một dòng mức DEBUG, không trả gì.

### Giao diện

- **F1. Tải file:** link tạm của file vừa tải được thu hồi sau vài giây, không ngay lúc bấm (một số trình duyệt huỷ tải khi thu hồi quá sớm).
- **F2. Nút "Cấp mã"** ở Tổng quan mở trang Danh mục và cuộn tới đúng phần "Mã tự cấp".
- **F3. So sánh version:** tiêu đề mỗi cột ghi thêm hành động của version (Tạo, Sửa, Xoá, Khôi phục…).
- **F4. Lịch sử địa điểm:** địa chỉ chỉ tính là "đổi" khi chữ địa chỉ đầy đủ đổi (đổi mã phường bên trong mà chữ giữ nguyên thì không tô).
- **F5. Trang in** ghi tỷ lệ sở hữu cùng cách với tab Sở hữu (`formatShare`).
- **F6. Chỗ trống:**
  - xoá hay khôi phục địa điểm thì danh sách chỗ trống tải lại;
  - bộ lọc "Toà" liệt kê mọi địa điểm cấp cao nhất, không chỉ 20 cái đầu.
- **F7. Màn hình "Đã nhập"** của trang nhập: các nút link có viền thấy rõ ở nền sáng.

### Test

- **T1.** Test frontend chạy với múi giờ cố định (`Asia/Ho_Chi_Minh`), để test bộ lọc ngày không đổi kết quả theo máy.
- **T2.** Dọn các log "request chưa có backend giả" còn lại trong test frontend (trang Người và 3 chỗ cũ).
- **T3.** Một test của `TodoSeenIT` tách riêng phần mốc sửa của mục tuỳ biến (cache danh mục được xoá đúng lúc).

### Không làm, và vì sao (tôi chọn)

- Việc cần làm bị tính hai lần trên trang Việc cần làm (danh sách và tóm tắt): chỉ trên đúng trang đó, dữ liệu nhỏ; thêm cache sẽ làm số trên thanh bên cũ đi sau mỗi lệnh ghi.
- Thanh bên chớp rồi ẩn với registry cũ: registry nay đã có `/todos`.
- Cột Excel của sheet Đồ đạc dời chỗ so với file cũ: việc nhập nhận cột theo tiêu đề, nên file cũ vẫn nhập được.

## 3. Cách làm

Một hướng: sửa từng điểm tại chỗ của nó, mỗi điểm một test viết trước (TDD), gom theo khu vực thành task. Không có migration. Phép thử khứ hồi của Đợt 9 và toàn bộ test hiện có phải vẫn qua.

## 4. Kiểm tra

- Backend: test cho E1 tới E7, P1, P2; `ImportRoundTripIT` vẫn qua; `mvnw verify` cả repo.
- Frontend: test cho P1 (chữ "đang bận"), F1 tới F7, T1, T2; `npm test`, build, lint 0 lỗi.
- Xem bằng mắt: nút "Cấp mã" cuộn tới đúng phần, hộp so sánh version, màn hình "Đã nhập" ở nền sáng.
