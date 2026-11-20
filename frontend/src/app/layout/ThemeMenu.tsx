import { LaptopIcon, MoonIcon, SunIcon, SunMoonIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

import { useThemeMode, type ThemeMode } from '../theme'

const CHOICES: { mode: ThemeMode; label: string; icon: typeof SunIcon }[] = [
  { mode: 'light', label: 'Sáng', icon: SunIcon },
  { mode: 'dark', label: 'Tối', icon: MoonIcon },
  { mode: 'system', label: 'Theo máy', icon: LaptopIcon },
]

/** Nút "Giao diện": chọn Sáng, Tối hay Theo máy. */
export function ThemeMenu() {
  const [mode, setMode] = useThemeMode()
  return (
    <DropdownMenu>
      <DropdownMenuTrigger render={<Button variant="ghost" size="icon-sm" aria-label="Giao diện" />}>
        <SunMoonIcon aria-hidden="true" />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" side="top" className="min-w-40">
        <DropdownMenuGroup>
          <DropdownMenuLabel>Giao diện</DropdownMenuLabel>
          <DropdownMenuRadioGroup value={mode} onValueChange={(value) => setMode(value as ThemeMode)}>
            {CHOICES.map(({ mode: choice, label, icon: Icon }) => (
              <DropdownMenuRadioItem key={choice} value={choice}>
                <Icon aria-hidden="true" />
                {label}
              </DropdownMenuRadioItem>
            ))}
          </DropdownMenuRadioGroup>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
