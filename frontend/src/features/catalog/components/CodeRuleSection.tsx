import { PlusIcon } from 'lucide-react'
import { useId, useState } from 'react'

import { ErrorState } from '@/components/common/ErrorState'
import { FormError } from '@/components/common/FormError'
import { TextField } from '@/components/common/TextField'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { Switch } from '@/components/ui/switch'
import { toast } from '@/components/ui/toast'
import { todayInBusinessZone } from '@/lib/dates'

import { codeRuleApi } from '../api'
import { codeExample, PREFIX_PATTERN } from '../codeRules'
import { useBackfillCodes, useCatalogWrite, useCodeRules, useCodeStatus, useItemCategories, usePlaceTypes } from '../queries'
import type { CodeRule, CodeTarget } from '../types'
import { CatalogEntryDialog } from './CatalogEntryDialog'
import { CatalogSection, CatalogTable } from './CatalogSection'

const DIGITS = [3, 4, 5, 6, 7, 8, 9]

const PREFIX_ERROR = '1 đến 10 chữ cái không dấu hoặc số, bắt đầu bằng chữ'

type Editing = { rule: CodeRule; label: string } | 'new'

/**
 * Phần "Mã tự cấp": dạng mã của địa điểm và đồ đạc (chung, hoặc riêng cho một loại, một nhóm), và
 * nút cấp mã cho bản ghi có từ trước khi có mã tự cấp.
 */
export function CodeRuleSection({ ws }: { ws: string }) {
  const rules = useCodeRules(ws)
  const types = usePlaceTypes(ws).data ?? []
  const categories = useItemCategories(ws).data ?? []
  const [editing, setEditing] = useState<Editing | null>(null)
  const remove = useCatalogWrite(ws, (rule: CodeRule) => codeRuleApi.remove(ws, rule.target, rule.typeId))
  const year = Number(todayInBusinessZone().slice(0, 4))

  function labelOf(rule: { target: CodeTarget; typeId: string }): string {
    if (rule.typeId === '0') return rule.target === 'PLACE' ? 'Mọi địa điểm' : 'Mọi đồ đạc'
    const name =
      rule.target === 'PLACE'
        ? types.find((type) => type.id === rule.typeId)?.name
        : categories.find((category) => category.id === rule.typeId)?.name
    return `${rule.target === 'PLACE' ? 'Loại' : 'Nhóm'}: ${name ?? `#${rule.typeId}`}`
  }

  return (
    <CatalogSection
      title="Mã tự cấp"
      description="Mã của địa điểm và đồ đạc do hệ thống cấp và không sửa được. Đổi dạng mã chỉ áp dụng cho mã cấp từ nay."
      action={
        <Button variant="outline" onClick={() => setEditing('new')}>
          <PlusIcon aria-hidden="true" />
          Thêm dạng riêng
        </Button>
      }
    >
      <BackfillBanner ws={ws} />
      {rules.error ? (
        <ErrorState error={rules.error} onRetry={() => void rules.refetch()} />
      ) : (
        <CatalogTable headers={['Áp dụng cho', 'Tiền tố', 'Số chữ số', 'Theo năm', 'Ví dụ']}>
          {(rules.data ?? []).map((rule) => {
            const label = labelOf(rule)
            return (
              <tr key={`${rule.target}:${rule.typeId}`}>
                <td className={rule.typeId === '0' ? 'px-3 py-2 font-medium' : 'py-2 pr-3 pl-7'}>{label}</td>
                <td className="px-3 py-2 font-mono text-xs">{rule.prefix}</td>
                <td className="px-3 py-2">{rule.digits}</td>
                <td className="px-3 py-2">{rule.yearly ? 'Có' : 'Không'}</td>
                <td className="px-3 py-2">
                  <span className="font-mono text-xs">{codeExample(rule, year)}</span>
                  {rule.builtIn && <span className="ml-2 text-xs text-muted-foreground">(có sẵn)</span>}
                </td>
                <td className="px-3 py-2 text-right whitespace-nowrap">
                  <Button variant="ghost" size="sm" aria-label={`Sửa ${label}`} onClick={() => setEditing({ rule, label })}>
                    Sửa
                  </Button>
                  {!rule.builtIn && (
                    <Button
                      variant="ghost"
                      size="sm"
                      aria-label={rule.typeId === '0' ? `Về mặc định ${label}` : `Bỏ ${label}`}
                      disabled={remove.isPending}
                      onClick={() => remove.mutate(rule, { onError: (error) => toast.add({ title: error.detail, type: 'error' }) })}
                    >
                      {rule.typeId === '0' ? 'Về mặc định' : 'Bỏ'}
                    </Button>
                  )}
                </td>
              </tr>
            )
          })}
        </CatalogTable>
      )}
      {editing && (
        <CodeRuleDialog
          ws={ws}
          editing={editing}
          rules={rules.data ?? []}
          choices={[
            ...types
              .filter((type) => !(rules.data ?? []).some((rule) => rule.target === 'PLACE' && rule.typeId === type.id))
              .map((type) => ({ target: 'PLACE' as const, typeId: type.id })),
            ...categories
              .filter((category) => !(rules.data ?? []).some((rule) => rule.target === 'ITEM' && rule.typeId === category.id))
              .map((category) => ({ target: 'ITEM' as const, typeId: category.id })),
          ].map((choice) => ({ ...choice, label: labelOf(choice) }))}
          year={year}
          onClose={() => setEditing(null)}
        />
      )}
    </CatalogSection>
  )
}

