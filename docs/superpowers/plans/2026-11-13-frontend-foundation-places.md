# Frontend Foundation and Places Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Nền của frontend (thư viện, gọi API có kiểu, phiên làm việc, khung trang, chọn không gian) và màn hình chính: cây toà nhà › tầng › phòng cùng trang một địa điểm với thêm, sửa, di chuyển, xoá, địa chỉ.

**Architecture:** Thư mục `app/` (khởi động, route, phiên, khung), `api/` (client có kiểu sinh từ OpenAPI, lỗi, phân trang), `features/<nghiệp vụ>/` (mỗi cái có `api.ts`, `queries.ts`, `components/`). Dữ liệu server qua TanStack Query; mọi hàm gọi registry nhận `workspaceId` làm tham số đầu, `userId` lấy từ phiên.

**Tech Stack:** React 19.3, Vite 8, TypeScript 6, Tailwind 4, shadcn (base-ui) có sẵn; thêm `react-router@7.18.5`, `@tanstack/react-query@5.107.1`, `openapi-typescript@7.13.0` (dev), `openapi-fetch@0.17.0`, `react-hook-form@7.92.0`, `zod@4.6.7`, `@hookform/resolvers@7.0.6`; test: `vitest@5.1.0`, `jsdom@31.0.1`, `@testing-library/react@16.3.3`, `@testing-library/dom` (^10, bản mới nhất), `@testing-library/user-event@14.6.10`, `@testing-library/jest-dom@6.10.0`, `msw@2.15.0`.

**Spec:** `docs/superpowers/specs/2026-11-13-frontend-structure-design.md` (mục 5, 6, 7, 8 bước 1–2).

## Global Constraints

- Mọi lệnh chạy trong `frontend/`. Cài bằng `npm install --save-exact` (và `--save-dev` cho công cụ), đúng các phiên bản ở Tech Stack.
- Không sửa file trong `src/components/ui` (shadcn). Thêm component shadcn mới chỉ bằng `npx shadcn add <tên>` nếu thiếu.
- Giao diện tiếng Việt. Tên biến, hàm, file tiếng Anh.
- `id` là chuỗi ở mọi nơi; không bao giờ `Number(id)`.
- Mọi gọi API qua `features/*/api.ts`; component chỉ dùng hook trong `queries.ts`.
- Một feature chỉ nhập feature khác qua `index.ts` của nó.
- TDD: test trước, thấy hỏng, rồi mới viết code. Test component dùng MSW, không gọi backend thật.
- Lệnh kiểm tra: `npm test` (Vitest, chạy một lần), `npm run build` (tsc + vite), `npm run lint` (oxlint). Cả ba phải sạch ở cuối mỗi task.
- Mỗi task một commit (danh tính git của Windows như các bước backend); push `main` sau task cuối.

## Review Focus

1. `id` 18 chữ số (vượt 2^53) đi từ API vào URL và quay lại API không bị làm tròn. Test: `keepsLargeIdsAsStrings` (Task 2), `navigatesWithTheExactId` (Task 4).
2. Đổi tên một phòng thì cây và đường dẫn tổ tiên cập nhật ngay, không cần tải lại trang. Test: `renameUpdatesTreeAndBreadcrumb` (Task 6).
3. Gặp 409 "đã có người sửa" thì chữ đang nhập trong form còn nguyên. Test: `versionConflictKeepsTypedValues` (Task 6).
4. Mở trang `/w/...` khi chưa chọn người dùng thì về màn hình chọn người dùng, không gửi request thiếu header. Test: `redirectsToLoginWithoutUser` (Task 3).
5. "Tải thêm" trên nhánh nhiều phòng không lặp phòng nào. Test: `loadMoreAppendsWithoutDuplicates` (Task 4).

---

### Task 1: Công cụ và khung test

