import { MailIcon, PhoneIcon } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'

import { PersonAvatar } from '@/components/common/PersonAvatar'
import { HoverCard, HoverCardContent, HoverCardTrigger } from '@/components/ui/hover-card'
import { Skeleton } from '@/components/ui/skeleton'

import { usePerson } from '../queries'

/**
 * Tên một người là liên kết tới hồ sơ của họ; rê chuột (hay đưa tiêu điểm) thì hiện số điện thoại,
 * email. Chỉ tải hồ sơ khi thẻ mở, để một danh sách dài không gọi API cho từng dòng.
 */
export function PersonHoverCard({ ws, personId, name }: { ws: string; personId: string; name: string }) {
  const [open, setOpen] = useState(false)
  const person = usePerson(ws, open ? personId : undefined)
  return (
    <HoverCard open={open} onOpenChange={setOpen}>
      <HoverCardTrigger delay={250} render={<Link to={`/w/${ws}/people/${personId}`} className="font-medium hover:underline" />}>
        {name}
      </HoverCardTrigger>
      <HoverCardContent align="start" className="w-72">
        <div className="flex items-center gap-3">
          <PersonAvatar name={name} size="lg" />
          <div className="flex min-w-0 flex-col gap-1">
            <span className="truncate font-semibold">{name}</span>
            {person.data ? (
              <>
                <span className="inline-flex items-center gap-1.5 text-muted-foreground">
                  <PhoneIcon className="size-3.5" aria-hidden="true" />
                  {person.data.phone ?? 'Chưa có số điện thoại'}
                </span>
                {person.data.email && (
                  <span className="inline-flex min-w-0 items-center gap-1.5 text-muted-foreground">
                    <MailIcon className="size-3.5 shrink-0" aria-hidden="true" />
                    <span className="truncate">{person.data.email}</span>
                  </span>
                )}
              </>
            ) : person.isError ? (
              <span className="text-muted-foreground">Không mở được hồ sơ</span>
            ) : (
              <Skeleton className="h-4 w-32" />
            )}
          </div>
        </div>
      </HoverCardContent>
    </HoverCard>
  )
}
