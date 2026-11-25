# Bảo trì và nhắc việc (Đợt 8)

Ngày: 2026-11-25 · Trạng thái: **đã duyệt hướng làm** (bạn: "Được tôi đồng ý", với đề xuất 5 điểm ngày 2026-11-25)

Ký hiệu: **(bạn)** là điều bạn đã nói; **(tôi chọn)** là quyết định của tôi, bạn có thể bác.

Liên quan: lộ trình `2026-11-20-roadmap-design.md` (mục Đợt 8), luật nghiệp vụ `2026-11-13-business-rules-design.md` (đồ đạc, mục tuỳ biến), tìm và lấy lại `2026-11-16-find-and-recover-design.md` (hoàn tác), chỗ ở `2026-11-25-capacity-and-vacancies-design.md` (L5 "sắp chuyển đi").

## 1. Mục đích

Đã thấy được món nào hỏng (trạng thái `BROKEN`), ai sắp chuyển đi. Bước tiếp theo là **theo dõi việc sửa chữa** và **không quên hạn**: hết bảo hành, tới kỳ bảo trì, giấy tờ hết hạn.

| Câu hỏi | Hiện nay | Sau đợt này |
|---|---|---|
| Món này đã sửa những gì, ai sửa, bao giờ? | Đọc ghi chú trong version | Tab **Bảo trì** trên trang món đồ: nhật ký từng lần sửa, bảo trì |
| Còn bảo hành không? Bao giờ phải bảo trì? | Không ghi được, trừ mục tuỳ biến tự đặt | Hai ô mới cho đồ: **hết bảo hành** và **bảo trì mỗi N tháng**; hạn kế tiếp tự tính |
| Hôm nay cần làm gì? | Lật từng trang | Trang **Việc cần làm** gom đủ năm loại việc; số việc trên thanh bên |
| Có gì mới cần để ý? | Không có | **Chuông** ở đầu trang: việc mới xuất hiện từ lần cuối bạn mở chuông |

## 2. Mục tiêu, phần không làm, điều phải tránh

**Mục tiêu**

- Ghi một lần sửa hay bảo trì trong vài giây từ trang món đồ; nếu món đang Hỏng thì cùng lúc chuyển nó về Đang dùng.
- Đặt ngày hết bảo hành, chu kỳ bảo trì cho một món; có trong lịch sử version, so sánh version, xuất Excel.
- Đánh dấu một mục tuỳ biến kiểu ngày là "hạn cần nhắc" (ví dụ "CCCD hết hạn" của người, "Hạn kiểm định PCCC" của toà nhà).
- Mở một trang là thấy mọi việc cần làm, việc gấp nhất trước; bấm là tới đúng hồ sơ.
- Chuông chỉ báo thứ **mới**: tải lại trang không làm chuông kêu lại.

**Không làm ở đợt này** (tôi chọn)

- Gửi email, tin nhắn, thông báo đẩy. Chỉ có trong ứng dụng **(lộ trình)**.
- Ẩn, hoãn ("nhắc lại sau") từng việc. Việc chỉ rời danh sách khi đã giải quyết hoặc hết khung thời gian.
- Chi phí sửa chữa, hoá đơn, nhà cung cấp: tiền nằm ngoài lộ trình **(bạn, Đợt 1)**.
- Việc lặp lại không gắn với món đồ (ví dụ "dọn hành lang mỗi tuần"), giao việc cho một người, đánh dấu "đã xong" một việc trên trang Việc cần làm.
- Việc chạy định kỳ phía máy chủ (scheduler). Danh sách được tính khi đọc.
- Bảo hành, bảo trì cho địa điểm. Muốn nhắc hạn của địa điểm thì dùng mục tuỳ biến kiểu ngày có nhắc (L4).
- Nhật ký bảo trì trong Thùng rác và trong file Excel.

**Điều phải tránh**

