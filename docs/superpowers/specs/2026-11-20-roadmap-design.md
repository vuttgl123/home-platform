# Lộ trình các đợt tiếp theo

Ngày: 2026-11-20 · Trạng thái: **đề xuất** (bạn: "lên luôn kế hoạch triển khai nhiều tính năng khác"; "cứ đề xuất rồi tự làm luôn")

Ký hiệu: **(bạn)** là điều bạn đã nói; **(tôi chọn)** là đề xuất của tôi, bạn có thể bác hay đổi thứ tự.

Mỗi đợt dưới đây khi bắt đầu sẽ có thiết kế riêng (`docs/superpowers/specs/`) và kế hoạch riêng (`docs/superpowers/plans/`), như các đợt trước. Tài liệu này chỉ chốt **làm gì, vì sao, theo thứ tự nào**.

## 1. Đã có (Đợt 1 đến 4)

Ba loại hồ sơ (địa điểm lồng nhau, người, đồ đạc) với mục tuỳ biến; cư trú, sở hữu, chủ món đồ; mã tự cấp; lịch sử version, khôi phục, thùng rác, hoàn tác; tìm chung Ctrl+K; trang tổng quan, xuất Excel, in hồ sơ, lọc hoạt động, so sánh version.

## 2. Nguyên tắc xếp thứ tự (tôi chọn)

1. Thứ người dùng thấy hằng ngày trước, hạ tầng sau.
2. Mỗi đợt đứng được một mình: xong là dùng được, không để dở cho đợt sau.
3. Đợt cần quyết định hạ tầng (kho file, đăng nhập) đi sau các đợt chỉ cần dữ liệu đang có.
4. Giữ những gì bạn đã loại: **tiền (hợp đồng, đặt phòng, hoá đơn) không nằm trong lộ trình.** **(bạn, Đợt 1)**

## 3. Các đợt

### Đợt 5: Giao diện mới (đang làm)

Thiết kế: `2026-11-20-ui-refresh-design.md`. Màu mang nghĩa, font Be Vietnam Pro, chế độ tối, dùng `avatar`, `tooltip`, `card`, `progress`, `chart`, `hover-card`, `scroll-area`. Không đổi backend.

### Đợt 6: Chỗ ở và sức chứa (D3)

Vì sao: trang tổng quan đã đếm "trống", nhưng chưa biết một phòng chứa được mấy người, chưa có danh sách chỗ trống để xếp người vào.

- Sức chứa của một địa điểm (số người tối đa, tuỳ chọn); cảnh báo khi xếp quá sức chứa (không chặn).
- Trang "Chỗ trống": lọc theo loại, theo toà; thấy còn mấy chỗ; bấm để xếp người vào.
- Dòng thời gian cư trú của một địa điểm và của một người (ai ở từ khi nào tới khi nào), dùng `chart` hoặc dải thời gian.
- Danh sách "sắp chuyển đi" (cư trú có ngày kết thúc trong 30 ngày tới) trên trang tổng quan.
- Component: `toggle-group` cho bộ lọc nhanh. (Ô chọn ngày có lịch, `calendar` + `popover`, đã làm ở Đợt 5 cho mọi ô ngày.)
- Backend: một cột `capacity` cho địa điểm (migration), truy vấn chỗ trống.

### Đợt 7: Ảnh (media)

Vì sao: hồ sơ một món đồ, một căn phòng có ảnh thì dễ nhận ra hơn mọi dòng mô tả; `media-service` đã có khung và luật (`docs/superpowers/todo/media.md`).

- Kho file: thư mục trên đĩa máy chủ, đổi sang S3/MinIO bằng cấu hình sau. **(tôi chọn, theo đề xuất trong todo)**
- Tải ảnh lên (JPEG, PNG, WebP, tối đa 10 MB), gắn vào địa điểm, người, món đồ; ảnh bìa, ảnh đại diện; chú thích; sắp xếp; gỡ ảnh (giữ dấu vết).
- Ảnh thu nhỏ để danh sách nhanh.
- Giao diện: `carousel` xem ảnh, `aspect-ratio`, `attachment` khi tải lên, ảnh đại diện thay chữ cái đầu trong `avatar`.
- Cần quyết khi bắt đầu: cách trả ảnh (service trả thẳng hay đường dẫn có chữ ký), kích thước thu nhỏ.

