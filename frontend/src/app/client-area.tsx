import { useQuery } from '@tanstack/react-query'
import {
  CalendarIcon,
  ChartLineIcon,
  HouseIcon,
  MessageCircleIcon,
  SlidersHorizontalIcon,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { matchPath, NavLink, Outlet, useLocation } from 'react-router'
import { Button } from '@/components/ui/button'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet'
import { currentAccountQuery } from '@/features/account/api/account'
import { AccountAvatar } from '@/features/account/components/account-avatar'
import { SignOutButton } from '@/features/account/components/sign-out-button'
import { cn } from '@/lib/utils'

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

  return (
    <div className="mx-auto flex min-h-svh max-w-[640px] flex-col">
      <header className="bg-background sticky top-0 z-10 flex items-center justify-between gap-2 px-4 pt-4 pb-3">
        <h1 className="text-xl font-semibold">
          {active ? t(active.labelKey) : ''}
        </h1>
        <SettingsSheet />
      </header>
      {/* bottom padding keeps the last content above the fixed tab bar */}
      <main className="flex-1 pb-20">
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
                  'focus-visible:ring-ring/50 flex min-h-14 flex-col items-center justify-center gap-1 rounded-md text-xs outline-none focus-visible:ring-3',
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
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  )
}

// units, language and theme join the sheet with task #45
function SettingsSheet() {
  const { t } = useTranslation()
  const { data: account } = useQuery(currentAccountQuery)

  return (
    <Sheet>
      <SheetTrigger asChild>
        <Button
          variant="ghost"
          size="icon-lg"
          className="size-12"
          aria-label={t('nav.settings')}
        >
          <SlidersHorizontalIcon aria-hidden="true" />
        </Button>
      </SheetTrigger>
      <SheetContent side="right" className="w-full gap-6 sm:max-w-sm">
        <SheetHeader>
          <SheetTitle>{t('nav.settings')}</SheetTitle>
        </SheetHeader>
        <div className="flex flex-col gap-6 px-4 pb-6">
          {account && (
            <div className="flex min-w-0 items-center gap-3">
              <AccountAvatar name={account.name} size="lg" />
              <p className="truncate font-semibold">{account.name}</p>
            </div>
          )}
          <SignOutButton variant="outline" size="touch" className="w-full" />
        </div>
      </SheetContent>
    </Sheet>
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
