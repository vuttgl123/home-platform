# Tìm nhanh và lấy lại: tìm kiếm chung, trang Đồ đạc, thùng rác, hoàn tác

Ngày: 2026-11-16 · Trạng thái: **đã duyệt, đã xây** (2026-11-16)

Ký hiệu: **(bạn)** là điều bạn đã nói; **(tôi chọn)** là quyết định của tôi, bạn có thể bác.

Liên quan: logic nghiệp vụ `2026-11-13-business-rules-design.md` (mục 3, 5.4, 5.9, 5.10, câu hỏi mở về "thùng rác" ở mục 9), cấu trúc frontend `2026-11-13-frontend-structure-design.md`.

## 1. Mục đích

Năm việc bạn đã duyệt trong danh sách ý tưởng (F2, C1, G1, G3, D9). **(bạn: "ổn")** Chúng chung một điểm: backend gần như đã có, người dùng chỉ thiếu chỗ bấm.

| Việc | Hiện nay | Sau đợt này |
|---|---|---|
| Tìm một thứ bất kỳ | Chỉ tìm được địa điểm (ô tìm trên cây) và người (trang Người); đồ đạc không có ô tìm nào | Một ô tìm duy nhất, mở bằng Ctrl+K, tìm cả ba loại theo tên và theo mã |
| Xem đồ đạc của cả không gian | Phải vào từng địa điểm | Trang "Đồ đạc" có lọc theo trạng thái và nhóm |
| Lấy lại thứ đã xoá | Dò trong trang Hoạt động cho tới dòng "Xoá" | Trang "Thùng rác" liệt kê đúng những thứ đang bị xoá |
| Xoá nhầm | Vào Hoạt động để khôi phục | Nút "Hoàn tác" ngay trên thông báo vừa xoá |
| Xem cả toà nhà dạng bảng | Mở từng nhánh trên cây | Công tắc "Cả bên trong" ở tab Bên trong |

## 2. Mục tiêu, phần không làm, điều phải tránh

**Mục tiêu**

- Từ bất kỳ trang nào, gõ Ctrl+K, vài chữ, Enter là tới đúng bản ghi.
- Trả lời "trong không gian này những món nào đang hỏng" bằng hai lần bấm.
- Thứ đã xoá tìm lại được mà không cần nhớ nó bị xoá khi nào.

**Không làm ở đợt này**

- Tìm theo từ khoá ở giữa tên hay trong mô tả. Vẫn là "bắt đầu bằng" và "đúng mã", như logic nghiệp vụ đã chọn; tìm toàn văn chờ Elasticsearch.
- Xoá hẳn khỏi thùng rác, tự dọn sau N ngày. Hệ thống không làm mất quá khứ. **(tôi chọn)**
- Hoàn tác cho dòng cư trú đã xoá: cư trú chưa có lệnh khôi phục ở backend.
- Số liệu tổng hợp (bao nhiêu món hỏng, bao nhiêu phòng trống): thuộc trang tổng quan, đợt khác.
- Xuất bảng ra Excel: đợt khác.

**Điều phải tránh**

- Ô tìm gửi request theo từng phím gõ.
- Khôi phục từ thùng rác bỏ qua luật đang có (cha của địa điểm còn bị xoá, giấy tờ của người đã có người khác dùng).
- "Hoàn tác" khôi phục nhầm một bản ghi đã được người khác đổi sau lần xoá.

## 3. Cách làm

### 3.1 Tìm chung

| Hướng | Được | Mất |
|---|---|---|
| **a. Frontend gọi song song các API tìm đã có của từng loại** (đã chọn) | Không thêm gì ở backend; mỗi loại vẫn dùng đúng index của nó; một loại lỗi không làm hỏng hai loại kia | Mỗi lần tìm là 5 request nhỏ |
| b. Một API `/search` mới gộp ba loại | Một request | Thêm một lớp ở backend chỉ để gọi lại ba service; phân trang của ba danh sách gộp lại không có nghĩa |

Mỗi lần tìm gửi: tên địa điểm, mã địa điểm, tên người, tên đồ, mã đồ; thêm số điện thoại của người khi chuỗi gõ chỉ gồm chữ số (từ 3 số). Mỗi request lấy 5 kết quả đầu.

### 3.2 Danh sách đồ của cả không gian

| Hướng | Được | Mất |
|---|---|---|
| **a. Nới `GET /items`: không chỉ định phạm vi nghĩa là cả không gian, thêm lọc `status`** (đã chọn) | Một API danh sách duy nhất cho đồ đạc; lọc theo nhóm dùng lại được | Lệnh gọi không tham số trước đây là 400 nay trả dữ liệu |
| b. Thêm API riêng `/items/all` | Không đổi hành vi cũ | Hai API danh sách làm cùng một việc |

