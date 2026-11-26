# Frontend

Giao diện web của home-platform: cây toà nhà › tầng › phòng và trang của từng địa điểm. Thiết kế ở
`docs/superpowers/specs/2026-11-13-frontend-structure-design.md`.

React 19, Vite 8, TypeScript, Tailwind 4, component shadcn (kiểu base-ui) trong `src/components/ui`.
Dữ liệu server qua TanStack Query, điều hướng bằng React Router, form bằng React Hook Form + Zod.

## Chạy

Cần Node 20.19 trở lên. `node_modules` cài bằng Node của Windows; trong WSL thì gọi qua nó, ví dụ
`cmd.exe /c "npm test"` (đứng ở thư mục này), vì các gói có mã máy (rolldown, oxlint, tailwind) chỉ có
bản Windows.

```bash
npm install
npm run dev          # http://localhost:5173, cần identity-service (8081) và registry-service (8082) đang chạy
npm test             # Vitest, chạy một lần; npm run test:watch để chạy theo khi sửa
npm run build        # kiểm tra kiểu rồi đóng gói vào dist/
npm run lint         # oxlint
```

Trong lúc phát triển, trình duyệt gọi `/api/identity/...`, `/api/registry/...`, `/api/media/...`; Vite
chuyển chúng tới service tương ứng và bỏ tiền tố (`vite.config.ts`). Biến môi trường `HP_IDENTITY_PORT`,
`HP_REGISTRY_PORT`, `HP_MEDIA_PORT` đổi cổng đích, để chạy một bộ service thứ hai song song với bộ đang
dùng (ví dụ registry `--server.port=18082` và `npm run dev -- --port 15173`).

Chưa có đăng nhập: trang đầu cho tạo người dùng hoặc nhập `id` của người dùng đã có. `id` lưu ở
`localStorage` (`hp.userId`) và được gửi trong header `X-User-Id`; mọi lời gọi registry gửi thêm
`X-Workspace-Id` của không gian trên URL.

## Kiểu dữ liệu từ API

`src/api/schema/` giữ tài liệu OpenAPI của từng service (`identity.json`, `registry.json`) và kiểu
TypeScript sinh từ chúng (`*.d.ts`). Không sửa tay các file này.

```bash
npm run api:sync     # tải lại OpenAPI từ hai service đang chạy rồi sinh kiểu; commit kết quả
npm run api:types    # chỉ sinh lại kiểu từ JSON đã có
```

API đổi mà quên chạy `api:sync` thì `npm run build` sẽ báo lỗi kiểu ở chỗ gọi.

OpenAPI của backend chưa ghi trường nào là bắt buộc, nên trong kiểu sinh ra mọi trường đều tuỳ chọn.
Mỗi feature khai báo một lần những trường luôn có bằng `Present`, trong `types.ts` của nó:

```ts
export type PlaceRef = Present<RegistrySchemas['PlaceRef'], 'id' | 'name'>
// id: string; name: string. Trường không nằm trong danh sách có kiểu `... | null`.
```

`id` là số 64 bit nên luôn là chuỗi, ở mọi nơi. Không bao giờ `Number(id)`. Một `id` lấy từ URL phải
qua `isId` (`src/lib/ids.ts`) trước khi gửi đi.

## Cấu trúc

