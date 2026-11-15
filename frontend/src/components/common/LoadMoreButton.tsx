import { Button } from '@/components/ui/button'

/** Nút cuối một danh sách tải dần. */
export function LoadMoreButton({ onClick, isLoading }: { onClick: () => void; isLoading: boolean }) {
  return (
    <Button variant="ghost" size="sm" className="justify-start text-muted-foreground" disabled={isLoading} onClick={onClick}>
      {isLoading ? 'Đang tải…' : 'Tải thêm'}
    </Button>
  )
}
