# Sở hữu: chủ của địa điểm và chủ của đồ đạc

Ngày: 2026-11-16 · Trạng thái: **chờ duyệt**

Ký hiệu: **(bạn)** là điều bạn đã nói; **(tôi chọn)** là quyết định của tôi, bạn có thể bác.

Liên quan: logic nghiệp vụ `2026-11-13-business-rules-design.md` (mục 3, 5.4, 5.6, 5.7, 5.9, 5.10), cấu trúc frontend `2026-11-13-frontend-structure-design.md`, mã tự cấp `2026-11-16-auto-codes-design.md` (đợt trước).

## 1. Mục đích

Bạn nêu "sở hữu" làm ví dụ cho tính năng muốn có. **(bạn)** Hệ thống hiện trả lời được "ai đang ở đây" và "ở đây có đồ gì", nhưng chưa trả lời được hai câu về sở hữu:

1. **Chỗ này của ai?** Hiện "Chủ" chỉ là một kiểu quan hệ của cư trú. Chủ nhà không ở tại căn hộ vẫn phải hiện trong danh sách "Đang ở"; hai vợ chồng cùng đứng tên không ghi được mỗi người bao nhiêu phần; bán nhà không có thao tác nào ghi lại.
2. **Món này của ai?** Tủ lạnh của người thuê và tủ lạnh của chủ nhà nằm cùng một phòng, hiện không phân biệt được.

Bạn chưa nói rõ nghĩa nào, nên thiết kế này làm **cả hai** thành hai phần độc lập. **(tôi chọn)** Bỏ một phần không ảnh hưởng phần kia.

## 2. Mục tiêu, phần không làm, điều phải tránh

**Mục tiêu**

- Ghi được ai sở hữu một địa điểm, từ ngày nào tới ngày nào, bao nhiêu phần trăm, theo giấy tờ số mấy; tách khỏi việc ai đang ở. **(tôi chọn)**
- Chuyển nhượng là một thao tác: chủ cũ thôi từ ngày X, chủ mới bắt đầu từ ngày X, trong một bước. **(tôi chọn, mục B3 bạn đã duyệt)**
- Xem được một người đang và đã sở hữu những chỗ nào, và những món đồ nào là của riêng họ.
- Ghi được một món đồ là của riêng ai; không ghi thì nó thuộc về địa điểm.

**Không làm ở đợt này**

- Tiền và hợp đồng (giá mua bán, tiền thuê, công chứng). Đã là phần không làm của cả hệ thống. Chỉ có một ô chữ "số giấy tờ" để tra cứu.
- Tổ chức (công ty, hộ gia đình) làm chủ. Chủ là một người trong danh sách Người; công ty thì tạm ghi như một "người" tên công ty. **(tôi chọn)**
- Chuyển nhượng một phần (bán nửa căn). Làm bằng tay: sửa tỷ lệ của dòng cũ và thêm dòng mới. **(tôi chọn)**
- Thừa kế tự động: đồ trong một địa điểm không tự đổi chủ khi địa điểm đổi chủ.
- Ảnh giấy tờ sở hữu: chờ media.

**Điều phải tránh**

- Tổng tỷ lệ sở hữu của một địa điểm vượt 100% trong bất kỳ ngày nào.
- Một người bị xoá trong khi vẫn đang đứng tên một địa điểm hay một món đồ đang dùng.
- Khôi phục một version cũ của món đồ mà làm đổi chủ của nó: chủ, cũng như chỗ đặt, không phải "nội dung".
- Hai chỗ khác nhau cùng ghi "chủ" mà lệch nhau (quan hệ "Chủ" của cư trú và bảng sở hữu).

## 3. Hiện trạng

| Thứ | Hiện nay |
|---|---|
| `reg_occupancy.relation` | OWNER, RESIDENT, GUEST, STAFF, OTHER; giao diện gọi OWNER là "Chủ" và xếp vào tab "Người ở" |
| Luật cư trú | Một người không có hai dòng giao thời gian ở cùng một địa điểm, nên một người không thể vừa là "Chủ" vừa là "Người ở" của cùng một chỗ |
| `reg_item` | Có chỗ đặt (`place_id`), không có chủ |
| Xoá người | Bị chặn khi còn cư trú hiện tại hoặc sắp tới |
| Xoá địa điểm | Bị chặn khi còn chỗ con, cư trú hiện tại hoặc sắp tới, đồ chưa gỡ |
| Trang một người | Ba tab: Thông tin, Nơi ở, Lịch sử |

## 4. Cách làm