```
src/
├── app/            khởi động (main.tsx), cây route (router.tsx), phiên (session/), khung trang (layout/)
├── api/            client có kiểu, ApiError, phân trang (useCursorList), cấu hình cache, kiểu sinh tự động
├── features/       mỗi nghiệp vụ một thư mục
│   ├── users/          tạo và kiểm tra người dùng
│   ├── workspaces/     danh sách, tạo không gian
│   ├── catalog/        trang Danh mục: loại địa điểm, nhóm đồ, mục tuỳ biến (cả phần xem và ô nhập dùng chung)
│   ├── places/         cây, trang địa điểm, thêm, sửa, di chuyển, xoá, địa chỉ
│   ├── people/         danh sách và hồ sơ người, cư trú (tab Người ở của địa điểm, nơi ở của một người),
│   │                   dòng thời gian cư trú, cảnh báo vượt sức chứa trong form cư trú
│   ├── ownership/      tab Sở hữu của địa điểm và của một người: thêm chủ, sửa, thôi sở hữu, chuyển nhượng
│   ├── items/          tab Đồ đạc của địa điểm, trang món đồ: thêm, sửa, chuyển chỗ, gỡ, thay, đưa lại, xoá
│   ├── history/        tab Lịch sử dùng chung: danh sách version, xem và khôi phục một version, so sánh hai version
│   ├── activity/       trang Hoạt động (lọc theo loại, hành động, ngày, "chỉ việc tôi làm") và Thùng rác
│   ├── overview/       trang tổng quan của không gian (trang chủ `/w/:ws` khi đã có địa điểm)
│   ├── media/          tab Ảnh dùng chung (lưới, tải nhiều ảnh, xem lớn, chú thích, sắp xếp, gỡ, ảnh bìa/đại diện)
│   ├── vacancies/      trang Chỗ trống (`/w/:ws/vacancies`): chỗ còn xếp người, lọc theo loại và toà, nút Xếp người
│   ├── maintenance/    tab Bảo trì của món đồ: nhật ký sửa chữa, bảo trì; ghi, sửa, xoá có Hoàn tác
│   ├── todos/          trang Việc cần làm (`/w/:ws/todos`), số trên thanh bên, chuông ở đầu trang
│   ├── exports/        nút tải file Excel (`ExportButton`, cả file mẫu nhập) và tên file
│   ├── imports/        trang Nhập từ Excel (`/w/:ws/import`): chọn file, xem trước từng dòng, nhập
│   ├── print/          trang in hồ sơ một địa điểm (`/w/:ws/places/:id/print`, ngoài khung trang)
│   └── search/         ô tìm chung (Ctrl+K): địa điểm, người, đồ đạc theo tên và theo mã
├── components/
│   ├── ui/         component shadcn, không sửa tay; thêm bằng `npx shadcn add <tên>`
│   └── common/     component dùng chung tự viết: ô nhập có nhãn, lỗi, trạng thái trống, "Tải thêm",
│                   nhãn có màu (ToneBadge), avatar chữ cái đầu, khối thông tin (InfoCard), chú thích (WithTooltip)
├── hooks/, lib/    hook và hàm nhỏ dùng chung (định dạng ngày giờ, kiểm tra id)
└── test/           cấu hình Vitest, server giả (MSW), dữ liệu mẫu, backend giả của địa điểm
```

Mỗi feature có:

- `api.ts`: hàm gọi API. Đây là nơi duy nhất gọi `identity` hay `registry`.
- `queries.ts`: khoá cache và hook đọc, ghi. Component chỉ dùng hook, không gọi `api.ts`.
- `types.ts`: kiểu response (`Present<...>`).
- `components/`, `pages/`; `index.ts` là những gì feature khác được dùng.

Luật phụ thuộc:

- Một feature chỉ nhập feature khác qua `index.ts` của nó.
- `features/` không nhập gì từ `app/`; `api/`, `components/`, `lib/` không nhập gì từ `features/` hay `app/`.
- `places` không nhập feature nào dựa trên nó. Tab của trang địa điểm thuộc nghiệp vụ khác (Người ở,
  Đồ đạc) được `app/router.tsx` ghép vào qua prop `tabs` (`PlaceTabSlots`).
- `people` không nhập `items` hay `ownership`; tab "Sở hữu" và "Đồ đạc" của trang một người cũng được ghép vào
  như vậy (`PersonTabSlots`). `places` không nhập `ownership`; tab "Sở hữu" của địa điểm là một slot.
