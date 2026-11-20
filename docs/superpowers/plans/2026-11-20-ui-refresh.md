# UI Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Màu mang nghĩa cho trạng thái và hành động, font Be Vietnam Pro, chế độ sáng tối, và dùng `avatar`, `tooltip`, `card`, `progress`, `chart`, `hover-card`, `scroll-area`, `alert` đúng chỗ, không đổi nghiệp vụ hay API.

**Architecture:** Màu đổi qua token CSS trong `src/index.css` (sáng và `.dark`), thêm token ngữ nghĩa `--success`, `--warning`, `--danger`, `--info`, `--violet`. Một `ToneBadge` dùng chung nhận `tone`; mỗi feature giữ bảng nghĩa màu cạnh bảng nhãn trong `labels.ts`. Chế độ sáng tối là một store nhỏ ở `app/theme.ts` gắn lớp `dark` lên `<html>`. `components/ui` không sửa tay.

**Tech Stack:** như `2026-11-17-overview-and-export.md` (frontend); thêm `@fontsource/be-vietnam-pro`, bỏ `@fontsource-variable/geist`. `recharts` đã có sẵn (component `chart`).

**Spec:** `docs/superpowers/specs/2026-11-20-ui-refresh-design.md`. Lộ trình các đợt sau: `docs/superpowers/specs/2026-11-20-roadmap-design.md`.

## Global Constraints

- Lệnh: `npm` chạy bằng Node của Windows (`cmd.exe /c "npm test"` trong `frontend/`). Cuối mỗi task: `npm test`, `npm run build`, `npm run lint` (0 lỗi; 19 cảnh báo cũ trong `components/ui`, `hooks` được phép). Mỗi task một commit; push `main` chỉ khi bạn đồng ý.
- `components/ui` không sửa tay (README frontend). Mọi thứ mới nằm ở `components/common`, `lib`, `app`, `features`.
- Bảng màu (spec 3.2): xanh cổ vịt `#0F6B73` (primary), mực `#172A30` (foreground), nền giấy lạnh `#F3F6F7` (background), thanh bên đêm `#0F2E33` (sidebar), vàng đèn lồng `#C98510` (warning); success `#1E8A57`, danger `#C8352A`, info `#2F6BD6`, violet `#7650C2`. Chữ trên nền màu đạt WCAG AA.
- Nghĩa màu (spec 3.2), dùng giống nhau ở mọi trang:
  - đồ: `IN_USE` success, `BROKEN` danger, `STORED` warning, `REMOVED` neutral;
  - địa điểm: `ACTIVE` success, `INACTIVE` neutral;
  - cư trú và sở hữu: `CURRENT` success, `UPCOMING` warning, `PAST` neutral;
  - hành động: `CREATE` success, `UPDATE` info, `DELETE` danger, `RESTORE` violet;
  - danh mục: đang dùng success, đã lưu trữ neutral; "Hiện tại" (version) primary; giới tính, loại địa điểm, nhóm đồ neutral.
- Màu không thay chữ: nhãn vẫn ghi đủ chữ như hiện nay; `ToneBadge` gắn `data-tone="<tone>"`.
- Chữ mới: "Giao diện", "Sáng", "Tối", "Theo máy", "Thông tin", "Mục tuỳ biến", "Địa điểm" (nhãn nhóm cây), "Có người", "Trống". Không viết hoa toàn bộ nhãn nào.
- Số đếm, mã, ngày: lớp `tabular-nums`; không dùng font monospace.
- Trang in (`features/print`) giữ đen trắng: không dùng `ToneBadge`.

## Review Focus

1. Tên tiếng Việt lạ cho `avatar`: một chữ ("An"), khoảng trắng thừa, chữ thường, "Đỗ Mai Phương", tên rỗng: chữ cái đầu đúng ("A", "NL", "ĐP", "?"), không vỡ. Test: `initialsOfVietnameseNames` (Task 2).
2. Trình duyệt chặn `localStorage` (chế độ riêng tư): chọn chế độ vẫn đổi được trong phiên, mở lại thì theo máy, không lỗi. Test: `blockedStorageStillSwitchesTheme` (Task 1).
3. "Theo máy" và người dùng đổi giao diện của máy khi trang đang mở: trang đổi theo ngay. Test: `systemModeFollowsTheMachineLive` (Task 1).
4. Một loại địa điểm có 0 chỗ có người, hay mọi chỗ đều có người: thanh tiến độ 0% và 100%, phần trăm làm tròn (2/3 → 67%). Test: `occupancyIsShownAsAShareOfPlaces` (Task 4).
5. Chế độ tối: mọi nhãn màu và chữ phụ còn đọc được. Không test tự động được tương phản; Task 6 chụp màn hình chế độ tối các trang chính và sửa chỗ thiếu tương phản.