### 4.1 Chủ của địa điểm

| Hướng | Được | Mất |
|---|---|---|
| **a. Bảng riêng `reg_ownership`, mỗi dòng một người, một địa điểm, một khoảng thời gian** (đã chọn) | Sở hữu tách khỏi cư trú: chủ không ở vẫn ghi được, chủ đang ở có cả hai dòng; có chỗ cho tỷ lệ và số giấy tờ; cùng mẫu với cư trú nên người dùng và người xây đều đã quen | Thêm một bảng, một tab, một nhóm API |
| b. Giữ trong cư trú, thêm cột tỷ lệ và giấy tờ cho dòng có quan hệ OWNER | Không thêm bảng | Vẫn dính luật "không giao thời gian" nên chủ đang ở không ghi được là người ở; danh sách "Đang ở" vẫn lẫn chủ không ở; các cột mới vô nghĩa với bốn quan hệ còn lại |
| c. Một cột `owner_person_id` trên `reg_place` | Đơn giản nhất | Chỉ một chủ, không đồng sở hữu; không có ngày nên đổi chủ là mất chủ cũ, trừ khi lần ngược version |

### 4.2 Chủ của đồ đạc

| Hướng | Được | Mất |
|---|---|---|
| **a. Một cột `owner_person_id` trên `reg_item`, đổi bằng thao tác riêng** (đã chọn) | Đủ cho câu hỏi "món này của ai"; chủ cũ xem được trong lịch sử version của món, như chỗ đặt cũ | Một món chỉ một chủ |
| b. Dùng chung bảng `reg_ownership` cho cả địa điểm lẫn đồ | Một khái niệm "sở hữu" duy nhất, có ngày và tỷ lệ | Tỷ lệ và giấy tờ gần như không ai ghi cho một cái tủ; bảng phải trỏ tới hai loại bản ghi nên mất khoá ngoại; món đồ đã có sẵn lịch sử version để giữ chủ cũ |
| c. Một mục tuỳ biến kiểu "người" | Không đổi bảng | Mục tuỳ biến chưa có kiểu tham chiếu; không lọc được "đồ của người này"; không chặn được xoá người |

Hai phần dùng hai cách khác nhau vì hai loại bản ghi khác nhau: địa điểm đổi chủ là sự kiện có ngày, có giấy tờ, có thể nhiều người; món đồ thì gần như luôn một chủ và đã có version ghi lại mọi lần đổi.

## 5. Luật nghiệp vụ

### 5.1 Sở hữu địa điểm (`reg_ownership`)

Một dòng nghĩa là: người X sở hữu `share_percent` phần trăm của địa điểm Y từ ngày A tới ngày B.

- **Tạo**: địa điểm và người phải tồn tại, chưa xoá. `start_date` bắt buộc. `end_date` trống (đang sở hữu) hoặc không trước `start_date`; đó là ngày cuối cùng còn sở hữu. `share_percent` tuỳ chọn, lớn hơn 0 và không quá 100, tối đa hai chữ số thập phân; trống nghĩa là không ghi tỷ lệ. `document_no` (số giấy chứng nhận, số hợp đồng) tuỳ chọn, tối đa 100 ký tự. `note` tối đa 500.
- **Không giao nhau**: một người không có hai dòng chưa xoá ở cùng một địa điểm có khoảng thời gian giao nhau; vi phạm trả 409. Sở hữu nhiều địa điểm cùng lúc thì được.
- **Tổng tỷ lệ**: trong bất kỳ ngày nào, tổng `share_percent` của các dòng chưa xoá có hiệu lực hôm đó ở cùng một địa điểm không vượt 100; vi phạm trả 409 kèm ngày đầu tiên bị vượt. Dòng không ghi tỷ lệ không tính. **(tôi chọn)** Kiểm tra khi tạo, sửa và chuyển nhượng.
- **Trạng thái** suy ra từ ngày, không lưu, cùng luật với cư trú: **đang sở hữu**, **sắp tới**, **đã thôi**.
- **Sửa** tỷ lệ, ngày, số giấy tờ, ghi chú khi nhập nhầm; người và địa điểm của dòng không đổi được. Địa điểm và người phải chưa bị xoá, như cư trú.
- **Xoá** (mềm) chỉ dành cho dòng nhập nhầm.
- Không có version riêng: lịch sử sở hữu chính là các dòng với ngày của chúng, như cư trú. **(tôi chọn)**
- Sở hữu một địa điểm không kéo theo các chỗ con: sở hữu toà nhà và sở hữu từng căn là các dòng riêng. **(tôi chọn)** Hệ thống không biết luật nào đúng cho từng trường hợp, nên không suy diễn.
- Hai lệnh ghi sở hữu của cùng một địa điểm chạy lần lượt (giữ riêng dòng địa điểm), để hai lệnh cùng lúc không cùng qua bước kiểm tra tổng tỷ lệ.

