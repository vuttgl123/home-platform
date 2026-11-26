import { cn } from '@/lib/utils'

const STEPS = ['Chọn file', 'Xem trước', 'Nhập'] as const

/** Ba bước của trang nhập; bước đang ở có `aria-current="step"` và tô đậm. */
export function ImportSteps({ current }: { current: 1 | 2 | 3 }) {
  return (
    <ol aria-label="Các bước" className="flex flex-wrap items-center gap-x-3 gap-y-2 text-sm">
      {STEPS.map((label, index) => {
        const step = index + 1
        const active = step === current
        return (
          <li
            key={label}
            aria-current={active ? 'step' : undefined}
            className={cn('flex items-center gap-2', active ? 'font-semibold text-foreground' : 'text-muted-foreground')}
          >
            <span
              aria-hidden="true"
              className={cn(
                'flex size-6 items-center justify-center rounded-full border text-xs',
                active ? 'border-primary bg-primary text-primary-foreground' : step < current ? 'border-primary text-primary' : '',
              )}
            >
              {step}
            </span>
            {label}
            {step < STEPS.length ? (
              <span aria-hidden="true" className="h-px w-6 bg-border sm:w-10" />
            ) : null}
          </li>
        )
      })}
    </ol>
  )
}