---

### Task 1: Token màu, font, chế độ sáng tối

**Files:** sửa `frontend/package.json`, `frontend/src/index.css`, `frontend/src/app/main.tsx`, `frontend/src/app/App.tsx` (thêm `TooltipProvider`), `frontend/src/app/layout/UserMenu.tsx`; tạo `frontend/src/app/theme.ts`; test `frontend/src/app/theme.test.ts`, thêm vào `frontend/src/app/app.test.tsx`.

**Interfaces:**
- Produces:

```ts
// app/theme.ts
export type ThemeMode = 'light' | 'dark' | 'system'
export const themeStore: {
  getMode(): ThemeMode                 // đã lưu ở localStorage 'hp.theme'; lạ hoặc không đọc được → 'system'
  setMode(mode: ThemeMode): void       // lưu (bỏ qua nếu bị chặn) rồi áp dụng
  subscribe(listener: () => void): () => void
  apply(): void                        // gắn/bỏ lớp 'dark' trên document.documentElement theo mode và prefers-color-scheme
}
export function useThemeMode(): [ThemeMode, (mode: ThemeMode) => void]
// token CSS mới (sáng và .dark): --success, --success-foreground, --warning, --warning-foreground,
// --danger(= --destructive), --info, --info-foreground, --violet, --violet-foreground;
// lớp Tailwind tương ứng qua @theme inline: bg-success, text-success, …
```

- `main.tsx` gọi `themeStore.apply()` trước khi render; `themeStore` lắng nghe `matchMedia('(prefers-color-scheme: dark)')` khi mode là `system`.
- `UserMenu`: thêm nút biểu tượng "Giao diện" (mở `DropdownMenu` có nhóm chọn một: Sáng, Tối, Theo máy). Nút "Đổi người dùng" giữ nguyên tên (test hiện có bấm nó).

- [ ] Test `theme.test.ts`: `defaultsToSystem`, `choosingDarkAddsTheClassAndRemembersIt` (`setMode('dark')` → `documentElement` có lớp `dark`, `localStorage['hp.theme'] === 'dark'`), `systemModeFollowsTheMachineLive` (giả `matchMedia` có `addEventListener('change')`; phát change matches=true → có lớp `dark`), `blockedStorageStillSwitchesTheme` (`localStorage.setItem` ném lỗi → `setMode('dark')` vẫn gắn lớp, không ném).
- [ ] Test `app.test.tsx`: `themeMenuSwitchesToDark` (mở nút "Giao diện", chọn "Tối" → `document.documentElement` có lớp `dark`).
- [ ] Hỏng → viết → PASS. Đổi token sáng và tối trong `index.css` theo Global Constraints; font Be Vietnam Pro 400/500/600/700 (`--font-sans`); bỏ Geist. Ba lệnh npm sạch. Commit `Add the colour tokens, Be Vietnam Pro and a dark mode`.

### Task 2: Nhãn có màu và avatar

**Files:** tạo `frontend/src/components/common/ToneBadge.tsx`, `frontend/src/components/common/PersonAvatar.tsx`, `frontend/src/lib/initials.ts`; sửa `features/items/labels.ts`, `features/places/labels.ts`, `features/people/labels.ts`, `features/ownership/labels.ts`, `features/history/labels.ts` (+ `index.ts` của từng feature nếu cần xuất); dùng `ToneBadge` ở `features/activity/components/ActivityRow.tsx`, `features/catalog/components/CatalogSection.tsx`, `features/history/components/HistoryTab.tsx`, `features/items/components/ItemList.tsx`, `features/items/pages/ItemPage.tsx`, `features/ownership/components/OwnershipList.tsx`, `features/people/components/OccupancyList.tsx`, `features/people/pages/PersonPage.tsx`, `features/places/components/PlaceHeader.tsx`, `features/places/components/PlaceSubtreeTable.tsx`, `features/search/components/SearchPalette.tsx`; test `frontend/src/lib/initials.test.ts`, thêm vào `features/items/ItemsPage.test.tsx`, `features/activity/ActivityPage.test.tsx`, `features/people/PlaceOccupants.test.tsx`.

**Interfaces:**
- Produces:

```ts
// components/common/ToneBadge.tsx
export type Tone = 'success' | 'warning' | 'danger' | 'info' | 'violet' | 'neutral' | 'primary'
export function ToneBadge(props: { tone: Tone; children: ReactNode; className?: string }): JSX.Element  // <Badge> + data-tone + nền nhạt, chữ đậm màu
// lib/initials.ts
export function initials(name: string | null | undefined): string   // chữ đầu của từ đầu + từ cuối, viết hoa theo 'vi'; rỗng → '?'
// components/common/PersonAvatar.tsx
export function PersonAvatar(props: { name: string | null | undefined; size?: 'sm' | 'default' | 'lg' }): JSX.Element
  // <Avatar><AvatarFallback> chữ cái đầu; màu nền chọn ổn định theo tên trong 6 sắc nhạt; aria-hidden (tên đã có chữ bên cạnh)
// labels: ITEM_STATUS_TONE, PLACE_STATUS_TONE, OCCUPANCY_STATUS_TONE, OWNERSHIP_STATUS_TONE, ACTION_TONE: Record<…, Tone>
```

