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
chuyển chúng tới service tương ứng và bỏ tiền tố (`vite.config.ts`).

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
│   ├── people/         danh sách và hồ sơ người, cư trú (tab Người ở của địa điểm, nơi ở của một người)
│   ├── ownership/      tab Sở hữu của địa điểm và của một người: thêm chủ, sửa, thôi sở hữu, chuyển nhượng
│   ├── items/          tab Đồ đạc của địa điểm, trang món đồ: thêm, sửa, chuyển chỗ, gỡ, thay, đưa lại, xoá
│   ├── history/        tab Lịch sử dùng chung: danh sách version, xem và khôi phục một version
│   ├── activity/       trang Hoạt động và Thùng rác: mọi thay đổi của không gian, khôi phục thứ đã xoá
│   └── search/         ô tìm chung (Ctrl+K): địa điểm, người, đồ đạc theo tên và theo mã
├── components/
│   ├── ui/         component shadcn, không sửa tay; thêm bằng `npx shadcn add <tên>`
│   └── common/     component dùng chung tự viết: ô nhập có nhãn, lỗi, trạng thái trống, "Tải thêm"
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
