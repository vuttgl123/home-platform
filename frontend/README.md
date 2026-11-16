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
│   ├── catalog/        loại địa điểm, mục tuỳ biến (xem và ô nhập)
│   └── places/         cây, trang địa điểm, thêm, sửa, di chuyển, xoá, địa chỉ
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
- `components/ui` không biết gì về nghiệp vụ.

## Sau một lệnh ghi

Hook ghi tự làm mới cache. Với địa điểm, `refreshAfterWrite` (`features/places/queries.ts`) đặt chi
tiết bằng response rồi làm cũ:

| Lệnh | Làm cũ |
|---|---|
| Thêm | danh sách con của cha, kết quả tìm kiếm |
| Sửa, đặt hoặc gỡ địa chỉ | như trên, cộng mọi trang chi tiết khác (đường dẫn tổ tiên và địa chỉ thừa hưởng của con cháu) |
| Di chuyển | cả cây của không gian |
| Xoá | danh sách con của cha, kết quả tìm kiếm; rời trang rồi mới bỏ chi tiết khỏi cache |

Lỗi 409 "đã có người sửa" (`ApiError.isVersionConflict`) hiện `VersionConflictAlert`: form giữ chữ đã
nhập, "Tải bản mới" lấy bản mới nhất và dùng `version` của nó.

## Thêm một feature

1. Tạo `src/features/<tên>/` với `types.ts`, `api.ts`, `queries.ts`, `index.ts`.
2. Viết test trước (`*.test.tsx` cạnh code). Test dựng cả ứng dụng bằng `renderApp({ route, userId })`
   (`src/test/render.tsx`) và trả lời API bằng MSW (`server.use(...)`); request không có handler làm test
   hỏng. `mockShell()` (`src/test/handlers.ts`) trả lời những request mà khung trang luôn gọi.
3. Thêm route trong `src/app/router.tsx`.
