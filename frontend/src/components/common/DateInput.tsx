import { vi } from 'date-fns/locale'
import { CalendarIcon } from 'lucide-react'
import { useRef, useState, type ComponentProps, type Ref } from 'react'

import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Input } from '@/components/ui/input'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'

const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})$/

/** `yyyy-MM-dd` thành ngày theo giờ máy; sai dạng hay trống thì không có. */
function toDate(value: string | undefined): Date | undefined {
  const match = ISO_DATE.exec(value ?? '')
  return match ? new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3])) : undefined
}

function toIso(date: Date): string {
  const two = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${two(date.getMonth() + 1)}-${two(date.getDate())}`
}

/**
 * Đặt giá trị cho ô như khi người dùng gõ: qua setter gốc của trình duyệt rồi phát sự kiện `input`,
 * để `onChange` của React (cả ô có `value` lẫn ô của React Hook Form) nhận được.
 */
function typeInto(input: HTMLInputElement, value: string) {
  Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set?.call(input, value)
  input.dispatchEvent(new Event('input', { bubbles: true }))
}

/**
 * Ô ngày: vẫn gõ được như ô ngày của trình duyệt (nhanh, và dùng tốt trên điện thoại), thêm nút mở
 * lịch tiếng Việt (tuần bắt đầu thứ Hai) để chọn bằng chuột. Giá trị luôn là `yyyy-MM-dd`.
 *
 * @param pickerLabel tên của nút lịch cho công cụ đọc màn hình, ví dụ "Chọn Từ ngày trên lịch"
 */
export function DateInput({ ref, pickerLabel, className, disabled, ...props }: ComponentProps<'input'> & { pickerLabel: string }) {
  const inputRef = useRef<HTMLInputElement | null>(null)
  const [open, setOpen] = useState(false)
  const [selected, setSelected] = useState<Date | undefined>(undefined)

  const setRefs = (node: HTMLInputElement | null) => {
    inputRef.current = node
    if (typeof ref === 'function') ref(node)
    else if (ref) (ref as { current: HTMLInputElement | null }).current = node
  }

  const min = toDate(typeof props.min === 'string' ? props.min : undefined)
  const max = toDate(typeof props.max === 'string' ? props.max : undefined)

  return (
    <div className="relative">
      <Input
        ref={setRefs as Ref<HTMLInputElement>}
        type="date"
        disabled={disabled}
        // Ẩn biểu tượng lịch của trình duyệt (Chrome, Edge): nút lịch bên phải thay cho nó.
        className={`pr-9 [&::-webkit-calendar-picker-indicator]:hidden ${className ?? ''}`}
        {...props}
      />
      <Popover
        open={open}
        onOpenChange={(next) => {
          // Đọc ngày đang có trong ô mỗi lần mở: ô có thể vừa được gõ tay.
          if (next) setSelected(toDate(inputRef.current?.value))
          setOpen(next)
        }}
      >
        <PopoverTrigger
          disabled={disabled}
          render={
            <Button type="button" variant="ghost" size="icon-sm" aria-label={pickerLabel} className="absolute top-1/2 right-1 -translate-y-1/2 text-muted-foreground" />
          }
        >
          <CalendarIcon aria-hidden="true" />
        </PopoverTrigger>
        <PopoverContent align="end" className="w-auto p-0">
          <Calendar
            mode="single"
            locale={vi}
            weekStartsOn={1}
            captionLayout="dropdown"
            // Tên thứ viết tắt kiểu Việt Nam (T2…T7, CN): vừa ô, khỏi dính vào nhau.
            formatters={{ formatWeekdayName: (day) => (day.getDay() === 0 ? 'CN' : `T${day.getDay() + 1}`) }}
            className="[--cell-size:--spacing(8)]"
            startMonth={new Date(1900, 0)}
            endMonth={new Date(2100, 11)}
            selected={selected}
            // Không có ngày đang chọn thì lịch tự mở ở tháng hiện tại.
            defaultMonth={selected}
            disabled={[...(min ? [{ before: min }] : []), ...(max ? [{ after: max }] : [])]}
            onSelect={(date) => {
              if (!date || !inputRef.current) return
              typeInto(inputRef.current, toIso(date))
              setOpen(false)
              inputRef.current.focus()
            }}
          />
        </PopoverContent>
      </Popover>
    </div>
  )
}
