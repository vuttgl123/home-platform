import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { FormError } from '@/components/common/FormError'
import { TextField } from '@/components/common/TextField'
import { Button } from '@/components/ui/button'

import { useCreateWorkspace } from '../queries'
import type { Workspace } from '../types'

const schema = z.object({
  name: z.string().trim().min(1, 'Nhập tên không gian').max(150, 'Tối đa 150 ký tự'),
})

/**
 * Form tạo một không gian.
 * @param onCreated gọi với không gian vừa tạo
 * @param onCancel có thì hiện nút "Huỷ"
 */
export function WorkspaceForm({
  onCreated,
  onCancel,
}: {
  onCreated: (workspace: Workspace) => void
  onCancel?: () => void
}) {
  const createWorkspace = useCreateWorkspace()
  const form = useForm({ resolver: zodResolver(schema), defaultValues: { name: '' } })

  return (
    <form
      className="flex flex-col gap-4"
      noValidate
      onSubmit={form.handleSubmit(({ name }) => createWorkspace.mutate({ name }, { onSuccess: onCreated }))}
    >
      <TextField
        label="Tên không gian"
        placeholder="Ví dụ: Nhà An, Chung cư Hoà Bình"
        autoComplete="off"
        error={form.formState.errors.name?.message}
        {...form.register('name')}
      />
      <FormError messages={createWorkspace.error ? [createWorkspace.error.detail] : []} />
      <div className="flex justify-end gap-2">
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel}>
            Huỷ
          </Button>
        )}
        <Button type="submit" disabled={createWorkspace.isPending}>
          {createWorkspace.isPending ? 'Đang chuẩn bị…' : 'Tạo không gian'}
        </Button>
      </div>
    </form>
  )
}
