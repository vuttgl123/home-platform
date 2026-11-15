import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'

/** Chỗ giữ cho một màn hình thuộc đợt xây sau. */
export function ComingSoonPage() {
  return (
    <Empty>
      <EmptyHeader>
        <EmptyTitle>Sắp có</EmptyTitle>
        <EmptyDescription>Phần này sẽ có ở đợt sau.</EmptyDescription>
      </EmptyHeader>
    </Empty>
  )
}
