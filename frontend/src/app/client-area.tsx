import {
  CalendarIcon,
  ChartLineIcon,
  HouseIcon,
  MessageCircleIcon,
} from 'lucide-react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { matchPath, Navigate, NavLink, Outlet, useLocation } from 'react-router'
import { ConfirmEmailBanner } from '@/features/account/components/confirm-email-banner'
import { useUnreadCount } from '@/features/messaging/api/messaging'
import { UnreadCount } from '@/features/messaging/components/unread-count'
import { useOwnPlan } from '@/features/plan/api/client-plan'
import { NotificationBell } from '@/features/notifications/components/notification-bell'
import { PlanReadyPage } from '@/features/plan/pages/plan-ready-page'
import { QuizStatusCheck } from '@/features/quiz/components/quiz-status-check'
import { cn } from '@/lib/utils'
import { SettingsSheet } from './settings-sheet'

const TABS = [
  { path: 'today', labelKey: 'nav.today', Icon: HouseIcon },
  { path: 'plan', labelKey: 'nav.plan', Icon: CalendarIcon },
  { path: 'progress', labelKey: 'nav.progress', Icon: ChartLineIcon },
  { path: 'messages', labelKey: 'nav.messages', Icon: MessageCircleIcon },
] as const

// a published plan the client has not seen comes first: every tab but the Plan tab, which marks it seen, sends them to
// the plan-ready screen. A client without a published plan, or a plan that cannot be read, gets the tab as it is.
function PlanReadyGate({ children }: { children: ReactNode }) {
  const { pathname } = useLocation()
  const query = useOwnPlan()

  // nothing while the first answer is on its way, so a tab does not flash before the redirect; a failing request
  // must not hold the client area back
  if (query.isPending && query.failureCount === 0) return null
  if (
    query.data?.seen === false &&
    !matchPath({ path: '/app/plan', end: false }, pathname)
  ) {
    return <Navigate replace to="/app/plan-ready" />
  }
  return children
}

// until a plan is published, the quiz or the waiting state stands in for every tab (spec 50, User Story 3)
export function ClientArea() {
  return (
    <QuizStatusCheck status="PLAN_PUBLISHED">
      <ClientTabs />
    </QuizStatusCheck>
  )
}

// inside the check, so the area's queries, such as the unread count, run only for a client let in
function ClientTabs() {
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
      <header
        data-slot="client-header"
        className="bg-background sticky top-0 z-10 flex items-center justify-between gap-2 px-4 pt-4 pb-3"
      >
        <h1 className="text-xl font-semibold">
          {active ? t(active.labelKey) : ''}
        </h1>
        <div className="flex items-center">
          <NotificationBell area="client" />
          <SettingsSheet />
        </div>
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
        <PlanReadyGate>
          <Outlet />
        </PlanReadyGate>
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

// the plan-ready screen has no tab bar, so it sits beside the client area and shows the Settings button itself
export function PlanReadyRoute() {
  return <PlanReadyPage settings={<SettingsSheet />} />
}
