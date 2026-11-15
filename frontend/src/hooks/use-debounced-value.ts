import { useEffect, useState } from 'react'

/** `value` sau khi nó đã đứng yên `delayMs`; dùng để không gọi API theo từng phím gõ. */
export function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(timer)
  }, [value, delayMs])
  return debounced
}
