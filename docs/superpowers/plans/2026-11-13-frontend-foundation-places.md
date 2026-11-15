# Frontend Foundation and Places Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Nền của frontend (thư viện, gọi API có kiểu, phiên làm việc, khung trang, chọn không gian) và màn hình chính: cây toà nhà › tầng › phòng cùng trang một địa điểm với thêm, sửa, di chuyển, xoá, địa chỉ.

**Architecture:** Thư mục `app/` (khởi động, route, phiên, khung), `api/` (client có kiểu sinh từ OpenAPI, lỗi, phân trang), `features/<nghiệp vụ>/` (mỗi cái có `api.ts`, `queries.ts`, `types.ts`, `components/`). Dữ liệu server qua TanStack Query. Mọi hàm gọi registry nhận `workspaceId` làm tham số đầu và tự gắn hai header người gọi; `userId` lấy từ phiên.

**Tech Stack:** React 19.3, Vite 8, TypeScript 6, Tailwind 4, shadcn (base-ui) có sẵn; thêm `react-router@7.18.5`, `@tanstack/react-query@5.107.1`, `openapi-typescript@7.13.0` (dev), `openapi-fetch@0.17.0`, `react-hook-form@7.92.0`, `zod@4.6.7`, `@hookform/resolvers@7.0.6`; test: `vitest@4.1.12`, `jsdom@29.1.1`, `@testing-library/react@16.3.3`, `@testing-library/dom@10.4.2`, `@testing-library/user-event@14.6.10`, `@testing-library/jest-dom@6.9.1`, `msw@2.15.0`. Mọi phiên bản đã kiểm tra có trên npm ngày 2026-11-15. Giữ React Router 7, MSW 2 dù đã có bản lớn hơn: đổi bản lớn là việc riêng. Ba gói test lùi một bậc so với bản mới nhất (`vitest@5`, `jsdom@31`, `jest-dom@6.10`) vì các bản đó đòi Node 22, còn máy phát triển đang dùng Node 20.19; nâng Node lên 22 hoặc 24 thì nâng được cả ba.

**Spec:** `docs/superpowers/specs/2026-11-13-frontend-structure-design.md` (mục 5, 6, 7, 8 bước 1–2).

## Đã sửa so với bản 2026-11-13

Bản đầu được đối chiếu lại với backend và với thư mục `frontend/` ngày 2026-11-15. Những chỗ đổi, kèm lý do:

1. **Kiểu response.** OpenAPI của backend không ghi trường nào là bắt buộc, nên kiểu sinh ra có mọi trường tuỳ chọn (`id?: string`). Dùng thẳng thì chỗ nào cũng phải xử lý `undefined`. Thêm kiểu `Present<T, K>`: mỗi feature khai báo một lần những trường luôn có; tên và kiểu trường vẫn lấy từ file sinh tự động.
2. **Header người gọi.** OpenAPI ghi `X-User-Id`, `X-Workspace-Id` là tham số bắt buộc của từng API, nên không gắn ngầm bằng middleware được (TypeScript báo thiếu). Mỗi hàm trong `api.ts` truyền `header: workspaceHeaders(ws)` hoặc `userHeaders()`; hai hàm này ném lỗi khi chưa có người dùng, nên không request nào đi ra mà thiếu header.
3. **Phiên.** Người dùng hiện tại được đặt cho lớp API một cách đồng bộ (lúc nạp module và trong `signIn`/`signOut`), không qua `useEffect`: effect của component con chạy trước effect của cha, nên request đầu tiên sẽ đi trước khi phiên kịp đặt. Đổi người dùng thì xoá sạch cache.
4. **Làm mới sau khi ghi.** Đổi tên một địa điểm phải làm cũ mọi trang chi tiết đang cache (đường dẫn tổ tiên của con cháu chứa tên cũ) và kết quả tìm kiếm, không chỉ chi tiết của nó và danh sách con của cha. Bảng cụ thể ở Task 6.
5. **409 "đã có người sửa".** Một nút "Tải bản mới": lấy bản mới về, ô nào người dùng đã sửa thì giữ, ô chưa đụng tới thì nhận giá trị mới, rồi lưu lại với `version` mới.
6. **Cây.** `PlaceResponse` không cho biết một địa điểm có con hay không: mũi tên hiện cho tới khi mở ra thấy trống. Mở thẳng một phòng nằm ngoài trang đầu của cha nó thì phòng đó vẫn hiện trên cây. Việc lấy chi tiết (`getPlace`) chuyển từ Task 5 lên Task 4 vì cây cần tổ tiên.
7. **Không gian trên URL** được đối chiếu với danh sách không gian của tôi; không có thì báo "Không tìm thấy không gian" và không gọi registry.
8. **Trang mẫu của Vite** còn nằm trong `index.css` (`#root` rộng 1126px căn giữa, `h1` 56px, khối màu tối ghi đè `--accent`, `--border` của shadcn): Task 1 dọn cả file này, không chỉ `App.css`.
9. **Môi trường.** `node_modules` được cài từ Windows; chạy `npm` trong WSL sẽ hỏng ở các gói có mã máy (rolldown, oxlint, tailwind). Cách chạy ở Global Constraints.
10. **Thư viện.** `openapi-typescript@7.13.0` khai báo cần TypeScript 5, dự án dùng 6: thêm `overrides` trong `package.json`. `openapi-fetch` cần `baseUrl` tuyệt đối để chạy được trong test.

## Global Constraints

