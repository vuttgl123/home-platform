# Ảnh cho địa điểm, người và đồ đạc (Đợt 7)

Ngày: 2026-11-25 · Trạng thái: **đã xây** (2026-11-25), **đã duyệt theo uỷ quyền** (bạn: "Làm tiếp cho tôi Đợt tiếp theo cho tôi"; các đợt trước: "cứ đề xuất rồi tự làm luôn", "các quy tắc bạn tự đề xuất tự làm")

Ký hiệu: **(bạn)** là điều bạn đã nói; **(tôi chọn)** là quyết định của tôi, bạn có thể bác.

Liên quan:
- lộ trình: `2026-11-20-roadmap-design.md` (mục Đợt 7);
- luật nghiệp vụ: `2026-11-13-business-rules-design.md` (mục 6, Media);
- việc còn lại: `docs/superpowers/todo/media.md`;
- giao diện: `2026-11-20-ui-refresh-design.md`.

## 1. Mục đích

Một căn phòng, một món đồ, một người có ảnh thì nhận ra ngay, nhanh hơn đọc mọi dòng mô tả. `media-service` mới chỉ có khung: bảng `med_file` và `med_attachment`, luật tải lên, interface kho file chưa có bản cài đặt nào. Bên registry đã có sẵn chỗ ghi ảnh bìa (`cover_file_id` của địa điểm và đồ đạc) và ảnh đại diện (`avatar_file_id` của người), nhưng chưa ai dùng.

| Việc | Hiện nay | Sau đợt này |
|---|---|---|
| Thêm ảnh | Không có | Tab **Ảnh** trên trang địa điểm, người, món đồ: chọn nhiều ảnh một lần rồi tải lên |
| Xem ảnh | Không có | Lưới ảnh thu nhỏ; bấm vào thì xem ảnh lớn, lật trước sau bằng nút hoặc phím mũi tên |
| Sắp xếp | Không có | Chú thích; đưa ảnh lên trước, ra sau; gỡ ảnh (vẫn giữ dấu vết) |
| Ảnh bìa, đại diện | Có cột, chưa dùng | "Đặt làm ảnh bìa" (địa điểm, đồ) hiện ở đầu trang; "Đặt làm ảnh đại diện" (người) thay chữ cái đầu trong avatar |

## 2. Mục tiêu, phần không làm, điều phải tránh

**Mục tiêu**

- Chụp ảnh bằng điện thoại rồi tải lên thì ảnh đứng đúng chiều, cả ảnh lớn lẫn ảnh thu nhỏ.
- Lưới ảnh tải nhanh: chỉ tải ảnh thu nhỏ, ảnh gốc chỉ tải khi mở xem.
- Tải 20 ảnh một lần vẫn dùng được: từng ảnh báo xong hay lỗi riêng, ảnh lỗi không làm hỏng ảnh khác.
- Cùng một ảnh tải hai lần thì chỉ lưu một bản (luật đã chốt: checksum **(bạn)**).

**Không làm ở đợt này** (tôi chọn)

- Video, PDF, giấy tờ; cắt, xoay, sửa ảnh trong ứng dụng.
- Kéo thả để sắp xếp. Đợt này dùng nút "Đưa lên trước", "Đưa ra sau", dùng được bằng bàn phím và trên điện thoại.
- Kho S3 hay MinIO. Interface `FileStorage` giữ chỗ cho chúng; đổi bằng cấu hình về sau.
- Trang xem lại ảnh đã gỡ, hay khôi phục ảnh đã gỡ. Dữ liệu vẫn còn (`removed_*`) để làm về sau.
- Ảnh trong file Excel và trang in.
- Kiểm tra quyền thật. Media, cũng như registry, tin header không gian cho tới Đợt 10 (đăng nhập).

**Điều phải tránh**

