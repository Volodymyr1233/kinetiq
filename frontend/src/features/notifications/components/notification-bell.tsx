import type { TFunction } from 'i18next'
import { BellIcon } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet'
import {
  useMarkNotificationRead,
  useNotifications,
  type Notification,
} from '../api/notifications'
import { targetOf, type Area } from '../notification-target'

const MAX_SHOWN = 99

// a notification about a client names them; one about nobody, or about a client who is gone, does not
function describe(t: TFunction, item: Notification) {
  return t(`notifications.kind.${item.kind}`, {
    name: item.clientName,
    context: item.clientName === undefined ? undefined : 'named',
  })
}

/**
 * The bell with the unread count, the side sheet with the latest notifications and the toast for one that arrives while
 * the app is open. Choosing a notification marks it read and goes to the screen it is about.
 */
export function NotificationBell({ area }: { area: Area }) {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const { data } = useNotifications()
  const markRead = useMarkNotificationRead()
  const [open, setOpen] = useState(false)
  const unread = data?.unread ?? 0
  // what the first answer held is not news: only a notification that arrives later gets a toast
  const known = useRef<Set<string> | undefined>(undefined)

  const choose = (item: Notification) => {
    if (!item.read) markRead.mutate(item.id)
    setOpen(false)
    const target = targetOf(area, item)
    if (target !== undefined) void navigate(target)
  }

  useEffect(() => {
    if (data === undefined) return
    const seen = known.current
    known.current = new Set(data.items.map((item) => item.id))
    if (seen === undefined) return
    data.items
      .filter((item) => !item.read && !seen.has(item.id))
      .forEach((item) => toast(describe(t, item)))
  }, [data, t])

  const when = new Intl.DateTimeFormat(i18n.language, {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button
          variant="ghost"
          size="icon-lg"
          className="relative size-12"
          aria-label={
            unread === 0
              ? t('notifications.bell')
              : t('notifications.bellUnread', { count: unread })
          }
        >
          <BellIcon aria-hidden="true" />
          {unread > 0 && (
            <span
              aria-hidden="true"
              className="bg-brand text-brand-foreground num absolute top-1.5 right-1 flex min-w-5 items-center justify-center rounded-full px-1 text-xs leading-5 font-semibold"
            >
              {unread > MAX_SHOWN ? `${MAX_SHOWN}+` : unread}
            </span>
          )}
        </Button>
      </SheetTrigger>
      <SheetContent
        side="right"
        className="gap-4 data-[side=right]:w-full data-[side=right]:sm:max-w-sm"
      >
        <SheetHeader>
          <SheetTitle>{t('notifications.title')}</SheetTitle>
        </SheetHeader>
        {data === undefined || data.items.length === 0 ? (
          <p className="text-muted-foreground px-4 text-sm">
            {t('notifications.empty')}
          </p>
        ) : (
          <ul className="flex min-h-0 flex-1 flex-col divide-y overflow-y-auto border-y">
            {data.items.map((item) => (
              <li key={item.id}>
                <button
                  type="button"
                  className="hover:bg-accent focus-visible:ring-ring/50 flex min-h-14 w-full items-start gap-3 px-4 py-3 text-left outline-none focus-visible:ring-3"
                  onClick={() => choose(item)}
                >
                  <span
                    aria-hidden="true"
                    className={
                      item.read
                        ? 'mt-2 size-2 shrink-0 rounded-full'
                        : 'bg-brand mt-2 size-2 shrink-0 rounded-full'
                    }
                  />
                  <span className="min-w-0 flex-1">
                    <span
                      className={
                        item.read
                          ? 'block text-sm break-words'
                          : 'block text-sm font-medium break-words'
                      }
                    >
                      {describe(t, item)}
                    </span>
                    <span className="text-muted-foreground block text-xs">
                      {!item.read && (
                        <span className="sr-only">
                          {t('notifications.unread')}{' '}
                        </span>
                      )}
                      {when.format(new Date(item.createdAt))}
                    </span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </SheetContent>
    </Sheet>
  )
}
