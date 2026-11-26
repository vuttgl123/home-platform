import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { mockDownloads } from '@/test/downloads'
import { emptyOverview, importReport, rowReport, sheetReport } from '@/test/fixtures'
import { mockShell, recordRequests } from '@/test/handlers'
import { mockImportBackend, type ImportResponder } from '@/test/importBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

const XLSX = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

function xlsx(name = 'nha.xlsx', size = 10) {
  return new File([new Uint8Array(size)], name, { type: XLSX })
}

/** Báo cáo có Người 2 dòng (dòng 3 lỗi Email) và Đồ đạc 3 dòng ổn. */
function reportWithAnError() {
  return importReport({
    sheets: [
      sheetReport({
        items: [
          rowReport({ row: 2, label: 'Nguyễn An' }),
          rowReport({
            row: 3,
            label: 'Trần Bình',
            status: 'ERROR',
            messages: [{ level: 'ERROR', column: 'Email', text: 'Email không hợp lệ' }],
          }),
        ],
      }),
      sheetReport({
        sheet: 'ITEM',
        name: 'Đồ đạc',
        items: [1, 2, 3].map((n) => rowReport({ row: n + 1, label: `M-${n} · Món ${n}`, code: `M-${n}` })),
      }),
    ],
  })
}

async function choose(user: ReturnType<typeof renderApp>['user'], file = xlsx()) {
  await user.upload(await screen.findByLabelText('Chọn file .xlsx'), file)
}

async function main() {
  return within(await screen.findByRole('main'))
}

test('templateDownloadsWithTheWorkspaceName', async () => {
  const saved = mockDownloads()
  mockShell()
  server.use(http.get('/api/registry/imports/template.xlsx', () => new HttpResponse('file')))
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })

  await user.click(await screen.findByRole('button', { name: 'Tải mẫu' }))

  await waitFor(() => expect(saved).toHaveLength(1))
  expect(saved[0].name).toMatch(/^nha-an-mau-nhap-\d{4}-\d{2}-\d{2}\.xlsx$/)
})

test('choosingAFileSendsAPreview', async () => {
  mockShell()
  let release!: () => void
  const gate = new Promise<void>((resolve) => {
    release = resolve
  })
  const calls = mockImportBackend({
    preview: async () => {
      await gate
      return importReport()
    },
  })
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })
  expect(await screen.findByRole('heading', { name: 'Nhập từ Excel' })).toBeInTheDocument()

  await choose(user, xlsx('phong.xlsx'))

  expect(await (await main()).findByText('Đang đọc file…')).toBeInTheDocument()
  release()
  // Tên và nội dung file không đo được ở đây: FormData của jsdom đi qua Request của undici không giữ
  // nguyên File (như test tải ảnh của media). Tên file thật được kiểm bằng trình duyệt.
  await waitFor(() => expect(calls).toHaveLength(1))
  expect(calls[0].path).toBe('preview')
  const steps = within(screen.getByRole('list', { name: 'Các bước' }))
  await waitFor(() => expect(steps.getByText('Xem trước').closest('li')).toHaveAttribute('aria-current', 'step'))
})

test('summaryTabsAndRows', async () => {
  mockShell()
  mockImportBackend({ preview: reportWithAnError() })
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })

  await choose(user)

  const summary = within(await screen.findByRole('list', { name: 'Tóm tắt' }))
  expect(summary.getByText('Người 2 (1 lỗi)')).toBeInTheDocument()
  expect(summary.getByText('Đồ đạc 3')).toBeInTheDocument()
  expect(screen.getByRole('tab', { name: /^Người/ })).toHaveAttribute('aria-selected', 'true')
  await user.click(screen.getByRole('switch', { name: 'Chỉ hiện dòng có lỗi hoặc cảnh báo' }))
  const rows = within(screen.getByRole('table')).getAllByRole('row').slice(1)
  expect(rows).toHaveLength(2)
  expect(within(rows[0]).getByText('Ổn')).toBeInTheDocument()
  expect(within(rows[1]).getByText('3')).toBeInTheDocument()
  expect(within(rows[1]).getByText('Email không hợp lệ')).toBeInTheDocument()
  expect(within(rows[1]).getByText('Email')).toBeInTheDocument()
})

test('errorsOnlySwitchIsOnWhenThereAreErrors', async () => {
  mockShell()
  mockImportBackend({ preview: reportWithAnError() })
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })

  await choose(user)

  const toggle = await screen.findByRole('switch', { name: 'Chỉ hiện dòng có lỗi hoặc cảnh báo' })
  expect(toggle).toBeChecked()
  expect(within(screen.getByRole('table')).getAllByRole('row')).toHaveLength(2)
  await user.click(screen.getByRole('tab', { name: /^Đồ đạc/ }))
  expect(await screen.findByText('Không có dòng nào có lỗi hay cảnh báo')).toBeInTheDocument()
})

test('importIsLockedWhileThereAreErrors', async () => {
  mockShell()
  mockImportBackend({ preview: reportWithAnError() })
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })

  await choose(user)

  expect(await screen.findByRole('button', { name: 'Nhập 5 dòng' })).toBeDisabled()
  expect(screen.getByText('Sửa 1 lỗi trong file rồi chọn lại file')).toBeInTheDocument()
})