- `search` và `activity` đứng trên ba feature bản ghi (places, people, items) và nhập chúng qua `index.ts`;
  chiều ngược lại không có.
- `overview` và `print` cũng đứng trên các feature bản ghi (print ghép places, people, ownership, items,
  catalog); không feature nào nhập chúng, chỉ `app/`.
- `media` là lá: không nhập feature nào. `places`, `people`, `items` nhập `thumbnailUrl` của nó cho ảnh bìa, avatar;
  tab Ảnh của địa điểm và người là slot ghép ở `app/photos/` (nối `PhotosTab` với `useSetPlaceCover`,
  `useSetPersonAvatar`), trang món đồ tự gắn `ItemPhotosTab`.
- `vacancies` đứng trên `places`, `people`, `catalog`; không feature nào nhập nó. Ghi cư trú hay địa điểm làm
  mới khoá `['vacancies', ws]` bằng chữ (không nhập `vacancies`) để không có vòng phụ thuộc.
- `maintenance` nhập `items` (kiểu `ItemDetail`, `itemKeys`, `useFetchFreshItem`); trang món đồ không nhập nó,
  tab Bảo trì là slot `ItemTabSlots` ghép ở `app/router.tsx`.
- `todos` không nhập feature nào; `app/layout/AppLayout.tsx` gắn chuông và số trên thanh bên.
- `imports` chỉ nhập `exports` (`ExportButton` cho nút Tải mẫu). Trang Tổng quan và trang chủ của không gian
  trống chỉ dẫn link tới `/w/:ws/import`, không nhập `imports`.
- `exports` chỉ nhập `workspaces` (tên không gian cho tên file); trang Đồ đạc và trang Người nhập `ExportButton`.
- `components/ui` không biết gì về nghiệp vụ.

## Cây địa điểm

Cây theo mẫu ARIA "tree": Tab vào cây rồi dùng ↑ ↓ để đi giữa các dòng đang hiện, → mở nhánh (lần
nữa thì xuống con đầu), ← đóng nhánh (lần nữa thì lên cha), Home và End tới dòng đầu, dòng cuối, Enter
mở trang của dòng, gõ một chữ để nhảy tới dòng kế có tên bắt đầu bằng chữ đó (không phân biệt dấu).
Chỉ một dòng nằm trong thứ tự Tab (`useTreeKeyboard`). Một nhánh chỉ được tải khi mở, và nút mà
backend báo `hasChildren: false` không có mũi tên.

## Sau một lệnh ghi

Hook ghi tự làm mới cache. Với địa điểm, `refreshAfterWrite` (`features/places/queries.ts`) đặt chi
tiết bằng response rồi làm cũ:

| Lệnh | Làm cũ |
|---|---|
| Thêm | mọi danh sách con (cha có thể vừa có con đầu tiên), kết quả tìm kiếm |
| Sửa, đặt hoặc gỡ địa chỉ | như trên, cộng mọi trang chi tiết khác (đường dẫn tổ tiên và địa chỉ thừa hưởng của con cháu) |
| Di chuyển | cả cây của không gian |
| Xoá | mọi danh sách con (cha có thể vừa thành lá), kết quả tìm kiếm; rời trang rồi mới bỏ chi tiết khỏi cache |

Với người và cư trú (`features/people/queries.ts`):

| Lệnh | Làm cũ |
|---|---|
| Thêm, sửa, chuyển đi, xoá một dòng cư trú | mọi danh sách cư trú (của địa điểm, của cả toà, của người) và chi tiết của người đó |
| Sửa một người | danh sách người, mọi danh sách cư trú (dòng mang tên người) |
| Xoá một người | như trên; rời trang rồi mới bỏ chi tiết khỏi cache |

