import { useNavigate } from 'react-router'

import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'

import { WorkspaceForm } from './WorkspaceForm'

/** Hộp thoại tạo không gian; tạo xong thì chuyển sang không gian mới. */
export function CreateWorkspaceDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const navigate = useNavigate()
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Tạo không gian</DialogTitle>
          <DialogDescription>
            Mỗi không gian có cây địa điểm, người và đồ đạc riêng, cùng một bộ danh mục mặc định.
          </DialogDescription>
        </DialogHeader>
        <WorkspaceForm
          onCancel={() => onOpenChange(false)}
          onCreated={(workspace) => {
            onOpenChange(false)
            void navigate(`/w/${workspace.id}`)
          }}
        />
      </DialogContent>
    </Dialog>
  )
}
