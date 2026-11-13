# Thiết kế cấu trúc frontend

Ngày: 2026-11-13 · Trạng thái: **chờ duyệt**

Ký hiệu: **(bạn)** là điều bạn đã nói; **(tôi chọn)** là quyết định của tôi, bạn có thể bác.

Liên quan: logic nghiệp vụ `2026-11-13-business-rules-design.md`; API đã có ghi ở README (mục "Nghiệp vụ đã có").

## 1. Mục đích

Một giao diện web để làm việc với kho hồ sơ địa điểm đã có ở backend. Thiết kế cấu trúc trước để các màn hình sau này thêm vào đúng chỗ, không phải sắp lại. **(bạn)**

Người dùng đi theo cây **toà nhà › tầng › phòng**: chọn một địa điểm trên cây, xem và sửa mọi thứ của nó (thông tin, địa chỉ, chỗ con bên trong, người ở, đồ đạc, lịch sử). **(bạn)**

## 2. Mục tiêu, phần không làm, điều phải tránh

**Mục tiêu**

- Cấu trúc thư mục mà mỗi nghiệp vụ (địa điểm, người, đồ đạc, danh mục) nằm gọn một chỗ, giống cách chia package ở backend. **(tôi chọn)**
- Kiểu dữ liệu của request và response không lệch với backend: sinh tự động từ OpenAPI của từng service. **(tôi chọn)**
- Cây địa điểm tải từng nhánh khi mở, không tải cả cây một lần; chịu được toà nhà có hàng nghìn phòng. **(tôi chọn)**
- Mọi lỗi của API (Problem Details) hiện ra thống nhất: lỗi theo từng trường cạnh ô nhập, 409 "đã có người sửa" có nút tải lại. **(tôi chọn)**

**Không làm ở đợt này**

- Đăng nhập thật. Thay bằng màn hình chọn người dùng tạm (mục 5.1). **(bạn: phân quyền để sau)**
- Ảnh (media chưa có backend).
- Giao diện điện thoại tối ưu riêng: chạy được trên màn hình hẹp, nhưng thiết kế cho máy tính trước. **(tôi chọn)**
- Đa ngôn ngữ: giao diện tiếng Việt, chữ để thẳng trong code, gom về một chỗ sau nếu cần. **(tôi chọn)**

**Điều phải tránh**

- Gọi API rải rác trong component: mọi lời gọi đi qua lớp `api` của từng nghiệp vụ.
- Dữ liệu cũ sau khi ghi: sửa xong mà cây hay danh sách vẫn hiện tên cũ.
- Mất thao tác của người dùng khi gặp 409: form giữ nguyên chữ đã nhập.

## 3. Ràng buộc

- Giữ nền đã có: React 19, Vite 8, TypeScript, Tailwind 4, các component shadcn (kiểu base-ui) trong `src/components/ui`. Không thay thư viện giao diện.
- Ba service ở ba cổng (8081, 8082, 8083), gọi qua proxy của Vite trong lúc phát triển.
- Mọi `id` là chuỗi trong JSON (quy ước backend); frontend giữ nguyên dạng chuỗi, không đổi sang số.
- Mọi lệnh ghi cần `version` đã đọc; danh sách phân trang bằng `cursor`.

## 4. Cách làm đã chọn

| Hướng | Được | Mất |
|---|---|---|
| **1. Chia theo nghiệp vụ + thư viện điều hướng + thư viện dữ liệu server + kiểu sinh từ OpenAPI** (đã chọn) | Mỗi nghiệp vụ một chỗ; cache, tải lại sau khi ghi, trạng thái đang tải có sẵn; kiểu luôn khớp backend | Thêm vài thư viện; có bước sinh kiểu khi API đổi |
| 2. Chia theo loại file (`pages`, `components`, `services`) | Quen, ít quy ước | Một nghiệp vụ rải nhiều thư mục khi màn hình nhiều lên |
| 3. Tự viết `fetch` + `useEffect`, không thêm thư viện | Ít phụ thuộc | Tự làm cache và tải lại; dễ sai nhất ở cây mở từng nhánh |

Thư viện thêm vào **(tôi chọn)**:

| Việc | Thư viện | Lý do |
|---|---|---|
| Điều hướng trang | React Router (chế độ khai báo route bằng code) | Phổ biến nhất, đủ cho cây route ở mục 6 |
| Dữ liệu từ server | TanStack Query | Cache theo khoá, tải lại có chủ đích sau khi ghi, phân trang "tải thêm" có sẵn |
| Kiểu từ API | `openapi-typescript` (sinh kiểu) + `openapi-fetch` (gọi có kiểu) | Sinh từ `/v3/api-docs` của từng service; nhỏ, không sinh code chạy |
| Form | React Hook Form + Zod | Hợp với component `field` của shadcn; kiểm tra trước khi gửi |
| Thông báo | `toast` đã có trong `components/ui` | Không thêm thư viện |
| Test | Vitest + Testing Library + MSW | Chạy nhanh, giả lập API theo đúng dạng Problem Details |

