# Giao diện mới: màu có nghĩa, chữ cho tiếng Việt, dùng đủ bộ component

Ngày: 2026-11-20 · Trạng thái: **đã duyệt, đã xây** (2026-11-20, bạn: "cứ đề xuất rồi tự làm luôn")

Ký hiệu: **(bạn)** là điều bạn đã nói; **(tôi chọn)** là quyết định của tôi, bạn có thể bác.

Liên quan: cấu trúc frontend `2026-11-13-frontend-structure-design.md`; lộ trình các đợt sau `2026-11-20-roadmap-design.md`.

## 1. Mục đích

Giao diện hiện chỉ có trắng, đen, xám: "Hỏng" và "Đang dùng" cùng một kiểu nhãn, "Xoá" và "Sửa" chỉ khác chữ, bốn con số trên trang tổng quan giống hệt nhau, trang một địa điểm gần như trống. Nhiều thứ "chưa rõ ràng". **(bạn)** Bạn đã thêm 59 component shadcn; 40 cái chưa được dùng ở đâu. **(bạn: "dùng linh hoạt các component tôi add")**

Đợt này đổi cách giao diện **nói**: màu mang nghĩa (trạng thái, hành động, mức cần chú ý), chữ đọc tốt tiếng Việt có dấu, và mỗi loại thông tin dùng đúng component của nó. Không đổi nghiệp vụ, không đổi API.

Người dùng: người quản lý một toà nhà hay một gia đình, mở trang để trả lời "ai ở đâu, món gì hỏng, ai vừa đổi gì". Công việc chính của giao diện: nhìn là thấy cái cần chú ý.

## 2. Mục tiêu, phần không làm, điều phải tránh

**Mục tiêu**

- Mỗi trạng thái và hành động có một màu cố định, dùng giống nhau ở mọi trang (danh sách, chi tiết, hoạt động, lịch sử, tìm kiếm).
- Trang tổng quan đọc được trong vài giây: số chính có biểu tượng và màu, tỷ lệ có người theo loại là thanh tiến độ, đồ theo trạng thái là biểu đồ.
- Trang chi tiết (địa điểm, người, đồ) có khối thông tin rõ ràng thay cho các dòng chữ rời.
- Có chế độ tối, chọn được Sáng, Tối hoặc Theo máy. **(tôi chọn)**
- Mọi test hiện có vẫn xanh; thêm test cho hành vi mới (màu theo trạng thái, chế độ tối, chú thích của nút chỉ có biểu tượng).

**Không làm ở đợt này**

- Đổi ô chọn ngày (`<input type="date">`) sang lịch bật lên (`calendar` + `popover`), đổi `NativeSelect` sang `select`/`combobox`. Ô gốc của trình duyệt dùng tốt trên điện thoại, và test hiện gõ thẳng vào chúng. **(tôi chọn)** Ghi vào lộ trình.
- Phân trang bằng số trang (`pagination`): danh sách đi bằng cursor, "Tải thêm" đúng với API.
- Thêm thư viện JS. Chỉ thêm một gói font. **(tôi chọn)**
- Trang in hồ sơ: giữ đen trắng cho máy in.

**Điều phải tránh**

- Màu chỉ để trang trí: màu nào cũng phải mang nghĩa, và nghĩa được nói thêm bằng chữ (không ai phải đoán trạng thái chỉ qua màu).
- Chữ trên nền màu không đủ tương phản (WCAG AA cho chữ thường).
- Mọi thứ thành thẻ (card) giống hệt nhau với cùng một bóng đổ.

## 3. Cách làm

### 3.1 Các hướng đã cân nhắc

| Hướng | Được | Mất |
|---|---|---|
| **a. Giữ shadcn + Tailwind, đổi bộ token màu trong `index.css`, thêm token ngữ nghĩa, một `ToneBadge` dùng chung** (đã chọn) | Mọi component tự đổi theo token; không đụng code bên trong `components/ui`; chế độ tối có sẵn khung | Phải rà từng chỗ dùng `Badge` để gắn đúng nghĩa |
| b. Chọn một preset màu khác của shadcn (blue, green…) | Nhanh | Chỉ đổi màu chính; trạng thái vẫn không có màu riêng |
| c. Thêm một thư viện giao diện khác | Nhiều sẵn | Hai hệ component chồng nhau, trái với bộ bạn đã chọn |

### 3.2 Bảng màu (tôi chọn)

Gốc từ đề tài: nhà, toà nhà ở Việt Nam. Màu chính là **xanh cổ vịt** (petrol), không phải xanh tím than mặc định của các trang quản lý; điểm nhấn **vàng đèn lồng** cho "cần chú ý".