- Mọi lệnh `npm` chạy trong `frontend/` bằng Node của Windows. Từ WSL: `cmd.exe /c "npm test"` (đứng ở `frontend/`). Cài bằng `npm install --save-exact` (thêm `--save-dev` cho công cụ), đúng các phiên bản ở Tech Stack.
- Không sửa file trong `src/components/ui` (shadcn). Thêm component shadcn mới chỉ bằng `npx shadcn add <tên>` nếu thiếu.
- Giao diện tiếng Việt. Tên biến, hàm, file, tên test tiếng Anh.
- `id` là chuỗi ở mọi nơi; không bao giờ `Number(id)`. Một `id` lấy từ URL phải khớp `/^\d+$/` trước khi gửi đi; không khớp thì hiện "Không tìm thấy", không gọi API.
- Mọi gọi API nằm trong `features/*/api.ts`; component chỉ dùng hook trong `queries.ts`.
- Một feature chỉ nhập feature khác qua `index.ts` của nó. `api/` và `components/` không nhập gì từ `features/` hay `app/`.
- Kiểu response khai báo bằng `Present<…>` trong `types.ts` của feature. Trường không nằm trong danh sách "luôn có" có thể là `null`: so sánh bằng `== null` hoặc `??`.
- Backend trả thông báo lỗi bằng tiếng Anh. Lỗi kiểm tra được ở trình duyệt (bắt buộc, độ dài, định dạng số) hiện tiếng Việt trước khi gửi; lỗi từ server hiện nguyên văn.
- TDD: test trước, thấy hỏng, rồi mới viết code. Test component dùng MSW (`onUnhandledRequest: 'error'`), không gọi backend thật.
- Lệnh kiểm tra: `npm test` (Vitest, chạy một lần), `npm run build` (tsc + vite), `npm run lint` (oxlint: 0 lỗi, không thêm cảnh báo ngoài 19 cảnh báo có sẵn của các file shadcn). Cả ba phải sạch ở cuối mỗi task.
- Mỗi task một commit (danh tính git của Windows như các bước backend); push `main` sau task cuối.

## Review Focus

1. `id` 18 chữ số (vượt 2^53) đi từ API vào URL và quay lại API không bị làm tròn. Test: `keepsLargeIdsAsStrings` (Task 2), `navigatesWithTheExactId` (Task 4).
2. Đổi tên một địa điểm thì cây, tiêu đề và đường dẫn tổ tiên của con cháu nó hiện tên mới mà không tải lại trang. Test: `renameUpdatesTreeAndHeader`, `renameReachesDescendantBreadcrumbs` (Task 6).
3. Gặp 409 "đã có người sửa" thì chữ đang nhập còn nguyên, và sau "Tải bản mới" vẫn còn. Test: `versionConflictKeepsTypedValues`, `reloadKeepsMyEditsAndTakesNewVersion` (Task 6).
4. Chưa chọn người dùng thì không request nào đi ra. Test: `callsWithoutUserThrow` (Task 2), `redirectsToLoginWithoutUser` (Task 3).
5. "Tải thêm" không lặp dòng nào, kể cả khi server trả lại một dòng đã có (có người thêm phòng giữa hai lần tải). Test: `loadMoreSkipsRepeatedItems` (Task 2), `loadMoreAppendsWithoutDuplicates` (Task 4).

---

### Task 1: Công cụ, khung test, dọn trang mẫu

**Files:** sửa `frontend/package.json`, `frontend/vite.config.ts`, `frontend/index.html`, `frontend/src/index.css`, `frontend/.oxlintrc.json`; tạo `frontend/vitest.config.ts`, `frontend/src/test/{setup.ts,server.ts,render.tsx,smoke.test.tsx}`, `frontend/src/app/main.tsx`; xoá `frontend/src/{main.tsx,App.tsx,App.css}`, `frontend/src/assets/{hero.png,react.svg,vite.svg}`, `frontend/public/icons.svg`.

**Interfaces:**
- Produces: `server` (MSW, `src/test/server.ts`); `renderWithProviders(ui: ReactElement): RenderResult & { user: UserEvent }` (`src/test/render.tsx`).

  - Cài một lần mọi thư viện ở Tech Stack. `package.json` thêm `"overrides": { "openapi-typescript": { "typescript": "$typescript" } }`.
  - Proxy trong `vite.config.ts`: `/api/identity` → 8081, `/api/registry` → 8082, `/api/media` → 8083, mỗi cái bỏ tiền tố (thay hai dòng `/api/authz`, `/api/audit` còn sót từ dự án khác).
  - Script: `"test": "vitest run"`, `"test:watch": "vitest"`.
  - `vitest.config.ts` dùng lại `vite.config.ts` (`mergeConfig`), `environment: 'jsdom'`, `setupFiles: ['src/test/setup.ts']`; thêm file này vào `include` của `tsconfig.node.json`.
  - `setup.ts`: `import '@testing-library/jest-dom/vitest'`; bật MSW `server` (`onUnhandledRequest: 'error'`), reset handler và xoá `localStorage` sau mỗi test; giả lập những thứ jsdom thiếu mà shadcn cần (`matchMedia`, `ResizeObserver`, `Element.prototype.scrollIntoView`).
  - `index.html`: `lang="vi"`, `<title>Home Platform</title>`, script trỏ `/src/app/main.tsx`.
  - `index.css`: chỉ giữ các `@import`, `@custom-variant`, khối `:root` gồm biến của shadcn (`--background` … `--sidebar-ring`, `--border`, `--accent`, `--radius`), `@theme inline`, `.dark`, `@layer base`. Bỏ mọi thứ của trang mẫu: biến `--text`, `--bg`, `--sans`…, `font`, `color-scheme`, khối `@media (prefers-color-scheme: dark)`, `#root`, `body`, `h1`, `h2`, `p`, `code`.
  - `.oxlintrc.json`: bỏ qua `src/api/schema/**` (file sinh tự động của Task 2).
  - `app/main.tsx` tạm thời render `<h1>Home Platform</h1>`.

- [ ] Viết `src/test/smoke.test.tsx`: `rendersWithProviders` render `<p>xin chào</p>` qua `renderWithProviders`, thấy chữ; `unhandledRequestFails`: `fetch('/api/registry/x')` bị từ chối.
- [ ] Chạy `npm test`. Expected: hỏng vì chưa có script `test`.
- [ ] Cài thư viện, viết cấu hình, dọn trang mẫu. Chạy `npm test`. Expected: 2 test PASS. `npm run build`, `npm run lint`: sạch.
- [ ] Commit `Set up frontend tooling and test harness`.

