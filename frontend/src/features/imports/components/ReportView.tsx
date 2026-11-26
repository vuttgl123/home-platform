import { CircleCheckIcon, CircleXIcon, InfoIcon, TriangleAlertIcon } from 'lucide-react'
import { useId, useState } from 'react'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { ScrollArea, ScrollBar } from '@/components/ui/scroll-area'
import { Switch } from '@/components/ui/switch'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { cn } from '@/lib/utils'

import { PAGE_SIZE, sheetSummary } from '../report'
import type { ImportReport, RowReport, SheetReport } from '../types'

/**
 * Bước ②: dải số, lỗi cả file, ghi chú, rồi bảng từng dòng theo tab của sheet. Có lỗi thì công tắc
 * "chỉ dòng lỗi" bật sẵn và tab đầu tiên có lỗi được chọn.
 */
export function ReportView({ report }: { report: ImportReport }) {
  const firstWithErrors = report.sheets.findIndex((sheet) => sheet.errors > 0)
  const [tab, setTab] = useState(String(Math.max(firstWithErrors, 0)))
  const [onlyProblems, setOnlyProblems] = useState(report.totals.errors > 0)
  const [limit, setLimit] = useState(PAGE_SIZE)
  const switchId = useId()
  const sheet = report.sheets[Number(tab)]

  return (
    <div className="flex flex-col gap-4">
      {report.sheets.length > 0 ? (
        <ul aria-label="Tóm tắt" className="flex flex-wrap gap-2 text-sm">
          {report.sheets.map((item) => (
            <li
              key={item.sheet}
              className={cn(
                'rounded-full border px-3 py-1',
                item.errors > 0 ? 'border-destructive/40 text-destructive' : item.warnings > 0 ? 'border-amber-500/40 text-amber-700 dark:text-amber-400' : '',
              )}
            >
              {sheetSummary(item)}
            </li>
          ))}
        </ul>
      ) : null}
      {report.fileErrors.length > 0 ? (
        <Alert variant="destructive">
          <TriangleAlertIcon aria-hidden="true" />
          <AlertTitle>Không đọc được file</AlertTitle>
          <AlertDescription>
            <ul className="list-disc pl-4">
              {report.fileErrors.map((error) => (
                <li key={error}>{error}</li>
              ))}
            </ul>
          </AlertDescription>
        </Alert>
      ) : null}
      {report.notices.length > 0 ? (
        <Alert role="status">
          <InfoIcon aria-hidden="true" />
          <AlertTitle>Ghi chú</AlertTitle>
          <AlertDescription>
            <ul className="list-disc pl-4">
              {report.notices.map((notice) => (
                <li key={notice}>{notice}</li>
              ))}
            </ul>
          </AlertDescription>
        </Alert>
      ) : null}
      {sheet ? (
        <div className="flex flex-col gap-3">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <Tabs
              value={tab}
              onValueChange={(value: string) => {
                setTab(value)
                setLimit(PAGE_SIZE)
              }}
            >
              <ScrollArea className="max-w-full">
                <TabsList className="overflow-y-hidden">
                  {report.sheets.map((item, index) => (
                    <TabsTrigger key={item.sheet} value={String(index)}>
                      {item.name}
                      <CountBadge sheet={item} />
                    </TabsTrigger>
                  ))}
                </TabsList>
                <ScrollBar orientation="horizontal" />
              </ScrollArea>
            </Tabs>
            <div className="flex items-center gap-2">
              <Switch
                id={switchId}
                checked={onlyProblems}
                onCheckedChange={(checked: boolean) => {
                  setOnlyProblems(checked)
                  setLimit(PAGE_SIZE)
                }}
              />
              <label htmlFor={switchId} className="text-sm">
                Chỉ hiện dòng có lỗi hoặc cảnh báo
              </label>
            </div>
          </div>
          <SheetTable
            rows={onlyProblems ? sheet.items.filter((row) => row.status !== 'OK') : sheet.items}
            limit={limit}
            onShowMore={() => setLimit((current) => current + PAGE_SIZE)}
          />
        </div>
      ) : null}
    </div>
  )
}

function CountBadge({ sheet }: { sheet: SheetReport }) {
  if (sheet.errors > 0) {
    return (
      <span className="rounded-full bg-destructive/10 px-1.5 text-xs text-destructive">
        {sheet.errors}
        <span className="sr-only"> lỗi</span>
      </span>
    )
  }
  if (sheet.warnings > 0) {
    return (
      <span className="rounded-full bg-amber-500/10 px-1.5 text-xs text-amber-700 dark:text-amber-400">
        {sheet.warnings}
        <span className="sr-only"> cảnh báo</span>
      </span>
    )
  }
  return null
}

function SheetTable({ rows, limit, onShowMore }: { rows: RowReport[]; limit: number; onShowMore: () => void }) {
  if (rows.length === 0) {
    return <p className="rounded-lg border px-4 py-6 text-center text-sm text-muted-foreground">Không có dòng nào có lỗi hay cảnh báo</p>
  }
  return (
    <div className="flex flex-col gap-3">
      <ScrollArea className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-16">Dòng</TableHead>
              <TableHead>Hồ sơ</TableHead>
              <TableHead>Kết quả</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.slice(0, limit).map((row) => (
              <TableRow key={row.row}>
                <TableCell className="align-top tabular-nums text-muted-foreground">{row.row}</TableCell>
                <TableCell className="align-top font-medium whitespace-normal">{row.label}</TableCell>
                <TableCell className="align-top whitespace-normal">
                  <RowResult row={row} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <ScrollBar orientation="horizontal" />
      </ScrollArea>
      {rows.length > limit ? (
        <Button variant="outline" className="self-center" onClick={onShowMore}>
          Hiện thêm
        </Button>
      ) : null}
    </div>
  )
}

function RowResult({ row }: { row: RowReport }) {
  if (row.messages.length === 0) {
    return (
      <span className="inline-flex items-center gap-1.5 text-emerald-700 dark:text-emerald-400">
        <CircleCheckIcon aria-hidden="true" className="size-4" />
        Ổn
      </span>
    )
  }
  return (
    <ul className="flex flex-col gap-1">
      {row.messages.map((message, index) => (
        <li
          key={index}
          className={cn(
            'flex flex-wrap items-baseline gap-1.5',
            message.level === 'ERROR' ? 'text-destructive' : 'text-amber-700 dark:text-amber-400',
          )}
        >
          {message.level === 'ERROR' ? (
            <CircleXIcon aria-hidden="true" className="size-3.5 shrink-0 self-center" />
          ) : (
            <TriangleAlertIcon aria-hidden="true" className="size-3.5 shrink-0 self-center" />
          )}
          <span className="sr-only">{message.level === 'ERROR' ? 'Lỗi:' : 'Cảnh báo:'}</span>
          {message.column ? (
            <span className="rounded border border-current/30 px-1 text-xs">{message.column}</span>
          ) : null}
          <span>{message.text}</span>
        </li>
      ))}
    </ul>
  )
}