test('aCleanReportCanBeImported', async () => {
  mockShell()
  mockImportBackend({ preview: importReport() })
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })

  await choose(user)

  expect(await screen.findByRole('button', { name: 'Nhập 1 dòng' })).toBeEnabled()
  expect(screen.getByRole('switch', { name: 'Chỉ hiện dòng có lỗi hoặc cảnh báo' })).not.toBeChecked()
})

test('fileErrorsAndNotices', async () => {
  mockShell()
  mockImportBackend({
    preview: importReport({
      fileErrors: ['Sheet Người thiếu cột bắt buộc: Họ tên (dòng 1 phải là tiêu đề cột)'],
      notices: ["Cột 'Tầng' ở sheet Người không có trong mẫu, bị bỏ qua"],
      sheets: [],
    }),
  })
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })

  await choose(user)

  const alert = await screen.findByRole('alert')
  expect(alert).toHaveTextContent('Sheet Người thiếu cột bắt buộc: Họ tên (dòng 1 phải là tiêu đề cột)')
  expect(screen.getByText("Cột 'Tầng' ở sheet Người không có trong mẫu, bị bỏ qua")).toBeInTheDocument()
  expect(screen.getByRole('button', { name: /^Nhập/ })).toBeDisabled()
})

test('showMoreAfterTwoHundredRows', async () => {
  mockShell()
  const items = Array.from({ length: 250 }, (_, index) => rowReport({ row: index + 2, label: `Người ${index + 1}` }))
  mockImportBackend({ preview: importReport({ sheets: [sheetReport({ items })] }) })
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })

  await choose(user)

  const table = within(await screen.findByRole('table'))
  expect(table.getAllByRole('row')).toHaveLength(201)
  await user.click(screen.getByRole('button', { name: 'Hiện thêm' }))
  expect(table.getAllByRole('row')).toHaveLength(251)
  expect(screen.queryByRole('button', { name: 'Hiện thêm' })).not.toBeInTheDocument()
})

test('chooseAnotherFileStartsOver', async () => {
  mockShell()
  mockImportBackend({ preview: reportWithAnError() })
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })

  await choose(user)
  await user.click(await screen.findByRole('button', { name: 'Chọn file khác' }))

  expect(await screen.findByLabelText('Chọn file .xlsx')).toBeInTheDocument()
  expect(screen.queryByRole('table')).not.toBeInTheDocument()
})

test('sidebarAndOverviewLinkToTheImportPage', async () => {
  mockShell({ overview: { ...emptyOverview(), places: 3 } })

  renderApp({ route: '/w/7', userId: '42' })

  expect(await screen.findByRole('link', { name: 'Nhập từ Excel' })).toHaveAttribute('href', '/w/7/import')
  expect(await (await main()).findByRole('link', { name: 'Nhập Excel' })).toHaveAttribute('href', '/w/7/import')
})

test('anEmptyWorkspaceOffersTheImport', async () => {
  mockShell()

  renderApp({ route: '/w/7', userId: '42' })

  expect(await (await main()).findByRole('link', { name: 'Nhập từ Excel' })).toHaveAttribute('href', '/w/7/import')
})

test('tooLargeAndOldRegistry', async () => {
  mockShell()
  mockImportBackend({
    preview: HttpResponse.json({ status: 413, detail: 'File must be at most 5 MB' }, { status: 413 }),
  })
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })

  await choose(user)
  expect(await screen.findByText('File lớn hơn 5 MB')).toBeInTheDocument()

  mockImportBackend({ preview: HttpResponse.json({ status: 404, detail: 'Not found' }, { status: 404 }) })
  await choose(user, xlsx('khac.xlsx'))
  expect(await screen.findByText('Máy chủ chưa hỗ trợ nhập Excel; cần cập nhật registry')).toBeInTheDocument()
})

test('aFileOverFiveMegabytesIsNotSent', async () => {
  mockShell()
  const calls = mockImportBackend()
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })

  await choose(user, xlsx('lon.xlsx', 5 * 1024 * 1024 + 1))

  expect(await screen.findByText('File lớn hơn 5 MB')).toBeInTheDocument()
  expect(calls).toHaveLength(0)
})

/** Xem trước một báo cáo sạch (5 dòng), rồi trả `commit` khi bấm Nhập. */
function cleanPreview(commit?: ImportResponder) {
  const clean = importReport({
    sheets: [
      sheetReport({ items: [rowReport({ row: 2 }), rowReport({ row: 3, label: 'Trần Bình' })] }),
      sheetReport({ sheet: 'ITEM', name: 'Đồ đạc', items: [2, 3, 4].map((row) => rowReport({ row, label: `Món ${row}` })) }),
    ],
  })
  return mockImportBackend({ preview: clean, commit: commit ?? { ...clean, committed: true } })
}