### Task 2: Gọi API có kiểu

**Files:** tạo `frontend/scripts/api-sync.mjs`, `frontend/src/api/{client.ts,errors.ts,headers.ts,paging.ts,types.ts,queryClient.ts,index.ts}`, `frontend/src/api/schema/{identity.json,registry.json,identity.d.ts,registry.d.ts}`; test `frontend/src/api/{errors,client,paging}.test.ts(x)`.

**Interfaces:**
- Produces (mọi thứ xuất qua `@/api`):

```ts
// headers.ts
export function setCurrentUser(userId: string | null): void          // phiên gọi, đồng bộ
export function userHeaders(): { 'X-User-Id': string }                // ném Error('No user is signed in') khi chưa có
export function workspaceHeaders(workspaceId: string): { 'X-Workspace-Id': string; 'X-User-Id': string }

// client.ts
export const identity: Client<IdentityPaths>   // openapi-fetch, baseUrl `${window.location.origin}/api/identity`
export const registry: Client<RegistryPaths>   // baseUrl `${window.location.origin}/api/registry`
export async function unwrap<T>(call: Promise<{ data?: T; error?: unknown; response: Response }>): Promise<T>
	// 2xx: trả data (204 trả undefined). Không 2xx: ném ApiError.from(status, error).
	// fetch bị từ chối (mất mạng, backend tắt): ném ApiError.network().

// errors.ts
export class ApiError extends Error {
	readonly status: number                           // 0: không tới được máy chủ
	readonly detail: string
	readonly fieldErrors: Record<string, string>      // errors[] có field; nhiều lỗi một trường nối bằng '; '
	readonly formErrors: string[]                     // errors[] có field null
	readonly requestId: string | null                 // có ở lỗi 500
	get isVersionConflict(): boolean                  // 409 và detail kết thúc bằng "was changed by someone else"
	get isNotFound(): boolean                         // 404
	static from(status: number, body: unknown): ApiError   // body không phải Problem Details: detail 'Unexpected error'
	static network(): ApiError                             // status 0, detail 'Không kết nối được máy chủ'
}

// types.ts
/** Trong T (kiểu response sinh từ OpenAPI, mọi trường tuỳ chọn): các trường K luôn có; trường còn lại có thể null. */
export type Present<T, K extends keyof T> = { [P in K]-?: NonNullable<T[P]> } & { [P in Exclude<keyof T, K>]-?: NonNullable<T[P]> | null }
export type CursorPage<T> = { items: T[]; nextCursor: string | null }

// paging.ts
export type CursorList<T> = { items: T[]; hasMore: boolean; loadMore: () => void; isLoading: boolean; isLoadingMore: boolean; error: ApiError | null; refetch: () => void }
export function useCursorList<T extends { id: string }>(options: { queryKey: QueryKey; fetchPage: (cursor?: string) => Promise<CursorPage<T>>; enabled?: boolean }): CursorList<T>
	// dựng trên useInfiniteQuery; items là các trang nối lại, một id chỉ giữ lần xuất hiện đầu
export async function fetchAllPages<T>(fetchPage: (cursor?: string) => Promise<CursorPage<T>>): Promise<T[]>

// queryClient.ts
export function createQueryClient(): QueryClient
	// staleTime 30 giây; refetchOnWindowFocus false (cây có nhiều danh sách đang mở, không tải lại cả loạt mỗi lần
	// quay lại cửa sổ); query thử lại 1 lần với lỗi mạng và 5xx, không thử lại 4xx; mutation không thử lại
```

  - `scripts/api-sync.mjs`: tải `http://localhost:8081/v3/api-docs` và `http://localhost:8082/v3/api-docs` vào `src/api/schema/*.json` (ghi lại bằng `JSON.stringify(doc, null, 2)`, bỏ `servers`), rồi chạy `openapi-typescript` cho từng file ra `*.d.ts`. Script `"api:sync"`. Script `"api:types"` chỉ sinh `.d.ts` từ JSON đã có.
  - Lấy JSON lần đầu: build và chạy `identity-service`, `registry-service` bằng jar, chạy `npm run api:sync`. Commit cả JSON và `.d.ts`.
  - `baseUrl` phải tuyệt đối: `openapi-fetch` tự tạo `Request`, và `Request` của Node (dùng trong test) không nhận URL tương đối.