### Đợt 8: Bảo trì và nhắc việc

Vì sao: "món gì hỏng" đã thấy được; bước tiếp theo là theo dõi sửa chữa và không quên hạn.

- Nhật ký bảo trì của một món đồ: ngày, việc đã làm, người làm, ghi chú (một bảng mới, có version như các bản ghi khác).
- Ngày hết bảo hành, ngày bảo trì định kỳ cho đồ; ngày hết hạn giấy tờ của người (mục tuỳ biến kiểu ngày đã có thể dùng).
- Trang "Việc cần làm": đồ hỏng chưa sửa, bảo hành sắp hết, bảo trì tới hạn, cư trú sắp kết thúc. Hiện số trên thanh bên.
- Thông báo trong ứng dụng (chuông ở đầu trang, `popover`); chưa gửi email, tin nhắn.
- Ghi chú: Đợt 1 đã để "thông báo" ra ngoài **(tôi chọn)**; đây là lúc đưa vào.

### Đợt 9: Nhập từ Excel (F5)

Vì sao: không gian có sẵn danh sách trên Excel thì nhập tay từng dòng là rào cản lớn nhất để bắt đầu dùng.

- Tải mẫu Excel (cùng cột với file xuất ở Đợt 4), điền, tải lên.
- Xem trước từng dòng: dòng hợp lệ, dòng lỗi kèm lý do (trùng mã, thiếu loại, ngày sai…); sửa rồi nhập lại; nhập là một lần ghi có version.
- Thứ tự nhập: địa điểm, người, cư trú, sở hữu, đồ đạc.
- Giao diện: các bước bằng `tabs` hoặc thanh tiến trình (`progress`), bảng xem trước (`table`, `scroll-area`).

### Đợt 10: Đăng nhập, thành viên, phân quyền

Vì sao: hiện "phiên" chỉ là id người dùng chọn trên trình duyệt, registry tin header `X-Workspace-Id`. Trước khi nhiều người cùng dùng một không gian, phải có đăng nhập thật và kiểm tra quyền.

- Đăng nhập bằng email và mã một lần gửi qua email (`input-otp` đã có), phiên bằng cookie.
- Mời thành viên vào không gian; vai trò OWNER, EDITOR, VIEWER (đã có trong dữ liệu) được kiểm tra ở mọi API.
- Registry và media kiểm tra người gọi là thành viên của không gian (sửa lỗ hổng: hiện chỉ frontend chặn).
- Ghi chú: Đợt 1 bạn để đăng nhập và phân quyền ra ngoài phạm vi lúc đó **(bạn)**; đề xuất đưa vào ở đây vì các đợt sau cần nhiều người dùng.

### Đợt 11: Tem mã QR và dùng trên điện thoại

Vì sao: mỗi địa điểm, món đồ đã có mã; dán tem QR lên cửa phòng, lên tủ lạnh thì quét là mở đúng hồ sơ.

- In tem QR hàng loạt (theo tầng, theo nhóm đồ), chọn khổ giấy tem.
- Quét mã bằng camera điện thoại để mở hồ sơ, đánh dấu "đã kiểm tra hôm nay" (kiểm kê).
- Giao diện điện thoại: `drawer` cho thao tác nhanh, `sheet` cho bộ lọc.

## 4. Để dành (chưa xếp đợt)

- Lọc danh sách theo mục tuỳ biến. **(Đợt 1: tôi chọn để sau)**
- Bản đồ địa điểm (đã có toạ độ và geohash).
- Danh mục đơn vị hành chính chuẩn (tỉnh, phường) thay chữ tự do. **(câu hỏi mở từ Đợt 1)**
- Luật "loại nào chứa được loại nào". **(Đợt 1: tôi chọn để sau)**
- Tìm theo từ khoá trong nội dung. **(bạn, Đợt 1: không làm lúc đó)**

## 5. Còn chờ bạn

- Thứ tự Đợt 6 đến 11 là đề xuất; đổi thứ tự không làm hỏng đợt nào, trừ: Đợt 7 (ảnh) cần chọn kho file, Đợt 10 cần chọn cách gửi mã đăng nhập.
- Các ghi chú nhỏ còn lại từ lần duyệt Đợt 4 (9 mục "để sau") được làm dần trong Đợt 5 nếu chạm cùng chỗ, còn lại gom vào đợt gần nhất.