- Số việc lệch nhau giữa thanh bên, chuông và trang Việc cần làm.
- Chuông "kêu" mỗi lần tải lại trang, hoặc báo lại việc đã thấy.
- Nhắc về thứ đã xoá, đồ đã gỡ, người đã xoá.
- Món bảo trì mỗi tháng nằm thường trực trong danh sách (khung nhắc dài hơn chu kỳ).
- Ghi bảo trì làm hỏng lần sửa món đồ của người khác (xung đột `version` của món đồ).

## 3. Luật nghiệp vụ (tôi chọn)

**L1. Nhật ký bảo trì.** Mỗi dòng gắn với một món đồ chưa xoá (món đã gỡ vẫn ghi được, vì có khi ghi muộn). Các trường:
- `kind`: `REPAIR` (sửa chữa) hoặc `SERVICE` (bảo trì định kỳ), bắt buộc;
- `doneOn`: ngày làm, bắt buộc, không sau hôm nay (theo `BusinessDate`);
- `work`: việc đã làm, bắt buộc, tối đa 500 ký tự;
- `doneBy`: người làm, chữ tự do, tối đa 150 ký tự, tuỳ chọn (thợ ngoài thường không có hồ sơ người);
- `note`: ghi chú, tối đa 1000 ký tự, tuỳ chọn.

Sửa, xoá mềm, khôi phục như các bản ghi khác: có cột `version` để khoá lạc quan, mỗi lần ghi thêm một version vào `reg_entity_version` với `entity_type = 'MAINTENANCE'` (`CREATE`, `UPDATE`, `DELETE`, `RESTORE`). Không đổi món đồ của một dòng sau khi tạo. Xoá món đồ không xoá nhật ký của nó; các dòng chỉ ẩn theo món, khôi phục món thì thấy lại. Sửa, xoá hay khôi phục một dòng của món đã xoá thì trả 409 "Restore item first", như cách sở hữu làm với địa điểm đã xoá.

**L2. Sửa xong.** Khi tạo một dòng `REPAIR` cho món đang `BROKEN`, có thể kèm `markFixed = true` và `itemVersion` (version hiện tại của món). Cùng transaction: ghi dòng bảo trì, rồi đổi món sang `IN_USE` như một lần sửa món bình thường (tăng `version`, ghi version `UPDATE` của món với ghi chú "Đã sửa: <work>", cắt còn 255 ký tự). `itemVersion` lệch thì trả 409 và **không ghi gì**. `markFixed` với món không `BROKEN`, hoặc với `SERVICE`, thì trả 400.

**L3. Bảo hành và bảo trì định kỳ của đồ.** Hai cột mới, bỏ trống được:
- `warrantyUntil`: ngày cuối cùng còn bảo hành;
- `serviceEveryMonths`: chu kỳ bảo trì, từ 1 tới 120 tháng.

**Hạn bảo trì kế tiếp** = (ngày `doneOn` muộn nhất trong các dòng `SERVICE` chưa xoá; không có thì `placedAt`; không có thì ngày tạo món theo `BusinessDate`) + `serviceEveryMonths` tháng. Chỉ dòng `SERVICE` làm lùi hạn; một lần sửa chữa không thay cho bảo trì định kỳ. Chưa đặt chu kỳ thì không có hạn. Sửa hai cột này là một lần sửa món bình thường (form sửa món đồ, có version).

**L4. Hạn tuỳ biến.** Mục tuỳ biến kiểu `DATE` có thêm `remindDays` (1 tới 365, bỏ trống là không nhắc; kiểu khác phải bỏ trống, sai thì 400). Áp dụng cho cả ba đích PLACE, PERSON, ITEM. Chỉ mục đang `ACTIVE` mới nhắc. Bản ghi có giá trị ngày hợp lệ ở mục đó thì sinh một việc (L5). Giá trị sai định dạng thì bỏ qua, không báo lỗi.

**L5. Việc cần làm.** Tính khi đọc, từ năm nguồn. Mỗi việc có `kind`, `dueOn` (ngày dùng để sắp) và `since` (thời điểm việc xuất hiện, dùng cho chuông ở L6):

