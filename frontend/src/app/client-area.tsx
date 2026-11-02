import {
  CalendarIcon,
  ChartLineIcon,
  HouseIcon,
  MessageCircleIcon,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { matchPath, NavLink, Outlet, useLocation } from 'react-router'
import { ConfirmEmailBanner } from '@/features/account/components/confirm-email-banner'
import { useUnreadCount } from '@/features/messaging/api/messaging'
import { UnreadCount } from '@/features/messaging/components/unread-count'
import { cn } from '@/lib/utils'
import { SettingsSheet } from './settings-sheet'

const TABS = [
  { path: 'today', labelKey: 'nav.today', Icon: HouseIcon },
  { path: 'plan', labelKey: 'nav.plan', Icon: CalendarIcon },
  { path: 'progress', labelKey: 'nav.progress', Icon: ChartLineIcon },
  { path: 'messages', labelKey: 'nav.messages', Icon: MessageCircleIcon },
] as const

export function ClientArea() {
  const { t } = useTranslation()
  const { pathname } = useLocation()
  const active = TABS.find((tab) =>
    matchPath({ path: `/app/${tab.path}`, end: false }, pathname),
  )

  const { data: unread } = useUnreadCount()
  // a conversation scrolls inside the screen, with its composer in reach above the tab bar
  const fillsScreen = active?.path === 'messages'

  return (
    <div
      className={cn(
        'mx-auto flex max-w-[640px] flex-col',
        fillsScreen ? 'h-dvh' : 'min-h-svh',
      )}
    >
      <header className="bg-background sticky top-0 z-10 flex items-center justify-between gap-2 px-4 pt-4 pb-3">
        <h1 className="text-xl font-semibold">
          {active ? t(active.labelKey) : ''}
        </h1>
        <SettingsSheet />
      </header>
      <ConfirmEmailBanner />
      {/* bottom padding keeps the last content above the fixed tab bar */}
      <main
        className={cn(
          'flex-1',
          fillsScreen
            ? 'flex min-h-0 flex-col pb-[calc(3.5rem+1px+env(safe-area-inset-bottom))]'
            : 'pb-20',
        )}
      >
        <Outlet />
      </main>
      <nav
        aria-label={t('areas.client')}
        className="bg-background fixed inset-x-0 bottom-0 border-t pb-[env(safe-area-inset-bottom)]"
      >
        <ul className="mx-auto grid max-w-[640px] grid-cols-4">
          {TABS.map(({ path, labelKey, Icon }) => (
            <li key={path}>
              <NavLink
                to={path}
                className={cn(
                  'focus-visible:ring-ring/50 relative flex min-h-14 flex-col items-center justify-center gap-1 rounded-md text-xs outline-none focus-visible:ring-3',
                  active?.path === path
                    ? 'text-foreground font-medium'
                    : 'text-muted-foreground',
                )}
              >
                <span
                  className={cn(
                    'rounded-full px-3 py-0.5',
                    active?.path === path && 'bg-brand-muted',
                  )}
                >
                  <Icon aria-hidden className="size-5" />
                </span>
                {t(labelKey)}
                {path === 'messages' && (
                  <UnreadCount
                    count={unread?.count ?? 0}
                    className="absolute top-1 left-[calc(50%+6px)]"
                  />
                )}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  )
}

// the four tabs show this until their features are built
export function ClientPlaceholder() {
  const { t } = useTranslation()

  return (
    <p className="text-muted-foreground px-4 text-sm">
      {t('placeholderPage.body')}
    </p>
  )
}
