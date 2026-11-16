import { useEffect, useState, type FocusEvent, type KeyboardEvent } from 'react'

const ROW = '[data-tree-row]'

/** Bỏ dấu và viết thường, để "t" khớp cả "Tầng" lẫn "Toà". */
function fold(text: string): string {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLocaleLowerCase('vi')
}

function levelOf(row: HTMLElement): number {
  return Number(row.getAttribute('aria-level'))
}

/**
 * Phím của cây theo mẫu ARIA "tree", với tabindex luân phiên: chỉ một dòng nằm trong thứ tự Tab.
 *
 * Thứ tự các dòng đọc thẳng từ DOM (`[data-tree-row]`, chỉ những dòng đang hiện), nên hook không
 * giữ bản sao nào của cây. Dòng "Đang tải", "Tải thêm", "Không tải được" không phải dòng của cây
 * và bị phím mũi tên bỏ qua.
 *
 * @param preferredId dòng nên nhận Tab khi người dùng chưa đi tới dòng nào (địa điểm đang mở)
 * @param toggle mở hoặc đóng một nhánh
 * @returns `tabStopId`: dòng duy nhất có `tabIndex=0`; `setRoot` (dùng làm `ref`), `onKeyDown`, `onFocus` gắn vào gốc của cây
 */
export function useTreeKeyboard(preferredId: string | undefined, toggle: (id: string) => void) {
  const [root, setRoot] = useState<HTMLElement | null>(null)
  const [focusedId, setFocusedId] = useState<string | null>(null)
  // Đổi địa điểm đang mở thì Tab quay về dòng của nó.
  const [preferredSeen, setPreferredSeen] = useState(preferredId)
  if (preferredId !== preferredSeen) {
    setPreferredSeen(preferredId)
    setFocusedId(null)
  }
  const tabStopId = focusedId ?? preferredId ?? null

  // Dòng giữ Tab không còn hiện (nhánh vừa đóng, chưa tải tới): trao Tab cho dòng đầu, để cây
  // không bao giờ bị Tab bỏ qua. Các dòng hiện ra hay mất đi theo dữ liệu tải về, nên theo dõi
  // chính DOM của cây.
  useEffect(() => {
    if (!root) {
      return
    }
    const ensureTabStop = () => {
      const rows = Array.from(root.querySelectorAll<HTMLElement>(ROW))
      if (rows.length > 0 && !rows.some((row) => row.dataset.id === tabStopId)) {
        setFocusedId(rows[0].dataset.id ?? null)
      }
    }
    const observer = new MutationObserver(ensureTabStop)
    observer.observe(root, { childList: true, subtree: true })
    const initial = requestAnimationFrame(ensureTabStop)
    return () => {
      observer.disconnect()
      cancelAnimationFrame(initial)
    }
  }, [root, tabStopId])

  function onFocus(event: FocusEvent<HTMLElement>) {
    const row = event.target as HTMLElement
    if (row.matches(ROW) && row.dataset.id) {
      setFocusedId(row.dataset.id)
    }
  }

  function onKeyDown(event: KeyboardEvent<HTMLElement>) {
    const current = event.target as HTMLElement
    if (!root || !current.matches(ROW) || event.altKey || event.ctrlKey || event.metaKey) {
      return
    }
    const rows = Array.from(root.querySelectorAll<HTMLElement>(ROW))
    const index = rows.indexOf(current)
    const level = levelOf(current)
    const expanded = current.getAttribute('aria-expanded')
    const id = current.dataset.id ?? ''
    let target: HTMLElement | undefined

    switch (event.key) {
      case 'ArrowDown':
        target = rows[index + 1]
        break
      case 'ArrowUp':
        target = rows[index - 1]
        break
      case 'Home':
        target = rows[0]
        break
      case 'End':
        target = rows.at(-1)
        break
      case 'ArrowRight':
        if (expanded === 'false') {
          toggle(id)
        } else if (expanded === 'true' && rows[index + 1] && levelOf(rows[index + 1]) === level + 1) {
          target = rows[index + 1]
        }
        break
      case 'ArrowLeft':
        if (expanded === 'true') {
          toggle(id)
        } else {
          target = rows.slice(0, index).reverse().find((row) => levelOf(row) === level - 1)
        }
        break
      case 'Enter':
        current.querySelector<HTMLAnchorElement>(':scope > div a')?.click()
        break
      default:
        if (event.key.length !== 1 || event.key === ' ') {
          return
        }
        {
          const letter = fold(event.key)
          const after = [...rows.slice(index + 1), ...rows.slice(0, index)]
          target = after.find((row) => fold(row.dataset.name ?? '').startsWith(letter))
        }
    }
    event.preventDefault()
    target?.focus()
  }

  return { tabStopId, setRoot, onKeyDown, onFocus }
}