| Tên | Hex (gần đúng) | Dùng cho |
|---|---|---|
| Xanh cổ vịt | `#0F6B73` | Màu chính: nút chính, liên kết, mục đang chọn, thanh tiến độ |
| Mực | `#172A30` | Chữ chính |
| Nền giấy lạnh | `#F3F6F7` | Nền trang (thẻ và bảng trắng nổi lên trên) |
| Thanh bên đêm | `#0F2E33` | Nền thanh bên (đậm, chữ sáng) |
| Vàng đèn lồng | `#C98510` | Cảnh báo, "Cần xử lý", Cất kho |

Màu ngữ nghĩa (token `--success`, `--warning`, `--danger`, `--info`, `--violet`, mỗi màu có bản nền nhạt và bản chữ):

| Nghĩa | Màu | Ở đâu |
|---|---|---|
| success | xanh lá `#1E8A57` | Đang dùng, Đang ở, Hiện tại, hành động Tạo |
| warning | vàng `#C98510` | Cất kho, Sắp tới, mục "Cần xử lý" |
| danger | đỏ `#C8352A` | Hỏng, hành động Xoá, lỗi |
| info | xanh dương `#2F6BD6` | hành động Sửa |
| violet | tím `#7650C2` | hành động Khôi phục |
| neutral | xám | Đã gỡ, Đã qua, Tạm ngưng, Đã lưu trữ |

Chế độ tối: cùng sắc độ, nền `#0E1A1D`, thẻ sáng hơn nền một bậc, màu ngữ nghĩa sáng hơn để giữ tương phản.

### 3.3 Chữ (tôi chọn)

- **Be Vietnam Pro** (gói `@fontsource/be-vietnam-pro`, các độ đậm 400, 500, 600, 700), thay Geist. Font được vẽ cho tiếng Việt: dấu chồng (ặ, ẫ, ổ) không bị dính dòng trên, và là tiếng nói riêng cho một ứng dụng tiếng Việt.
- Thang chữ: tiêu đề trang 24/600, tiêu đề khối 16/600, chữ thường 14, chữ phụ 13 và 12. Không viết hoa toàn bộ nhãn.
- Số đếm, mã (`DD-000007`), ngày dùng số đều chiều rộng (`tabular-nums`) để cột thẳng hàng; không dùng font monospace.

### 3.4 Bố cục khung (tôi chọn)

```
┌──────────────┬─────────────────────────────────────────────────┐
│ [CB] Chung cư│ [≡]  Toà A › Tầng 1 › P101       [Tìm… Ctrl K]  │
│      Hoà Bình├─────────────────────────────────────────────────┤
│──────────────│                                                 │
│ ▣ Tổng quan  │  Tiêu đề trang              [Hành động chính]   │
│ ◎ Người      │  mô tả ngắn                                     │
│ ▢ Đồ đạc     │                                                 │
│ ◇ Danh mục   │  ┌ khối ───────────┐ ┌ khối ──────────────┐     │
│ ∿ Hoạt động  │  │                 │ │                    │     │
│ ⌫ Thùng rác  │  └─────────────────┘ └────────────────────┘     │
│──────────────│                                                 │
│ ĐỊA ĐIỂM     │                                                 │
│ › Toà A      │                                                 │
│   › Tầng 1   │                                                 │
│──────────────│                                                 │
│ (NT) Người   │                                                 │
│      thử  ⚙  │                                                 │
└──────────────┴─────────────────────────────────────────────────┘
```

- Thanh bên nền đậm: điều hướng chính lên **trên**, cây địa điểm ở dưới trong vùng cuộn riêng (`scroll-area`), người dùng ở đáy với `avatar` và menu (đổi chế độ sáng tối, thoát).
- Nhãn nhóm "Địa điểm" viết thường như mọi nhãn khác (không viết hoa toàn bộ; sơ đồ trên chỉ để dễ nhìn).
- Nền trang màu giấy lạnh, khối trắng có viền mảnh; bóng đổ chỉ dùng cho thứ nổi lên (hộp thoại, menu).
- Nút chỉ có biểu tượng đều có `tooltip`.

### 3.5 Component theo chỗ dùng (tôi chọn)