### 3.3 Thùng rác

| Hướng | Được | Mất |
|---|---|---|
| **a. Mỗi loại một danh sách, đọc thẳng các dòng đang bị xoá, mới xoá trước** (đã chọn) | Đúng những thứ *đang* bị xoá; mỗi dòng có sẵn `version` để khôi phục; phân trang đơn giản | Cần index theo `deleted_at` ở ba bảng; không có danh sách trộn ba loại |
| b. Lọc bảng version lấy các dòng `DELETE` | Không cần index mới | Thứ đã khôi phục rồi vẫn hiện; phải đối chiếu lại từng dòng |

## 4. Luật nghiệp vụ

### 4.1 Tìm chung

- Chuỗi tìm được cắt khoảng trắng; rỗng thì không gọi. Chờ người dùng ngừng gõ 300 ms rồi mới gọi.
- Kết quả gom theo nhóm, thứ tự: khớp mã (địa điểm, đồ), rồi Địa điểm, Người, Đồ đạc. Một bản ghi vừa khớp mã vừa khớp tên chỉ hiện một lần, ở nhóm khớp mã.
- Mỗi dòng đủ để phân biệt hai thứ trùng tên: địa điểm kèm đường dẫn tổ tiên và mã; người kèm số điện thoại; đồ kèm chỗ đặt, mã và nhãn "Đã gỡ" nếu đã gỡ.
- Khi ô tìm còn trống: hiện các lối tắt tới Người, Đồ đạc, Danh mục, Hoạt động, Thùng rác.
- Một loại lỗi thì nhóm đó hiện "Không tìm được", các nhóm khác vẫn dùng được.

### 4.2 Danh sách đồ của cả không gian

- `GET /items` nhận **nhiều nhất một** phạm vi (`placeId`, `subtreeOf`, `categoryId`, và `ownerId` nếu đợt sở hữu đã làm). Không có phạm vi nào là cả không gian.
- `status` (IN_USE, BROKEN, STORED, REMOVED) dùng được khi không có phạm vi hoặc cùng `categoryId`; cùng `placeId`, `subtreeOf` hay `ownerId` thì 400.
- Không có `status`: mọi món chưa gỡ. Có `status`: đúng trạng thái đó; `REMOVED` là các món đã gỡ.
- Luôn theo tên. Món đã xoá không bao giờ hiện.

### 4.3 Thùng rác

- Liệt kê địa điểm, người, đồ đạc **đang** bị xoá mềm của không gian, mỗi loại một danh sách, xoá gần nhất trước. Cư trú không có trong thùng rác.
- Mỗi dòng: tên, mã (nếu có), ngữ cảnh (địa điểm: tên chỗ chứa nó; đồ: tên chỗ đặt), xoá lúc nào, ai xoá, `version` hiện tại.
- Khôi phục dùng đúng các lệnh khôi phục đã có, gửi `version` của dòng. Mọi luật cũ giữ nguyên và thông báo hiện nguyên văn: cha còn bị xoá (`Restore the parent place 12 first`), địa điểm của món còn bị xoá, giấy tờ của người đã có người khác dùng.
- Không có xoá hẳn.

### 4.4 Hoàn tác sau khi xoá

- Sau khi xoá một địa điểm, một người hay một món đồ, thông báo "Đã xoá …" có nút **Hoàn tác** trong 8 giây.
- Hoàn tác gọi lệnh khôi phục với `version` = `version` lúc xoá cộng 1 (lần xoá làm tăng `version` một đơn vị). Nếu trong lúc đó có ai đổi bản ghi thì backend trả 409 và thông báo nói "Không hoàn tác được: bản ghi đã thay đổi"; không thử lại.
- Hoàn tác xong: thông báo "Đã khôi phục …" và mở lại trang của bản ghi.

### 4.5 Bảng "Cả bên trong"

- Tab Bên trong của một địa điểm có chỗ con thêm công tắc "Cả bên trong": mọi hậu duệ chưa xoá, đọc từ `GET /places/{id}/subtree` (đã có, sắp theo `path` nên cha luôn đứng trước con).
- Mỗi dòng: tên thụt theo cấp, loại, mã, trạng thái; bấm vào tên mở trang của nó. Tải dần 50 dòng.

## 5. Dữ liệu

Một migration của registry (số V kế tiếp chưa dùng), chỉ thêm index:

