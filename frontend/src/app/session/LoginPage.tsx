import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { FormError } from '@/components/common/FormError'
import { TextField } from '@/components/common/TextField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useCheckUser, useCreateUser } from '@/features/users'

import { useSession } from './useSession'

const newUserSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, 'Nhập email')
    .max(255, 'Tối đa 255 ký tự')
    .regex(/^\S+@\S+\.\S+$/, 'Email không hợp lệ'),
  displayName: z.string().trim().min(1, 'Nhập tên hiển thị').max(150, 'Tối đa 150 ký tự'),
})

const existingUserSchema = z.object({
  userId: z.string().trim().regex(/^\d+$/, 'Id là một dãy chữ số'),
})

/**
 * Thay cho đăng nhập khi backend chưa có: tạo một người dùng mới, hoặc dùng lại một id đã có.
 * Trang chỉ đặt người dùng của phiên; đi đâu tiếp là việc của `StartPage`.
 */
export function LoginPage() {
  const { signIn: enter } = useSession()

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-3xl flex-col justify-center gap-8 p-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Home Platform</h1>
        <p className="text-muted-foreground">
          Hồ sơ toà nhà, tầng, phòng cùng người ở và đồ đạc. Chưa có đăng nhập: chọn một người dùng để bắt đầu.
        </p>
      </header>
      <div className="grid gap-4 md:grid-cols-2">
        <NewUserCard onEntered={enter} />
        <ExistingUserCard onEntered={enter} />
      </div>
    </main>
  )
}

function NewUserCard({ onEntered }: { onEntered: (userId: string) => void }) {
  const createUser = useCreateUser()
  const form = useForm({ resolver: zodResolver(newUserSchema), defaultValues: { email: '', displayName: '' } })
  const { errors } = form.formState

  // 409 duy nhất của API này là trùng email.
  const failure = createUser.error
    ? [createUser.error.status === 409 ? 'Email này đã có người dùng' : createUser.error.detail]
    : []

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <h2>Tạo người dùng</h2>
        </CardTitle>
        <CardDescription>Lần đầu dùng: nhập email và tên của bạn.</CardDescription>
      </CardHeader>
      <CardContent>
        <form
          className="flex flex-col gap-4"
          noValidate
          onSubmit={form.handleSubmit((values) =>
            createUser.mutate(values, { onSuccess: (user) => onEntered(user.id) }),
          )}
        >
          <TextField
            label="Email"
            type="email"
            autoComplete="email"
            error={errors.email?.message}
            {...form.register('email')}
          />
          <TextField
            label="Tên hiển thị"
            autoComplete="name"
            error={errors.displayName?.message}
            {...form.register('displayName')}
          />
          <FormError messages={failure} />
          <Button type="submit" disabled={createUser.isPending}>
            Tạo và vào
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

function ExistingUserCard({ onEntered }: { onEntered: (userId: string) => void }) {
  const checkUser = useCheckUser()
  const form = useForm({ resolver: zodResolver(existingUserSchema), defaultValues: { userId: '' } })

  const failure = checkUser.error
    ? [checkUser.error.isNotFound ? 'Không tìm thấy người dùng' : checkUser.error.detail]
    : []

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <h2>Tôi đã có id</h2>
        </CardTitle>
        <CardDescription>Đã tạo người dùng từ trước: nhập id của người dùng đó.</CardDescription>
      </CardHeader>
      <CardContent>
        <form
          className="flex flex-col gap-4"
          noValidate
          onSubmit={form.handleSubmit(({ userId }) =>
            checkUser.mutate(userId, { onSuccess: (user) => onEntered(user.id) }),
          )}
        >
          <TextField
            label="Id người dùng"
            inputMode="numeric"
            autoComplete="off"
            error={form.formState.errors.userId?.message}
            {...form.register('userId')}
          />
          <FormError messages={failure} />
          <Button type="submit" variant="outline" disabled={checkUser.isPending}>
            Vào
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
