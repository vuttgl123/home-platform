import { ApiError, registry, unwrap, workspaceHeaders, type CursorPage } from '@/api'

import type { Todo, TodoKind, TodoSummary } from './types'

type Row = Omit<Todo, 'id'>

const withId = (row: Row): Todo => ({ ...row, id: row.key })

/** Việc cần làm, gấp nhất trước; `kind` lọc một loại. */
export async function listTodos(ws: string, kind: TodoKind | null, cursor?: string): Promise<CursorPage<Todo>> {
  const call = registry.GET('/todos', { params: { header: workspaceHeaders(ws), query: { kind: kind ?? undefined, cursor } } })
  const page = (await unwrap(call)) as CursorPage<Row>
  return { items: page.items.map(withId), nextCursor: page.nextCursor }
}

/** Tóm tắt; `null` khi registry còn bản cũ chưa có việc cần làm (404), để thanh bên và chuông ẩn đi. */
export async function getTodoSummary(ws: string): Promise<TodoSummary | null> {
  try {
    const summary = (await unwrap(registry.GET('/todos/summary', { params: { header: workspaceHeaders(ws) } }))) as Omit<
      TodoSummary,
      'latest'
    > & { latest: Row[] }
    return { ...summary, latest: summary.latest.map(withId) }
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null
    throw error
  }
}

/** Người dùng vừa mở chuông: mọi việc đang có không còn là mới với họ. */
export async function markTodosSeen(ws: string): Promise<void> {
  await unwrap(registry.POST('/todos/seen', { params: { header: workspaceHeaders(ws) } }))
}