| `kind` | Khi nào có trong danh sách | `dueOn` | `since` |
|---|---|---|---|
| `BROKEN` | Món `BROKEN`, chưa gỡ | ngày món chuyển sang `BROKEN` | thời điểm của version đó |
| `WARRANTY` | `hôm nay ≤ warrantyUntil ≤ hôm nay + 30` | `warrantyUntil` | đầu ngày `warrantyUntil − 30` |
| `SERVICE` | hạn kế tiếp `≤ hôm nay + 7` (quá hạn vẫn hiện tới khi ghi bảo trì) | hạn kế tiếp | đầu ngày `hạn − 7` |
| `MOVE_OUT` | đúng luật L5 của Đợt 6 (cư trú kết thúc trong 30 ngày tới) | `end_date` | đầu ngày `end_date − 30` |
| `CUSTOM_DATE` | `ngày − remindDays ≤ hôm nay ≤ ngày + 30` | ngày ở mục đó | đầu ngày `ngày − remindDays` |

Chung cho mọi nguồn: chỉ tính bản ghi chưa xoá; đồ đã gỡ không sinh việc nào; đồ, cư trú ở địa điểm đã xoá cũng không. "Đầu ngày" tính theo múi giờ nghiệp vụ; `since` không bao giờ sau thời điểm đọc (việc vừa đặt đã nằm trong khung thì `since` = lúc đặt, xem L6).

Lý do chọn khung: bảo hành hết thì không còn gì để làm, nên chỉ nhắc trước 30 ngày. Bảo trì chỉ nhắc trước 7 ngày, vì chu kỳ có thể ngắn tới một tháng. Hạn tuỳ biến quá ngày thì còn hiện thêm 30 ngày (giấy tờ hết hạn vẫn cần đi làm lại), sau đó tự rời danh sách để dữ liệu cũ không đọng mãi.

Sắp xếp: `dueOn` tăng dần (quá hạn lâu nhất và hỏng lâu nhất lên đầu), cùng ngày thì theo `kind` như thứ tự bảng trên, rồi theo id.

**L6. Chuông.** Mỗi người dùng trong mỗi không gian có một mốc `seenAt`. Một việc là **mới** khi `since > seenAt` (chưa có mốc thì mọi việc là mới). Mở chuông thì `seenAt` = lúc đó. Số trên chuông là số việc mới; số trên thanh bên là tổng số việc. Cả hai lấy từ cùng một lần tính (L5), nên không lệch nhau.

Vì `since` dựa vào dữ liệu chứ không dựa vào lần đọc, tải lại trang không sinh việc mới. Một việc rời danh sách rồi quay lại (ví dụ món sửa xong rồi hỏng lại) thì có `since` mới, nên lại là việc mới.

`since` của việc vừa vào khung do người dùng vừa sửa dữ liệu (ví dụ đặt `warrantyUntil` là tuần sau) bằng **thời điểm đặt giá trị đó**, không phải đầu ngày `warrantyUntil − 30` đã qua. Nếu không thì việc đó sẽ không bao giờ là "mới". Cách lấy: `since` = `max(mốc theo bảng L5, lúc giá trị gây ra việc được đặt)`. "Lúc được đặt" là `changed_at` của version mới nhất mà giá trị đó khác version liền trước, tính riêng cho từng trường:
- `WARRANTY`: `warrantyUntil`;
- `SERVICE`: `serviceEveryMonths`, cùng `updated_at` muộn nhất trong các dòng `SERVICE` của món, kể cả dòng đã xoá;
- `CUSTOM_DATE`: giá trị của mục đó trong `attributes`, cùng `updated_at` của định nghĩa mục;
- `MOVE_OUT`: `updated_at` của dòng cư trú, vì cư trú không có version.

Không lấy `updated_at` của cả bản ghi (tôi chọn khi lập kế hoạch): nếu lấy, chỉ cần đổi tên một món là chuông lại báo việc bảo hành của nó.

## 4. Cách làm

### 4.1 Hướng đã cân nhắc

