# Mã tự cấp và khoá mã

Ngày: 2026-11-16 · Trạng thái: **chờ duyệt**

Ký hiệu: **(bạn)** là điều bạn đã nói; **(tôi chọn)** là quyết định của tôi, bạn có thể bác.

Liên quan: logic nghiệp vụ `2026-11-13-business-rules-design.md` (mục 3, 5.2, 5.4, 5.9, 5.10), cấu trúc frontend `2026-11-13-frontend-structure-design.md`.

## 1. Mục đích

Mã của địa điểm và đồ đạc hiện gõ tay, không bắt buộc, sửa lúc nào cũng được và không chặn trùng. Bạn muốn mã **tự sinh và bị khoá, để không ai tự sửa rồi gây trùng**. **(bạn)**

Sau đợt này: mỗi địa điểm, mỗi món đồ nhận một mã khi được tạo, mã đó không sửa được bằng tay và không bao giờ trùng với mã khác trong cùng không gian.

## 2. Mục tiêu, phần không làm, điều phải tránh

**Mục tiêu**

- Hệ thống cấp mã khi tạo địa điểm và đồ đạc; không còn ô gõ mã. **(bạn: tự sinh)**
- Mã bị khoá: lệnh sửa không đổi được mã. **(bạn: khoá)**
- Mã không trùng trong không gian, kể cả khi hai người tạo cùng lúc. **(bạn: đỡ bị trùng)**
- Mỗi không gian tự đặt dạng mã (tiền tố, số chữ số, có theo năm không), chung hoặc riêng cho từng loại địa điểm, từng nhóm đồ. **(tôi chọn, mục A4 bạn đã duyệt)**
- Bản ghi cũ chưa có mã được cấp mã bằng một nút. **(mục A7)**
- Mã của loại địa điểm trong Danh mục tự sinh từ tên. **(mục A8)**

**Không làm ở đợt này**

- Mã cho người (A6), mã ghép theo cây `A-03-301` (A5), nhãn QR (A9). Bảng mới chừa sẵn chỗ cho người.
- Gõ tay một mã có sẵn từ sổ sách hay nhãn đã in. **(tôi chọn)** Việc đó thuộc về nhập dữ liệu hàng loạt (F5); cho gõ tay ở form là mở lại đúng thứ bạn muốn khoá.
- Mẫu mã tự do kiểu chuỗi định dạng (`{TYPE}/{YY}-{SEQ}`). Ba lựa chọn tiền tố, số chữ số, theo năm là đủ cho các dạng thường gặp. **(tôi chọn)**

**Điều phải tránh**

- Hai bản ghi cùng mã, trong bất kỳ tình huống nào.
- Một mã đã cấp bị đổi mà không để lại version.
- Một mã đã in ra nhãn rồi lại trỏ sang bản ghi khác: mã đã cấp không bao giờ được cấp lại, kể cả khi bản ghi cũ đã bị xoá.

## 3. Hiện trạng

| Thứ | Hiện nay |
|---|---|
| `reg_place.code`, `reg_item.code` | Tuỳ chọn, tối đa 50 ký tự, gõ tay ở form tạo và form sửa, không duy nhất (index thường `idx_reg_place_code`, `idx_reg_item_code`) |
| Khôi phục version cũ | Đưa cả mã của version đó trở lại |
| `reg_place_type.code` | Bắt buộc, gõ tay khi tạo, duy nhất, đã khoá sau khi tạo |
| `reg_field_def.field_key` | Gõ tay, frontend đã gợi ý từ nhãn (`suggestFieldKey`), đã khoá sau khi tạo |
| Người | Không có mã |

## 4. Cách làm

### 4.1 Số thứ tự lấy từ đâu

| Hướng | Được | Mất |
|---|---|---|
| **1. Bảng đếm theo dãy, tăng trong cùng transaction với lệnh tạo** (đã chọn) | Không trùng; lệnh tạo hỏng thì số được trả lại, không hở số; test rollback sạch như mọi test khác | Hai lệnh tạo cùng dãy chạy cùng lúc phải chờ nhau vài mili giây |
| 2. `MAX(mã) + 1` mỗi lần tạo | Không cần bảng mới | Hai lệnh cùng lúc đọc cùng một MAX và ra cùng một mã; phải tách số ra khỏi chuỗi; chậm dần |
| 3. Bộ đếm cấp số ở transaction riêng | Không chờ nhau | Hở số mỗi lần lệnh tạo hỏng; test để lại rác trên database dùng chung; phức tạp hơn mà quy mô này chưa cần |

