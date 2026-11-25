import type { Present, RegistrySchemas } from '@/api'

type S = RegistrySchemas

/** Một việc cần làm, tính ở backend khi đọc; `id` là `key` của việc, để danh sách tải dần không lặp dòng. */
export type Todo = Present<
  S['TodoResponse'],
  'key' | 'kind' | 'dueOn' | 'since' | 'isNew' | 'subjectType' | 'subjectId' | 'subjectName'
> & { id: string }

export type TodoKind = Todo['kind']

/** Số việc cho thanh bên và chuông; `byKind` có đủ mọi loại. */
export type TodoSummary = {
  total: number
  byKind: Record<TodoKind, number>
  newCount: number
  latest: Todo[]
}