Phiên bản cụ thể chốt lúc lập kế hoạch, sau khi kiểm tra trên npm.

## 5. Các phần chung

### 5.1 "Tôi là ai" (thay đăng nhập)

- Màn hình `/` khi chưa chọn người dùng: tạo người dùng mới (`POST /users`) hoặc nhập `id` người dùng đã có. `id` lưu ở `localStorage`. **(tôi chọn)**
- Mọi request tới identity gửi `X-User-Id`; mọi request tới registry gửi thêm `X-Workspace-Id` của không gian đang chọn.
- Gom vào một chỗ (`src/app/session`) để khi có đăng nhập thật chỉ phải đổi chỗ này.

### 5.2 Chọn không gian

- Ô chọn không gian ở đầu thanh bên: danh sách `GET /workspaces` của tôi, kèm nút "Tạo không gian".
- Tạo không gian = `POST /workspaces` (identity) rồi ngay sau đó `POST /setup` (registry), như luồng ghi trong README. **(tôi chọn: frontend làm hai bước này liền nhau, hiện một trạng thái "đang chuẩn bị")**
- Không gian đang chọn nằm trên URL (`/w/:workspaceId/...`), để chép đường dẫn gửi người khác vẫn mở đúng chỗ. **(tôi chọn)**

### 5.3 Gọi API

- Mỗi service một client có kiểu (`identity`, `registry`), tự gắn header ở 5.1 và đổi lỗi thành một kiểu `ApiError` có `status`, `detail`, `errors[]`.
- File OpenAPI của từng service được lưu vào repo (`src/api/schema/*.json`); một lệnh `npm run api:sync` tải lại chúng từ service đang chạy rồi sinh kiểu. Nhờ vậy build frontend không cần bật backend; khi API đổi thì chạy lệnh này và commit. **(tôi chọn)**
- Mỗi nghiệp vụ có file `api.ts` (hàm gọi) và `queries.ts` (khoá cache và hook đọc/ghi). Component chỉ dùng hook.
- Sau khi ghi: làm mới đúng các khoá liên quan. Ví dụ đổi tên một phòng thì làm mới chi tiết phòng, danh sách con của cha nó và nút của nó trên cây.

### 5.4 Lỗi

| Lỗi | Hiện ra |
|---|---|
| 400 có `errors[]` | Lỗi cạnh đúng ô nhập (kể cả `attributes.<khoá>` với mục tuỳ biến) |
| 404 | Trang "Không tìm thấy" trong khung chính; cây vẫn dùng được |
| 409 version lệch | Thông báo "Đã có người sửa trước bạn" + nút "Tải bản mới"; form giữ chữ đã nhập |
| 409 luật nghiệp vụ (còn con, trùng mã...) | Hiện nguyên `detail` của backend |
| Khác | Thông báo chung, ghi lỗi ra console |

## 6. Màn hình và route

```
/                                  chọn hoặc tạo người dùng
/w/:workspaceId                    trạng thái trống "Chọn một địa điểm trên cây" + nút thêm địa điểm cấp cao nhất
/w/:workspaceId/places/:placeId    trang một địa điểm (màn hình chính)
/w/:workspaceId/people             danh sách, tìm người
/w/:workspaceId/people/:personId   hồ sơ một người (thông tin, nơi đang ở, lịch sử)
/w/:workspaceId/items/:itemId      một món đồ
/w/:workspaceId/catalog            loại địa điểm, nhóm đồ, mục tuỳ biến
/w/:workspaceId/activity           hoạt động gần đây
```

Màn hình chính:

```
┌──────────────────────────┬──────────────────────────────────────────────────┐
│ [Không gian: Nhà An ▾]   │ Toà A › Tầng 3 › P301                  [Sửa] [⋯] │
│ 🔍 Tìm địa điểm…         │ P301 · Phòng · Đang dùng · v4                     │
│                          │ ───────────────────────────────────────────────── │
│ ▾ Toà A                  │ [Tổng quan] [Bên trong] [Người ở] [Đồ đạc] [Lịch sử]│
│   ▾ Tầng 3               │                                                  │
│     ● P301               │ Địa chỉ: 1 Tràng Tiền (lấy từ Toà A)             │
│       P302               │ Diện tích: 25 m²   Số giường: 2                   │
│   ▸ Tầng 4               │ Mô tả: …                                         │
│ ▸ Toà B                  │                                                  │
│ + Thêm địa điểm          │                                                  │
│ ──────────────           │                                                  │
│ Người · Danh mục ·       │                                                  │
│ Hoạt động                │                                                  │
└──────────────────────────┴──────────────────────────────────────────────────┘
```