| Việc | Hướng | Được | Mất |
|---|---|---|---|
| Việc cần làm và chuông | **a. Tính khi đọc; chuông so `since` với một mốc `seenAt` mỗi người** (đã chọn) | Không có scheduler, không có bảng thông báo phải dọn; số luôn khớp dữ liệu | Không ẩn được từng việc; mỗi lần đọc chạy năm truy vấn |
| | b. Bảng thông báo, một việc chạy hằng đêm sinh dòng | Ẩn, đánh dấu đã đọc từng dòng | Thêm scheduler (và khoá khi chạy nhiều bản), dữ liệu lệch khi sửa giữa ngày |
| Hạn giấy tờ | **a. `remindDays` trên mục tuỳ biến kiểu ngày** (đã chọn) | Một cơ chế cho mọi loại hạn, của cả ba đích | Đọc ngày trong JSON `attributes` |
| | b. Cột cố định `id_expires_on` cho người | Truy vấn đơn giản | Chỉ dùng được cho một loại hạn |
| Lịch sử nhật ký | **a. Version riêng `MAINTENANCE`** (đã chọn) | Sửa dòng bảo trì không chạm `version` của món đồ; có trong Hoạt động | Hoạt động cần biết món đồ của dòng để dẫn link |
| | b. Ghi vào version của món đồ | Không thêm loại version | Mỗi lần ghi bảo trì làm tăng `version` món, gây 409 cho người đang sửa món |

### 4.2 Backend (registry-service)

**Migration `V8__maintenance_and_reminders.sql`:**
- bảng `reg_maintenance` (`workspace_id`, `id`, `item_id`, `kind`, `done_on`, `work`, `done_by`, `note`, các cột kiểm toán, `version`, `deleted_at`), khoá `(workspace_id, id)`, index `(workspace_id, item_id, done_on)`, khoá ngoại tới `reg_item`;
- `reg_item` thêm `warranty_until DATE NULL`, `service_every_months TINYINT UNSIGNED NULL`, cùng index `(workspace_id, warranty_until)`;
- `reg_field_def` thêm `remind_days SMALLINT UNSIGNED NULL`;
- bảng `reg_todo_seen` (`workspace_id`, `user_id`, `seen_at DATETIME(3)`), khoá `(workspace_id, user_id)`.

**Đồ đạc.** `warrantyUntil`, `serviceEveryMonths` đi qua `Item`, `ItemSnapshot`, `ItemRequest`, `ItemUpdateRequest` (`@Min(1) @Max(120)`), `ItemDetailResponse`, câu ghi của repository và sheet "Đồ đạc" của file Excel (cột **Hết bảo hành**, **Bảo trì (tháng)**). `ItemDetailResponse` có thêm `nextServiceOn` (L3, `null` khi chưa đặt chu kỳ). Version cũ không có hai trường này thì đọc ra là `null`.

**Nhật ký, gói `maintenance` mới:**
- `GET /maintenance?itemId=&cursor=&limit=`: các dòng chưa xoá của một món, `doneOn` mới nhất trước, rồi id;
- `POST /maintenance` (`itemId`, `kind`, `doneOn`, `work`, `doneBy`, `note`, `markFixed`, `itemVersion`) theo L1 và L2; trả dòng vừa tạo (frontend tải lại món đồ để có `version` mới);
- `PUT /maintenance/{id}` (có `version`), `DELETE /maintenance/{id}?version=`, `POST /maintenance/{id}/restore` (có `version`).

Thứ tự khoá trong transaction: dòng bảo trì, rồi món đồ, giống cách dịch vụ sở hữu khoá dòng rồi khoá địa điểm.

**Mục tuỳ biến.** `remindDays` có trong `FieldDef`, request tạo, request sửa, response; kiểm tra theo L4.