- [ ] Test:
  - `readsFieldErrors`: body `{status:400, detail:'Invalid request content.', errors:[{field:'name', message:'must not be blank'}, {field:'name', message:'size must be between 0 and 255'}, {field:null, message:'bad object'}]}` → `fieldErrors.name === 'must not be blank; size must be between 0 and 255'`, `formErrors` là `['bad object']`.
  - `recognisesVersionConflict`: 409 "Place 1 was changed by someone else" → `isVersionConflict` true; 409 "Place 1 still has 2 child places, 0 occupancies and 0 items" → false.
  - `nonProblemBodyIsUnexpected`: body `''` và body `'<html>'` → `detail === 'Unexpected error'`. `keepsTheRequestIdOfA500`.
  - `callsWithoutUserThrow`: chưa `setCurrentUser` thì `userHeaders()` và `workspaceHeaders('1')` ném lỗi; sau `setCurrentUser('42')` trả đúng hai header.
  - `keepsLargeIdsAsStrings`: MSW trả `{id: "114540094935228416"}` cho `GET /api/identity/users/me` → `unwrap` trả đúng chuỗi đó, và request mang `X-User-Id: 42`.
  - `turnsProblemDetailsIntoApiError`: MSW trả 404 `{status:404, detail:'User 1 not found'}` → `unwrap` ném `ApiError` có `isNotFound`. `networkFailureIsAnApiError`: MSW `HttpResponse.error()` → `status === 0`.
  - `loadMoreAppendsPages`: hai trang `[a, b]` + cursor "c1", `[c]` + null → sau `loadMore` có `[a, b, c]`, `hasMore` false, lần gọi thứ hai nhận cursor "c1". `loadMoreSkipsRepeatedItems`: trang hai là `[b, c]` → vẫn `[a, b, c]`.
  - `fetchAllPagesFollowsEveryCursor`.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add typed API clients generated from OpenAPI`.

### Task 3: Phiên, router, khung trang, không gian

**Files:** `frontend/src/app/{router.tsx,App.tsx}`, `frontend/src/app/session/{sessionStore.ts,useSession.ts,RequireUser.tsx,LoginPage.tsx}`, `frontend/src/app/layout/{StartPage.tsx,AppLayout.tsx,WorkspaceSwitcher.tsx,UserMenu.tsx,WorkspaceHomePage.tsx,NotFoundPage.tsx,ComingSoonPage.tsx}`, `frontend/src/features/users/{api.ts,queries.ts,types.ts,index.ts}`, `frontend/src/features/workspaces/{api.ts,queries.ts,types.ts,index.ts,components/CreateWorkspaceDialog.tsx}`, `frontend/src/features/catalog/{api.ts,queries.ts,types.ts,index.ts}`, `frontend/src/components/common/{ErrorState.tsx,FormError.tsx}`; hoàn thiện `src/test/render.tsx`, tạo `src/test/fixtures.ts`; test `frontend/src/app/*.test.tsx`, `frontend/src/features/workspaces/*.test.tsx`.

**Interfaces:**
- Consumes: `@/api` (Task 2).
- Produces:

```ts
// app/session/sessionStore.ts — kho ngoài React, đọc bằng useSyncExternalStore
export const sessionStore: { getUserId(): string | null; signIn(userId: string): void; signOut(): void; subscribe(listener: () => void): () => void }
	// lưu localStorage 'hp.userId'; gọi setCurrentUser ngay khi module nạp và trong signIn/signOut
// app/session/useSession.ts
export function useSession(): { userId: string | null; signIn(userId: string): void; signOut(): void }   // signOut xoá cache của QueryClient
export function useWorkspaceId(): string        // từ URL
// app/router.tsx
export const routes: RouteObject[]
// test/render.tsx
export function renderApp(options: { route: string; userId?: string }): RenderResult & { user: UserEvent; router: Router }

// features/users
export type User = Present<S['UserResponse'], 'id' | 'email' | 'displayName' | 'status'>
export function useCreateUser(): UseMutationResult<User, ApiError, { email: string; displayName: string }>
export function useCheckUser(): UseMutationResult<User, ApiError, string>   // GET /users/me với X-User-Id là id cần kiểm tra
export function useCurrentUser(): UseQueryResult<User, ApiError>
// features/workspaces
export type Workspace = Present<S['WorkspaceResponse'], 'id' | 'name' | 'status' | 'ownerUserId' | 'role'>
export function useMyWorkspaces(): UseQueryResult<Workspace[], ApiError>   // mọi trang của GET /workspaces
export function useCreateWorkspace(): UseMutationResult<Workspace, ApiError, { name: string }>
	// POST /workspaces (identity) rồi POST /setup (registry, header của không gian mới). Setup hỏng thì vẫn trả
	// không gian đã tạo: trang chủ của không gian sẽ mời chạy lại (xem dưới).
// features/catalog
export type PlaceType = Present<S['PlaceTypeResponse'], 'id' | 'code' | 'name' | 'sortOrder' | 'status'>
export function usePlaceTypes(ws: string): UseQueryResult<PlaceType[], ApiError>
export function useSetupWorkspace(ws: string): UseMutationResult<void, ApiError, void>   // POST /setup, rồi làm mới danh mục
```

  - Route: `/` `StartPage`; `/w/:workspaceId` `RequireUser` › `AppLayout` (thanh bên + `Outlet`) với con: index `WorkspaceHomePage`, `places/:placeId` (Task 5; ở task này là `ComingSoonPage`), `people`, `catalog`, `activity` (`ComingSoonPage` "Sắp có"), `*` `NotFoundPage`; ngoài cùng `*` `NotFoundPage`.
  - `RequireUser`: chưa có `userId` → `<Navigate to="/" state={{ from }}>`; đăng nhập xong quay lại `from`.
  - `StartPage`: chưa có người dùng → `LoginPage`. Đã có → về không gian dùng lần trước (`localStorage 'hp.lastWorkspaceId'`, nếu còn trong danh sách), không thì không gian đầu tiên; chưa có không gian nào → form "Tạo không gian đầu tiên".
  - `LoginPage`: hai phần "Tạo người dùng" (email, tên hiển thị → `POST /users`; 409 → "Email này đã có người dùng") và "Tôi đã có id" (ô nhập id → kiểm tra; 404 → "Không tìm thấy người dùng").
  - `AppLayout`: `Sidebar` của shadcn; trên cùng `WorkspaceSwitcher` (tên không gian, danh sách để chuyển, "Tạo không gian"), giữa là chỗ cho cây (Task 4), cuối là liên kết Người, Danh mục, Hoạt động và `UserMenu` (tên hiển thị, "Đổi người dùng"). `workspaceId` trên URL không có trong `useMyWorkspaces()` → cả trang là "Không tìm thấy không gian" kèm liên kết về `/`, không gọi registry. `GET /users/me` trả 404 (id đã lưu không còn) → `signOut`.
  - `WorkspaceHomePage`: "Chọn một địa điểm trên cây" + nút "Thêm địa điểm" (Task 6 gắn form; ở task này nút bị vô hiệu). Khi `usePlaceTypes` trả danh sách rỗng: "Không gian chưa có danh mục" + nút "Tạo danh mục mặc định" (`useSetupWorkspace`).
  - `ErrorState`: `{ error: ApiError; onRetry?: () => void }`, hiện `detail`, mã request nếu có, nút "Thử lại". `FormError`: `{ messages: string[] }`, khối lỗi chung của một form (`role="alert"`).

- [ ] Test:
  - `redirectsToLoginWithoutUser`: route `/w/1` khi chưa có người dùng → thấy màn hình đăng nhập; không request nào đi ra (không handler nào được đăng ký, `onUnhandledRequest: 'error'`).
  - `createsAUserAndEntersTheirWorkspace`: điền email, tên → `POST /users` → `GET /workspaces` trả một không gian → URL thành `/w/<id>`; `localStorage['hp.userId']` có id.
  - `existingIdIsChecked`: id không tồn tại (404) → "Không tìm thấy người dùng", chưa vào phiên.
  - `returnsToTheRequestedPageAfterLogin`: mở `/w/7/people` khi chưa đăng nhập → đăng nhập → URL lại là `/w/7/people`.
  - `firstWorkspaceIsCreatedFromTheStartPage` và `createWorkspaceCallsIdentityThenSetup`: thứ tự request đúng `POST /api/identity/workspaces` rồi `POST /api/registry/setup` với `X-Workspace-Id` của không gian mới; xong thì URL là `/w/<id mới>`.
  - `switcherNavigates`: chọn không gian khác → URL đổi.
  - `unknownWorkspaceShowsNotFound`: `/w/999` không có trong danh sách → "Không tìm thấy không gian".
  - `offersSetupWhenCatalogIsEmpty`: `GET /place-types` trả `[]` → bấm "Tạo danh mục mặc định" → `POST /setup` → danh mục được tải lại, lời mời biến mất.
  - `signOutClearsCachedData`: người A thấy "Nhà A"; "Đổi người dùng"; vào bằng người B (server trả "Nhà B") → chỉ thấy "Nhà B".
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add session, routing, layout and workspace switching`.

### Task 4: Cây địa điểm

**Files:** `frontend/src/features/places/{api.ts,queries.ts,types.ts,index.ts,components/PlaceTree.tsx,components/PlaceTreeNode.tsx,components/PlaceSearchResults.tsx}`, `frontend/src/components/common/LoadMoreButton.tsx`; gắn vào `AppLayout`; test `frontend/src/features/places/PlaceTree.test.tsx`.

**Interfaces:**
- Consumes: `useCursorList`, `Present`, `workspaceHeaders` (Task 2); `useWorkspaceId`, `renderApp` (Task 3).
- Produces:

```ts
// types.ts
export type PlaceSummary = Present<S['PlaceResponse'], 'id' | 'placeTypeId' | 'name' | 'status' | 'depth' | 'version'>
export type PlaceRef = Present<S['PlaceRef'], 'id' | 'name'>
export type Address = Present<S['AddressResponse'], 'countryCode' | 'fullAddress'>
export type PlaceDetail = Omit<Present<S['PlaceDetailResponse'], 'id' | 'placeTypeId' | 'path' | 'depth' | 'name' | 'status' | 'createdAt' | 'createdBy' | 'updatedAt' | 'updatedBy' | 'version'>, 'ancestors' | 'address' | 'effectiveAddress'>
	& { ancestors: PlaceRef[]; address: Address | null; effectiveAddress: { fromPlaceId: string; address: Address } | null }
// api.ts (Task 6–7 thêm hàm); danh sách của cây lấy limit 50
export function listTopLevel(ws: string, cursor?: string): Promise<CursorPage<PlaceSummary>>
export function listChildren(ws: string, parentId: string, cursor?: string): Promise<CursorPage<PlaceSummary>>
export function searchPlaces(ws: string, name: string, cursor?: string): Promise<CursorPage<PlaceSummary>>
export function getPlace(ws: string, id: string): Promise<PlaceDetail>
// queries.ts
export const placeKeys = {
	all: (ws: string) => ['places', ws] as const,
	lists: (ws: string) => ['places', ws, 'children'] as const,
	children: (ws: string, parentId: string | null) => ['places', ws, 'children', parentId ?? 'root'] as const,
	details: (ws: string) => ['places', ws, 'detail'] as const,
	detail: (ws: string, id: string) => ['places', ws, 'detail', id] as const,
	searches: (ws: string) => ['places', ws, 'search'] as const,
	search: (ws: string, name: string) => ['places', ws, 'search', name] as const,
}
export function useChildren(ws: string, parentId: string | null, options?: { enabled?: boolean }): CursorList<PlaceSummary>
export function usePlace(ws: string, id: string | undefined): UseQueryResult<PlaceDetail, ApiError>   // id sai dạng: không gọi
export function usePlaceSearch(ws: string, name: string): CursorList<PlaceSummary>                    // name rỗng: không gọi
export function isId(value: string | undefined): value is string                                      // /^\d+$/
```

  - Cây: cấp cao nhất là `useChildren(ws, null)`; mỗi nút chỉ tải con khi được mở (`enabled`); dòng "Tải thêm" khi `hasMore`. Nút đang chọn theo `placeId` trên URL mang `aria-current="page"`. Bấm tên → `/w/<ws>/places/<id>`; bấm mũi tên → mở hoặc đóng.
  - Mũi tên: hiện khi chưa biết; nút đã tải xong mà không có con thì thay bằng một chấm (không còn mở được). Trạng thái mở đóng giữ trong `PlaceTree`, đặt lại khi đổi không gian.
  - Mở thẳng một địa điểm sâu: `usePlace(ws, placeId)` cho `ancestors`; các tổ tiên được mở sẵn mỗi khi địa điểm đang chọn đổi. Nếu địa điểm đang chọn không nằm trong các trang đã tải của cha nó, hiện nó thành một dòng thêm ở cuối danh sách đó (trước "Tải thêm"); khi trang chứa nó được tải thì dòng thêm tự mất.
  - Ô "Tìm địa điểm…": gõ ít nhất 1 ký tự, chờ 300 ms sau lần gõ cuối, rồi `usePlaceSearch` (tên bắt đầu bằng) thay cây bằng danh sách kết quả (tên, mã); xoá ô thì cây hiện lại.
  - Không gian chưa có địa điểm nào: "Chưa có địa điểm" + nút "Thêm địa điểm" (Task 6 gắn form).

- [ ] Test:
  - `showsTopLevelPlacesInServerOrder`.
  - `expandingLoadsChildrenOnce`: mở, đóng, mở lại → `GET /places/{id}/children` chỉ một lần.
  - `emptyNodeLosesItsArrow`: mở một phòng không có con → không còn nút mở của phòng đó.
  - `loadMoreAppendsWithoutDuplicates`: trang 1 `[P101, P102]` cursor "c1", trang 2 `[P102, P103]` → đủ ba, mỗi phòng một dòng.
  - `navigatesWithTheExactId`: bấm nút có id `114540094935228416` → URL chứa đúng chuỗi đó.
  - `opensAncestorsOfTheSelectedPlace`: mở thẳng `/w/1/places/<P301>` → cây hiện Toà A › Tầng 3 › P301, P301 có `aria-current`.
  - `showsTheSelectedPlaceBeyondTheLoadedPage`: P301 không nằm trong trang đầu của Tầng 3 → vẫn có một dòng P301.
  - `emptyWorkspaceShowsAddButton`.
  - `searchReplacesTheTreeWithResults`: gõ "P10" → gọi `GET /places/search?name=P10`, thấy kết quả; xoá ô → cây hiện lại.
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the lazily loaded place tree`.

### Task 5: Trang một địa điểm (xem)

**Files:** `frontend/src/features/places/{pages/PlacePage.tsx,components/PlaceHeader.tsx,components/PlaceOverviewTab.tsx,components/PlaceChildrenTab.tsx}`; thêm vào `frontend/src/features/catalog/{api.ts,queries.ts,types.ts,index.ts}` và tạo `components/AttributeView.tsx`; sửa `app/router.tsx`; test `frontend/src/features/places/PlacePage.test.tsx`, `frontend/src/features/catalog/AttributeView.test.tsx`.

**Interfaces:**
- Consumes: `usePlace`, `useChildren`, `isId`, `placeKeys` (Task 4); `usePlaceTypes` (Task 3).
- Produces:

```ts
// catalog
export type FieldDef = Present<S['FieldDefResponse'], 'id' | 'target' | 'typeId' | 'fieldKey' | 'label' | 'dataType' | 'required' | 'sortOrder' | 'status'>
export function useFieldDefs(ws: string, target: 'PLACE' | 'PERSON' | 'ITEM', typeId: string | undefined): UseQueryResult<FieldDef[], ApiError>
	// GET /field-defs?target=&typeId= (mục chung và mục của loại đó); chưa có typeId thì không gọi
export function AttributeView(props: { defs: FieldDef[]; values: Record<string, unknown> | null }): JSX.Element
	// theo sortOrder; mục ACTIVE luôn hiện ("—" khi trống); mục ARCHIVED chỉ hiện khi có giá trị, chữ mờ;
	// BOOLEAN "Có"/"Không"; DATE dd/MM/yyyy; khoá không có mục thì bỏ qua
```

  - Trang: đường dẫn tổ tiên (`Breadcrumb` của shadcn, từ `ancestors`, mỗi phần là liên kết), tên, loại (tên từ `usePlaceTypes`), trạng thái ("Đang dùng"/"Tạm ngưng"), `v<version>`; các tab "Tổng quan", "Bên trong", "Người ở", "Đồ đạc", "Lịch sử" (ba tab sau hiện "Sắp có"); tab trên URL `?tab=overview|children|people|items|history`, mặc định và giá trị lạ là `overview`.
  - Tổng quan: địa chỉ của nó, hoặc địa chỉ hiệu lực kèm "lấy từ <tên tổ tiên>", hoặc "Chưa có địa chỉ"; mô tả; mục tuỳ biến qua `AttributeView`; người tạo/sửa không hiện (chỉ có id), thời điểm sửa cuối hiện dd/MM/yyyy HH:mm.
  - Bên trong: danh sách con (`useChildren`, cùng khoá cache với cây) có "Tải thêm"; mỗi dòng là liên kết.
  - Đang tải: khung xương. 404 hoặc `placeId` sai dạng → `NotFoundPage` trong khung chính, cây vẫn hiện. Lỗi khác → `ErrorState` có "Thử lại".

- [ ] Test:
  - `showsBreadcrumbAndHeader`.
  - `showsInheritedAddress`: `address` null, `effectiveAddress.fromPlaceId` là toà nhà → "lấy từ Toà A".
  - `rendersAttributesByType`: NUMBER 42 → "42", BOOLEAN true → "Có", DATE "2026-01-31" → "31/01/2026", mục ACTIVE trống → "—", mục ARCHIVED trống không hiện.
  - `tabFollowsTheUrl`: `?tab=children` mở tab Bên trong; bấm "Tổng quan" → URL không còn `tab=children`.
  - `unknownPlaceShowsNotFound` (404); `malformedIdShowsNotFoundWithoutCalling` (`/places/abc`, không request chi tiết nào).
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add the place page`.

### Task 6: Thêm và sửa địa điểm

**Files:** `frontend/src/features/places/components/{PlaceFormDialog.tsx,placeForm.ts}`; `frontend/src/features/catalog/{attributes.ts,components/AttributeFields.tsx}`; `frontend/src/components/common/{VersionConflictAlert.tsx,serverErrors.ts}`; thêm vào `places/api.ts`, `places/queries.ts`; gắn nút ở `PlaceHeader`, `PlaceChildrenTab`, `PlaceTree`, `WorkspaceHomePage`; test `frontend/src/features/places/PlaceForm.test.tsx`, `frontend/src/features/catalog/attributes.test.ts`.

**Interfaces:**
- Consumes: `FieldDef`, `useFieldDefs`, `usePlaceTypes` (catalog); `placeKeys`, `PlaceDetail` (Task 4); `FormError` (Task 3).
- Produces:

```ts
// catalog/attributes.ts — giá trị mục tuỳ biến trong form: chuỗi, hoặc boolean với BOOLEAN
export type AttributeFormValues = Record<string, string | boolean>
export function attributeDefaults(defs: FieldDef[], stored: Record<string, unknown> | null): AttributeFormValues
export function attributeErrors(defs: FieldDef[], values: AttributeFormValues): Record<string, string>
	// chỉ mục ACTIVE: bắt buộc mà trống → 'Bắt buộc'; NUMBER không phải số → 'Phải là số'
export function attributePayload(defs: FieldDef[], values: AttributeFormValues, stored: Record<string, unknown> | null): Record<string, unknown> | null
	// mục ACTIVE: lấy từ form (chuỗi rỗng → bỏ khoá; NUMBER → number; BOOLEAN → boolean; DATE 'YYYY-MM-DD');
	// mục ARCHIVED: giữ giá trị đã lưu; khoá không thuộc defs: bỏ; không còn khoá nào → null
export function AttributeFields(props: { defs: FieldDef[]; control: Control<any>; name: 'attributes' }): JSX.Element
	// chỉ mục ACTIVE, theo sortOrder; TEXT Input, NUMBER Input inputMode=decimal, BOOLEAN Switch, DATE Input type=date,
	// SELECT NativeSelect (có lựa chọn trống); bắt buộc có dấu *; lỗi hiện dưới ô

// components/common/serverErrors.ts
export function applyServerErrors(error: ApiError, setError: (name: string, error: { message: string }) => void, isField: (name: string) => boolean): string[]
	// fieldErrors mà isField(name) → setError; phần còn lại và formErrors → trả về để hiện bằng FormError;
	// lỗi không có errors[] → [error.detail]
export function VersionConflictAlert(props: { onReload: () => void; isReloading: boolean }): JSX.Element
	// "Đã có người sửa trước bạn." + nút "Tải bản mới"

// places
export function createPlace(ws: string, body: S['PlaceCreateRequest']): Promise<PlaceDetail>
export function updatePlace(ws: string, id: string, body: S['PlaceUpdateRequest']): Promise<PlaceDetail>
export function useCreatePlace(ws: string): UseMutationResult<PlaceDetail, ApiError, S['PlaceCreateRequest']>
export function useUpdatePlace(ws: string): UseMutationResult<PlaceDetail, ApiError, { id: string; body: S['PlaceUpdateRequest'] }>
export function PlaceFormDialog(props: { open: boolean; onOpenChange: (open: boolean) => void } & ({ mode: 'create'; parentId: string | null } | { mode: 'edit'; place: PlaceDetail })): JSX.Element
```

  - Form (React Hook Form + `zodResolver(placeFormSchema(defs))`): loại (`NativeSelect`; chỉ loại ACTIVE theo `sortOrder`; khi sửa thì giữ cả loại cũ đã lưu trữ), tên (bắt buộc, tối đa 255), mã (tối đa 50), trạng thái, mô tả (tối đa 10000), mục tuỳ biến theo loại đang chọn (đổi loại thì tải mục của loại mới). `placeFormSchema` đưa kết quả `attributeErrors` vào đường dẫn `attributes.<khoá>`.
  - Thêm từ cây và từ trang trống (cấp cao nhất), và "Thêm chỗ con" ở tab Bên trong (`parentId` là địa điểm đang xem). Thêm xong thì đóng hộp thoại, tới trang của địa điểm mới.
  - Sửa: `version` gửi đi là `version` lúc mở form (hoặc của lần "Tải bản mới" gần nhất), không nằm trong ô nhập. 409 version lệch → `VersionConflictAlert`, form không đóng, không đặt lại. "Tải bản mới" → lấy lại chi tiết, `reset(giá trị mới, { keepDirtyValues: true })`, dùng `version` mới.
  - 409 khác và 400 không gắn được vào ô nào → `FormError` nguyên văn.
  - Cache sau khi ghi:

| Lệnh | Việc với cache |
|---|---|
| Thêm | Đặt `detail(mới)` bằng response; làm cũ `children(cha)` |
| Sửa | Đặt `detail(id)` bằng response; làm cũ `children(cha)`, mọi `detail` khác của không gian (tổ tiên trong đó có thể mang tên cũ), mọi `search` |
| Di chuyển (Task 7) | Đặt `detail(id)` bằng response; làm cũ `placeKeys.all(ws)` trừ `detail(id)` |
| Đặt, gỡ địa chỉ (Task 7) | Đặt `detail(id)` bằng response; làm cũ mọi `detail` khác (con cháu thừa hưởng địa chỉ) |
| Xoá (Task 7) | Làm cũ `children(cha)`, mọi `search`; bỏ `detail(id)` |

- [ ] Test:
  - `attributes.test.ts`: `numberTextBecomesANumber` ("42" → 42, "4,5" → lỗi 'Phải là số'), `blankValuesAreDropped`, `requiredBlankIsAnError`, `archivedValuesAreKept`, `valuesOfOtherTypesAreDropped`, `nothingLeftIsNull`.
  - `createsATopLevelPlace` (body không có `parentId`; URL tới địa điểm mới), `createsAChildFromTheChildrenTab` (body có `parentId`).
  - `blankNameIsStoppedBeforeSending`: để trống tên → "Nhập tên", không có request `POST`.
  - `showsFieldErrorsFromServer`: 400 với `attributes.area_m2` → lỗi hiện dưới ô "Diện tích (m²)".
  - `numberAttributeIsSentAsNumber`: nhập 42 → body `attributes.area_m2 === 42`.
  - `versionConflictKeepsTypedValues`: 409 → ô tên vẫn là chữ vừa nhập, thấy "Đã có người sửa trước bạn".
  - `reloadKeepsMyEditsAndTakesNewVersion`: sau 409, "Tải bản mới" (server trả v5, mô tả khác) → ô tên giữ chữ đã nhập, ô mô tả nhận giá trị mới; lưu lại → body `version === 5`.
  - `renameUpdatesTreeAndHeader`: sửa tên P301 → cây và tiêu đề hiện tên mới.
  - `renameReachesDescendantBreadcrumbs`: xem P301 (đường dẫn có "Toà A"), sang Toà A, đổi tên thành "Toà Alpha", quay lại P301 → đường dẫn có "Toà Alpha".
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add creating and editing places`.

### Task 7: Di chuyển, xoá, địa chỉ

**Files:** `frontend/src/features/places/components/{PlaceActionsMenu.tsx,MovePlaceDialog.tsx,DeletePlaceDialog.tsx,AddressDialog.tsx}`; thêm vào `places/api.ts`, `places/queries.ts`; gắn vào `PlaceHeader`, `PlaceOverviewTab`; test `frontend/src/features/places/PlaceActions.test.tsx`.

**Interfaces:**
- Consumes: `VersionConflictAlert`, `applyServerErrors`, `FormError`, `placeKeys`, `usePlaceSearch`, bảng cache của Task 6.
- Produces:

```ts
export function movePlace(ws: string, id: string, body: S['PlaceMoveRequest']): Promise<PlaceDetail>
export function deletePlace(ws: string, id: string, version: number): Promise<void>
export function setAddress(ws: string, id: string, body: S['PlaceAddressRequest']): Promise<PlaceDetail>
export function removeAddress(ws: string, id: string, version: number): Promise<PlaceDetail>
export function useMovePlace(ws: string), useDeletePlace(ws: string), useSetAddress(ws: string), useRemoveAddress(ws: string)
```

  - Menu `⋯` trên đầu trang địa điểm: Di chuyển, Địa chỉ, Xoá.
  - Di chuyển: hộp thoại có lựa chọn "Lên cấp cao nhất" (ẩn khi đang ở cấp cao nhất) và ô tìm địa điểm; kết quả không gồm chính nó; chọn một kết quả rồi "Chuyển". Backend chặn con cháu và độ sâu quá 20: 409 hiện nguyên văn.
  - Xoá: `AlertDialog` xác nhận có tên địa điểm; 409 "still has …" hiện nguyên văn trong hộp thoại; xong thì về trang của cha (hoặc trang chủ không gian), báo "Đã xoá <tên>".
  - Địa chỉ: form gồm địa chỉ đầy đủ (bắt buộc, tối đa 500), đường, phường/xã, tỉnh/thành, vĩ độ, kinh độ (phải có cả hai hoặc không có cả hai; −90…90, −180…180). Các trường của địa chỉ cũ mà form không có ô (mã tỉnh, quận…) được gửi lại nguyên. Nút "Gỡ địa chỉ" khi địa điểm có địa chỉ riêng. Tổng quan có nút "Thêm địa chỉ"/"Sửa địa chỉ" mở cùng hộp thoại.
  - 409 version lệch ở cả ba hộp thoại → `VersionConflictAlert`; "Tải bản mới" lấy lại chi tiết, hộp thoại dùng `version` mới.

- [ ] Test: `movesToAnotherParent` (body `{version, parentId}`; cây hiện nó dưới cha mới), `movesToTheTop` (`parentId: null`), `moveShowsBusinessConflict`, `deleteShowsBusinessConflict` (409 nguyên văn, vẫn ở trang cũ), `deleteReturnsToParent` (URL về cha; không còn request `GET` chi tiết của địa điểm đã xoá), `setsAnAddress` (body có `version` và `address.fullAddress`), `coordinatesMustComeTogether` (chỉ nhập vĩ độ → lỗi, không gửi), `removesTheAddress` (`DELETE …/address?version=`), `staleVersionCanBeReloaded` (409 version ở hộp thoại xoá → "Tải bản mới" → xoá lại gửi `version` mới).
- [ ] Hỏng → viết → PASS; build, lint sạch. Commit `Add moving, deleting and addressing places`.

### Task 8: Tài liệu, kiểm tra, chạy thử

- [ ] `frontend/README.md` viết lại: cách chạy (`npm run dev` với hai service đang chạy), `npm test`, `npm run api:sync`, cấu trúc thư mục và luật phụ thuộc (spec mục 7), cách khai báo kiểu response (`Present`), cách thêm một feature. README gốc thêm một dòng trỏ tới đó.
- [ ] `npm test`, `npm run build`, `npm run lint`: sạch.
- [ ] Chạy thử trên backend thật: bật `identity-service`, `registry-service` và `npm run dev`; qua giao diện (trình duyệt tự động) tạo người dùng, không gian, một toà nhà, một tầng, một phòng; đổi tên, đặt địa chỉ cho toà nhà, xem phòng thừa hưởng địa chỉ; chụp màn hình. Dừng tất cả; xoá các dòng vừa tạo (thành viên, không gian, người dùng bên identity; dòng của không gian đó bên registry), chỉ theo đúng `id` vừa tạo.
- [ ] Commit `Document the frontend` và push `main`.

## Để lại cho backend

Không chặn plan này; mỗi việc làm xong thì bỏ được một phần xử lý ở frontend.

- OpenAPI ghi rõ trường nào của response luôn có (`required`) và trường nào có thể `null`: khi đó bỏ được các khai báo `Present<…>`.
- `PlaceResponse` thêm `hasChildren` (hoặc `childCount`): cây biết trước nút nào là lá.
- Kết quả `GET /places/search` kèm tên tổ tiên: phân biệt được hai phòng "P301" ở hai toà.
- Lỗi 409 có một mã máy đọc (ví dụ `code: "VERSION_CONFLICT"`): frontend đang nhận ra bằng đuôi của `detail`.
