import { useState } from 'react'
import { useNavigate } from 'react-router'

import { ToneBadge } from '@/components/common/ToneBadge'
import { Command, CommandGroup, CommandInput, CommandItem, CommandList } from '@/components/ui/command'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog'
import { useItemQuickFind } from '@/features/items'
import { usePersonQuickFind } from '@/features/people'
import { usePlaceQuickFind } from '@/features/places'
import { useDebouncedValue } from '@/hooks/use-debounced-value'
import { MIN_SEARCH_LENGTH } from '@/lib/searchText'

import { groupResults, type SearchRow } from '../groupResults'

const SHORTCUTS = [
  { label: 'Người', path: 'people' },
  { label: 'Địa điểm', path: 'places' },
  { label: 'Đồ đạc', path: 'items' },
  { label: 'Danh mục', path: 'catalog' },
  { label: 'Hoạt động', path: 'activity' },
  { label: 'Thùng rác', path: 'trash' },
]

/** Trang danh sách mở được với `?q=` (spec Đợt 11, S7), theo thứ tự hiện. */
const CONTENT_SEARCHES = [
  { label: 'Người', path: 'people' },
  { label: 'Đồ đạc', path: 'items' },
  { label: 'Địa điểm', path: 'places' },
]

/**
 * Ô tìm chung: địa điểm, người, đồ đạc theo tên (bắt đầu bằng) và theo mã (đúng mã). Gọi API sau khi
 * người dùng ngừng gõ 300 ms; mỗi loại một nhóm, lỗi của một loại không làm hỏng các loại khác. Từ 2
 * ký tự, cuối danh sách có "Tìm … trong" từng trang danh sách: tìm cả trong ghi chú, mô tả.
 */
export function SearchPalette({ ws, open, onOpenChange }: { ws: string; open: boolean; onOpenChange: (open: boolean) => void }) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="top-1/4 translate-y-0 overflow-hidden p-0 sm:max-w-xl" showCloseButton={false}>
        <DialogTitle className="sr-only">Tìm nhanh</DialogTitle>
        <Palette ws={ws} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  )
}

function Palette({ ws, onDone }: { ws: string; onDone: () => void }) {
  const navigate = useNavigate()
  const [text, setText] = useState('')
  const typed = text.trim()
  const settled = useDebouncedValue(typed, 300)
  const term = typed === '' ? '' : settled
  const places = usePlaceQuickFind(ws, term)
  const people = usePersonQuickFind(ws, term)
  const items = useItemQuickFind(ws, term)
  const groups = groupResults(ws, { places: places.data, people: people.data, items: items.data })
  const loading = places.isFetching || people.isFetching || items.isFetching || typed !== term
  const nothing =
    term !== '' &&
    !loading &&
    groups.codeMatches.length + groups.places.length + groups.people.length + groups.items.length === 0 &&
    !places.isError &&
    !people.isError &&
    !items.isError

  function go(href: string) {
    onDone()
    void navigate(href)
  }

  return (
    // cmdk đặt tên ô nhập bằng `label` của Command (aria-labelledby), không dùng aria-label của ô.
    <Command shouldFilter={false} loop label="Tìm địa điểm, người, đồ đạc">
      <CommandInput placeholder="Tìm địa điểm, người, đồ đạc…" value={text} onValueChange={setText} />
      <CommandList>
        {typed === '' ? (
          <CommandGroup heading="Đi tới">
            {SHORTCUTS.map((shortcut) => (
              <CommandItem key={shortcut.path} value={`goto:${shortcut.path}`} onSelect={() => go(`/w/${ws}/${shortcut.path}`)}>
                {shortcut.label}
              </CommandItem>
            ))}
          </CommandGroup>
        ) : (
          <>
            <Group heading="Khớp mã" rows={groups.codeMatches} onSelect={go} />
            <Group heading="Địa điểm" rows={groups.places} failed={places.isError} onSelect={go} />
            <Group heading="Người" rows={groups.people} failed={people.isError} onSelect={go} />
            <Group heading="Đồ đạc" rows={groups.items} failed={items.isError} onSelect={go} />
            {/* Không dùng CommandEmpty: nó chỉ hiện khi danh sách không có dòng nào, mà "Tìm trong nội dung" luôn có. */}
            {nothing && <p className="py-6 text-center text-sm">Không tìm thấy gì.</p>}
            {/* Chỉ khi kết quả đã về: hiện sớm hơn thì Enter chọn nhầm dòng này thay cho kết quả đầu. */}
            {!loading && [...typed].length >= MIN_SEARCH_LENGTH && (
              <CommandGroup heading="Tìm trong nội dung">
                {CONTENT_SEARCHES.map((target) => (
                  <CommandItem
                    key={target.path}
                    value={`content:${target.path}`}
                    onSelect={() => go(`/w/${ws}/${target.path}?q=${encodeURIComponent(typed)}`)}
                  >
                    Tìm “{typed}” trong {target.label}
                  </CommandItem>
                ))}
              </CommandGroup>
            )}
          </>
        )}
      </CommandList>
    </Command>
  )
}

function Group({
  heading,
  rows,
  failed = false,
  onSelect,
}: {
  heading: string
  rows: SearchRow[]
  failed?: boolean
  onSelect: (href: string) => void
}) {
  if (failed) {
    return <p className="px-3 py-2 text-sm text-destructive">Không tìm được {heading}</p>
  }
  if (rows.length === 0) return null
  return (
    <CommandGroup heading={heading}>
      {rows.map((row) => (
        <CommandItem key={row.key} value={row.key} onSelect={() => onSelect(row.href)}>
          <span className="flex min-w-0 flex-1 flex-col">
            <span className="truncate">{row.title}</span>
            {row.detail && <span className="truncate text-xs text-muted-foreground">{row.detail}</span>}
          </span>
          {row.removed && <ToneBadge tone="neutral">Đã gỡ</ToneBadge>}
          {row.code && <span className="shrink-0 text-xs text-muted-foreground tabular-nums">{row.code}</span>}
        </CommandItem>
      ))}
    </CommandGroup>
  )
}