- Ảnh chụp dọc bằng điện thoại hiện nằm ngang.
- Lưới ảnh tải nguyên ảnh 10 MB.
- Một file không phải ảnh, chỉ đổi đuôi thành `.jpg`, lọt vào kho.
- Xoá một bản ghi rồi khôi phục thì mất ảnh (luật đã chốt: xoá bản ghi không gỡ ảnh).
- Ảnh của không gian này đọc được từ không gian khác qua API (ngoài giới hạn đã nêu ở trên).

## 3. Luật (bổ sung mục 6 của luật nghiệp vụ)

**M1. Nhận file** (tôi chọn): ngoài kiểu khai báo (JPEG, PNG, WebP) và cỡ (tối đa 10 MB), server **giải mã thật** nội dung. Không giải mã được, hoặc định dạng thật khác kiểu khai báo, thì trả 400 và không lưu gì. Đây là phần còn dở `TODO(media)` của `UploadPolicy` (magic bytes).

**M2. Chiều ảnh** (tôi chọn): đọc thẻ EXIF Orientation của JPEG. `width` và `height` lưu theo **chiều hiển thị**, tức là sau khi xoay. Ảnh gốc giữ nguyên byte, vì trình duyệt tự xoay theo EXIF. Ảnh thu nhỏ được xoay sẵn.

**M3. Ảnh thu nhỏ** (tôi chọn): một cỡ duy nhất, cạnh dài tối đa 480 px, không phóng to ảnh nhỏ hơn. Lưu dạng JPEG chất lượng 0,85; phần trong suốt của PNG và WebP thành nền trắng. Tạo ngay lúc tải lên và lưu trong kho cạnh ảnh gốc.

**M4. Trùng** **(bạn)**: cùng checksum SHA-256 trong cùng không gian, file đang READY, thì dùng lại file cũ (không ghi kho lần hai) và tạo thêm một dòng gắn mới.

**M5. Gắn**: một bản ghi (`owner_type` là PLACE, PERSON hay ITEM, kèm `owner_id`) có tối đa **100 ảnh đang hiện** (tôi chọn). Ảnh thứ 101 trả 409. Ảnh mới đứng cuối (`sort_order` = lớn nhất + 1). Chú thích dài tối đa 255 ký tự, bỏ trống được.

**M6. Gỡ** **(bạn)**: ghi `removed_by`, `removed_at`; dòng và file đều còn. Gỡ một ảnh đã gỡ thì trả 404.

**M7. Sắp xếp**: gửi đủ danh sách id các ảnh đang hiện của một bản ghi theo thứ tự mới. Thiếu, thừa hay sai id thì trả 409, vì đó là danh sách cũ, ai đó vừa thêm hay gỡ ảnh. Thành công thì `sort_order` = 0, 1, 2…

**M8. Ảnh bìa, đại diện** (tôi chọn): chọn từ các ảnh đã gắn của chính bản ghi đó, ghi vào `cover_file_id` hay `avatar_file_id` bằng lệnh sửa có sẵn của registry (một version mới). Gỡ đúng ảnh đang là bìa hay đại diện thì frontend bỏ luôn ảnh bìa hay đại diện ở registry. Registry không kiểm tra được file có tồn tại hay không, vì hai service chưa gọi nhau.

**M9. Dọn dở dang** (tôi chọn): dòng `med_file` ở PENDING quá 1 giờ (tải lên hỏng giữa chừng) bị xoá cùng nội dung trong kho, mỗi giờ một lần.

## 4. Cách làm

### 4.1 Hướng đã cân nhắc