**Việc cần làm, gói `todo` mới:**
- `GET /todos?kind=&cursor=&limit=`: trang `{ key, kind, dueOn, since, isNew, subjectType, subjectId, subjectName, subjectCode, context }`, sắp theo L5. `key` là chuỗi duy nhất của việc, dùng cho con trỏ và cho React. `subject*` là hồ sơ cần mở: món đồ với `BROKEN`, `WARRANTY`, `SERVICE`; **người** với `MOVE_OUT`; bản ghi mang mục tuỳ biến với `CUSTOM_DATE`. `context` là chữ phụ: tên địa điểm của món hoặc của dòng cư trú, hoặc nhãn của mục tuỳ biến. Tính trong Java: mỗi nguồn một truy vấn trả mọi dòng trong khung, gộp lại, sắp, rồi cắt trang theo con trỏ `(dueOn, kind, key)`.
- `GET /todos/summary`: `{ total, byKind, newCount, latest[] }`, trong đó `latest` là tối đa 10 việc có `since` mới nhất, dùng cho chuông và thanh bên.
- `POST /todos/seen`: đặt `seenAt` = bây giờ cho người đang gọi (`WorkspaceContext.userId`), trả `204`.

Nguồn `BROKEN` lấy ngày hỏng từ version mới nhất của món có `snapshot.status = 'BROKEN'` mà version liền trước không phải `BROKEN` (hoặc là `CREATE`). Không tìm được thì dùng `updated_at` của món. Nguồn `CUSTOM_DATE` chạy một truy vấn cho mỗi mục có `remindDays`, đọc `JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.<field_key>'))`. `field_key` đã được giới hạn `[a-z][a-z0-9_]*` nên ghép vào đường dẫn JSON an toàn, nhưng vẫn truyền bằng tham số khi được.

**Hoạt động.** `ActivityService` tra tên cho `MAINTENANCE`, dạng "<tên món>: <work>". `ActivityResponse` có thêm `parentId` (id món đồ với `MAINTENANCE`; `null` với loại khác) để frontend dẫn link. Bộ lọc loại bản ghi có thêm `MAINTENANCE`.

### 4.3 Frontend

- **Form món đồ:** ô ngày "Hết bảo hành" (`DateInput`), ô số "Bảo trì mỗi … tháng". Thẻ Thông tin của món có hai dòng: "Bảo hành tới 12/03/2027 · còn 108 ngày" (hoặc "đã hết", nhãn màu theo `ToneBadge`), "Bảo trì mỗi 6 tháng · lần tới 01/02/2027" (quá hạn thì màu cảnh báo). So sánh version có thêm hai nhãn.
- **Tab "Bảo trì"** trên trang món đồ, giữa "Ảnh" và "Lịch sử" (`?tab=maintenance`). Gồm nút **Ghi bảo trì** và danh sách mới nhất trước. Mỗi dòng có ngày, nhãn loại (Sửa chữa, Bảo trì), việc, người làm, ghi chú, và menu Sửa, Xoá. Hộp thoại ghi có: loại (`toggle-group`, mặc định Sửa chữa khi món Hỏng, ngược lại là Bảo trì), ngày (mặc định hôm nay), việc, người làm, ghi chú. Món đang Hỏng và loại là Sửa chữa thì có ô "Đã sửa xong, chuyển về Đang dùng", đánh sẵn. Xoá xong có thông báo kèm **Hoàn tác** trong 8 giây (như mục 4.4 của thiết kế tìm và lấy lại). Trạng thái rỗng: "Chưa có lần sửa hay bảo trì nào".
- **Danh mục, mục tuỳ biến:** với kiểu Ngày, thêm công tắc "Nhắc trong Việc cần làm" và ô "trước … ngày" (mặc định 30).
- **Trang Việc cần làm** `/w/:ws/todos`, trên thanh bên ngay dưới "Tổng quan", biểu tượng danh sách có dấu tích. Số tổng dùng `SidebarMenuBadge`, ẩn khi bằng 0. Lọc nhanh theo loại bằng `toggle-group` ("Tất cả", Hỏng, Bảo hành, Bảo trì, Chuyển đi, Hạn khác), mỗi nút kèm số; bộ lọc nằm trên URL. Mỗi dòng có biểu tượng loại, tên hồ sơ (bấm để mở; việc bảo trì mở `?tab=maintenance`), chữ phụ, và nhãn hạn: "quá hạn 3 ngày", "hôm nay", "còn 12 ngày", "hỏng từ 20/11". Có "Tải thêm". Rỗng: "Không có việc gì cần làm". Trên điện thoại, mỗi dòng thành một thẻ.
- **Chuông** ở bên phải thanh đầu trang: nút biểu tượng chuông, có chấm số khi `newCount > 0` ("9+" khi nhiều hơn 9). Bấm mở `popover`: tối đa 10 việc mới nhất theo `since`, việc mới có chấm, và "Xem tất cả việc cần làm" ở cuối. Mở popover thì gọi `POST /todos/seen` và cập nhật số tại chỗ. Rỗng: "Chưa có gì mới".
- **Làm mới:** tóm tắt việc cần làm được tải lại sau các lệnh ghi đồ đạc, cư trú, bảo trì, mục tuỳ biến, khi quay lại tab trình duyệt, và mỗi 5 phút.
- **Hoạt động:** dòng `MAINTENANCE` có nhãn "Bảo trì" và dẫn tới `/items/{parentId}?tab=maintenance`. Không có nút khôi phục cho dòng này (khôi phục qua Hoàn tác).
- Registry bản cũ chưa có `/todos` thì ẩn mục trên thanh bên và chuông, như cách đã làm với `activityByDay`.