/** Dải "Có N địa điểm và M món đồ chưa có mã" với nút cấp mã; ẩn khi mọi bản ghi đã có mã. */
function BackfillBanner({ ws }: { ws: string }) {
  const status = useCodeStatus(ws)
  const backfill = useBackfillCodes(ws)
  const places = status.data?.placesWithoutCode ?? 0
  const items = status.data?.itemsWithoutCode ?? 0
  if (places + items === 0 && !backfill.isRunning) return null

  async function start() {
    try {
      const assigned = await backfill.run(places + items)
      toast.add({ title: `Đã cấp mã cho ${assigned} bản ghi`, type: 'success' })
    } catch {
      // Lỗi hiện ngay dưới dải thông báo.
    }
  }

  return (
    <Alert>
      <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
        <span>
          {backfill.isRunning
            ? `Đã cấp ${backfill.assigned} / ${backfill.total}…`
            : `Có ${places} địa điểm và ${items} món đồ chưa có mã.`}
        </span>
        <Button size="sm" disabled={backfill.isRunning} onClick={() => void start()}>
          Cấp mã
        </Button>
        <FormError messages={backfill.error ? [backfill.error.detail] : []} />
      </AlertDescription>
    </Alert>
  )
}

function CodeRuleDialog({
  ws,
  editing,
  rules,
  choices,
  year,
  onClose,
}: {
  ws: string
  editing: Editing
  rules: CodeRule[]
  choices: { target: CodeTarget; typeId: string; label: string }[]
  year: number
  onClose: () => void
}) {
  const existing = editing === 'new' ? null : editing.rule
  const [choice, setChoice] = useState(existing ? '' : choices[0] ? `${choices[0].target}:${choices[0].typeId}` : '')
  const [prefix, setPrefix] = useState(existing?.prefix ?? '')
  const [digits, setDigits] = useState(existing?.digits ?? 6)
  const [yearly, setYearly] = useState(existing?.yearly ?? false)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const choiceId = useId()
  const digitsId = useId()

  const [target, typeId] = existing ? [existing.target, existing.typeId] : (choice.split(':') as [CodeTarget, string])
  const otherTarget = target === 'PLACE' ? 'ITEM' : 'PLACE'
  const sharesPrefix = prefix !== '' && rules.some((rule) => rule.target === otherTarget && rule.prefix === prefix)

  function validate() {
    const next: Record<string, string> = {}
    if (!existing && choice === '') next.choice = 'Chọn một loại địa điểm hoặc nhóm đồ'
    if (!PREFIX_PATTERN.test(prefix)) next.prefix = PREFIX_ERROR
    setErrors(next)
    return Object.keys(next).length === 0
  }

  return (
    <CatalogEntryDialog
      ws={ws}
      title={existing ? `Dạng mã: ${editing === 'new' ? '' : editing.label}` : 'Thêm dạng riêng'}
      submitLabel="Lưu"
      done="Đã lưu dạng mã"
      validate={validate}
      save={() => codeRuleApi.put(ws, target, typeId, { prefix, digits, yearly })}
      onClose={onClose}
    >
      {!existing && (
        <Field>
          <FieldLabel htmlFor={choiceId}>Áp dụng cho</FieldLabel>
          <NativeSelect id={choiceId} className="w-full" value={choice} onChange={(event) => setChoice(event.target.value)}>
            {choices.map((option) => (
              <NativeSelectOption key={`${option.target}:${option.typeId}`} value={`${option.target}:${option.typeId}`}>
                {option.label}
              </NativeSelectOption>
            ))}
          </NativeSelect>
          <FormError messages={errors.choice ? [errors.choice] : []} />
        </Field>
      )}
      <TextField
        label="Tiền tố"
        required
        autoComplete="off"
        value={prefix}
        onChange={(event) => setPrefix(event.target.value.toUpperCase())}
        error={errors.prefix}
      />
      <Field>
        <FieldLabel htmlFor={digitsId}>Số chữ số</FieldLabel>
        <NativeSelect id={digitsId} className="w-full" value={String(digits)} onChange={(event) => setDigits(Number(event.target.value))}>
          {DIGITS.map((value) => (
            <NativeSelectOption key={value} value={String(value)}>
              {value}
            </NativeSelectOption>
          ))}
        </NativeSelect>
      </Field>
      <label className="flex items-center gap-2 text-sm">
        <Switch checked={yearly} onCheckedChange={setYearly} aria-label="Bắt đầu lại mỗi năm" />
        Bắt đầu lại mỗi năm
      </label>
      <p className="font-mono text-sm">
        {`Ví dụ: ${PREFIX_PATTERN.test(prefix) ? codeExample({ prefix, digits, yearly }, year) : '—'}`}
      </p>
      {sharesPrefix && <p className="text-sm text-amber-700 dark:text-amber-400">Địa điểm và đồ đạc sẽ có mã giống nhau</p>}
    </CatalogEntryDialog>
  )
}