| Việc | Hướng | Được | Mất |
|---|---|---|---|
| Kho file | **a. Thư mục trên đĩa** (đã chọn, như lộ trình) | Chạy ngay, không thêm hạ tầng | Sao lưu theo máy chủ; nhiều máy chủ thì phải dùng ổ chung |
| | b. MinIO hay S3 | Chuẩn cho chạy nhiều máy | Phải dựng thêm dịch vụ, khoá truy cập |
| Trình duyệt nhận ảnh | **a. Media trả thẳng nội dung; id không gian nằm trên đường dẫn** (đã chọn) | Dùng được `<img src>` (thẻ này không gửi được header); cache được lâu vì nội dung một file không bao giờ đổi | Ai biết đường dẫn thì mở được, cho tới khi có đăng nhập ở Đợt 10 |
| | b. Frontend tải bằng `fetch` có header rồi tạo `blob:` URL | Giữ cách xác định không gian bằng header | Mỗi ảnh một đoạn JS, không có cache của trình duyệt, phải tự giải phóng bộ nhớ |
| | c. Đường dẫn có chữ ký, hết hạn | An toàn hơn ngay bây giờ | Thêm khoá ký, hạn dùng; thừa khi Đợt 10 có cookie |
| Ảnh thu nhỏ | **a. Server tạo lúc tải lên** (đã chọn) | Test được; đúng chiều nhờ EXIF; client nào cũng như nhau | Thêm thư viện đọc WebP |
| | b. Trình duyệt tự thu nhỏ rồi gửi kèm | Server nhẹ | Phải tin dữ liệu client gửi; khó test |
| | c. Không có ảnh thu nhỏ | Đơn giản | Lưới ảnh tải nguyên ảnh 10 MB |

### 4.2 Backend (`media-service`)

**Kho:** `LocalFileStorage` cài `FileStorage`, gốc là `media.storage.root` (mặc định `data/media`, tính từ thư mục chạy service; `data/` vào `.gitignore`). Ghi vào file tạm rồi đổi tên, để không ai đọc được file ghi dở. Khoá tên trong kho:
- ảnh gốc: `workspaces/<ws>/files/<id>`;
- ảnh thu nhỏ: `workspaces/<ws>/files/<id>/thumb`.

Một test hợp đồng (`FileStorageContract`) chạy chung cho mọi bản cài đặt, gồm cả bản về sau.

**Thư viện:** TwelveMonkeys `imageio-webp` (đọc WebP) và `imageio-metadata` (đọc EXIF), qua ImageIO.

**API:**

| Đường dẫn | Việc |
|---|---|
| `POST /attachments` (multipart: `file`, `ownerType`, `ownerId`, `caption`) | Tải lên và gắn (M1 tới M5). Trả 201 kèm dòng gắn và thông tin file (`width`, `height`, `contentType`, `sizeBytes`, `originalName`) |
| `GET /attachments?ownerType=&ownerId=` | Ảnh đang hiện của một bản ghi, theo thứ tự; tối đa 100 nên không phân trang |
| `PUT /attachments/{id}` (`caption`) | Đổi chú thích |
| `PUT /attachments/order` (`ownerType`, `ownerId`, `attachmentIds`) | Sắp lại (M7) |
| `DELETE /attachments/{id}` | Gỡ (M6) |
| `GET /workspaces/{ws}/files/{id}` | Nội dung ảnh gốc. Header `Cache-Control: private, max-age=31536000, immutable`, `X-Content-Type-Options: nosniff`. File không có, khác không gian hay chưa READY thì 404 |
| `GET /workspaces/{ws}/files/{id}/thumbnail` | Ảnh thu nhỏ, cùng các header trên |

Multipart tối đa 10 MB cho file (`spring.servlet.multipart`); quá cỡ thì trả 413 cùng thông báo như các lỗi khác. Tác vụ định kỳ dọn PENDING (M9) dùng `@Scheduled`.

**Registry:** `PersonResponse` (danh sách người) có thêm `avatarFileId`, để danh sách hiện ảnh đại diện. `PersonDetailResponse`, `PlaceDetailResponse`, `ItemDetailResponse` đã có trường này.

### 4.3 Frontend