## 5. Kiểm tra

- **Backend (IT trên MySQL thật, trong transaction rollback như hiện nay):**
  - L1: kiểm tra trường, ngày trong tương lai, món đã xoá, sửa, xoá, khôi phục, version `MAINTENANCE`;
  - L2: sửa xong đổi trạng thái và ghi version món; `itemVersion` lệch thì không ghi gì; `markFixed` sai trường hợp thì trả 400;
  - L3: hạn kế tiếp từ dòng `SERVICE`, từ `placedAt`, từ ngày tạo; dòng `REPAIR` không làm lùi hạn; cộng tháng ở cuối tháng (31/01 + 1 tháng);
  - L4: `remindDays` sai kiểu thì trả 400; giá trị ngày sai thì bỏ qua;
  - L5: hai đầu của mọi khung, mỗi điều kiện loại trừ (đã xoá, đã gỡ, địa điểm đã xoá), thứ tự sắp, phân trang, lọc theo `kind`;
  - L6: `isNew` và `newCount` trước và sau `POST /todos/seen`, việc vào khung do sửa dữ liệu, hai người dùng có mốc riêng;
  - hai cột mới có trong lịch sử, khôi phục version, Excel; Hoạt động có `MAINTENANCE` kèm `parentId`.
- **Frontend (Vitest):** form món đồ (hai ô mới), tab Bảo trì (ghi, đã sửa xong, sửa, xoá, hoàn tác, rỗng), ô nhắc của mục tuỳ biến, trang Việc cần làm (lọc, nhãn hạn, link, rỗng), chuông (số, popover, gọi `seen`, registry cũ), thanh bên có số.
- **Xem bằng mắt** ở khổ 1440, 1920 và 390 (điện thoại), cả nền sáng và nền tối, bằng bộ preview riêng.

## 6. Các quyết định nhỏ tôi chốt khi đọc lại (tôi chọn)

- Món đồ bị xoá thì nhật ký của nó ẩn theo; lệnh ghi lên dòng của món đã xoá trả 409 (L1).
- Chỉ mục tuỳ biến đang `ACTIVE` mới nhắc (L4).
- Việc `MOVE_OUT` mở trang của người, chữ phụ là địa điểm (4.2).
- Nhật ký bảo trì không vào Thùng rác và file Excel; lấy lại dòng xoá nhầm bằng Hoàn tác (mục 2).

## 7. Để lại cho người làm

Tên class, cách chia task, query key, vị trí file theo cấu trúc sẵn có: `features/maintenance` và `features/todos` mới; chuông đặt trong `features/todos` và gắn vào `AppLayout`. Biểu tượng, câu chữ phụ của từng loại việc, cỡ chấm số trên chuông có thể chỉnh khi xem bằng mắt. Cách sinh lại kiểu API cho frontend làm như các đợt trước.