- [ ] Test `initials.test.ts`: `initialsOfVietnameseNames`: `"Nguyễn Thị Lan"` → `"NL"`, `"An"` → `"A"`, `"  trần   văn minh "` → `"TM"`, `"Đỗ Mai Phương"` → `"ĐP"`, `""` và `null` → `"?"`.
- [ ] Test trang: `brokenItemIsMarkedDanger` (ItemsPage: nhãn "Hỏng" có `data-tone="danger"`, "Đang dùng" `success`); `actionsHaveTheirTone` (ActivityPage: "Xoá" `danger`, "Sửa" `info`, "Khôi phục" `violet`); `currentResidentIsMarkedSuccess` (PlaceOccupants: "Đang ở" `success`).
- [ ] Hỏng → viết → PASS; mọi `Badge` mang trạng thái/hành động trong danh sách file trên đổi sang `ToneBadge` theo Global Constraints. Ba lệnh npm sạch. Commit `Give statuses and actions their colours`.

### Task 3: Khung ứng dụng

**Files:** sửa `frontend/src/app/layout/AppLayout.tsx`, `frontend/src/app/layout/WorkspaceSwitcher.tsx`, `frontend/src/app/layout/UserMenu.tsx`; test thêm vào `frontend/src/app/app.test.tsx`.

**Interfaces:**
- Consumes: `PersonAvatar` (Task 2), `themeStore` (Task 1), token `--sidebar*` (Task 1).
- Thanh bên (spec 3.4): `SidebarHeader` không gian (ô chữ cái đầu màu primary + tên); `SidebarContent`: nhóm điều hướng chính trước (Tổng quan … Thùng rác), rồi nhóm "Địa điểm" chứa `PlaceTree` trong `ScrollArea`; `SidebarFooter`: `UserMenu` với `PersonAvatar`.
- Đầu trang: `SidebarTrigger` và các nút chỉ có biểu tượng bọc `Tooltip` (nội dung bằng đúng `aria-label` của nút).

- [ ] Test `app.test.tsx`: `iconButtonsExplainThemselves` (rê chuột lên "Ẩn hoặc hiện thanh bên" → hiện tooltip cùng chữ); `navigationComesBeforeThePlaceTree` (trong thanh bên, liên kết "Tổng quan" đứng trước cây "Cây địa điểm" theo thứ tự tài liệu: `compareDocumentPosition`). Test thanh bên có sẵn vẫn xanh.
- [ ] Hỏng → viết → PASS; ba lệnh npm sạch. Commit `Rework the app shell: navigation first, avatars and tooltips`.

### Task 4: Trang tổng quan

**Files:** sửa `frontend/src/features/overview/components/OverviewPage.tsx` (có thể tách `StatTile.tsx`, `OccupancyByType.tsx`, `ItemsByStatusChart.tsx` trong `features/overview/components/`), `frontend/src/features/activity/components/RecentActivity.tsx`; test thêm vào `frontend/src/features/overview/OverviewPage.test.tsx`.

**Interfaces:**
- Consumes: `ToneBadge`, `PersonAvatar`, `ITEM_STATUS_TONE`, `ACTION_TONE` (Task 2); `Card`, `Progress`, `ChartContainer`, `Alert`.
- Bốn khối số (spec 3.6): biểu tượng trong ô nền nhạt (địa điểm primary, người đang ở success, đồ đang dùng success, đồ hỏng danger); số `tabular-nums` cỡ lớn; liên kết như hiện có.
- "Chỗ ở theo loại": mỗi loại một dòng: tên, `Progress` (giá trị `round(occupied / total * 100)`, `aria-label="Tỷ lệ có người của <tên loại>"`), chữ "`occupied` có người · `total − occupied` trống" giữ nguyên.
- "Đồ đạc theo trạng thái": biểu đồ cột ngang (`ChartContainer` + recharts `BarChart`, màu theo `ITEM_STATUS_TONE`) cạnh danh sách liên kết hiện có (test vẫn tìm các liên kết `?status=`).
- "Cần xử lý": `Alert` sắc warning.
- `RecentActivity`: mỗi dòng có chấm màu theo `ACTION_TONE` và `PersonAvatar` cỡ nhỏ cho "Bạn"/"Người dùng khác".