**Files:** sửa `frontend/package.json`, `frontend/vite.config.ts`; tạo `frontend/vitest.config.ts`, `frontend/src/test/{setup.ts,server.ts,render.tsx}`; xoá `frontend/src/App.tsx`, `frontend/src/App.css`, `frontend/src/assets/{hero.png,react.svg,vite.svg}`; chuyển `frontend/src/main.tsx` thành `frontend/src/app/main.tsx` (sửa `index.html` cho khớp).

  - Proxy trong `vite.config.ts`: `/api/identity` → 8081, `/api/registry` → 8082, `/api/media` → 8083, mỗi cái bỏ tiền tố như hiện tại.
  - Script: `"test": "vitest run"`, `"test:watch": "vitest"`.
  - `vitest.config.ts` dùng lại `vite.config.ts` (`mergeConfig`), `environment: 'jsdom'`, `setupFiles: ['src/test/setup.ts']`.
  - `setup.ts`: `import '@testing-library/jest-dom/vitest'`; bật MSW `server` (`onUnhandledRequest: 'error'`), reset handler sau mỗi test, xoá `localStorage`.
  - `server.ts`: `export const server = setupServer()`.
  - `render.tsx`: `export function renderApp(options: { route: string; userId?: string }): RenderResult` dựng router thật (Task 3 điền), `QueryClient` mới mỗi lần (`retry: false`). Ở Task 1 chỉ cần `renderWithProviders(ui: ReactElement)` bọc `QueryClientProvider`.
  - `app/main.tsx` tạm thời render một `<h1>home-platform</h1>`.

- [ ] Viết `src/test/smoke.test.tsx`: `rendersWithProviders` render `<p>xin chào</p>` qua `renderWithProviders`, thấy chữ; `unhandledRequestFails` gọi `fetch('/api/registry/x')` thì bị MSW báo lỗi.
- [ ] Chạy `npm test`: hỏng (chưa có vitest). Cài thư viện, viết cấu hình. Chạy lại: PASS. `npm run build`, `npm run lint`: sạch.
- [ ] Commit `Set up frontend tooling and test harness`.

### Task 2: Gọi API có kiểu

**Files:** tạo `frontend/scripts/api-sync.mjs`, `frontend/src/api/{client.ts,errors.ts,paging.ts,headers.ts,index.ts}`, `frontend/src/api/schema/{identity.json,registry.json,identity.d.ts,registry.d.ts}`; test `frontend/src/api/*.test.ts`.

```ts
// headers.ts
export function setCurrentUser(userId: string | null): void       // phiên gọi khi đổi người dùng
export function workspaceHeaders(workspaceId: string): Record<string, string>   // { 'X-Workspace-Id': workspaceId }

// client.ts
export const identity: Client<identityPaths>   // openapi-fetch, baseUrl '/api/identity'
export const registry: Client<registryPaths>   // baseUrl '/api/registry'
// middleware: gắn 'X-User-Id' khi đã có người dùng
export async function unwrap<T>(call: Promise<{ data?: T; error?: unknown; response: Response }>): Promise<T>
	// 2xx: trả data (204 trả undefined); lỗi: ném ApiError.from(response.status, error)

// errors.ts
export class ApiError extends Error {
	readonly status: number; readonly detail: string;
	readonly fieldErrors: Record<string, string>;     // từ errors[] {field, message}
	get isVersionConflict(): boolean                  // 409 và detail kết thúc bằng "was changed by someone else"
	static from(status: number, body: unknown): ApiError   // body không phải Problem Details: detail "Unexpected error"
}

// paging.ts
export type CursorPage<T> = { items: T[]; nextCursor: string | null }
export function useCursorList<T>(key: QueryKey, fetchPage: (cursor?: string) => Promise<CursorPage<T>>):
	{ items: T[]; hasMore: boolean; loadMore: () => void; isLoading: boolean; isLoadingMore: boolean; error: ApiError | null }
	// dựng trên useInfiniteQuery; items là các trang nối lại
```

  - `scripts/api-sync.mjs`: tải `http://localhost:8081/v3/api-docs` và `http://localhost:8082/v3/api-docs` vào `src/api/schema/*.json`, rồi chạy `openapi-typescript` cho từng file ra `*.d.ts`. Script `"api:sync"`. Script `"api:types"` chỉ sinh `.d.ts` từ JSON đã có (dùng khi build trên máy không có backend).
  - Lấy JSON lần đầu: build và chạy `identity-service` và `registry-service` bằng jar (như các bước backend), chạy `npm run api:sync`, dừng service. Commit cả JSON và `.d.ts`.