### 4.2 Chặn trùng bằng gì

| Hướng | Được | Mất |
|---|---|---|
| **a. Khoá duy nhất `(workspace_id, code)` trên cả bảng, kể cả dòng đã xoá** (đã chọn) | Mã không bao giờ dùng lại; khôi phục một bản ghi đã xoá không thể vấp mã trùng | Bản ghi đã xoá vẫn giữ mã của nó |
| b. Cột ảo chỉ tính dòng chưa xoá (như `uk_reg_person_live_document`) | Xoá xong mã được nhả | Nhãn đã in của món đã xoá sẽ trỏ sang món khác; khôi phục có thể vấp trùng |

Với giấy tờ của người, hướng b đúng vì một số CCCD phải dùng lại được sau khi xoá hồ sơ nhập nhầm. Với mã do hệ thống cấp thì không có lý do dùng lại.

## 5. Luật nghiệp vụ

### 5.1 Dạng mã

Một **mẫu mã** có ba phần. **(tôi chọn)**

| Phần | Giá trị | Mặc định |
|---|---|---|
| `prefix` (tiền tố) | 1 đến 10 ký tự `A-Z`, `0-9`, bắt đầu bằng chữ; tự viết hoa | Địa điểm `DD`, đồ đạc `TS` |
| `digits` (số chữ số) | 3 đến 9 | 6 |
| `yearly` (theo năm) | có hoặc không | không |

Mã = tiền tố, gạch nối, (năm 4 chữ số và gạch nối nếu theo năm), số thứ tự thêm số 0 cho đủ `digits`.

| Mẫu | Mã cấp ra |
|---|---|
| `DD`, 6, không | `DD-000001`, `DD-000002` |
| `P`, 4, không | `P-0001` |
| `TS`, 4, có | `TS-2026-0001`; sang năm 2027 bắt đầu lại `TS-2027-0001` |

Số vượt quá `digits` thì in đủ số, không cắt: mẫu 3 chữ số sau `P-999` là `P-1000`. Năm tính theo "hôm nay" của nghiệp vụ (`registry.business-zone`, mặc định `Asia/Ho_Chi_Minh`).

### 5.2 Mẫu nào áp dụng

Giống mục tuỳ biến (mục 5.3 của logic nghiệp vụ): một mẫu thuộc một đích và một phạm vi.

| `target` | `type_id` | Áp dụng cho |
|---|---|---|
| PLACE | 0 | Mọi địa điểm không có mẫu riêng |
| PLACE | `id` loại địa điểm | Địa điểm tạo với loại đó |
| ITEM | 0 | Mọi món đồ không có mẫu riêng |
| ITEM | `id` nhóm đồ | Món tạo với nhóm đó |

Khi cấp mã: lấy mẫu riêng của loại (nhóm); không có thì mẫu chung của đích; không có nữa thì mẫu có sẵn trong code (`DD`, `TS`). Không gian chưa đặt gì vẫn có mã ngay.

- Đổi một mẫu chỉ ảnh hưởng mã cấp từ lúc đó; mã đã cấp giữ nguyên.
- Xoá hẳn một loại địa điểm hay nhóm đồ thì xoá luôn mẫu riêng của nó.
- Mẫu là danh mục: không có `version`, hai người sửa cùng lúc thì người sau thắng; đọc qua cache Caffeine theo `workspace_id` như các danh mục khác.

### 5.3 Dãy số

**Dãy** là phần đầu của mã, tới hết gạch nối cuối: `P-`, `TS-2026-`. Mỗi cặp (đích, dãy) trong một không gian có một bộ đếm. **(tôi chọn)**