### 5.2 Thôi sở hữu và chuyển nhượng

- **Thôi sở hữu**: đặt `end_date` (mặc định hôm nay). Dòng đã có ngày kết thúc thì 409.
- **Chuyển nhượng** một dòng đang không có ngày kết thúc sang người khác, trong một transaction:
  1. Dòng cũ nhận `end_date` = ngày chuyển nhượng trừ một ngày.
  2. Một dòng mới cho người nhận: cùng địa điểm, cùng tỷ lệ, `start_date` = ngày chuyển nhượng (mặc định hôm nay), số giấy tờ và ghi chú lấy từ lệnh.
- Ngày chuyển nhượng phải sau `start_date` của dòng cũ (chủ cũ sở hữu ít nhất một ngày). Người nhận phải khác chủ cũ, tồn tại, chưa xoá.
- Người nhận đã có một dòng giao thời gian ở địa điểm này (đồng sở hữu mua nốt phần còn lại) thì 409; người dùng thôi sở hữu dòng của chủ cũ rồi sửa tỷ lệ dòng của người kia. **(tôi chọn)**

### 5.3 Quan hệ "Chủ" của cư trú

Bảng sở hữu là nơi duy nhất ghi chủ. **(tôi chọn)**

- Cư trú **không nhận thêm** quan hệ OWNER: tạo dòng mới với OWNER, hoặc sửa một dòng sang OWNER, trả 400. Dòng đang là OWNER vẫn sửa ngày, ghi chú được và giữ nguyên quan hệ.
- **Dòng cũ không bị đổi**: các dòng cư trú OWNER đang có giữ nguyên, vẫn hiện "Chủ" trong tab Người ở. Migration **chép** mỗi dòng OWNER chưa xoá thành một dòng sở hữu (cùng người, địa điểm, ngày, ghi chú; không tỷ lệ). Người dùng tự dọn các dòng cư trú đó (sửa thành "Người ở" nếu họ thật sự ở, hoặc xoá).
- Chủ đang ở tại chỗ của mình có hai dòng: một dòng sở hữu, một dòng cư trú "Người ở".

### 5.4 Xoá địa điểm, xoá người

- Xoá địa điểm bị chặn thêm khi còn sở hữu hiện tại hoặc sắp tới. Thông báo liệt kê cả bốn số: `Place 42 still has 1 child places, 0 occupancies, 2 ownerships and 0 items`.
- Xoá người bị chặn thêm khi còn sở hữu hiện tại hoặc sắp tới, hoặc còn là chủ của món đồ chưa gỡ, chưa xoá: `Person 7 still has 0 current or upcoming occupancies, 1 ownerships and 2 items`.
- Khôi phục một địa điểm hay một người đã xoá không đụng tới các dòng sở hữu của nó.

### 5.5 Chủ của đồ đạc (`reg_item.owner_person_id`)

- Trống nghĩa là món thuộc về địa điểm (của chung, của chủ nhà). Có giá trị nghĩa là tài sản riêng của người đó, dù họ có ở đó hay không.
- **Đặt khi tạo**: lệnh tạo nhận `ownerPersonId` tuỳ chọn; người đó phải tồn tại, chưa xoá.
- **Đổi chủ** là thao tác riêng, cần `version`, ghi version `UPDATE`; gửi trống là trả món về cho địa điểm. Lệnh sửa không nhận `ownerPersonId`. `owner_person_id` thêm vào danh sách trường do hệ thống quản lý ở mục 3 của logic nghiệp vụ. **(tôi chọn)**
- **Khôi phục version cũ** không đổi chủ.
- **Thay** một món: món mới cùng chủ với món cũ, trừ khi lệnh thay chỉ định chủ khác. Muốn món mới không có chủ thì đổi chủ sau khi thay.
- Món đã gỡ chỉ đọc, nên không đổi chủ được.
- **Đưa lại** món đã gỡ, hoặc **khôi phục** món đã xoá, mà chủ của nó đã bị xoá thì 409 `Restore person 7 first`, như luật với địa điểm của món. **(tôi chọn)** Tình huống hiếm; đổi lại không có món đang dùng nào trỏ tới một người đã xoá.
- Chuyển món sang địa điểm khác không đổi chủ.
- Đọc: đồ chưa gỡ của một người (theo tên); danh sách đồ nào cũng kèm tên chủ.

