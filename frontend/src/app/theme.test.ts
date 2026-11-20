import { afterEach, beforeEach, expect, test, vi } from 'vitest'

import { themeStore } from './theme'

/** Giả `matchMedia` của máy; `setDark` phát sự kiện change như khi người dùng đổi giao diện máy. */
function fakeMachine(initiallyDark: boolean) {
  const listeners = new Set<(event: { matches: boolean }) => void>()
  const query = {
    matches: initiallyDark,
    addEventListener: (_type: string, listener: (event: { matches: boolean }) => void) => listeners.add(listener),
    removeEventListener: (_type: string, listener: (event: { matches: boolean }) => void) => listeners.delete(listener),
  }
  vi.stubGlobal('matchMedia', () => query)
  return {
    setDark(dark: boolean) {
      query.matches = dark
      listeners.forEach((listener) => listener({ matches: dark }))
    },
  }
}

const isDark = () => document.documentElement.classList.contains('dark')

beforeEach(() => {
  localStorage.clear()
  document.documentElement.classList.remove('dark')
})

afterEach(() => {
  themeStore.setMode('system')
  localStorage.clear()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

test('defaultsToSystem', () => {
  fakeMachine(false)

  expect(themeStore.getMode()).toBe('system')
})

test('choosingDarkAddsTheClassAndRemembersIt', () => {
  fakeMachine(false)

  themeStore.setMode('dark')

  expect(isDark()).toBe(true)
  expect(localStorage.getItem('hp.theme')).toBe('dark')
  expect(themeStore.getMode()).toBe('dark')
})

test('unknownStoredValueMeansSystem', () => {
  fakeMachine(true)
  localStorage.setItem('hp.theme', 'purple')

  expect(themeStore.getMode()).toBe('system')
})

test('systemModeFollowsTheMachineLive', () => {
  const machine = fakeMachine(false)
  themeStore.setMode('system')
  expect(isDark()).toBe(false)

  machine.setDark(true)
  expect(isDark()).toBe(true)

  machine.setDark(false)
  expect(isDark()).toBe(false)
})

test('explicitLightIgnoresTheMachine', () => {
  const machine = fakeMachine(true)

  themeStore.setMode('light')
  machine.setDark(true)

  expect(isDark()).toBe(false)
})

test('blockedStorageStillSwitchesTheme', () => {
  fakeMachine(false)
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
    throw new Error('blocked')
  })
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
    throw new Error('blocked')
  })

  expect(() => themeStore.setMode('dark')).not.toThrow()
  expect(isDark()).toBe(true)
})