- **Cây bên trái:** cấp cao nhất từ `GET /places`; mở một nút thì tải `GET /places/{id}/children`; nhánh nhiều con có dòng "Tải thêm" (cursor). Nút đang chọn theo URL. Kéo thả để di chuyển: **để sau**; đợt đầu di chuyển bằng hộp thoại chọn cha mới. **(tôi chọn)**
- **Các tab của một địa điểm:**
  - *Tổng quan:* thông tin, địa chỉ (của nó hoặc lấy từ tổ tiên), mục tuỳ biến theo loại.
  - *Bên trong:* danh sách con, thêm chỗ con.
  - *Người ở:* đang ở, sắp đến, đã rời; tick "cả toà nhà" dùng `subtreeOf`; ghi người chuyển vào và chuyển đi.
  - *Đồ đạc:* đang có và đã gỡ; tick "cả toà nhà"; thêm, chuyển chỗ, gỡ, thay, đưa lại.
  - *Lịch sử:* các version, xem nội dung từng version, khôi phục.
- Tab đang mở nằm trên URL (`?tab=people`).
- **Form mục tuỳ biến** sinh từ `GET /field-defs` theo loại của địa điểm (hoặc nhóm của món đồ): TEXT là ô chữ, NUMBER ô số, BOOLEAN công tắc, DATE chọn ngày, SELECT danh sách chọn.

## 7. Cấu trúc thư mục

```
frontend/src/
├── app/                    khởi động và khung chung
│   ├── main.tsx            gắn Router, QueryClient, Toaster
│   ├── router.tsx          cây route ở mục 6
│   ├── session/            người dùng và không gian đang chọn (5.1, 5.2)
│   └── layout/             khung có thanh bên, ô chọn không gian
├── api/                    phần gọi API dùng chung
│   ├── client.ts           client có kiểu cho identity, registry; gắn header
│   ├── errors.ts           ApiError, đọc Problem Details
│   ├── paging.ts           kiểu CursorPage và hook "tải thêm"
│   └── schema/             file OpenAPI đã lưu (identity.json, registry.json) và kiểu sinh từ chúng
│                           (identity.d.ts, registry.d.ts); không sửa tay
├── features/               mỗi nghiệp vụ một thư mục
│   ├── workspaces/         chọn, tạo không gian
│   ├── places/             cây, trang địa điểm, các tab, form, di chuyển, địa chỉ
│   ├── people/             danh sách, hồ sơ, cư trú
│   ├── items/              đồ đạc
│   ├── catalog/            loại địa điểm, nhóm đồ, mục tuỳ biến, form mục tuỳ biến dùng chung
│   ├── history/            danh sách version, xem và khôi phục (dùng cho địa điểm, người, đồ đạc)
│   └── activity/           hoạt động gần đây
│   (mỗi thư mục: api.ts, queries.ts, components/, pages/ nếu có trang riêng)
├── components/
│   ├── ui/                 component shadcn (đã có, giữ nguyên)
│   └── common/             component dùng chung tự viết: ô lỗi, trạng thái trống, nút tải thêm
├── hooks/, lib/            như hiện tại
└── test/                   cấu hình Vitest, MSW
```

Luật phụ thuộc **(tôi chọn)**: `features/*` dùng `api/`, `components/`, `lib/`; một feature không nhập thẳng file bên trong feature khác mà chỉ qua `index.ts` của nó. `components/ui` không biết gì về nghiệp vụ.

## 8. Thứ tự xây

1. **Nền:** sửa proxy (`/api/identity`, `/api/registry`, `/api/media`), thêm thư viện, sinh kiểu từ OpenAPI, client và lỗi, router, khung có thanh bên, màn hình chọn người dùng, chọn và tạo không gian. Xoá trang mẫu của Vite.
2. **Cây và trang địa điểm:** cây tải từng nhánh, tab Tổng quan và Bên trong, thêm, sửa, di chuyển, xoá, địa chỉ, form mục tuỳ biến.
3. **Người và cư trú:** tab Người ở, trang Người.
4. **Đồ đạc:** tab Đồ đạc, trang món đồ.
5. **Lịch sử, danh mục, hoạt động.**

Mỗi bước là một kế hoạch riêng, chạy được trước khi sang bước sau. Kế hoạch đầu tiên là bước 1 cộng bước 2, để có ngay một màn hình dùng được. **(tôi chọn)**

## 9. Để lại cho người xây

- Tên cụ thể của component, hook, khoá cache.
- Cách sắp xếp chi tiết bên trong mỗi tab (bảng hay danh sách thẻ).
- Màu sắc và khoảng cách: theo mặc định của bộ shadcn đã cài.

## 10. Câu hỏi còn mở, không chặn bước 1

- Khi có đăng nhập thật thì dùng cách nào (sẽ đổi `app/session`).
- Frontend chạy thật ở đâu (cùng máy chủ với các service qua một reverse proxy, hay tách riêng); quyết định khi triển khai.