## 6. Dữ liệu

Hai migration của registry, mỗi phần một cái (số V kế tiếp chưa dùng; nếu đợt mã tự cấp đã chạy thì là V4 và V5).

**Chủ đồ đạc**

```sql
ALTER TABLE reg_item
    ADD COLUMN owner_person_id BIGINT NULL AFTER replaced_by_item_id,
    ADD KEY idx_reg_item_owner (workspace_id, owner_person_id, removed_at, name),
    ADD CONSTRAINT fk_reg_item_owner FOREIGN KEY (workspace_id, owner_person_id)
        REFERENCES reg_person (workspace_id, id);
```

**Sở hữu địa điểm**

```sql
CREATE TABLE reg_ownership (
    workspace_id  BIGINT        NOT NULL,
    id            BIGINT        NOT NULL,
    place_id      BIGINT        NOT NULL,
    person_id     BIGINT        NOT NULL,
    share_percent DECIMAL(5,2)  NULL,
    start_date    DATE          NOT NULL,
    end_date      DATE          NULL,
    document_no   VARCHAR(100)  NULL,
    note          VARCHAR(500)  NULL,
    created_at    DATETIME(3)   NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    created_by    BIGINT        NOT NULL,
    updated_at    DATETIME(3)   NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    updated_by    BIGINT        NOT NULL,
    version       INT           NOT NULL DEFAULT 1,
    deleted_at    DATETIME(3)   NULL,
    PRIMARY KEY (workspace_id, id),
    KEY idx_reg_ownership_place (workspace_id, place_id, end_date, start_date),
    KEY idx_reg_ownership_person (workspace_id, person_id, start_date),
    CONSTRAINT fk_reg_ownership_place FOREIGN KEY (workspace_id, place_id) REFERENCES reg_place (workspace_id, id),
    CONSTRAINT fk_reg_ownership_person FOREIGN KEY (workspace_id, person_id) REFERENCES reg_person (workspace_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

Sau đó chép các dòng cư trú OWNER chưa xoá sang (dùng lại `id` của dòng cư trú; hai bảng có khoá riêng nên không đụng nhau).

## 7. API (registry)

**Sở hữu địa điểm**

| Lệnh | Việc |
|---|---|
| `GET /ownerships?placeId=&status=` | Sở hữu của một địa điểm theo trạng thái (`CURRENT` mặc định, `UPCOMING`, `PAST`) |
| `GET /ownerships?subtreeOf=` | Sở hữu hiện tại trong cả cây con (ai sở hữu các căn của toà này) |
| `GET /ownerships?personId=` | Mọi dòng của một người, mới nhất trước |
| `POST /ownerships` | Thân `{placeId, personId, sharePercent?, startDate, endDate?, documentNo?, note?}` |
| `GET /ownerships/{id}`, `PUT /ownerships/{id}` | Sửa: `{version, sharePercent?, startDate, endDate?, documentNo?, note?}` |
| `POST /ownerships/{id}/end` | `{version, endDate?}` |
| `POST /ownerships/{id}/transfer` | `{version, toPersonId, transferDate?, documentNo?, note?}`; trả `{ended, started}` |
| `DELETE /ownerships/{id}?version=` | Xoá mềm |

Đúng một trong `placeId`, `subtreeOf`, `personId`, như cư trú. Response có `placeName`, `personFullName`, `status`.

**Chủ đồ đạc**

| Lệnh | Việc |
|---|---|
| `POST /items`, `POST /items/{id}/replace` | Nhận thêm `ownerPersonId` tuỳ chọn |
| `POST /items/{id}/owner` | `{version, ownerPersonId?, note?}`; trả chi tiết món |
| `GET /items?ownerId=` | Đồ chưa gỡ của một người, theo tên; là lựa chọn thứ tư bên cạnh `placeId`, `subtreeOf`, `categoryId` |
| Mọi response của món đồ | Thêm `ownerPersonId`, `ownerName` |

## 8. Giao diện

**Trang địa điểm, tab mới "Sở hữu"** (đứng trước "Người ở")

```
[ ] Cả bên trong                                             [+ Thêm chủ]

Đang sở hữu
Người              Tỷ lệ    Từ ngày       Giấy tờ          
Nguyễn Văn An      50%      01/03/2024    CS 123456        [⋯]
Trần Thị Bình      50%      01/03/2024    CS 123456        [⋯]
                   ─────
                   100%