- Hai loại dùng chung tiền tố thì chung một dãy, nên không thể ra hai mã giống nhau.
- Đổi tiền tố `P` thành `PH` bắt đầu dãy `PH-` từ 1; đổi lại `P` thì đếm tiếp chỗ dãy `P-` đang dừng.
- Mẫu theo năm tự có dãy mới mỗi năm vì năm nằm trong dãy.
- Địa điểm và đồ đạc đếm riêng. Hai đích dùng cùng tiền tố thì một địa điểm và một món đồ có thể cùng mã; giao diện sẽ cảnh báo khi đặt tiền tố đã dùng ở đích kia, nhưng không cấm.

Cấp một mã, trong transaction của lệnh tạo:

1. Tăng bộ đếm của dãy lên 1 (tạo bộ đếm bằng 1 nếu chưa có) và giữ dòng đó tới hết transaction.
2. Dựng mã từ số vừa lấy.
3. Mã đó đã có trên bất kỳ dòng nào của bảng (kể cả dòng đã xoá, ví dụ một mã gõ tay từ trước) thì quay lại bước 1. Quá 1000 lần thì trả 409.

Lệnh tạo hỏng thì transaction rollback và bộ đếm trở về số cũ.

### 5.4 Khoá mã

`code` trở thành trường do hệ thống quản lý, thêm vào danh sách ở mục 3 "Sửa là gửi lại cả phần sửa được" của logic nghiệp vụ.

- Lệnh tạo và lệnh sửa địa điểm, đồ đạc không còn nhận `code`. Client cũ còn gửi `code` thì giá trị đó bị bỏ qua, không báo lỗi.
- Đổi loại của địa điểm hay nhóm của món đồ **không** đổi mã. **(tôi chọn)** Mã là tên gọi ổn định; nó không phải mô tả.
- Khôi phục nội dung một version cũ **không** đổi mã (như đã không đổi chỗ đứng).
- Thay một món đồ: món mới là bản ghi mới nên có mã mới.
- Mã không duy nhất giữa các không gian; duy nhất trong không gian, theo từng bảng. Luật "mã địa điểm không bắt buộc duy nhất" ở mục 5.4 cũ được thay bằng luật này. **(tôi chọn, thay một lựa chọn cũ của chính tôi)**

**Cấp mã mới** cho một bản ghi đang có: thao tác riêng, cần `version`, cấp mã kế tiếp theo mẫu đang áp dụng cho loại (nhóm) hiện tại của nó, ghi version `UPDATE` kèm ghi chú (mặc định `Code changed from <mã cũ>`). Mã cũ không được trả lại bộ đếm và không ai nhận lại nó. Món đồ đã gỡ không cấp mã mới được (món đã gỡ chỉ đọc). **(tôi chọn)** Đây là lối duy nhất để đổi mã: khi mã gõ tay cũ quá lộn xộn, hoặc sau khi đổi mẫu mà muốn bản ghi theo mẫu mới.

### 5.5 Mã cũ

- Mã gõ tay đang có: giữ nguyên và bị khoá như mã mới.
- **Mã đang trùng nhau** (trong cùng không gian, cùng bảng): migration giữ mã cho bản ghi tạo sớm nhất và bỏ trống mã của các bản còn lại; chúng nhận mã mới ở bước cấp mã bên dưới. **(tôi chọn)** Đây là lần duy nhất hệ thống tự bỏ một mã; mã cũ của các bản ghi đó vẫn xem được trong các version trước của chúng. Trước khi chạy migration, người xây đếm số bản ghi bị ảnh hưởng trên UAT và báo lại cho bạn.
- **Bản ghi chưa có mã**: trang Danh mục hiện "Có N địa điểm và M món đồ chưa có mã" với nút **Cấp mã**. Mỗi bản ghi được cấp mã theo mẫu của loại (nhóm) nó đang có, cũ trước mới sau, và có một version `UPDATE` ghi chú `Code assigned`. Backend làm từng lô 100 bản ghi trong một transaction (lô nhỏ để bộ đếm không bị giữ lâu, lệnh tạo đang chạy cùng lúc không phải chờ quá hạn khoá 5 giây); frontend gọi lặp tới khi hết.
- Bản ghi đã xoá và món đồ đã gỡ (chỉ đọc) không được cấp mã, và không tính vào số "chưa có mã". Khôi phục hoặc đưa lại sau này thì nó lại hiện trong số đó.

