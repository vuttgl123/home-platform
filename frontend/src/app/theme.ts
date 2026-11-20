import { useSyncExternalStore } from 'react'

/** Chế độ giao diện người dùng chọn; `system` là theo giao diện của máy. */
export type ThemeMode = 'light' | 'dark' | 'system'

const KEY = 'hp.theme'
const MODES: ThemeMode[] = ['light', 'dark', 'system']

// Chế độ đã chọn trong phiên này; null là chưa chọn, đọc từ localStorage.
let chosen: ThemeMode | null = null
// Bỏ theo dõi giao diện của máy (chỉ theo dõi khi đang ở chế độ `system`).
let stopFollowing: (() => void) | null = null
const listeners = new Set<() => void>()

function read(): ThemeMode {
  try {
    const stored = localStorage.getItem(KEY)
    return MODES.includes(stored as ThemeMode) ? (stored as ThemeMode) : 'system'
  } catch {
    // Trình duyệt chặn localStorage (chế độ riêng tư): coi như chưa chọn.
    return 'system'
  }
}

function write(mode: ThemeMode) {
  try {
    localStorage.setItem(KEY, mode)
  } catch {
    // Bị chặn: chỉ nhớ trong phiên này.
  }
}

function machinePrefersDark(): MediaQueryList | null {
  return typeof window.matchMedia === 'function' ? window.matchMedia('(prefers-color-scheme: dark)') : null
}

function setDarkClass(dark: boolean) {
  document.documentElement.classList.toggle('dark', dark)
}

/**
 * Chế độ sáng, tối của ứng dụng: gắn lớp `dark` lên `<html>` (token tối trong `index.css`).
 * Chọn "Theo máy" thì đổi ngay khi người dùng đổi giao diện của máy.
 */
export const themeStore = {
  getMode(): ThemeMode {
    return chosen ?? read()
  },

  setMode(mode: ThemeMode): void {
    chosen = mode
    write(mode)
    themeStore.apply()
    listeners.forEach((listener) => listener())
  },

  subscribe(listener: () => void): () => void {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },

  apply(): void {
    stopFollowing?.()
    stopFollowing = null
    const mode = themeStore.getMode()
    const machine = machinePrefersDark()
    setDarkClass(mode === 'dark' || (mode === 'system' && machine?.matches === true))
    if (mode === 'system' && machine) {
      const onChange = (event: { matches: boolean }) => setDarkClass(event.matches)
      machine.addEventListener('change', onChange)
      stopFollowing = () => machine.removeEventListener('change', onChange)
    }
  },
}

/** Chế độ đang chọn và hàm đổi chế độ, cho menu "Giao diện". */
export function useThemeMode(): [ThemeMode, (mode: ThemeMode) => void] {
  const mode = useSyncExternalStore(themeStore.subscribe, themeStore.getMode)
  return [mode, themeStore.setMode]
}