test('importSendsTheFileAgainAndShowsTheResult', async () => {
  mockShell()
  let release!: () => void
  const gate = new Promise<void>((resolve) => {
    release = resolve
  })
  const calls = cleanPreview(async () => {
    await gate
    return importReport({
      committed: true,
      sheets: [
        sheetReport({ items: [rowReport({ row: 2 }), rowReport({ row: 3 })] }),
        sheetReport({ sheet: 'ITEM', name: 'Đồ đạc', items: [2, 3, 4].map((row) => rowReport({ row })) }),
      ],
    })
  })
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })
  await choose(user)

  await user.click(await screen.findByRole('button', { name: 'Nhập 5 dòng' }))

  expect(await screen.findByText('Đang nhập, đừng đóng trang')).toBeInTheDocument()
  expect(screen.getByRole('progressbar')).toBeInTheDocument()
  release()
  expect(await screen.findByText('Đã nhập 5 hồ sơ')).toBeInTheDocument()
  expect(calls.map((call) => call.path)).toEqual(['preview', 'commit'])
  const result = await main()
  expect(result.getByText('Người: 2')).toBeInTheDocument()
  expect(result.getByText('Đồ đạc: 3')).toBeInTheDocument()
  expect(result.getByRole('link', { name: 'Tổng quan' })).toHaveAttribute('href', '/w/7')
  expect(result.getByRole('link', { name: 'Người' })).toHaveAttribute('href', '/w/7/people')
  expect(result.getByRole('link', { name: 'Đồ đạc' })).toHaveAttribute('href', '/w/7/items')
  expect(result.getByRole('link', { name: 'Hoạt động' })).toHaveAttribute('href', '/w/7/activity')
  const steps = within(screen.getByRole('list', { name: 'Các bước' }))
  expect(steps.getByText('Nhập').closest('li')).toHaveAttribute('aria-current', 'step')
})

test('importButtonLocksWhileRunning', async () => {
  mockShell()
  let release!: () => void
  const gate = new Promise<void>((resolve) => {
    release = resolve
  })
  const calls = cleanPreview(async () => {
    await gate
    return importReport({ committed: true })
  })
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })
  await choose(user)

  await user.dblClick(await screen.findByRole('button', { name: 'Nhập 5 dòng' }))
  release()

  expect(await screen.findByText(/^Đã nhập/)).toBeInTheDocument()
  expect(calls.filter((call) => call.path === 'commit')).toHaveLength(1)
})

test('dataChangedSincePreview', async () => {
  mockShell()
  cleanPreview(
    importReport({
      committed: false,
      sheets: [
        sheetReport({
          items: [
            rowReport({
              row: 2,
              status: 'ERROR',
              messages: [{ level: 'ERROR', column: 'Số giấy tờ', text: 'Đã có người dùng giấy tờ này' }],
            }),
          ],
        }),
      ],
    }),
  )
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })
  await choose(user)

  await user.click(await screen.findByRole('button', { name: 'Nhập 5 dòng' }))

  expect(await screen.findByText('Dữ liệu đã thay đổi từ lúc xem trước')).toBeInTheDocument()
  expect(screen.getByText('Đã có người dùng giấy tờ này')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Nhập 1 dòng' })).toBeDisabled()
  const steps = within(screen.getByRole('list', { name: 'Các bước' }))
  expect(steps.getByText('Xem trước').closest('li')).toHaveAttribute('aria-current', 'step')
})

test('unreadableFileAsksToChooseAgain', async () => {
  mockShell()
  const calls = cleanPreview()
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })
  const file = xlsx()
  await choose(user, file)
  await screen.findByRole('button', { name: 'Nhập 5 dòng' })
  Object.defineProperty(file, 'arrayBuffer', {
    value: () => Promise.reject(new DOMException('gone', 'NotReadableError')),
  })

  await user.click(screen.getByRole('button', { name: 'Nhập 5 dòng' }))

  expect(await screen.findByText('Không đọc lại được file; chọn lại file')).toBeInTheDocument()
  expect(calls.filter((call) => call.path === 'commit')).toHaveLength(0)
})

test('importAnotherFileStartsOver', async () => {
  mockShell()
  cleanPreview()
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })
  await choose(user)
  await user.click(await screen.findByRole('button', { name: 'Nhập 5 dòng' }))

  await user.click(await screen.findByRole('button', { name: 'Nhập file khác' }))

  expect(await screen.findByLabelText('Chọn file .xlsx')).toBeInTheDocument()
  expect(screen.queryByText(/^Đã nhập/)).not.toBeInTheDocument()
})

test('successRefreshesWorkspaceData', async () => {
  mockShell()
  cleanPreview()
  const requests = recordRequests()
  const { user } = renderApp({ route: '/w/7/import', userId: '42' })
  await choose(user)
  await screen.findByRole('button', { name: 'Nhập 5 dòng' })
  const treeLoads = () => requests.filter((request) => request === 'GET /api/registry/places').length
  const beforeImport = treeLoads()

  await user.click(screen.getByRole('button', { name: 'Nhập 5 dòng' }))

  await screen.findByText(/^Đã nhập/)
  await waitFor(() => expect(treeLoads()).toBeGreaterThan(beforeImport))
})
