import { registry, unwrap, workspaceHeaders, type CursorPage } from '@/api'

import type { Vacancy, VacancyFilter } from './types'

/** Địa điểm còn chỗ hôm nay, theo tên. */
export async function listVacancies(ws: string, filter: VacancyFilter, cursor?: string): Promise<CursorPage<Vacancy>> {
  const call = registry.GET('/places/vacancies', {
    params: {
      header: workspaceHeaders(ws),
      query: { placeTypeId: filter.placeTypeId ?? undefined, rootId: filter.rootId ?? undefined, cursor },
    },
  })
  const found = (await unwrap(call)) as CursorPage<Omit<Vacancy, 'id'>>
  return { ...found, items: found.items.map((vacancy) => ({ ...vacancy, id: vacancy.placeId })) }
}