| Component | Dùng ở |
|---|---|
| `badge` qua `ToneBadge` (mới, `components/common`) | Mọi trạng thái và hành động: đồ, địa điểm, cư trú, sở hữu, hoạt động, lịch sử, danh mục, tìm kiếm |
| `avatar` (chữ cái đầu, màu theo tên) | Không gian, người dùng, danh sách người, đầu trang một người, người ở, chủ sở hữu, ai đã sửa |
| `tooltip` | Nút chỉ có biểu tượng (ẩn thanh bên, `⋯`, thoát), ô bị cắt chữ |
| `card` | Khối số và khối nội dung trên tổng quan; khối "Thông tin", "Mục tuỳ biến" ở trang chi tiết |
| `progress` | Tỷ lệ có người theo loại địa điểm; tổng tỷ lệ sở hữu |
| `chart` (recharts, đã cài) | Đồ theo trạng thái trên tổng quan |
| `alert` | "Cần xử lý" trên tổng quan |
| `breadcrumb` | Đường dẫn của địa điểm (đã có), thêm cho trang một món đồ |
| `scroll-area` | Cây địa điểm trong thanh bên |
| `separator` | Ngăn các nhóm trong thanh bên và đầu trang |
| `dropdown-menu` (nhóm chọn một) | Chọn chế độ Sáng, Tối, Theo máy |
| `hover-card` | Tên người trong danh sách người ở và chủ sở hữu: rê chuột thấy số điện thoại, email |
| `empty`, `skeleton`, `kbd`, `command`, `tabs`, `dialog`, `field`, `switch`, `table` | Như hiện có, đổi màu theo token |

Những component còn lại (`calendar`, `popover`, `select`, `combobox`, `sheet`, `drawer`, `toggle-group`, `pagination`, `carousel`, `input-otp`…) được xếp vào các đợt sau trong lộ trình, nơi chúng có việc thật để làm (lịch hẹn, ảnh, đăng nhập bằng mã).

### 3.6 Trang tổng quan: thứ đáng nhớ nhất (tôi chọn)

Chỉ một chỗ "mạnh tay": khối **Chỗ ở theo loại**. Mỗi loại một dòng: tên, thanh tiến độ màu xanh cổ vịt cho phần có người, chữ "5 có người · 4 trống" và tỷ lệ phần trăm. Phần còn lại tiết chế: bốn khối số có biểu tượng trong ô màu nhạt đúng nghĩa (địa điểm: xanh cổ vịt, người đang ở: xanh lá, đồ đang dùng: xanh lá, đồ hỏng: đỏ), biểu đồ cột ngang nhỏ cho đồ theo trạng thái, danh sách hoạt động gần đây có chấm màu theo hành động và `avatar` người sửa.

### 3.7 Trang chi tiết (tôi chọn)

- Địa điểm: tab "Tổng quan" có khối "Thông tin" (loại, mã, trạng thái, mô tả) và khối "Mục tuỳ biến", rồi khối "Địa chỉ" như hiện có. Đầu trang giữ `breadcrumb`, nhãn loại và trạng thái có màu.
- Người: đầu trang có `avatar` lớn, nhãn giới tính; khối "Thông tin" và "Mục tuỳ biến".
- Đồ: `breadcrumb` tới chỗ đặt; nhãn trạng thái có màu; khối "Thông tin".

## 4. Thay đổi theo lớp

| Lớp | Thay đổi |
|---|---|
| `src/index.css` | Bộ token sáng và tối mới; token ngữ nghĩa; font; `tabular-nums` cho lớp `.nums` |
| `components/common` | `ToneBadge`, `PersonAvatar` (chữ cái đầu, màu theo tên), `PageHeader`, `IconTooltipButton` |
| `app/` | Khung mới (thanh bên, đầu trang), chế độ sáng tối (`theme.ts`, menu người dùng) |
| `features/*` | Bảng nghĩa màu cạnh bảng nhãn (`labels.ts`): `ITEM_STATUS_TONE`, `ACTION_TONE`, …; các danh sách và trang chi tiết dùng `ToneBadge`, `avatar`, `card` |
| `package.json` | Thêm `@fontsource/be-vietnam-pro`, bỏ `@fontsource-variable/geist` |
| Backend | Không đổi |

## 5. Kiểm thử

- Test hành vi mới: nhãn trạng thái mang `data-tone` đúng nghĩa (đồ hỏng là `danger`, hành động Khôi phục là `violet`…); chọn chế độ Tối thêm lớp `dark` lên `<html>` và được nhớ qua lần mở sau; nút chỉ có biểu tượng có tên đọc được và hiện chú thích khi rê chuột; `avatar` lấy đúng chữ cái đầu của tên tiếng Việt ("Nguyễn Thị Lan" → "NL").
- Mọi test hiện có vẫn xanh; `npm run build`, `npm run lint` sạch.
- Xem bằng mắt: chụp màn hình các trang chính ở chế độ sáng, tối và khổ điện thoại trên dữ liệu thử của không gian -9001 (identity giả, registry thật chạy riêng cổng), so với ảnh trước khi đổi.

## 6. Để lại cho người xây

- Giá trị `oklch` chính xác của từng token, miễn đúng sắc độ ở mục 3.2 và đạt tương phản AA.
- Biểu tượng cụ thể cho từng mục (lucide).
- Cách chia nhỏ component trong từng trang.
