import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useCursorList, type ApiError, type CursorList } from '@/api'

import { getTodoSummary, listTodos, markTodosSeen } from './api'
import type { Todo, TodoKind, TodoSummary } from './types'

/** Tóm tắt tải lại mỗi 5 phút, để hạn mới tới trong ngày cũng hiện lên. */
const SUMMARY_REFRESH_MS = 300_000

export const todoKeys = {
  all: (ws: string) => ['todos', ws] as const,
  list: (ws: string, kind: TodoKind | null) => ['todos', ws, 'list', kind ?? 'all'] as const,
  summary: (ws: string) => ['todos', ws, 'summary'] as const,
}

/**
 * Danh sách việc. Việc gộp từ mọi loại bản ghi, nên mọi lệnh ghi thành công đều làm nó cũ
 * (`refreshAfterWrites`, như trang Tổng quan).
 */
export function useTodos(ws: string, kind: TodoKind | null): CursorList<Todo> {
  return useCursorList({
    queryKey: todoKeys.list(ws, kind),
    fetchPage: (cursor) => listTodos(ws, kind, cursor),
    meta: { refreshAfterWrites: true },
  })
}

/** Tóm tắt cho thanh bên và chuông; `data` là `null` khi registry chưa có việc cần làm. */
export function useTodoSummary(ws: string) {
  return useQuery<TodoSummary | null, ApiError>({
    queryKey: todoKeys.summary(ws),
    queryFn: () => getTodoSummary(ws),
    staleTime: 0,
    refetchInterval: SUMMARY_REFRESH_MS,
    refetchOnWindowFocus: true,
    retry: false,
    meta: { refreshAfterWrites: true },
  })
}

/** Đánh dấu đã xem; số việc mới trên chuông về 0 ngay, không chờ tải lại. */
export function useMarkTodosSeen(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<void, ApiError>({
    mutationFn: () => markTodosSeen(ws),
    onMutate: () => {
      queryClient.setQueryData<TodoSummary | null>(todoKeys.summary(ws), (summary) =>
        summary ? { ...summary, newCount: 0 } : summary,
      )
    },
  })
}
