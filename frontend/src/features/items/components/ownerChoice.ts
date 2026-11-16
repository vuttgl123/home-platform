import type { PersonSummary } from '@/features/people'

/** Chủ đang chọn: món của địa điểm, hoặc của một người (`person` trống khi chưa chọn ai). */
export type OwnerChoice = { kind: 'place' } | { kind: 'person'; person: PersonSummary | null }

/** Người đã chọn làm chủ, `null` khi món là của địa điểm hoặc chưa chọn ai. */
export function ownerIdOf(choice: OwnerChoice): string | null {
  return choice.kind === 'person' ? (choice.person?.id ?? null) : null
}