- [ ] Test `OverviewPage.test.tsx`: `occupancyIsShownAsAShareOfPlaces` (Phòng 2/3 → `progressbar` tên "Tỷ lệ có người của Phòng" có `aria-valuenow="67"`; loại 0/1 → `0`; loại 3/3 → `100`). Test có sẵn vẫn xanh.
- [ ] Hỏng → viết → PASS; ba lệnh npm sạch, không có cảnh báo kích thước biểu đồ làm test đỏ. Commit `Redesign the overview with coloured tiles, occupancy bars and a status chart`.

### Task 5: Trang chi tiết, danh sách người

**Files:** sửa `features/places/components/PlaceOverviewTab.tsx`, `features/places/components/PlaceHeader.tsx`, `features/people/pages/PersonPage.tsx`, `features/people/components/PersonInfoTab.tsx`, `features/people/pages/PeoplePage.tsx`, `features/people/components/OccupancyList.tsx`, `features/ownership/components/OwnershipList.tsx`, `features/items/pages/ItemPage.tsx`; tạo `features/people/components/PersonHoverCard.tsx` (xuất qua `features/people/index.ts`); test thêm vào `features/places/PlacePage.test.tsx`, `features/people/PlaceOccupants.test.tsx`, `features/items/ItemPage.test.tsx` (hoặc file test trang món đồ có sẵn).

**Interfaces:**
- Consumes: `ToneBadge`, `PersonAvatar` (Task 2), `usePerson`.
- Produces:

```ts
// features/people
export function PersonHoverCard(props: { ws: string; personId: string; name: string }): JSX.Element
  // liên kết tên tới /w/:ws/people/:id; rê chuột (HoverCard) thì tải usePerson và hiện avatar, số điện thoại, email
```

- Địa điểm, tab Tổng quan: `Card` "Thông tin" (Loại, Mã, Trạng thái, Mô tả), `Card` "Mục tuỳ biến" (`AttributeView`, chỉ khi có mục), rồi khối "Địa chỉ" hiện có.
- Người: đầu trang `PersonAvatar` cỡ lớn; tab thông tin trong `Card`. Danh sách người (`PeoplePage`): mỗi dòng có `PersonAvatar`.
- Người ở và chủ sở hữu: tên người là `PersonHoverCard`, kèm `PersonAvatar`.
- Tab Sở hữu của địa điểm, danh sách hiện tại: `Progress` tổng tỷ lệ của các dòng có tỷ lệ (`aria-label="Tổng tỷ lệ sở hữu"`, giá trị là tổng, tối đa 100); không dòng nào ghi tỷ lệ thì không hiện.
- Món đồ: `Breadcrumb` tới chỗ đặt (tổ tiên của địa điểm nếu có trong dữ liệu trang, không thì tên chỗ đặt); thông tin trong `Card`.

- [ ] Test: `placeOverviewShowsTheInformationCard` (PlacePage: vùng "Thông tin" có Loại "Phòng", Mã, Trạng thái "Đang dùng"); `hoveringAResidentShowsTheirPhone` (PlaceOccupants: rê chuột lên tên → hiện số điện thoại của người đó); `itemPageShowsWhereItIsAsABreadcrumb` (ItemPage: `navigation` tên "breadcrumb" có liên kết tới chỗ đặt); `currentOwnersShowTheirTotalShare` (PlaceOwners: hai chủ 50% + 30% → `progressbar` "Tổng tỷ lệ sở hữu" `aria-valuenow="80"`). Test có sẵn vẫn xanh.
- [ ] Hỏng → viết → PASS; ba lệnh npm sạch. Commit `Show details in cards, avatars and person hover cards`.

### Task 6: Xem bằng mắt, tài liệu

**Files:** sửa `frontend/README.md` (mục "Giao diện": token, nghĩa màu, `ToneBadge`, `PersonAvatar`, chế độ sáng tối, cách chạy bộ xem thử), `docs/superpowers/specs/2026-11-13-frontend-structure-design.md` (thư mục `components/common` mới), đánh dấu spec Đợt 5 "đã duyệt, đã xây"; sửa code nếu ảnh chụp lộ lỗi.

- [ ] Chạy bộ xem thử (identity giả cổng 18081, registry thật cổng 18082 với `--server.port`, Vite cổng 15173 với `HP_IDENTITY_PORT`/`HP_REGISTRY_PORT`, dữ liệu thử ở không gian -9001). Chụp sáng, tối, khổ 390px: tổng quan, một địa điểm (tab Tổng quan, Người ở), đồ đạc, một món đồ, người, hoạt động, so sánh version. Sửa mọi chỗ thiếu tương phản, tràn chữ, lệch hàng; mỗi sửa đổi có hành vi thì kèm test.
- [ ] Ba lệnh npm và `./mvnw test` xanh lần cuối. Commit `Document the new look`. Hỏi bạn trước khi push `main`.