Với đồ đạc (`features/items/queries.ts`), mọi lệnh ghi đặt chi tiết của (các) món nó trả về và làm cũ
mọi danh sách đồ: một lệnh chuyển chỗ hay thay ở chỗ khác chạm hai địa điểm và các toà chứa chúng.
Xoá thì rời trang rồi mới bỏ chi tiết khỏi cache. "Đổi chủ" cũng vậy: danh sách đồ của người cũ và người mới
đều nằm dưới `itemKeys.lists`.

Với sở hữu (`features/ownership/queries.ts`), mọi lệnh ghi làm cũ mọi danh sách sở hữu của không gian: một lần
chuyển nhượng chạm hai người.

Danh mục (`features/catalog`): mọi lệnh ghi làm cũ cả danh mục của không gian, vì loại, nhóm và mục
tuỳ biến được nhiều form đọc. Khôi phục một version (`features/history`) là một lần sửa: feature của bản
ghi tự làm mới như sau một lần sửa (`onRestored`). Khôi phục thứ đã xoá từ trang Hoạt động gửi `version`
bằng số version của dòng "Xoá" và làm mới như sau một lần tạo.

Ô chọn mà danh sách lựa chọn tải sau khi form dựng (nhóm đồ) phải có điều khiển (`Controller`): ô không
điều khiển rơi về lựa chọn đầu tiên và gửi đi giá trị đó.

Mã của địa điểm và đồ đạc do backend cấp khi tạo: form không có ô mã (`CodeNote` chỉ báo mã sẽ được
cấp, hoặc hiện mã đã khoá khi sửa), thông báo sau khi thêm kèm mã. "Cấp mã mới" là thao tác riêng
trong menu `⋯` (`ReissueCodeDialog`) và làm mới như một lần sửa. Phần "Mã tự cấp" của trang Danh mục
đặt dạng mã; "Cấp mã" cho bản ghi cũ chạy từng lô rồi làm cũ mọi cache, vì mọi danh sách và chi tiết
đều mang mã.

"Hôm nay" (ngày mặc định khi chuyển đến, chuyển đi, gỡ đồ) tính theo giờ Việt Nam như backend
(`todayInBusinessZone`, `src/lib/dates.ts`), không theo múi giờ của máy.

Lỗi 409 "đã có người sửa" (`ApiError.isVersionConflict`) hiện `VersionConflictAlert`: form giữ chữ đã
nhập, "Tải bản mới" lấy bản mới nhất và dùng `version` của nó.

## Thêm một feature

1. Tạo `src/features/<tên>/` với `types.ts`, `api.ts`, `queries.ts`, `index.ts`.
2. Viết test trước (`*.test.tsx` cạnh code). Test dựng cả ứng dụng bằng `renderApp({ route, userId })`
   (`src/test/render.tsx`) và trả lời API bằng MSW (`server.use(...)`); request không có handler làm test
   hỏng. `mockShell()` (`src/test/handlers.ts`) trả lời những request mà khung trang luôn gọi.
3. Thêm route trong `src/app/router.tsx`.

## Tìm chung và hoàn tác

Ctrl+K (⌘K) mở ô tìm chung ở mọi trang (`features/search`). Ô này chờ 300 ms sau phím cuối rồi gọi song
song các API tìm có sẵn (`/places/search`, `/people/search`, `/items/search`, mỗi loại 5 kết quả; số
điện thoại khi chuỗi chỉ có chữ số), gộp bằng `groupResults`: khớp mã đứng đầu và không lặp lại ở nhóm
theo tên. Kết quả nằm dưới khoá danh sách của từng feature, nên lệnh ghi nào làm cũ danh sách cũng làm
cũ kết quả tìm.

Xoá một địa điểm, người hay món đồ hiện thông báo có nút "Hoàn tác" trong 8 giây (`deletedToast`).
Hoàn tác gửi lệnh khôi phục với `version` lúc xoá cộng 1; bản ghi đã đổi sau lần xoá thì báo rõ, không
thử lại.

## Tổng quan, Excel, in, lọc và so sánh