- [ ] Test (`errors.test.ts`, `client.test.ts`, `paging.test.tsx`):
  - `readsFieldErrors`: body `{status:400, detail:'Invalid request content.', errors:[{field:'name', message:'must not be blank'}]}` → `fieldErrors.name === 'must not be blank'`.
  - `recognisesVersionConflict`: 409 "Place 1 was changed by someone else" → `isVersionConflict` true; 409 "Place 1 still has 2 child places, ..." → false.
  - `nonProblemBodyIsUnexpected`.
  - `addsUserHeaderOnceSet`: MSW bắt request `GET /api/identity/users/me`; trước `setCurrentUser` không có `X-User-Id`, sau có `'42'`.
  - `keepsLargeIdsAsStrings`: MSW trả `{id: "114540094935228416"}` → `unwrap` trả đúng chuỗi đó.
  - `loadMoreAppendsPages`: hai trang `[a, b]` + cursor, `[c]` + null → sau `loadMore` có `[a, b, c]`, `hasMore` false.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add typed API clients generated from OpenAPI`.

### Task 3: Phiên, router, khung trang, không gian

**Files:** `frontend/src/app/{router.tsx,session/SessionProvider.tsx,session/useSession.ts,layout/AppLayout.tsx,layout/WorkspaceSwitcher.tsx,pages/LoginPage.tsx,pages/NotFoundPage.tsx}`, `frontend/src/features/workspaces/{api.ts,queries.ts,index.ts,components/CreateWorkspaceDialog.tsx}`; hoàn thiện `src/test/render.tsx`; test `frontend/src/app/*.test.tsx`, `frontend/src/features/workspaces/*.test.tsx`.

```ts
// session
export function useSession(): { userId: string | null; signIn(userId: string): void; signOut(): void }
	// lưu localStorage key 'hp.userId'; gọi setCurrentUser
// route: '/' LoginPage; '/w/:workspaceId' AppLayout (thanh bên + Outlet), con: index (trạng thái trống),
//        'places/:placeId' (Task 5); '*' NotFoundPage. Route trong '/w' bị chặn khi chưa có userId → '/'.
export function useWorkspaceId(): string        // từ URL

// features/workspaces
export function useMyWorkspaces(): UseQueryResult<WorkspaceResponse[]>   // tải mọi trang của GET /workspaces
export function useCreateWorkspace(): UseMutationResult<WorkspaceResponse, ApiError, { name: string }>
	// POST /workspaces (identity) rồi POST /setup (registry, header của không gian mới); xong thì điều hướng tới /w/<id>
```

  - `LoginPage`: hai phần "Tạo người dùng" (email, tên hiển thị → `POST /users`) và "Tôi đã có id" (ô nhập id → `GET /users/me` với id đó để kiểm tra). Thành công thì `signIn` và về không gian đầu tiên của người đó, hoặc mở hộp thoại tạo không gian nếu chưa có.
  - `AppLayout`: dùng `Sidebar` của shadcn; trên cùng `WorkspaceSwitcher` (tên không gian, danh sách để chuyển, "Tạo không gian"), dưới là chỗ cho cây (Task 4), cuối là các liên kết Người, Danh mục, Hoạt động (trỏ tới route chưa có, hiện "Sắp có").
  - Trang trống `/w/:workspaceId`: "Chọn một địa điểm trên cây" + nút "Thêm địa điểm" (mở form của Task 6 khi có; ở task này chỉ hiện nút bị vô hiệu).

- [ ] Test:
  - `redirectsToLoginWithoutUser`: route `/w/1` khi chưa có người dùng → thấy màn hình đăng nhập, MSW không nhận request nào tới registry.
  - `createsAUserAndEntersTheirWorkspace`: điền email, tên → `POST /users` → `GET /workspaces` trả một không gian → URL thành `/w/<id>`; `localStorage['hp.userId']` có id.
  - `existingIdIsChecked`: id không tồn tại (404) → báo "Không tìm thấy người dùng".
  - `createWorkspaceCallsIdentityThenSetup`: thứ tự request đúng `POST /api/identity/workspaces` rồi `POST /api/registry/setup` với `X-Workspace-Id` của không gian mới.
  - `switcherNavigates`: chọn không gian khác → URL đổi.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add session, routing, layout and workspace switching`.

### Task 4: Cây địa điểm

**Files:** `frontend/src/features/places/{api.ts,queries.ts,index.ts,components/PlaceTree.tsx,components/PlaceTreeNode.tsx}`; gắn vào `AppLayout`; test `frontend/src/features/places/PlaceTree.test.tsx`.

```ts
// api.ts (Task 5–7 thêm hàm)
export function listTopLevel(workspaceId: string, cursor?: string): Promise<CursorPage<PlaceSummary>>
export function listChildren(workspaceId: string, parentId: string, cursor?: string): Promise<CursorPage<PlaceSummary>>
// queries.ts
export const placeKeys = {
	all: (ws: string) => ['places', ws],
	children: (ws: string, parentId: string | null) => ['places', ws, 'children', parentId ?? 'root'],
	detail: (ws: string, id: string) => ['places', ws, 'detail', id],
}
```

  - `PlaceSummary` là `PlaceResponse` trong kiểu sinh từ OpenAPI.
  - Cây: cấp cao nhất dùng `useCursorList(placeKeys.children(ws, null), …)`; mỗi nút mở ra mới tải con của nó; nút có dòng "Tải thêm" khi `hasMore`; nút đang chọn theo `placeId` trên URL được tô; khi mở trang trực tiếp vào một phòng sâu, các tổ tiên (lấy từ trang địa điểm, Task 5) được mở sẵn.
  - Bấm một nút → `/w/<ws>/places/<id>`.
  - Ô "Tìm địa điểm…" trên cây: gõ ít nhất 1 ký tự (chờ 300 ms sau lần gõ cuối) thì gọi `searchPlaces(ws, name)` (`GET /places/search?name=`) và thay cây bằng danh sách kết quả; xoá ô thì cây hiện lại. Thêm vào `api.ts`: `searchPlaces(workspaceId: string, name: string, cursor?: string): Promise<CursorPage<PlaceSummary>>`.

- [ ] Test:
  - `showsTopLevelPlacesSortedAsReturned`.
  - `expandingLoadsChildrenOnce`: mở, đóng, mở lại → `GET /places/{id}/children` chỉ một lần.
  - `loadMoreAppendsWithoutDuplicates`: trang 1 `[P101, P102]` cursor "c1", trang 2 `[P103]` → đủ ba, không lặp.
  - `navigatesWithTheExactId`: bấm nút có id `114540094935228416` → URL chứa đúng chuỗi đó.
  - `emptyWorkspaceShowsAddButton`.
  - `searchReplacesTheTreeWithResults`: gõ "P10" → gọi `GET /places/search?name=P10`, thấy kết quả; xoá ô → cây hiện lại.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the lazily loaded place tree`.

### Task 5: Trang một địa điểm (xem)

**Files:** `frontend/src/features/places/{pages/PlacePage.tsx,components/PlaceHeader.tsx,components/PlaceOverviewTab.tsx,components/PlaceChildrenTab.tsx}`; `frontend/src/features/catalog/{api.ts,queries.ts,index.ts,components/AttributeView.tsx}`; thêm vào `places/api.ts`: `getPlace(ws, id): Promise<PlaceDetail>`; test `frontend/src/features/places/PlacePage.test.tsx`.

```ts
// catalog
export function usePlaceTypes(ws: string): UseQueryResult<PlaceTypeResponse[]>
export function useFieldDefs(ws: string, target: 'PLACE' | 'PERSON' | 'ITEM', typeId?: string): UseQueryResult<FieldDefResponse[]>
export function AttributeView(props: { defs: FieldDefResponse[]; values: Record<string, unknown> | null }): JSX.Element
	// theo sortOrder; BOOLEAN "Có/Không"; DATE dd/MM/yyyy; mục ARCHIVED hiện mờ; khoá không có mục thì bỏ qua
```

  - Trang: đường dẫn tổ tiên (`Breadcrumb` của shadcn, từ `ancestors`, mỗi phần là liên kết), tên, loại (tên từ `usePlaceTypes`), trạng thái, `v<version>`; các tab "Tổng quan", "Bên trong", "Người ở", "Đồ đạc", "Lịch sử" (ba tab sau hiện "Sắp có"); tab trên URL `?tab=overview|children|…`, mặc định `overview`.
  - Tổng quan: địa chỉ của nó, hoặc địa chỉ hiệu lực kèm "lấy từ <tên tổ tiên>"; mô tả; mục tuỳ biến qua `AttributeView`.
  - Bên trong: danh sách con (`useCursorList` cùng khoá với cây, để dùng chung cache) có "Tải thêm".
  - 404 → `NotFoundPage` trong khung chính, cây vẫn hiện.

- [ ] Test:
  - `showsBreadcrumbAndHeader`.
  - `showsInheritedAddress`: `address` null, `effectiveAddress.fromPlaceId` là toà nhà → "lấy từ Toà A".
  - `rendersAttributesByType`: NUMBER 42 → "42", BOOLEAN true → "Có", DATE "2026-01-31" → "31/01/2026".
  - `tabFollowsTheUrl`: `?tab=children` mở tab Bên trong.
  - `unknownPlaceShowsNotFound`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the place page`.

### Task 6: Thêm và sửa địa điểm

**Files:** `frontend/src/features/places/components/{PlaceFormDialog.tsx,placeFormSchema.ts}`; `frontend/src/features/catalog/components/AttributeFields.tsx`; `frontend/src/components/common/{FormError.tsx,VersionConflictAlert.tsx}`; thêm vào `places/api.ts`: `createPlace(ws, body)`, `updatePlace(ws, id, body)`; thêm vào `queries.ts`: `useCreatePlace(ws)`, `useUpdatePlace(ws)`; test `frontend/src/features/places/PlaceForm.test.tsx`.

```ts
export function AttributeFields(props: { defs: FieldDefResponse[]; control: Control; name: 'attributes' }): JSX.Element
	// chỉ mục ACTIVE; TEXT Input, NUMBER Input type=number (gửi số, không gửi chuỗi), BOOLEAN Switch,
	// DATE Input type=date (gửi 'YYYY-MM-DD'), SELECT Select; bắt buộc có dấu *
export function applyServerErrors(error: ApiError, setError: UseFormSetError<FieldValues>): void
	// fieldErrors → setError từng trường (kể cả 'attributes.<khoá>'); không có trường nào → lỗi chung của form
```

  - Form: loại (chỉ loại ACTIVE; giữ loại cũ đã lưu trữ khi sửa), tên, mã, mô tả, trạng thái, mục tuỳ biến theo loại đang chọn (đổi loại thì tải lại mục). Thêm từ cây (cấp cao nhất), từ trang trống, và "Thêm chỗ con" ở tab Bên trong (`parentId` là địa điểm đang xem).
  - Sửa gửi `version` đang hiển thị. 409 version lệch: hiện `VersionConflictAlert` với nút "Tải bản mới" (làm mới chi tiết, giữ dữ liệu đang nhập cho tới khi người dùng bấm "Dùng bản mới").
  - Ghi xong: làm mới `placeKeys.detail` của nó, `placeKeys.children` của cha nó (hoặc gốc), rồi đóng hộp thoại; thêm mới thì điều hướng tới trang của địa điểm mới.

- [ ] Test:
  - `createsATopLevelPlace`, `createsAChildFromTheChildrenTab` (body có `parentId`).
  - `showsFieldErrorsFromServer`: 400 với `attributes.area` → lỗi hiện dưới ô "area".
  - `numberAttributeIsSentAsNumber`: nhập 42 → body `attributes.area === 42`.
  - `versionConflictKeepsTypedValues`: 409 → ô tên vẫn là chữ vừa nhập, thấy "Đã có người sửa trước bạn".
  - `renameUpdatesTreeAndBreadcrumb`: sửa tên P301 → cây và tiêu đề hiện tên mới mà không tải lại trang.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add creating and editing places`.

### Task 7: Di chuyển, xoá, địa chỉ

**Files:** `frontend/src/features/places/components/{MovePlaceDialog.tsx,DeletePlaceDialog.tsx,AddressDialog.tsx}`; thêm vào `api.ts`/`queries.ts`: `movePlace(ws, id, version, parentId | null)`, `deletePlace(ws, id, version)`, `setAddress(ws, id, version, address)`, `removeAddress(ws, id, version)` và hook tương ứng; test `frontend/src/features/places/PlaceActions.test.tsx`.

  - Di chuyển: hộp thoại có ô tìm địa điểm (`GET /places/search?name=`) và lựa chọn "Lên cấp cao nhất"; không cho chọn chính nó (backend vẫn chặn con cháu, lỗi 409 hiện nguyên văn). Xong thì làm mới cả `placeKeys.all(ws)` vì cả cây con đổi chỗ.
  - Xoá: hộp thoại xác nhận (`AlertDialog`); 409 "còn ..." hiện nguyên văn; xong thì về trang của cha (hoặc trang trống) và làm mới con của cha.
  - Địa chỉ: form địa chỉ (đầy đủ, tỉnh, phường, đường, toạ độ tuỳ chọn), "Gỡ địa chỉ".
  - Menu `⋯` trên đầu trang địa điểm chứa: Di chuyển, Địa chỉ, Xoá.

- [ ] Test: `movesToAnotherParent` (body `{version, parentId}`), `movesToTheTop` (`parentId: null`), `deleteShowsBusinessConflict` (409 nguyên văn), `deleteReturnsToParent`, `setsAndRemovesAddress`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add moving, deleting and addressing places`.

### Task 8: Tài liệu, kiểm tra, chạy thử

- [ ] `frontend/README.md` viết lại: cách chạy (`npm run dev` với ba service đang chạy), `npm test`, `npm run api:sync`, cấu trúc thư mục và luật phụ thuộc (spec mục 7).
- [ ] `npm test`, `npm run build`, `npm run lint`: sạch.
- [ ] Chạy thử trên backend thật: bật `identity-service`, `registry-service` và `npm run dev`; qua proxy (`curl` tới `http://localhost:5173/api/...`) tạo người dùng, không gian, `setup`, một toà nhà và một tầng; mở `http://localhost:5173/w/<id>/places/<tầng>` bằng trình duyệt tự động (Playwright) và chụp màn hình thấy cây và đường dẫn tổ tiên. Dừng tất cả; xoá các dòng vừa tạo (thành viên, không gian, người dùng bên identity; dòng của không gian đó bên registry).
- [ ] Commit `Document the frontend` và push `main`.
