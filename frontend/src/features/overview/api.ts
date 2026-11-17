import { registry, unwrap, workspaceHeaders } from '@/api'

import type { Overview } from './types'

/** Mọi con số của trang tổng quan trong một lần gọi. */
export async function getOverview(ws: string): Promise<Overview> {
  return (await unwrap(registry.GET('/overview', { params: { header: workspaceHeaders(ws) } }))) as Overview
}
