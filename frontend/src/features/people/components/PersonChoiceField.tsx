import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'

import { PersonPicker } from './PersonPicker'
import type { PersonChoiceState } from './personChoice'

/**
 * Chọn một người đã có, hoặc gõ tên một người mới; hồ sơ người mới chỉ được tạo khi form gửi đi
 * (`state.resolve()`), và chỉ một lần dù gửi lại sau lỗi.
 */
export function PersonChoiceField({ ws, state, label = 'Người' }: { ws: string; state: PersonChoiceState; label?: string }) {
  const { choice } = state
  return (
    <Field>
      <FieldLabel>{label}</FieldLabel>
      {choice?.kind === 'new' ? (
        <div className="flex items-center justify-between gap-2 rounded-lg border px-3 py-2 text-sm">
          <span>
            Người mới: <span className="font-medium">{choice.fullName}</span>
            <span className="block text-xs text-muted-foreground">Hồ sơ được tạo khi bấm Thêm; bổ sung thông tin sau.</span>
          </span>
          {!state.isCreated && (
            <Button type="button" variant="ghost" size="sm" onClick={() => state.setChoice(null)}>
              Đổi
            </Button>
          )}
        </div>
      ) : (
        <PersonPicker
          ws={ws}
          value={choice?.kind === 'existing' ? choice.person : null}
          onChange={(person) => state.setChoice({ kind: 'existing', person })}
          onCreateNew={(fullName) => state.setChoice({ kind: 'new', fullName })}
        />
      )}
    </Field>
  )
}