- **Tổng quan** (`features/overview`): một request `GET /overview` cho mọi con số; số theo trạng thái đồ
  là liên kết tới `/w/:ws/items?status=…`. Query đánh dấu `meta.refreshAfterWrites` được làm cũ sau mọi
  lệnh ghi thành công (`api/queryClient.ts`), nên khôi phục từ "Hoạt động gần đây" cập nhật ngay các số.
- **Excel** (`features/exports`, `api/download.ts`): `downloadFile` tải bằng `fetch` với header của không
  gian, rồi lưu blob dưới tên `<tên không gian bỏ dấu>-<toan-bo|do-dac|nguoi>-<yyyy-MM-dd>.xlsx` (ngày
  theo giờ Việt Nam). Luồng bị cắt giữa chừng thì không lưu gì.
- **In hồ sơ** (`features/print`): `useAllPages` gọi "tải thêm" tới khi hết trang; nút "In" chỉ bật khi mọi
  danh sách đã đủ. `print:hidden` ẩn nút và công tắc khi in; khối `@media print` ở `src/index.css` giữ
  nguyên một dòng bảng trên một trang.
- **Lọc hoạt động** (`features/activity/filters.ts`): bộ lọc nằm trên URL
  (`?type=&action=&mine=1&from=yyyy-MM-dd&to=yyyy-MM-dd`), giá trị lạ bị bỏ qua. `toQuery` đổi ngày sang mốc
  theo giờ máy: `from` là 00:00 của ngày đó, `to` là 00:00 của ngày hôm sau (backend không tính mốc `to`).
- **So sánh version** (`features/history`): mỗi tab Lịch sử đưa cho `HistoryTab` một hàm `snapshotRows` biến
  một bản thành các dòng `{ key, label, value, raw }`; hộp xem một version và hộp so sánh dùng chung các
  dòng đó. `compareRows` ghép hai bản theo `key` và so `raw` theo nội dung. Mục tuỳ biến lấy bằng
  `attributeRows` với mục của chính loại (nhóm) ở bản đó (`defsOfType`).

## Chỗ ở và sức chứa

Thiết kế: `docs/superpowers/specs/2026-11-25-capacity-and-vacancies-design.md`.

- **Sức chứa** là ô số trong form địa điểm (0 tới 9999, bỏ trống là chưa đặt; bỏ trống thì không gửi
  `capacity`, PUT ghi đè nên sức chứa bị bỏ). `CapacityLine` hiện "3/4 người · còn 1 chỗ" (success), "đã đầy"
  (warning), "vượt" (danger); `capacityText` cho chữ không màu (trang in, trang Chỗ trống).
- **Cảnh báo vượt** (`useCapacityCheck`): form cư trú gọi `GET /places/{id}/capacity-check` 300 ms sau lần đổi
  ngày hay địa điểm cuối; khi `peak + 1 > capacity` hiện `CapacityWarning`. Nút lưu không bị khoá.
- **Dòng thời gian** (`timelineLayout` trong `features/people/timeline.ts`, hàm thuần): trục từ ngày bắt đầu
  sớm nhất tới `max(hôm nay, ngày cuối muộn nhất)`, đệm 7 ngày; tối đa 8 vạch tháng (năm gõ nhầm như 2999
  không sinh hàng nghìn vạch); vị trí theo %. `OccupancyTimelineCard` tự tải `GET /occupancies/timeline`.
- **Sắp chuyển đi** trên trang Tổng quan đọc `endingSoon` của `GET /overview`; registry cũ không có trường
  này thì thẻ ẩn đi.

## Ảnh

Thiết kế: `docs/superpowers/specs/2026-11-25-photos-design.md`.

