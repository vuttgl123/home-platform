import { useCursorList, type CursorList } from '@/api'

import { listVacancies } from './api'
import type { Vacancy, VacancyFilter } from './types'

/** Khoá của danh sách chỗ trống; ghi cư trú hay địa điểm thì làm mới cả `all`. */
export const vacancyKeys = {
  all: (ws: string) => ['vacancies', ws] as const,
  list: (ws: string, filter: VacancyFilter) => ['vacancies', ws, filter] as const,
}

export function useVacancies(ws: string, filter: VacancyFilter): CursorList<Vacancy> {
  return useCursorList({ queryKey: vacancyKeys.list(ws, filter), fetchPage: (cursor) => listVacancies(ws, filter, cursor) })
}