```sql
ALTER TABLE reg_item   ADD KEY idx_reg_item_status (workspace_id, status, name),
                       ADD KEY idx_reg_item_deleted (workspace_id, deleted_at);
ALTER TABLE reg_place  ADD KEY idx_reg_place_deleted (workspace_id, deleted_at);
ALTER TABLE reg_person ADD KEY idx_reg_person_deleted (workspace_id, deleted_at);
```

Danh sách "mọi món chưa gỡ, theo tên" dùng index `(workspace_id, name)` đã có.

## 6. API (registry)

| Lệnh | Việc |
|---|---|
| `GET /items` | Như mục 4.2 |
| `GET /trash?entityType=PLACE\|PERSON\|ITEM` | Một trang các dòng đang bị xoá của loại đó; `entityType` bắt buộc, lạ thì 400 |

Dòng của `/trash`: `{entityType, id, name, code, context, deletedAt, deletedBy, version}`.

Không đổi: `/places/search`, `/people/search`, `/items/search`, `/places/{id}/subtree`, ba lệnh `restore`.

## 7. Giao diện

**Ô tìm chung** (mở bằng Ctrl+K hoặc ⌘K, hoặc nút "Tìm…" trên thanh đầu trang)

```
┌──────────────────────────────────────────────┐
│ 🔍 p-00                                       │
├──────────────────────────────────────────────┤
│ Khớp mã                                       │
│   Phòng 301          Toà A › Tầng 3   P-0042  │
│ Địa điểm                                      │
│   P-001 Kho          Toà B                    │
│ Người                (không có)               │
│ Đồ đạc                                        │
│   P-00 Máy bơm       ở Kho · TS-000120        │
└──────────────────────────────────────────────┘
```

Mũi tên lên xuống để chọn, Enter để mở, Esc để đóng. Mở trang xong thì ô tìm tự đóng.

**Thanh bên**: thêm "Đồ đạc" (sau "Người") và "Thùng rác" (cuối).

**Trang Đồ đạc** `/w/:ws/items`: tiêu đề, hai ô lọc (trạng thái: Chưa gỡ, Đang dùng, Hỏng, Cất kho, Đã gỡ; nhóm: Mọi nhóm và từng nhóm), bảng đồ có cột chỗ đặt. Lựa chọn lọc nằm trên URL (`?status=BROKEN&category=…`) để chép đường dẫn gửi người khác.

**Trang Thùng rác** `/w/:ws/trash`: ba tab Địa điểm, Người, Đồ đạc (trên URL `?type=`); mỗi dòng có tên, ngữ cảnh, "xoá lúc …", nút "Khôi phục". Trống thì "Không có gì đang bị xoá." Trang Hoạt động giữ nguyên nút "Khôi phục" của nó và có thêm một dòng dẫn sang Thùng rác.

**Thông báo sau khi xoá**: nút "Hoàn tác" như mục 4.4.

**Tab Bên trong**: công tắc "Cả bên trong" như mục 4.5.

## 8. Kiểm thử cần có

1. Gõ nhanh năm ký tự chỉ sinh một lượt request, sau khi ngừng gõ.
2. Một mã khớp cả tìm theo mã lẫn theo tên chỉ hiện một dòng.
3. Một API tìm trả lỗi: hai nhóm kia vẫn hiện kết quả.
4. `GET /items` không tham số trả món chưa gỡ của cả không gian; `status=BROKEN` chỉ trả món hỏng; `status` cùng `placeId` trả 400; không gian khác không thấy gì.
5. Thùng rác chỉ có thứ đang bị xoá: khôi phục xong thì nó biến mất khỏi danh sách.
6. Khôi phục một địa điểm có cha còn bị xoá: hiện nguyên văn lý do, dòng vẫn ở trong thùng rác.
7. Hoàn tác gửi `version` lớn hơn `version` lúc xoá đúng một đơn vị.
8. Các truy vấn mới dùng index (`EXPLAIN` không quét cả bảng).

## 9. Để lại cho người xây

- Tên lớp trong package `com.home_platform.registry.trash`; cách cho `KeysetQuery` đọc các dòng đã xoá.
- Bố cục chính xác của ô tìm và hai trang mới, miễn đủ thông tin ở mục 4 và 7.
- Cách chia nhóm khi vừa có kết quả khớp mã vừa có kết quả theo tên, miễn không lặp dòng.

## 10. Các quyết định của tôi cần bạn xem

1. Tìm chung do frontend gộp từ các API đã có, không thêm API tìm mới (mục 3.1).
2. `GET /items` không tham số nay trả cả không gian thay vì báo lỗi (mục 3.2).
3. Thùng rác không có xoá hẳn và không tự dọn (mục 2).
4. Hoàn tác trong 8 giây, và không áp dụng cho dòng cư trú (mục 2, 4.4).