- **Đường dẫn ảnh** (`features/media/urls.ts`): `/api/media/workspaces/<ws>/files/<id>` và `/thumbnail`. Không
  gian nằm trên đường dẫn vì `<img>` không gửi được header; trình duyệt giữ ảnh một năm (nội dung một file
  không đổi). Gọi API (danh sách, tải lên, chú thích, sắp xếp, gỡ) qua client `media` như các service khác.
- **Tải lên** (`PhotosTab`): kiểm tra kiểu (JPEG, PNG, WebP) và cỡ (10 MB) phía client, rồi gửi lần lượt, mỗi
  file một dòng trạng thái; lỗi server đổi sang chữ tiếng Việt theo mã (400, 413, 409) ở `uploadErrors.ts`.
  `ownerType`, `ownerId`, `caption` đi trên query, chỉ file nằm trong multipart.
- **Ảnh bìa, đại diện**: `PhotosTab` nhận `highlight` (`fileId`, nhãn, `onSet`). Ghi bằng lệnh sửa của bản ghi
  (một version mới, gửi lại đủ các trường). Gỡ đúng ảnh đang là bìa thì bỏ luôn ảnh bìa. Ảnh không tải được
  thì `CoverImage` ẩn đi, `PersonAvatar` quay về chữ cái đầu.
- **Test**: jsdom không giữ nguyên `File` khi `FormData` đi qua `Request` của undici, nên test tải lên chỉ kiểm
  multipart, query và có phần `file`; tải ảnh thật được kiểm bằng Chrome trong bộ preview.

## Bảo trì và nhắc việc

Thiết kế: `docs/superpowers/specs/2026-11-25-maintenance-and-reminders-design.md`.

- **Món đồ** có thêm "Hết bảo hành" và "Bảo trì mỗi (tháng)" (1 tới 120). Thẻ Thông tin hiện "Bảo hành tới …"
  (`warrantyLine`: còn ≤ 30 ngày hay hết hôm nay thì warning) và "Bảo trì mỗi N tháng · lần tới …" (`nextServiceOn`
  của backend; tới hạn thì warning).
- **Tab Bảo trì** (`MaintenanceTab`): loại Sửa chữa hay Bảo trì; món đang Hỏng thì mặc định Sửa chữa và đánh sẵn
  "Đã sửa xong, chuyển về Đang dùng" (gửi `markFixed` cùng `itemVersion`). 409 vì món vừa đổi thì hiện
  `VersionConflictAlert`, "Tải bản mới" lấy `version` mới và giữ chữ đã gõ. Xoá không hỏi lại; Hoàn tác 8 giây.
- **Mục tuỳ biến kiểu Ngày** có công tắc "Nhắc trong Việc cần làm" và "Nhắc trước (ngày)" (mặc định 30).
- **Việc cần làm** (`features/todos`): danh sách do backend tính, gấp nhất trước; lọc theo loại trên URL
  (`?kind=`). `dueLabel` cho nhãn hạn (quá hạn danger, trong 7 ngày warning). Danh sách và tóm tắt dùng
  `refreshAfterWrites`; tóm tắt còn tải lại mỗi 5 phút và khi quay lại tab. Registry cũ (404) thì ẩn mục trên
  thanh bên và chuông.
- **Chuông** (`TodoBell`): chấm số là `newCount` (tối đa "9+"). Mở chuông gọi `POST /todos/seen`, đặt số về 0
  ngay; danh sách trong popover giữ nguyên lúc mở để chấm "mới" còn tới khi đóng.

## Nhập từ Excel

Thiết kế: `docs/superpowers/specs/2026-11-26-excel-import-design.md`.

- **Ba bước** (`ImportPage`): ① chọn file (`FilePicker`: nút Tải mẫu, ô chọn file `.xlsx`, kéo thả; file trên
  5 MB bị từ chối ngay ở trình duyệt), ② xem trước (`ReportView`), ③ nhập (`ImportResult`). Vào từ thanh bên,
  từ nút "Nhập Excel" ở Tổng quan, và từ trang chủ của không gian chưa có địa điểm.