- **Kiểu API:** `api:sync` thêm service `media` (cổng 8083, `HP_MEDIA_PORT`); `src/api` có client `media`.
- **Feature mới `features/media`**, là lá: không nhập feature nào khác.
  - `PhotosTab` nhận `ownerType`, `ownerId`, và (tuỳ chọn) ảnh bìa hay đại diện hiện tại cùng hàm đặt nó.
  - Nút "Tải ảnh lên" chọn được nhiều file. Client kiểm tra trước kiểu và cỡ, rồi tải lần lượt, mỗi lần một ảnh. Mỗi ảnh có dòng tiến độ và lỗi riêng.
  - Lưới ảnh thu nhỏ (`aspect-ratio` vuông, `object-cover`, `loading="lazy"`), chú thích dưới ảnh.
  - Bấm ảnh mở hộp xem (`carousel` trong `Dialog`): ảnh gốc, chú thích, nút trước sau, phím mũi tên.
  - Mỗi ảnh có menu: "Sửa chú thích", "Đặt làm ảnh bìa" hay "Đặt làm ảnh đại diện", "Đưa lên trước", "Đưa ra sau", "Gỡ ảnh" (có hỏi lại). Ảnh đang là bìa có nhãn "Ảnh bìa".
  - Chưa có ảnh nào thì hiện "Chưa có ảnh nào." cùng nút tải lên.
- **Tab "Ảnh":**
  - trang địa điểm và trang người: một slot mới trong `app/router.tsx`, như các tab khác;
  - trang món đồ: thêm thẳng vào `ItemPage`, vì trang đó không dùng slot.
- **Ảnh bìa:** ảnh thu nhỏ 56 px bo góc, bên trái tên, ở đầu trang địa điểm và trang món đồ. Ảnh đại diện: `PersonAvatar` nhận `fileId` và hiện ảnh thu nhỏ, chữ cái đầu là phần dự phòng. Dùng ở trang một người và danh sách người.
- Chữ mới: "Ảnh", "Tải ảnh lên", "Chưa có ảnh nào.", "Sửa chú thích", "Đặt làm ảnh bìa", "Đặt làm ảnh đại diện", "Ảnh bìa", "Ảnh đại diện", "Đưa lên trước", "Đưa ra sau", "Gỡ ảnh", "Chỉ nhận ảnh JPEG, PNG, WebP", "Ảnh lớn hơn 10 MB", "Đã đủ 100 ảnh".

## 5. Kiểm tra

- **Media, test hợp đồng kho:** ghi, đọc, ghi đè, xoá, xoá thứ không có, tên khoá có thư mục con.
- **Media, IT trên MySQL thật, kho trong thư mục tạm:**
  - tải JPEG, PNG, WebP;
  - ảnh JPEG có EXIF xoay 90° thì `width` và `height` đổi chỗ, ảnh thu nhỏ đứng đúng chiều;
  - file giả ảnh và file quá cỡ thì trả 400 hay 413;
  - tải trùng thì không ghi kho lần hai;
  - ảnh thứ 101 trả 409;
  - đọc ảnh và ảnh thu nhỏ, kèm header cache; đọc sai không gian thì 404;
  - sửa chú thích, sắp lại (đủ và thiếu id), gỡ, gỡ lần hai;
  - dọn PENDING.
- **Ảnh mẫu cho test:** tạo bằng ImageIO trong test. Ảnh WebP và ảnh JPEG có EXIF là file nhỏ trong `src/test/resources`.
- **Frontend (Vitest):** tải nhiều ảnh (một ảnh lỗi), kiểm tra kiểu và cỡ phía client, hộp xem, chú thích, sắp xếp, gỡ (kèm bỏ ảnh bìa), đặt ảnh bìa hay đại diện, avatar có ảnh và dự phòng chữ.
- **Xem bằng mắt** ở khổ 1920 và 390, nền sáng và tối, với ảnh thật (dọc, ngang, PNG trong suốt).

## 6. Để lại cho người làm

- Tên lớp và cách chia task, theo cấu trúc sẵn có.
- Thư viện tạo ảnh mẫu cho test (WebP, JPEG có EXIF), miễn file mẫu nhỏ và được commit.