Sắp tới            (không có)
> Đã thôi
```

- Menu `⋯` của một dòng: Chuyển nhượng, Thôi sở hữu (hai mục này chỉ khi chưa có ngày kết thúc), Sửa, Xoá dòng nhập nhầm.
- "Thêm chủ": chọn người (hoặc tạo người mới ngay trong form, như form cư trú), tỷ lệ, ngày bắt đầu, số giấy tờ, ghi chú.
- "Chuyển nhượng": chọn người nhận, ngày, số giấy tờ mới. Hộp thoại nói rõ kết quả: "An thôi sở hữu từ 14/11/2026; Cường sở hữu 50% từ 15/11/2026".
- Dòng tổng chỉ hiện khi có ít nhất một dòng ghi tỷ lệ. Tổng dưới 100% không phải lỗi.

**Trang một người**: thêm hai tab, "Sở hữu" (các chỗ đang và đã sở hữu, mới nhất trước, có "Thêm chỗ sở hữu") và "Đồ đạc" (đồ chưa gỡ của riêng họ, có cột chỗ đặt).

**Form cư trú**: ô quan hệ không còn "Chủ" khi thêm; khi sửa một dòng đang là "Chủ" thì lựa chọn đó vẫn hiện.

**Đồ đạc**

- Form thêm món: ô "Của ai" với lựa chọn mặc định "Của địa điểm này" hoặc chọn một người.
- Trang món đồ: dòng "Của <liên kết người>" dưới tên khi có chủ; menu `⋯` có "Đổi chủ" (ẩn với món đã gỡ).
- Các bảng đồ đạc: tên chủ hiện chữ nhỏ cạnh tên món khi có.
- Hộp thoại "Thay": món mới mặc định cùng chủ, có ghi rõ.
- Tab Lịch sử của món: lần đổi chủ hiện "Đã đổi: Chủ".

## 9. Kiểm thử cần có

1. Hai dòng 60% và 50% giao nhau một ngày bị từ chối, kèm ngày; hai dòng 60% nối tiếp nhau (không giao) cùng dòng thứ ba 40% phủ cả hai thì được.
2. Hai lệnh thêm chủ 60% cho cùng một địa điểm chạy cùng lúc: chỉ một lệnh thành công.
3. Chuyển nhượng: dòng cũ kết thúc hôm trước, dòng mới bắt đầu đúng ngày, cùng tỷ lệ; người nhận sai thì không dòng nào đổi.
4. Người còn đứng tên một địa điểm, hoặc còn là chủ một món chưa gỡ, không xoá được; hết thì xoá được.
5. Cư trú mới với OWNER bị từ chối; dòng OWNER cũ vẫn sửa ngày được.
6. Sửa một món có gửi `ownerPersonId` không đổi chủ; khôi phục version cũ không đổi chủ.
7. Thay một món: món mới cùng chủ.
8. Món đầu tiên được sửa sau khi thêm cột chủ không hiện "Chủ" trong danh sách trường đã đổi.
9. Không gian này không thấy sở hữu của không gian khác; người của không gian khác không làm chủ được.

## 10. Để lại cho người xây

- Tên lớp trong package `com.home_platform.registry.ownership`; cách tính ngày bị vượt tỷ lệ (ở Java trên các dòng giao nhau, hoặc bằng SQL), miễn kết quả đúng với mục 5.1.
- Bố cục chính xác của tab "Sở hữu" và hai tab mới của trang người, miễn đủ cột và thao tác ở mục 8.
- Có tách phần chọn người thành một component dùng chung cho form cư trú, form sở hữu và ô "Của ai" hay không.

## 11. Các quyết định của tôi cần bạn xem

1. Làm cả hai nghĩa của "sở hữu" (mục 1). Nếu bạn chỉ cần một, bỏ phần kia.
2. Cư trú thôi nhận quan hệ "Chủ"; dòng cũ giữ nguyên và được chép sang bảng sở hữu (mục 5.3).
3. Có tỷ lệ phần trăm, tổng không quá 100% mỗi ngày; không bắt buộc ghi (mục 5.1).
4. Sở hữu toà nhà không tự thành sở hữu từng căn (mục 5.1).
5. Chuyển nhượng chỉ chuyển cả dòng; bán một phần làm bằng tay (mục 2, 5.2).
6. Đổi chủ món đồ là thao tác riêng, không nằm trong form sửa (mục 5.5).
7. Chủ là một người; chưa có "tổ chức" (mục 2).