- **Xem trước** gửi file tới `POST /imports/preview`; backend chạy thử mọi dòng rồi huỷ. Báo cáo gồm dải số theo
  sheet, lỗi cả file (`Alert` đỏ), ghi chú, rồi một tab mỗi sheet với bảng Dòng · Hồ sơ · Kết quả (200 dòng một
  lần, "Hiện thêm"). Lời nhắn lỗi và cảnh báo có biểu tượng và chữ ẩn "Lỗi:" / "Cảnh báo:", không chỉ khác màu.
  Còn lỗi thì nút Nhập khoá.
- **Nhập** gửi lại đúng file đã chọn tới `POST /imports` (đọc lại file trước khi gửi: file trên máy đổi thì báo
  "Không đọc lại được file"). Backend kiểm lại từ đầu; `committed: false` nghĩa là dữ liệu đổi từ lúc xem
  trước, trang quay về bước ② với báo cáo mới. Một ref chặn bấm hai lần. Đã lưu thì `invalidateQueries()` làm
  mới cả cache, vì lần nhập chạm mọi loại hồ sơ.
- Tên và nội dung file không đo được trong test (FormData của jsdom qua undici mất `File`); tên file thật được
  kiểm bằng trình duyệt: ghi chú version của hồ sơ nhập vào là "Nhập từ Excel: <tên file>".

## Giao diện

Thiết kế: `docs/superpowers/specs/2026-11-20-ui-refresh-design.md`.

- **Màu** là token trong `src/index.css` (sáng ở `:root`, tối ở `.dark`): xanh cổ vịt là màu chính, nền
  giấy lạnh, chữ màu mực. Không viết màu Tailwind thô (`amber-500`, `red-600`…) trong feature: dùng token.
- **Màu mang nghĩa** (`success`, `warning`, `danger`, `info`, `violet`, kèm bản `-foreground` để làm chữ
  trên nền nhạt `bg-success/12`). Bảng "trạng thái nào mang nghĩa nào" nằm cạnh bảng nhãn của feature:
  `ITEM_STATUS_TONE`, `PLACE_STATUS_TONE`, `OCCUPANCY_STATUS_TONE`, `OWNERSHIP_STATUS_TONE`, `ACTION_TONE`.
  Hiện bằng `<ToneBadge tone=…>`: nhãn vẫn ghi đủ chữ, màu chỉ giúp quét mắt; `data-tone` để test.
- **Chữ**: Be Vietnam Pro (`@fontsource/be-vietnam-pro`). Số đếm, mã, ngày dùng `tabular-nums`, không dùng
  font monospace.
- **Chế độ sáng tối** (`app/theme.ts`): Sáng, Tối, Theo máy; nhớ ở `localStorage` (`hp.theme`), "Theo máy"
  đổi ngay khi máy đổi. Thanh bên luôn tối: phần tử `Sidebar` mang lớp `dark`, nên mọi thứ bên trong dùng
  bộ token tối.
- **Component dùng chung**: `PersonAvatar` (chữ cái đầu, màu theo tên; ẩn với công cụ đọc màn hình),
  `InfoCard` và `InfoList` (khối có tiêu đề trên trang chi tiết, là một vùng có tên), `WithTooltip` (bọc
  nút chỉ có biểu tượng; nút vẫn cần `aria-label` cùng chữ), `UserChip` (feature users: "Bạn" hay "Người
  dùng khác" kèm avatar), `PersonHoverCard` (feature people: tên người, rê chuột thấy số điện thoại, chỉ tải
  hồ sơ khi mở), `DateInput` (ô ngày gõ được kèm nút lịch tiếng Việt; `TextField type="date"` tự dùng nó).
- **Điện thoại**: danh sách đồ đạc thành thẻ dưới mốc `sm` (cùng một bảng, chỉ đổi CSS).
- Trang in (`features/print`) giữ đen trắng.

