import { useEffect } from 'react'

/** Mở ô tìm chung bằng Ctrl+K (⌘K trên máy Mac), ở bất kỳ trang nào. */
export function useSearchShortcut(onOpen: () => void): void {
  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        onOpen()
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [onOpen])
}