### 5.6 Mã danh mục

- Loại địa điểm: form thêm loại không còn bắt gõ mã. Mã được suy từ tên (bỏ dấu, viết hoa, ký tự khác thành `_`): "Toà nhà" ra `TOA_NHA`. Trùng với loại đã có thì thêm `_2`, `_3`. Người dùng vẫn bấm "Tự đặt mã" để gõ nếu muốn; sau khi tạo mã vẫn khoá như hiện nay. Chỉ đổi ở frontend. **(tôi chọn)**
- Khoá của mục tuỳ biến đã được gợi ý từ nhãn; không đổi gì.

## 6. Dữ liệu

Migration `V3__auto_codes.sql` của registry (số V kế tiếp chưa dùng):

```sql
CREATE TABLE reg_code_rule (
    workspace_id BIGINT      NOT NULL,
    target       VARCHAR(10) NOT NULL,
    type_id      BIGINT      NOT NULL DEFAULT 0,
    prefix       VARCHAR(10) CHARACTER SET ascii NOT NULL,
    digits       TINYINT     NOT NULL,
    yearly       TINYINT(1)  NOT NULL DEFAULT 0,
    created_at   DATETIME(3) NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    updated_at   DATETIME(3) NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    PRIMARY KEY (workspace_id, target, type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reg_code_counter (
    workspace_id BIGINT      NOT NULL,
    target       VARCHAR(10) NOT NULL,
    series       VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    last_no      BIGINT      NOT NULL,
    PRIMARY KEY (workspace_id, target, series)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

Sau đó, cho cả `reg_place` và `reg_item`: bỏ trống mã trùng (giữ dòng có `id` nhỏ nhất của mỗi nhóm `(workspace_id, code)`), rồi thay index thường bằng khoá duy nhất `uk_reg_place_code`, `uk_reg_item_code` trên `(workspace_id, code)`. Nhiều dòng `code` trống vẫn hợp lệ với khoá duy nhất.

So sánh mã theo collation của cột (không phân biệt hoa thường và dấu), nên `p-0001` và `P-0001` là một mã.

## 7. API (registry)

| Lệnh | Việc |
|---|---|
| `POST /places`, `POST /items`, `POST /items/{id}/replace` | Không nhận `code`; response có mã vừa cấp |
| `PUT /places/{id}`, `PUT /items/{id}` | Không nhận `code` |
| `POST /places/{id}/reissue-code`, `POST /items/{id}/reissue-code` | Thân `{version, note?}`; cấp mã mới; trả chi tiết bản ghi |
| `GET /code-rules` | Mọi mẫu của không gian. Luôn có hai dòng `typeId` 0 (PLACE, ITEM); dòng chưa được không gian đặt có `builtIn: true` |
| `PUT /code-rules/{target}/{typeId}` | Thân `{prefix, digits, yearly}`; đặt hoặc thay mẫu. `typeId` khác 0 phải là loại (nhóm) của không gian, nếu không 404 |
| `DELETE /code-rules/{target}/{typeId}` | Bỏ mẫu riêng; với `typeId` 0 là quay về mẫu có sẵn |
| `GET /codes/status` | `{placesWithoutCode, itemsWithoutCode}` |
| `POST /codes/backfill` | Cấp mã cho tối đa 100 bản ghi chưa có mã (địa điểm trước, rồi đồ đạc); trả `{placesAssigned, itemsAssigned, placesRemaining, itemsRemaining}` |

Tìm theo mã (`/places/search?code=`, `/items/search?code=`) giữ nguyên; giờ mỗi mã chỉ ra tối đa một kết quả.

Lỗi: tiền tố, số chữ số sai dạng trả 400 theo từng trường; `target` lạ trả 400.

## 8. Giao diện

**Form địa điểm và form đồ đạc**

- Bỏ ô "Mã". Khi thêm: một dòng chữ nhỏ "Mã được cấp khi lưu". Khi sửa: dòng chỉ đọc "Mã P-0042 · do hệ thống cấp, không sửa được"; bản ghi chưa có mã thì không hiện dòng này.
- Thêm xong, thông báo kèm mã: "Đã thêm Phòng 301 (P-0042)".

**Trang địa điểm và trang món đồ**

- Menu `⋯` có thêm "Cấp mã mới" (bản ghi chưa có mã: "Cấp mã"). Hộp xác nhận nói rõ: mã hiện tại sẽ không còn tìm ra bản ghi này, nhãn đã in theo mã cũ sẽ sai. Gặp "đã có người sửa" thì có "Tải bản mới" như các hộp thoại khác.
- Món đã gỡ không có mục này.

**Trang Danh mục, phần mới "Mã tự cấp"**

```
Mã tự cấp
Mã của địa điểm và đồ đạc do hệ thống cấp và không sửa được. Đổi dạng mã chỉ áp dụng cho mã cấp từ nay.

[!] Có 12 địa điểm và 30 món đồ chưa có mã.                      [Cấp mã]

Áp dụng cho          Tiền tố   Số chữ số   Theo năm   Ví dụ                      [+ Thêm dạng riêng]
Mọi địa điểm         DD        6           Không      DD-000001        (có sẵn)   [Sửa]
  Loại: Phòng        P         4           Không      P-0001                      [Sửa] [Bỏ]
Mọi đồ đạc           TS        6           Không      TS-000001        (có sẵn)   [Sửa]
  Nhóm: Điện lạnh    DL        4           Có         DL-2026-0001                [Sửa] [Bỏ]
```

- Hộp thoại sửa: tiền tố, số chữ số, công tắc "Bắt đầu lại mỗi năm", và dòng "Ví dụ" đổi theo lúc gõ.
- "Thêm dạng riêng": chọn một loại địa điểm hoặc nhóm đồ chưa có dạng riêng.
- Tiền tố trùng với một dạng của đích kia: cảnh báo "Địa điểm và đồ đạc sẽ có mã giống nhau", vẫn cho lưu.
- "Cấp mã" chạy từng lô, hiện "Đã cấp 100 / 1 240…", xong thì báo tổng số và làm mới các danh sách.

**Form loại địa điểm**: theo mục 5.6.

## 9. Kiểm thử cần có

1. Hai lệnh tạo cùng dãy chạy cùng lúc ra hai mã liền nhau, không trùng.
2. Lệnh tạo hỏng (ví dụ mục tuỳ biến sai) không làm mất số.
3. Bộ đếm bỏ qua mã đã có trên một dòng khác, kể cả dòng đã xoá.
4. Lệnh sửa có gửi `code` cũng không đổi mã; khôi phục version cũ không đổi mã.
5. Mẫu theo năm: dãy của năm sau bắt đầu lại từ 1.
6. Mẫu riêng thắng mẫu chung; bỏ mẫu riêng thì về mẫu chung.
7. Cấp mã cho bản ghi cũ: cũ trước, mỗi bản ghi một version, chạy lại không cấp hai lần.
8. Không gian này không thấy và không ảnh hưởng bộ đếm, mẫu của không gian khác.

## 10. Để lại cho người xây

- Tên lớp trong package `com.home_platform.registry.code`; cách đăng ký cache `codeRules` cùng chỗ với ba cache danh mục.
- Câu SQL tăng bộ đếm (một câu `INSERT … ON DUPLICATE KEY UPDATE` rồi đọc lại, hoặc đọc kèm khoá rồi ghi), miễn là chạy trong transaction của lệnh tạo.
- Cách chia lô và thứ tự khoá dòng của lệnh cấp mã cho bản ghi cũ.
- Bố cục chính xác của phần "Mã tự cấp", miễn đủ các cột và thao tác ở mục 8.

## 11. Các quyết định của tôi cần bạn xem

1. Không cho gõ tay mã ở bất kỳ đâu (mục 2).
2. Mã không dùng lại sau khi bản ghi bị xoá (mục 4.2).
3. Tiền tố mặc định `DD` cho địa điểm, `TS` cho đồ đạc, 6 chữ số (mục 5.1).
4. Đổi loại hay nhóm không đổi mã (mục 5.4).
5. Mã trùng có sẵn: giữ bản tạo sớm nhất, các bản còn lại được cấp mã mới (mục 5.5).
